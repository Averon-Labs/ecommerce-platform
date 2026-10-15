<?php

namespace App\Http\Controllers;

use App\Exceptions\ApiException;
use App\Http\Requests\CheckoutQuoteRequest;
use App\Http\Requests\OrderCreateRequest;
use App\Http\Requests\OrderQueryRequest;
use App\Http\Requests\PaymentOutcomeRequest;
use App\Http\Resources\CartItemResource;
use App\Http\Resources\OrderPaymentResource;
use App\Http\Resources\OrderResource;
use App\Http\Resources\OrderSummaryResource;
use App\Models\Cart;
use App\Models\DemoPayment;
use App\Models\Inventory;
use App\Models\Order;
use App\Models\OrderItem;
use App\Support\CartPricing;
use App\Support\Idempotency;
use App\Support\Money;
use Illuminate\Database\QueryException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Collection;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;

/**
 * docs/openapi.yaml Checkout/Orders operations (ORD-001): a side-effect-free
 * quote, idempotent order creation from the current cart with immutable
 * snapshots, paged owned reads, and the explicitly simulated success/failure
 * payment. Stock is never reserved at creation — inventory changes only
 * inside the successful-payment transaction, which locks the rows and
 * re-checks availability so concurrent payments cannot oversell.
 */
class OrderController extends Controller
{
    /**
     * POST /checkout/quote: validate the address and price the current cart
     * without any side effect, so the review screen shows server-confirmed
     * money rather than client arithmetic.
     */
    public function quote(CheckoutQuoteRequest $request): JsonResponse
    {
        $data = $request->validated();
        $priced = $this->orderablePricing($request->user()->cart);
        $address = $this->normalizeAddress($data['shippingAddress']);
        $shippingCents = CartPricing::SAMPLE_SHIPPING_CENTS;

        return response()->json([
            'data' => [
                'items' => CartItemResource::collection($priced['lines']->pluck('item'))->resolve(),
                'subtotal' => Money::fromCents($priced['subtotalCents']),
                'shipping' => Money::fromCents($shippingCents),
                'total' => Money::fromCents($priced['subtotalCents'] + $shippingCents),
                'currency' => 'USD',
                'shippingAddress' => $address,
                'shippingAddressId' => $this->addressFingerprint($address),
            ],
        ]);
    }

    /**
     * POST /orders: convert the cart into immutable snapshots, empty the
     * cart, and store the response under the idempotency key. Everything —
     * including the key record — happens in one transaction, so a
     * conflicting concurrent reuse rolls the order back.
     */
    public function store(OrderCreateRequest $request): JsonResponse
    {
        $user = $request->user();
        $key = Idempotency::key($request);
        $hash = Idempotency::hash($request->json()->all());
        $endpoint = 'orders.create';

        if ($replay = Idempotency::replay($user->id, $endpoint, $key, $hash)) {
            return response()->json($replay['body'], $replay['code']);
        }

        $data = $request->validated();

        try {
            return DB::transaction(function () use ($user, $data, $key, $hash, $endpoint): JsonResponse {
                // Availability was gated by the quote; creation documents
                // only CART_EMPTY and INSUFFICIENT_STOCK (openapi.yaml).
                $priced = $this->orderablePricing($user->cart, false);
                $address = $this->normalizeAddress($data['shippingAddress']);
                $shippingCents = CartPricing::SAMPLE_SHIPPING_CENTS;

                $order = Order::create([
                    'user_id' => $user->id,
                    'status' => 'pending_payment',
                    'subtotal_amount' => Money::fromCents($priced['subtotalCents']),
                    'shipping_amount' => Money::fromCents($shippingCents),
                    'total_amount' => Money::fromCents($priced['subtotalCents'] + $shippingCents),
                    'shipping_address_snapshot' => $address,
                    'notes' => $data['notes'] ?? null,
                    'timeline' => [
                        ['status' => 'pending_payment', 'at' => $this->timelineStamp()],
                    ],
                ]);
                $order->reference = sprintf('ESV-%06d', $order->id);
                $order->save();

                foreach ($priced['lines'] as $line) {
                    OrderItem::create([
                        'order_id' => $order->id,
                        'product_id' => $line['item']->product_id,
                        'product_name' => $line['item']->product->name,
                        'quantity' => $line['item']->quantity,
                        'unit_price' => Money::fromCents($line['unitCents']),
                        'total_price' => Money::fromCents($line['lineCents']),
                    ]);
                }

                $user->cart?->items()->delete();

                $body = [
                    'data' => (new OrderResource($order->fresh(['items', 'latestPayment'])))->resolve(),
                ];
                Idempotency::remember($user->id, $endpoint, $key, $hash, 201, $body);

                return response()->json($body, 201);
            });
        } catch (QueryException $e) {
            // Concurrent first use of the same key: replay the winner's stored result.
            if ((string) $e->getCode() === '23000' && $replay = Idempotency::replay($user->id, $endpoint, $key, $hash)) {
                return response()->json($replay['body'], $replay['code']);
            }

            throw $e;
        }
    }

