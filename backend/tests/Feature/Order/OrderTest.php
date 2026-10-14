<?php

namespace Tests\Feature\Order;

use App\Models\DemoPayment;
use App\Models\Inventory;
use App\Models\Order;
use App\Models\Product;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Str;
use Illuminate\Testing\TestResponse;
use Tests\TestCase;

/**
 * ORD-001: checkout quote, idempotent order creation with immutable
 * snapshots, owned reads, and the explicitly simulated success/failure
 * payment against docs/openapi.yaml — including duplicate submission,
 * stale stock, rollback, totals, ownership, and both outcomes.
 */
class OrderTest extends TestCase
{
    use RefreshDatabase;

    /**
     * Create an active product with an inventory row.
     */
    private function makeProduct(array $productAttributes = [], array $inventoryAttributes = []): Product
    {
        $product = Product::factory()->create($productAttributes);

        Inventory::factory()->create(array_merge(['product_id' => $product->id], $inventoryAttributes));

        return $product;
    }

    /**
     * A contract-valid shipping address.
     *
     * @return array<string, mixed>
     */
    private function seedAddress(): array
    {
        return [
            'recipientName' => 'Sara Nouri',
            'line1' => '14 Maple Street',
            'city' => 'Seattle',
            'region' => 'WA',
            'postalCode' => '98101',
            'country' => 'US',
        ];
    }

    private function addLine(string $token, Product $product, int $quantity): void
    {
        $this->withToken($token)->postJson('/api/v1/cart/items', [
            'productId' => $product->id,
            'quantity' => $quantity,
        ])->assertOk();
    }

    /**
     * @param  array<string, mixed>  $body
     */
    private function placeOrder(string $token, array $body = [], ?string $key = null): TestResponse
    {
        return $this->withToken($token)
            ->withHeader('Idempotency-Key', $key ?? (string) Str::uuid())
            ->postJson('/api/v1/orders', $body ?: ['shippingAddress' => $this->seedAddress()]);
    }

    /**
     * @param  array<string, mixed>  $body
     */
    private function payOrder(string $token, int $orderId, string $outcome, ?string $key = null): TestResponse
    {
        return $this->withToken($token)
            ->withHeader('Idempotency-Key', $key ?? (string) Str::uuid())
            ->postJson('/api/v1/orders/'.$orderId.'/payment', ['outcome' => $outcome]);
    }

    public function test_checkout_and_order_endpoints_reject_guests(): void
    {
        $this->assertApiError(
            $this->postJson('/api/v1/checkout/quote', ['shippingAddress' => $this->seedAddress()]),
            401,
            'UNAUTHENTICATED',
        );
        $this->assertApiError(
            $this->postJson('/api/v1/orders', ['shippingAddress' => $this->seedAddress()]),
            401,
            'UNAUTHENTICATED',
        );
        $this->assertApiError($this->getJson('/api/v1/orders'), 401, 'UNAUTHENTICATED');
        $this->assertApiError($this->getJson('/api/v1/orders/1'), 401, 'UNAUTHENTICATED');
        $this->assertApiError(
            $this->postJson('/api/v1/orders/1/payment', ['outcome' => 'success']),
            401,
            'UNAUTHENTICATED',
        );
    }

