<?php

namespace App\Http\Controllers\Admin;

use App\Exceptions\ApiException;
use App\Http\Controllers\Controller;
use App\Http\Requests\AdminSalesReportRequest;
use App\Models\Order;
use App\Support\Money;
use Illuminate\Http\JsonResponse;
use Illuminate\Support\Carbon;

/**
 * docs/openapi.yaml Admin reports operation (RPT-001): server-computed
 * sales totals for an inclusive UTC date range. Revenue counts orders whose
 * latest simulated payment completed and that were not cancelled afterwards;
 * amounts and product names come from the immutable order snapshots, never
 * from the current catalog. No schema is added and nothing is written.
 */
class AdminSalesReportController extends Controller
{
    /** Defaults to the current day plus the preceding 29 days. */
    private const DEFAULT_RANGE_DAYS = 30;

    /** A report is a bounded read, so one request cannot scan an unbounded history. */
    private const MAX_RANGE_DAYS = 366;

    private const DEFAULT_TOP_LIMIT = 5;

    private const ORDER_STATUSES = [
        'pending_payment',
        'confirmed',
        'processing',
        'shipped',
        'delivered',
        'cancelled',
    ];

    /**
     * Sales summary, zero-filled daily series, top products, and order-status mix.
     */
    public function index(AdminSalesReportRequest $request): JsonResponse
    {
        $data = $request->validated();
        $to = Carbon::createFromFormat('Y-m-d', $data['to'] ?? now()->utc()->toDateString(), 'UTC')
            ->startOfDay();
        $from = isset($data['from'])
            ? Carbon::createFromFormat('Y-m-d', $data['from'], 'UTC')->startOfDay()
            : $to->copy()->subDays(self::DEFAULT_RANGE_DAYS - 1);

        if ($from->gt($to)) {
            throw new ApiException(
                422,
                'VALIDATION_ERROR',
                'One or more request fields are invalid.',
                [[
                    'field' => 'from',
                    'message' => 'The from date must not be after the to date.',
                ]],
            );
        }

        $rangeDays = (int) $from->diffInDays($to) + 1;

        if ($rangeDays > self::MAX_RANGE_DAYS) {
            throw new ApiException(
                422,
                'VALIDATION_ERROR',
                'One or more request fields are invalid.',
                [[
                    'field' => 'from',
                    'message' => 'The report range may cover at most '.self::MAX_RANGE_DAYS.' days.',
                ]],
            );
        }

        $orders = Order::query()
            ->with(['items', 'latestPayment'])
            ->whereBetween('created_at', [$from, $to->copy()->endOfDay()])
            ->get();

        $paidOrders = $orders
            ->filter(fn (Order $order): bool => $order->payment_status === 'completed' && $order->status !== 'cancelled')
            ->sortByDesc('created_at')
            ->values();

        $revenueCents = 0;
        $itemsSold = 0;
        $daily = [];

        for ($day = $from->copy(); $day->lte($to); $day->addDay()) {
            $daily[$day->toDateString()] = [
                'date' => $day->toDateString(),
                'revenueCents' => 0,
                'orders' => 0,
                'itemsSold' => 0,
            ];
        }

        $products = [];

        foreach ($paidOrders as $order) {
            $date = $order->created_at->utc()->toDateString();
            $orderCents = Money::toCents($order->total_amount);
            $lineItems = (int) $order->items->sum('quantity');

            $revenueCents += $orderCents;
            $itemsSold += $lineItems;
            $daily[$date]['revenueCents'] += $orderCents;
            $daily[$date]['orders'] += 1;
            $daily[$date]['itemsSold'] += $lineItems;

            foreach ($order->items as $item) {
                $productId = (int) $item->product_id;

                if (! isset($products[$productId])) {
                    $products[$productId] = [
                        'productId' => $productId,
                        // Paid orders are walked newest first, so a renamed
                        // product keeps its most recent snapshot name.
                        'name' => $item->product_name,
                        'quantity' => 0,
                        'revenueCents' => 0,
                    ];
                }

                $products[$productId]['quantity'] += (int) $item->quantity;
                $products[$productId]['revenueCents'] += Money::toCents($item->total_price);
            }
        }

        $topLimit = (int) ($data['topLimit'] ?? self::DEFAULT_TOP_LIMIT);
        $topProducts = array_values($products);

        usort($topProducts, function (array $left, array $right): int {
            return $right['revenueCents'] <=> $left['revenueCents']
                ?: $right['quantity'] <=> $left['quantity']
                ?: strcmp($left['name'], $right['name']);
        });

        $topProducts = array_map(fn (array $product): array => [
            'productId' => $product['productId'],
            'name' => $product['name'],
            'quantity' => $product['quantity'],
            'revenue' => Money::fromCents($product['revenueCents']),
        ], array_slice($topProducts, 0, $topLimit));

        $statusCounts = array_fill_keys(self::ORDER_STATUSES, 0);

        foreach ($orders as $order) {
            if (isset($statusCounts[$order->status])) {
                $statusCounts[$order->status] += 1;
            }
        }

        $ordersByStatus = array_map(
            fn (string $status): array => ['status' => $status, 'orders' => $statusCounts[$status]],
            self::ORDER_STATUSES,
        );

        $paidOrderCount = $paidOrders->count();

        return response()->json([
            'data' => [
                'range' => [
                    'from' => $from->toDateString(),
                    'to' => $to->toDateString(),
                    'timezone' => 'UTC',
                ],
                'currency' => 'USD',
                'summary' => [
                    'revenue' => Money::fromCents($revenueCents),
                    'totalOrders' => $orders->count(),
                    'paidOrders' => $paidOrderCount,
                    'itemsSold' => $itemsSold,
                    'averageOrderValue' => $paidOrderCount === 0
                        ? '0.00'
                        : Money::fromCents(intdiv($revenueCents + intdiv($paidOrderCount, 2), $paidOrderCount)),
                ],
                'daily' => array_map(fn (array $day): array => [
                    'date' => $day['date'],
                    'revenue' => Money::fromCents($day['revenueCents']),
                    'orders' => $day['orders'],
                    'itemsSold' => $day['itemsSold'],
                ], array_values($daily)),
                'topProducts' => $topProducts,
                'ordersByStatus' => $ordersByStatus,
            ],
        ]);
    }
}