    /**
     * GET /orders: paged summaries of the caller's own orders, newest first.
     */
    public function index(OrderQueryRequest $request): JsonResponse
    {
        $data = $request->validated();
        $perPage = (int) ($data['perPage'] ?? 20);

        $paginator = Order::query()
            ->where('user_id', $request->user()->id)
            ->with(['items', 'latestPayment'])
            ->orderByDesc('created_at')
            ->orderByDesc('id')
            ->paginate($perPage);
        $totalItems = $paginator->total();

        return response()->json([
            'data' => OrderSummaryResource::collection($paginator->items()),
            'pagination' => [
                'currentPage' => $paginator->currentPage(),
                'perPage' => $paginator->perPage(),
                'totalItems' => $totalItems,
                'totalPages' => $totalItems === 0 ? 0 : (int) ceil($totalItems / $paginator->perPage()),
            ],
        ]);
    }

    /**
     * GET /orders/{orderId}: another customer's order answers 404, never
     * 403 — the server must not confirm resources the caller does not own.
     */
    public function show(Request $request, int $orderId): JsonResponse
    {
        $order = Order::query()
            ->whereKey($orderId)
            ->where('user_id', $request->user()->id)
            ->with(['items', 'latestPayment'])
            ->first();

        if ($order === null) {
            throw new ApiException(404, 'NOT_FOUND', 'The requested order was not found.');
        }

        return response()->json(['data' => new OrderResource($order)]);
    }

    /**
     * POST /orders/{orderId}/payment: record the explicitly simulated
     * outcome. Success locks and re-checks every stock row, decrements
     * inventory, and confirms the order in one transaction; failure records
     * a failed attempt and leaves the order in pending_payment for a retry
     * with a new key (OPEN-4). No card data and no real provider exist.
     */
    public function pay(PaymentOutcomeRequest $request, int $orderId): JsonResponse
    {
        $user = $request->user();
        $key = Idempotency::key($request);
        $hash = Idempotency::hash($request->json()->all());
        $endpoint = 'orders.payment:'.$orderId;

        if ($replay = Idempotency::replay($user->id, $endpoint, $key, $hash)) {
            return response()->json($replay['body'], $replay['code']);
        }

        $data = $request->validated();

        try {
            return DB::transaction(function () use ($user, $data, $key, $hash, $endpoint, $orderId): JsonResponse {
                $order = Order::query()
                    ->whereKey($orderId)
                    ->where('user_id', $user->id)
                    ->lockForUpdate()
                    ->first();

                if ($order === null) {
                    throw new ApiException(404, 'NOT_FOUND', 'The requested order was not found.');
                }

                if ($order->status !== 'pending_payment') {
                    throw new ApiException(
                        409,
                        'INVALID_ORDER_STATE',
                        'Only an order in pending_payment accepts a simulated payment.',
                    );
                }

                $succeeded = $data['outcome'] === 'success';

                if ($succeeded) {
                    // Lock every stock row in one statement so availability
                    // can be re-checked without overselling, and in a single
                    // plan-driven order: per-line locking reads would take
                    // the rows in cart-line order, letting two concurrent
                    // payments on differently ordered carts interleave their
                    // locks and deadlock.
                    $stock = Inventory::query()
                        ->whereIn('product_id', $order->items->pluck('product_id'))
                        ->orderBy('product_id')
                        ->lockForUpdate()
                        ->get()
                        ->keyBy('product_id');

                    foreach ($order->items as $item) {
                        $available = $stock->get($item->product_id)?->quantity ?? 0;

                        if ($available < $item->quantity) {
                            throw new ApiException(
                                409,
                                'INSUFFICIENT_STOCK',
                                'The stock changed before the payment could complete.',
                                [[
                                    'field' => 'productId',
                                    'message' => 'Only '.$available.' unit(s) of product #'.$item->product_id.' remain.',
                                ]],
                            );
                        }
                    }

                    foreach ($order->items as $item) {
                        $stock->get($item->product_id)?->decrement('quantity', $item->quantity);
                    }

                    $order->status = 'confirmed';
                    $order->timeline = [
                        ...$order->timeline,
                        ['status' => 'confirmed', 'at' => $this->timelineStamp()],
                    ];
                    $order->save();
                }

                $payment = DemoPayment::create([
                    'order_id' => $order->id,
                    'amount' => $order->total_amount,
                    'method' => 'demo',
                    'status' => $succeeded ? 'completed' : 'failed',
                    'transaction_id' => 'DEMO-'.strtoupper(Str::random(12)),
                    'created_at' => now(),
                ]);

                $body = [
                    'data' => [
                        'orderId' => $order->id,
                        'orderReference' => $order->reference,
                        'orderStatus' => $order->status,
                        'paymentStatus' => $payment->status,
                        'simulated' => true,
                        'payment' => (new OrderPaymentResource($payment))->resolve(),
                    ],
                ];
                Idempotency::remember($user->id, $endpoint, $key, $hash, 200, $body);

                return response()->json($body, 200);
            });
        } catch (QueryException $e) {
            // Concurrent first use of the same key: replay the winner's stored result.
            if ((string) $e->getCode() === '23000' && $replay = Idempotency::replay($user->id, $endpoint, $key, $hash)) {
                return response()->json($replay['body'], $replay['code']);
            }

            throw $e;
        }
    }

