<?php

namespace App\Http\Controllers\Admin;

use App\Exceptions\ApiException;
use App\Http\Controllers\Controller;
use App\Http\Requests\AdminOrderQueryRequest;
use App\Http\Requests\AdminOrderStatusInputRequest;
use App\Http\Resources\AdminOrderResource;
use App\Http\Resources\AdminOrderSummaryResource;
use App\Models\Order;
use App\Support\OrderStateMachine;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Http\JsonResponse;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;

/**
 * docs/openapi.yaml Admin orders operations (ORD-001, unblocks the Admin
 * Roadmap's ORD-003): list with reference/customer-email/status/date
 * filters, read with the permitted transitions, and apply a permitted
 * status transition. `confirmed` is reachable only through a successful
 * simulated payment, never through this controller.
 */
class AdminOrderController extends Controller
{
    /**
     * Paged admin order rows, newest first.
     */
    public function index(AdminOrderQueryRequest $request): JsonResponse
    {
        $data = $request->validated();
        $perPage = (int) ($data['perPage'] ?? 20);

        $query = Order::query()->with(['user', 'items', 'latestPayment']);

        if (isset($data['q'])) {
            // MySQL's *_ci collations make LIKE case-insensitive; escape
            // the LIKE wildcards so user input stays literal.
            $needle = '%'.addcslashes($data['q'], '%_\\').'%';

            $query->where(function (Builder $builder) use ($needle): void {
                $builder->where('reference', 'like', $needle)
                    ->orWhereHas('user', fn (Builder $user) => $user->where('email', 'like', $needle));
            });
        }

        if (isset($data['status'])) {
            $query->where('status', $data['status']);
        }

        if (isset($data['from'])) {
            $query->where('created_at', '>=', Carbon::parse($data['from'])->startOfDay()->utc());
        }

        if (isset($data['to'])) {
            $query->where('created_at', '<=', Carbon::parse($data['to'])->endOfDay()->utc());
        }

        $query->orderByDesc('created_at')->orderByDesc('id');

        $paginator = $query->paginate($perPage);
        $totalItems = $paginator->total();

        return response()->json([
            'data' => AdminOrderSummaryResource::collection($paginator->items()),
            'pagination' => [
                'currentPage' => $paginator->currentPage(),
                'perPage' => $paginator->perPage(),
                'totalItems' => $totalItems,
                'totalPages' => $totalItems === 0 ? 0 : (int) ceil($totalItems / $paginator->perPage()),
            ],
        ]);
    }

    /**
     * Read one order with its snapshots, payment, and allowed transitions.
     */
    public function show(int $orderId): JsonResponse
    {
        return response()->json([
            'data' => new AdminOrderResource($this->findOrder($orderId)),
        ]);
    }

    /**
     * Apply a permitted transition from docs/openapi.yaml
     * x-order-state-machine; every applied transition joins the timeline.
     * The read-modify-write runs in one transaction on a locked row so a
     * concurrently committing payment or another admin transition can never
     * be overwritten by a stale status or timeline.
     */
    public function updateStatus(AdminOrderStatusInputRequest $request, int $orderId): JsonResponse
    {
        $target = $request->validated()['status'];

        $order = DB::transaction(function () use ($orderId, $target): Order {
            $order = Order::query()->whereKey($orderId)->lockForUpdate()->first();

            if ($order === null) {
                throw new ApiException(404, 'NOT_FOUND', 'The requested order was not found.');
            }

            if (! OrderStateMachine::canTransition($order->status, $target)) {
                throw new ApiException(
                    409,
                    'INVALID_TRANSITION',
                    'An order with status '.$order->status.' cannot become '.$target.'.',
                );
            }

            $order->status = $target;
            $order->timeline = [
                ...$order->timeline,
                ['status' => $target, 'at' => now()->utc()->format('Y-m-d\TH:i:s\Z')],
            ];
            $order->save();

            return $order;
        });

        return response()->json([
            'data' => new AdminOrderResource($order->fresh(['user', 'items', 'latestPayment'])),
        ]);
    }

    private function findOrder(int $orderId): Order
    {
        $order = Order::query()
            ->whereKey($orderId)
            ->with(['user', 'items', 'latestPayment'])
            ->first();

        if ($order === null) {
            throw new ApiException(404, 'NOT_FOUND', 'The requested order was not found.');
        }

        return $order;
    }
}