    public function test_quote_returns_server_priced_lines_and_a_deterministic_address_fingerprint(): void
    {
        $product = $this->makeProduct(['name' => 'Aurora Desk Lamp', 'price' => '39.99'], ['quantity' => 12]);
        [, $token] = $this->registerCustomer();
        $this->addLine($token, $product, 2);

        $response = $this->withToken($token)->postJson('/api/v1/checkout/quote', [
            'shippingAddress' => $this->seedAddress(),
        ]);

        $response->assertOk()
            ->assertJsonStructure([
                'data' => [
                    'items' => [[
                        'productId', 'name', 'imageUrl', 'unitPrice', 'currency', 'quantity', 'lineTotal',
                        'inStock', 'availableQuantity', 'priceChanged', 'unavailable',
                    ]],
                    'subtotal', 'shipping', 'total', 'currency', 'shippingAddress', 'shippingAddressId',
                ],
            ])
            ->assertJsonPath('data.items.0.unitPrice', '39.99')
            ->assertJsonPath('data.items.0.lineTotal', '79.98')
            ->assertJsonPath('data.subtotal', '79.98')
            ->assertJsonPath('data.shipping', '5.00')
            ->assertJsonPath('data.total', '84.98')
            ->assertJsonPath('data.shippingAddress.recipientName', 'Sara Nouri')
            ->assertJsonPath('data.shippingAddress.line2', null);

        $fingerprint = $response->json('data.shippingAddressId');
        $this->assertMatchesRegularExpression('/^addr_[0-9a-f]{12}$/', $fingerprint);

        // The quote is side-effect free and deterministic: same cart, same
        // address -> same fingerprint, and no order exists yet.
        $again = $this->withToken($token)->postJson('/api/v1/checkout/quote', [
            'shippingAddress' => $this->seedAddress(),
        ]);
        $again->assertOk()->assertJsonPath('data.shippingAddressId', $fingerprint);
        $this->assertSame(0, Order::query()->count());
        // itemCount is the sum of quantities: one line of two units.
        $this->withToken($token)->getJson('/api/v1/cart')
            ->assertOk()
            ->assertJsonPath('data.itemCount', 2);
    }

    public function test_quote_with_an_empty_cart_returns_409_cart_empty(): void
    {
        [, $token] = $this->registerCustomer();

        $response = $this->withToken($token)->postJson('/api/v1/checkout/quote', [
            'shippingAddress' => $this->seedAddress(),
        ]);

        $this->assertApiError($response, 409, 'CART_EMPTY');
    }

    public function test_quote_rejects_unavailable_and_short_stock_lines(): void
    {
        $unavailable = $this->makeProduct(['price' => '10.00'], ['quantity' => 9]);
        $shortStock = $this->makeProduct(['price' => '20.00'], ['quantity' => 9]);
        [, $token] = $this->registerCustomer();
        $this->addLine($token, $unavailable, 1);
        $this->addLine($token, $shortStock, 5);

        $unavailable->update(['status' => 'archived']);

        $this->assertApiError(
            $this->withToken($token)->postJson('/api/v1/checkout/quote', ['shippingAddress' => $this->seedAddress()]),
            409,
            'PRODUCT_UNAVAILABLE',
        );

        $unavailable->update(['status' => 'active']);
        $shortStock->inventory->update(['quantity' => 2]);

        $this->assertApiError(
            $this->withToken($token)->postJson('/api/v1/checkout/quote', ['shippingAddress' => $this->seedAddress()]),
            409,
            'INSUFFICIENT_STOCK',
        );
    }