    /**
     * Shared cart validation for quote and order creation: a non-empty cart
     * whose lines all fit the current stock. The quote additionally gates
     * product availability because its documented 409 list includes
     * PRODUCT_UNAVAILABLE; POST /orders documents only CART_EMPTY,
     * INSUFFICIENT_STOCK, and IDEMPOTENCY_KEY_REUSED, so creation relies on
     * the preceding quote for availability. Pricing itself is the same
     * CartPricing used by GET /cart.
     *
     * @return array{lines: Collection, subtotalCents: int, itemCount: int, hasIssues: bool}
     */
    private function orderablePricing(?Cart $cart, bool $requireAvailable = true): array
    {
        $priced = CartPricing::quote($cart);

        if ($priced['lines']->isEmpty()) {
            throw new ApiException(409, 'CART_EMPTY', 'Checkout requires at least one cart line.');
        }

        foreach ($priced['lines'] as $line) {
            $product = $line['item']->product;

            if ($requireAvailable && ! $line['active']) {
                throw new ApiException(409, 'PRODUCT_UNAVAILABLE', 'A product in your cart is no longer available.', [
                    ['field' => 'productId', 'message' => 'Product #'.$product->id.' is not active.'],
                ]);
            }

            if ($line['item']->quantity > $line['available']) {
                throw new ApiException(409, 'INSUFFICIENT_STOCK', 'A product in your cart exceeds the available stock.', [
                    ['field' => 'productId', 'message' => 'Only '.$line['available'].' unit(s) of product #'.$product->id.' are available.'],
                ]);
            }
        }

        return $priced;
    }

    /**
     * Normalize the validated address to a stable key order so the echo and
     * its fingerprint are identical for identical input.
     *
     * @param  array<string, mixed>  $address
     * @return array<string, mixed>
     */
    private function normalizeAddress(array $address): array
    {
        return [
            'recipientName' => $address['recipientName'],
            'line1' => $address['line1'],
            'line2' => $address['line2'] ?? null,
            'city' => $address['city'],
            'region' => $address['region'],
            'postalCode' => $address['postalCode'],
            'country' => $address['country'],
        ];
    }

    /**
     * Opaque deterministic fingerprint of a validated address
     * (docs/openapi.yaml shippingAddressId).
     *
     * @param  array<string, mixed>  $address
     */
    private function addressFingerprint(array $address): string
    {
        return 'addr_'.substr(hash('sha256', json_encode($address, JSON_THROW_ON_ERROR)), 0, 12);
    }

    /** Timeline stamps in the contract's example format. */
    private function timelineStamp(): string
    {
        return now()->utc()->format('Y-m-d\TH:i:s\Z');
    }
}