    public function test_quote_validates_the_shipping_address(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 5]);
        [, $token] = $this->registerCustomer();
        $this->addLine($token, $product, 1);

        $missingCity = $this->seedAddress();
        unset($missingCity['city']);
        $this->assertApiError(
            $this->withToken($token)->postJson('/api/v1/checkout/quote', ['shippingAddress' => $missingCity]),
            422,
            'VALIDATION_ERROR',
        );

        $lowercaseCountry = $this->seedAddress();
        $lowercaseCountry['country'] = 'usa';
        $this->assertApiError(
            $this->withToken($token)->postJson('/api/v1/checkout/quote', ['shippingAddress' => $lowercaseCountry]),
            422,
            'VALIDATION_ERROR',
        );
    }

    public function test_order_creation_snapshots_money_and_empties_the_cart(): void
    {
        $product = $this->makeProduct(['name' => 'Aurora Desk Lamp', 'price' => '39.99'], ['quantity' => 12]);
        [, $token] = $this->registerCustomer();
        $this->addLine($token, $product, 2);

        $response = $this->placeOrder($token, [
            'shippingAddress' => $this->seedAddress(),
            'notes' => 'Please leave at the front desk.',
        ]);

        $response->assertCreated()
            ->assertJsonStructure([
                'data' => [
                    'id', 'reference', 'status', 'paymentStatus',
                    'items' => [['productId', 'productName', 'quantity', 'unitPrice', 'lineTotal']],
                    'subtotal', 'shipping', 'total', 'currency', 'shippingAddress', 'notes',
                    'payment', 'timeline', 'createdAt', 'updatedAt',
                ],
            ])
            ->assertJsonPath('data.reference', 'ESV-'.str_pad((string) $response->json('data.id'), 6, '0', STR_PAD_LEFT))
            ->assertJsonPath('data.status', 'pending_payment')
            ->assertJsonPath('data.paymentStatus', 'pending')
            ->assertJsonPath('data.items.0.productName', 'Aurora Desk Lamp')
            ->assertJsonPath('data.items.0.unitPrice', '39.99')
            ->assertJsonPath('data.items.0.lineTotal', '79.98')
            ->assertJsonPath('data.subtotal', '79.98')
            ->assertJsonPath('data.shipping', '5.00')
            ->assertJsonPath('data.total', '84.98')
            ->assertJsonPath('data.notes', 'Please leave at the front desk.')
            ->assertJsonPath('data.payment', null)
            ->assertJsonPath('data.timeline.0.status', 'pending_payment');

        $this->assertMatchesRegularExpression('/^ESV-\d{6}$/', $response->json('data.reference'));
        // Order timestamps must also be RFC 3339 date-times.
        $this->assertMatchesRegularExpression('/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/', $response->json('data.createdAt'));

        // The cart is emptied exactly once by the conversion.
        $this->withToken($token)->getJson('/api/v1/cart')
            ->assertOk()
            ->assertJsonPath('data.itemCount', 0);

        // Snapshots are immutable: later catalog edits never rewrite the order.
        $product->update(['name' => 'Renamed Lamp', 'price' => '49.99']);

        $this->withToken($token)->getJson('/api/v1/orders/'.$response->json('data.id'))
            ->assertOk()
            ->assertJsonPath('data.items.0.productName', 'Aurora Desk Lamp')
            ->assertJsonPath('data.items.0.unitPrice', '39.99')
            ->assertJsonPath('data.subtotal', '79.98');
    }

    public function test_order_creation_ignores_client_money_fields(): void
    {
        $product = $this->makeProduct(['price' => '39.99'], ['quantity' => 9]);
        [, $token] = $this->registerCustomer();
        $this->addLine($token, $product, 1);

        $response = $this->placeOrder($token, [
            'shippingAddress' => $this->seedAddress(),
            'total' => '0.01',
        ]);

        $this->assertApiError($response, 422, 'VALIDATION_ERROR');
        $this->assertSame(0, Order::query()->count());
    }

    public function test_order_creation_with_an_empty_cart_returns_409(): void
    {
        [, $token] = $this->registerCustomer();

        $this->assertApiError($this->placeOrder($token), 409, 'CART_EMPTY');
        $this->assertSame(0, Order::query()->count());
    }

    public function test_order_creation_beyond_the_current_stock_returns_409(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 8]);
        [, $token] = $this->registerCustomer();
        $this->addLine($token, $product, 5);

        // Stock shrank after the line was added: the order must not be placed.
        $product->inventory->update(['quantity' => 2]);

        $response = $this->placeOrder($token);

        $this->assertApiError($response, 409, 'INSUFFICIENT_STOCK');
        $this->assertSame(0, Order::query()->count());
        // The cart survives the rejected checkout.
        $this->withToken($token)->getJson('/api/v1/cart')
            ->assertOk()
            ->assertJsonPath('data.itemCount', 5);
    }

    public function test_order_creation_gates_product_availability_through_the_quote(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 9]);
        [, $token] = $this->registerCustomer();
        $this->addLine($token, $product, 1);

        $product->update(['status' => 'archived']);

        // The quote is the documented availability gate for checkout
        // (openapi.yaml createCheckoutQuote 409: PRODUCT_UNAVAILABLE)...
        $this->assertApiError(
            $this->withToken($token)->postJson('/api/v1/checkout/quote', ['shippingAddress' => $this->seedAddress()]),
            409,
            'PRODUCT_UNAVAILABLE',
        );

        // ...while POST /orders documents only CART_EMPTY, INSUFFICIENT_STOCK,
        // and IDEMPOTENCY_KEY_REUSED for 409, so creation snapshots the line
        // instead of inventing an undocumented error code.
        $this->placeOrder($token)
            ->assertCreated()
            ->assertJsonPath('data.status', 'pending_payment')
            ->assertJsonPath('data.items.0.productId', $product->id);
    }

    public function test_order_creation_validates_notes_length(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 5]);
        [, $token] = $this->registerCustomer();
        $this->addLine($token, $product, 1);

        $response = $this->placeOrder($token, [
            'shippingAddress' => $this->seedAddress(),
            'notes' => str_repeat('n', 501),
        ]);

        $this->assertApiError($response, 422, 'VALIDATION_ERROR');
    }

    public function test_order_creation_replays_the_same_idempotency_key(): void
    {
        $product = $this->makeProduct(['price' => '39.99'], ['quantity' => 12]);
        [, $token] = $this->registerCustomer();
        $this->addLine($token, $product, 2);
        $key = (string) Str::uuid();

        $first = $this->placeOrder($token, [], $key);
        $first->assertCreated();

        // The duplicate submission replays the stored 201 and creates no
        // second order, even though the cart is now empty.
        $second = $this->placeOrder($token, [], $key);
        $second->assertCreated()
            ->assertJsonPath('data.id', $first->json('data.id'))
            ->assertJsonPath('data.reference', $first->json('data.reference'));

        $this->assertSame(1, Order::query()->count());
    }

    public function test_order_creation_rejects_a_key_reused_with_a_different_body(): void
    {
        $product = $this->makeProduct(['price' => '39.99'], ['quantity' => 12]);
        [, $tokenA] = $this->registerCustomer(['email' => 'a@example.com']);
        [, $tokenB] = $this->registerCustomer(['email' => 'b@example.com']);
        $this->addLine($tokenA, $product, 1);
        $key = (string) Str::uuid();

        $this->placeOrder($tokenA, [], $key)->assertCreated();

        // Same key, different body: the stored result must not be replayed.
        $conflict = $this->placeOrder($tokenA, [
            'shippingAddress' => $this->seedAddress(),
            'notes' => 'different body',
        ], $key);
        $this->assertApiError($conflict, 409, 'IDEMPOTENCY_KEY_REUSED');
        $this->assertSame(1, Order::query()->count());

        // Keys are scoped per customer: another customer may reuse the same
        // key value for their own order.
        $this->addLine($tokenB, $product, 1);
        $this->placeOrder($tokenB, [], $key)->assertCreated();
        $this->assertSame(2, Order::query()->count());
    }

    public function test_order_creation_requires_a_uuid_idempotency_key(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 5]);
        [, $token] = $this->registerCustomer();
        $this->addLine($token, $product, 1);

        $missing = $this->withToken($token)
            ->postJson('/api/v1/orders', ['shippingAddress' => $this->seedAddress()]);
        $this->assertApiError($missing, 400, 'MISSING_IDEMPOTENCY_KEY');

        $invalid = $this->withToken($token)
            ->withHeader('Idempotency-Key', 'not-a-uuid')
            ->postJson('/api/v1/orders', ['shippingAddress' => $this->seedAddress()]);
        $this->assertApiError($invalid, 400, 'MISSING_IDEMPOTENCY_KEY');

        $this->assertSame(0, Order::query()->count());
    }

    public function test_orders_list_is_owned_newest_first_and_paginated(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 50]);
        [, $tokenA] = $this->registerCustomer(['email' => 'a@example.com']);
        [, $tokenB] = $this->registerCustomer(['email' => 'b@example.com']);

        $this->addLine($tokenA, $product, 1);
        $this->placeOrder($tokenA)->assertCreated();
        $this->addLine($tokenB, $product, 1);
        $this->placeOrder($tokenB)->assertCreated();
        $this->addLine($tokenA, $product, 2);
        $this->placeOrder($tokenA)->assertCreated();

        $response = $this->withToken($tokenA)->getJson('/api/v1/orders');

        $response->assertOk()
            ->assertJsonStructure([
                'data' => [['id', 'reference', 'status', 'paymentStatus', 'total', 'currency', 'itemCount', 'createdAt']],
                'pagination' => ['currentPage', 'perPage', 'totalItems', 'totalPages'],
            ])
            ->assertJsonPath('data.0.itemCount', 2)
            ->assertJsonPath('data.1.itemCount', 1)
            ->assertJsonPath('pagination.totalItems', 2)
            ->assertJsonPath('pagination.totalPages', 1);

        // Only the caller's orders appear; page size is honored.
        $this->withToken($tokenA)->getJson('/api/v1/orders?perPage=1')
            ->assertOk()
            ->assertJsonPath('pagination.totalItems', 2)
            ->assertJsonPath('pagination.totalPages', 2)
            ->assertJsonPath('pagination.perPage', 1);

        $this->withToken($tokenB)->getJson('/api/v1/orders')
            ->assertOk()
            ->assertJsonPath('pagination.totalItems', 1);
    }

    public function test_order_detail_answers_404_for_another_customers_order(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 5]);
        [, $tokenA] = $this->registerCustomer(['email' => 'a@example.com']);
        [, $tokenB] = $this->registerCustomer(['email' => 'b@example.com']);
        $this->addLine($tokenA, $product, 1);
        $orderId = $this->placeOrder($tokenA)->assertCreated()->json('data.id');

        $this->withToken($tokenA)->getJson('/api/v1/orders/'.$orderId)->assertOk();

        // Never 403: the server must not confirm the resource exists.
        $this->assertApiError(
            $this->withToken($tokenB)->getJson('/api/v1/orders/'.$orderId),
            404,
            'NOT_FOUND',
        );
    }

    public function test_payment_success_confirms_the_order_and_decrements_stock_transactionally(): void
    {
        $product = $this->makeProduct(['price' => '39.99'], ['quantity' => 12]);
        [, $token] = $this->registerCustomer();
        $this->addLine($token, $product, 2);
        $orderId = $this->placeOrder($token)->assertCreated()->json('data.id');

        $response = $this->payOrder($token, $orderId, 'success');

        $response->assertOk()
            ->assertJsonStructure([
                'data' => [
                    'orderId', 'orderReference', 'orderStatus', 'paymentStatus', 'simulated',
                    'payment' => ['method', 'status', 'amount', 'currency', 'transactionId', 'createdAt'],
                ],
            ])
            ->assertJsonPath('data.orderId', $orderId)
            ->assertJsonPath('data.orderStatus', 'confirmed')
            ->assertJsonPath('data.paymentStatus', 'completed')
            ->assertJsonPath('data.simulated', true)
            ->assertJsonPath('data.payment.method', 'demo')
            ->assertJsonPath('data.payment.status', 'completed')
            ->assertJsonPath('data.payment.amount', '84.98');

        $this->assertMatchesRegularExpression('/^DEMO-[A-Z0-9]{12}$/', $response->json('data.payment.transactionId'));
        // The contract requires RFC 3339 date-times for payment timestamps.
        $this->assertMatchesRegularExpression(
            '/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/',
            $response->json('data.payment.createdAt'),
        );

        // Stock changed exactly by the ordered quantity.
        $this->assertSame(10, $product->fresh('inventory')->inventory->quantity);

        // The order detail shows the payment and the recorded transition.
        $this->withToken($token)->getJson('/api/v1/orders/'.$orderId)
            ->assertOk()
            ->assertJsonPath('data.status', 'confirmed')
            ->assertJsonPath('data.paymentStatus', 'completed')
            ->assertJsonPath('data.payment.transactionId', $response->json('data.payment.transactionId'))
            ->assertJsonPath('data.timeline.0.status', 'pending_payment')
            ->assertJsonPath('data.timeline.1.status', 'confirmed');
    }

    public function test_payment_failure_keeps_the_order_pending_and_stock_unchanged(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 8]);
        [, $token] = $this->registerCustomer();
        $this->addLine($token, $product, 3);
        $orderId = $this->placeOrder($token)->assertCreated()->json('data.id');

        $this->payOrder($token, $orderId, 'failure')
            ->assertOk()
            ->assertJsonPath('data.orderStatus', 'pending_payment')
            ->assertJsonPath('data.paymentStatus', 'failed')
            ->assertJsonPath('data.simulated', true)
            ->assertJsonPath('data.payment.status', 'failed');

        $this->assertSame(8, $product->fresh('inventory')->inventory->quantity);

        // A failed payment is never shown as paid.
        $this->withToken($token)->getJson('/api/v1/orders/'.$orderId)
            ->assertOk()
            ->assertJsonPath('data.status', 'pending_payment')
            ->assertJsonPath('data.paymentStatus', 'failed')
            ->assertJsonPath('data.timeline.0.status', 'pending_payment');
    }

    public function test_a_failed_payment_can_be_retried_with_a_new_key(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 8]);
        [, $token] = $this->registerCustomer();
        $this->addLine($token, $product, 3);
        $orderId = $this->placeOrder($token)->assertCreated()->json('data.id');

        $this->payOrder($token, $orderId, 'failure')->assertOk();

        // OPEN-4: retry with a NEW key succeeds and confirms the order.
        $this->payOrder($token, $orderId, 'success')
            ->assertOk()
            ->assertJsonPath('data.orderStatus', 'confirmed')
            ->assertJsonPath('data.paymentStatus', 'completed');

        $this->assertSame(5, $product->fresh('inventory')->inventory->quantity);
        $this->assertSame(2, DemoPayment::query()->where('order_id', $orderId)->count());
    }

    public function test_payment_rejects_an_order_that_is_not_pending(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 8]);
        [, $token] = $this->registerCustomer();
        $this->addLine($token, $product, 1);
        $orderId = $this->placeOrder($token)->assertCreated()->json('data.id');

        $this->payOrder($token, $orderId, 'success')->assertOk();

        $again = $this->payOrder($token, $orderId, 'success');
        $this->assertApiError($again, 409, 'INVALID_ORDER_STATE');

        // Only one successful payment and one decrement happened.
        $this->assertSame(7, $product->fresh('inventory')->inventory->quantity);
        $this->assertSame(1, DemoPayment::query()->where('order_id', $orderId)->count());
    }

    public function test_payment_replays_the_same_idempotency_key_without_double_decrement(): void
    {
        $product = $this->makeProduct(['price' => '39.99'], ['quantity' => 12]);
        [, $token] = $this->registerCustomer();
        $this->addLine($token, $product, 2);
        $orderId = $this->placeOrder($token)->assertCreated()->json('data.id');
        $key = (string) Str::uuid();

        $first = $this->payOrder($token, $orderId, 'success', $key);
        $first->assertOk();

        $second = $this->payOrder($token, $orderId, 'success', $key);
        $second->assertOk()
            ->assertJsonPath('data.payment.transactionId', $first->json('data.payment.transactionId'));

        // The duplicate submission decremented stock exactly once.
        $this->assertSame(10, $product->fresh('inventory')->inventory->quantity);
        $this->assertSame(1, DemoPayment::query()->where('order_id', $orderId)->count());
        $timeline = $this->withToken($token)->getJson('/api/v1/orders/'.$orderId)->json('data.timeline');
        $this->assertCount(2, $timeline);
    }

    public function test_payment_requires_a_uuid_idempotency_key(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 8]);
        [, $token] = $this->registerCustomer();
        $this->addLine($token, $product, 1);
        $orderId = $this->placeOrder($token)->assertCreated()->json('data.id');

        // A present-but-empty header is "not a UUID" and fails the same way;
        // the truly-absent case is covered by the order-creation key test
        // (default headers persist across calls within one test).
        $missing = $this->withToken($token)
            ->withHeader('Idempotency-Key', '')
            ->postJson('/api/v1/orders/'.$orderId.'/payment', ['outcome' => 'success']);
        $this->assertApiError($missing, 400, 'MISSING_IDEMPOTENCY_KEY');

        $invalid = $this->withToken($token)
            ->withHeader('Idempotency-Key', 'nope')
            ->postJson('/api/v1/orders/'.$orderId.'/payment', ['outcome' => 'success']);
        $this->assertApiError($invalid, 400, 'MISSING_IDEMPOTENCY_KEY');

        // Nothing changed: the order is still pending and stock untouched.
        $this->withToken($token)->getJson('/api/v1/orders/'.$orderId)
            ->assertJsonPath('data.status', 'pending_payment');
        $this->assertSame(8, $product->fresh('inventory')->inventory->quantity);
    }

    public function test_payment_success_with_stale_stock_is_rejected_and_rolls_back(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 12]);
        [, $token] = $this->registerCustomer();
        $this->addLine($token, $product, 5);
        $orderId = $this->placeOrder($token)->assertCreated()->json('data.id');

        // Stock vanished between order creation and payment.
        $product->inventory->update(['quantity' => 2]);

        $stale = $this->payOrder($token, $orderId, 'success');
        $this->assertApiError($stale, 409, 'INSUFFICIENT_STOCK');

        // Full rollback: order untouched, no payment row, stock unchanged.
        $this->withToken($token)->getJson('/api/v1/orders/'.$orderId)
            ->assertOk()
            ->assertJsonPath('data.status', 'pending_payment')
            ->assertJsonPath('data.paymentStatus', 'pending');
        $this->assertSame(0, DemoPayment::query()->where('order_id', $orderId)->count());
        $this->assertSame(2, $product->fresh('inventory')->inventory->quantity);

        // After restocking, a fresh key succeeds (the rejected key recorded nothing).
        $product->inventory->update(['quantity' => 12]);
        $this->payOrder($token, $orderId, 'success')
            ->assertOk()
            ->assertJsonPath('data.orderStatus', 'confirmed');
        $this->assertSame(7, $product->fresh('inventory')->inventory->quantity);
    }

    public function test_payment_on_another_customers_order_returns_404(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 8]);
        [, $tokenA] = $this->registerCustomer(['email' => 'a@example.com']);
        [, $tokenB] = $this->registerCustomer(['email' => 'b@example.com']);
        $this->addLine($tokenA, $product, 1);
        $orderId = $this->placeOrder($tokenA)->assertCreated()->json('data.id');

        $this->assertApiError(
            $this->payOrder($tokenB, $orderId, 'success'),
            404,
            'NOT_FOUND',
        );
        $this->withToken($tokenA)->getJson('/api/v1/orders/'.$orderId)
            ->assertJsonPath('data.status', 'pending_payment');
    }

    public function test_payment_accepts_no_card_data(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 8]);
        [, $token] = $this->registerCustomer();
        $this->addLine($token, $product, 1);
        $orderId = $this->placeOrder($token)->assertCreated()->json('data.id');

        // Any field beyond the outcome flag is rejected outright.
        $withCard = $this->withToken($token)
            ->withHeader('Idempotency-Key', (string) Str::uuid())
            ->postJson('/api/v1/orders/'.$orderId.'/payment', [
                'outcome' => 'success',
                'cardNumber' => '4111111111111111',
            ]);
        $this->assertApiError($withCard, 422, 'VALIDATION_ERROR');

        $badOutcome = $this->withToken($token)
            ->withHeader('Idempotency-Key', (string) Str::uuid())
            ->postJson('/api/v1/orders/'.$orderId.'/payment', ['outcome' => 'maybe']);
        $this->assertApiError($badOutcome, 422, 'VALIDATION_ERROR');

        $this->assertSame(0, DemoPayment::query()->count());
    }
}
