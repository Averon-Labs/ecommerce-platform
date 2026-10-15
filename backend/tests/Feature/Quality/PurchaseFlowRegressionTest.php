<?php

namespace Tests\Feature\Quality;

use App\Models\DemoPayment;
use App\Models\Inventory;
use App\Models\Order;
use App\Models\Product;
use Database\Seeders\DatabaseSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Str;
use Illuminate\Testing\TestResponse;
use Tests\TestCase;

/**
 * QUAL-001 (backend contribution): the deterministic API/database purchase
 * flow a portfolio reviewer exercises from the seeded demo store. It walks
 * the documented customer journey end to end — sign in, browse, add and
 * update a cart line, quote, create an order, complete the simulated
 * success, and read the resulting order — then covers the documented
 * boundary cases (simulated failure, duplicate submission, and an
 * object-level authorization boundary). Every assertion runs against the
 * real seeded catalog, uses isolated disposable data, and can never reach
 * a real payment provider (payment is a DemoPayment row).
 */
class PurchaseFlowRegressionTest extends TestCase
{
    use RefreshDatabase;

    /** Deterministic seeded product (see DatabaseSeeder) used as the happy path. */
    private const HEADPHONES_SKU = 'AH-1001';

    /** A second seeded product used for the simulated-failure path. */
    private const EARBUDS_SKU = 'AH-1002';

    /**
     * @return array<string, mixed>
     */
    private function address(): array
    {
        return [
            'recipientName' => 'Demo Reviewer',
            'line1' => '1 Portfolio Lane',
            'city' => 'Seattle',
            'region' => 'WA',
            'postalCode' => '98101',
            'country' => 'US',
        ];
    }

    private function productBySku(string $sku): Product
    {
        return Product::query()->where('sku', $sku)->firstOrFail();
    }

    private function stockOf(int $productId): int
    {
        return (int) Inventory::query()->where('product_id', $productId)->value('quantity');
    }

    /**
     * Sign in as the seeded demo customer and return the bearer token.
     */
    private function signInDemoCustomer(): string
    {
        $response = $this->postJson('/api/v1/auth/login', [
            'email' => 'demo@example.com',
            'password' => 'Password123',
        ]);

        $response->assertOk();

        return (string) $response->json('data.token');
    }

    /**
     * @param  array<string, mixed>  $body
     */
    private function placeOrder(string $token, array $body, string $key): TestResponse
    {
        return $this->withToken($token)
            ->withHeader('Idempotency-Key', $key)
            ->postJson('/api/v1/orders', $body);
    }

    public function test_seeded_customer_browses_carts_quotes_orders_and_completes_simulated_success(): void
    {
        $this->seed(DatabaseSeeder::class);

        $token = $this->signInDemoCustomer();

        // Browse the seeded catalog over the public read API (active products only).
        $catalog = $this->getJson('/api/v1/products?perPage=100');
        $catalog->assertOk()->assertJsonPath('pagination.totalItems', 10);

        // Add a seeded, in-stock product to the cart.
        $headphones = $this->productBySku(self::HEADPHONES_SKU);
        $stockBefore = $this->stockOf($headphones->id);

        $this->withToken($token)->postJson('/api/v1/cart/items', [
            'productId' => $headphones->id,
            'quantity' => 2,
        ])->assertOk()->assertJsonPath('data.itemCount', 2);

        // Update the line quantity.
        $this->withToken($token)->patchJson('/api/v1/cart/items/'.$headphones->id, ['quantity' => 2])
            ->assertOk()
            ->assertJsonPath('data.items.0.quantity', 2);

        // Quote the checkout: server-priced, deterministic address fingerprint.
        $quote = $this->withToken($token)->postJson('/api/v1/checkout/quote', [
            'shippingAddress' => $this->address(),
        ]);
        $quote->assertOk()
            ->assertJsonPath('data.subtotal', '298.00')
            ->assertJsonPath('data.shipping', '5.00')
            ->assertJsonPath('data.total', '303.00')
            ->assertJsonPath('data.currency', 'USD');
        $this->assertMatchesRegularExpression('/^addr_[0-9a-f]{12}$/', (string) $quote->json('data.shippingAddressId'));

        // Create the order.
        $key = (string) Str::uuid();
        $created = $this->placeOrder($token, ['shippingAddress' => $this->address()], $key);
        $created->assertCreated()
            ->assertJsonPath('data.status', 'pending_payment')
            ->assertJsonPath('data.paymentStatus', 'pending')
            ->assertJsonPath('data.total', '303.00')
            ->assertJsonPath('data.reference', 'ESV-'.sprintf('%06d', (int) $created->json('data.id')));
        $orderId = (int) $created->json('data.id');

        // The cart is emptied once the order is created.
        $this->withToken($token)->getJson('/api/v1/cart')
            ->assertOk()
            ->assertJsonPath('data.itemCount', 0);

        // Complete the simulated success.
        $this->withToken($token)
            ->withHeader('Idempotency-Key', (string) Str::uuid())
            ->postJson('/api/v1/orders/'.$orderId.'/payment', ['outcome' => 'success'])
            ->assertOk()
            ->assertJsonPath('data.orderStatus', 'confirmed')
            ->assertJsonPath('data.paymentStatus', 'completed')
            ->assertJsonPath('data.simulated', true)
            ->assertJsonPath('data.payment.transactionId', fn ($v) => is_string($v) && str_starts_with($v, 'DEMO-'));

        // Read the resulting order.
        $this->withToken($token)->getJson('/api/v1/orders/'.$orderId)
            ->assertOk()
            ->assertJsonPath('data.status', 'confirmed')
            ->assertJsonPath('data.paymentStatus', 'completed');

        // Database side effects: inventory decremented, payment completed,
        // timeline recorded — all in one consistent state.
        $this->assertSame($stockBefore - 2, $this->stockOf($headphones->id));
        $this->assertSame('confirmed', (string) Order::query()->whereKey($orderId)->value('status'));

        $timeline = Order::query()->whereKey($orderId)->first()->timeline;
        $this->assertSame(['pending_payment', 'confirmed'], array_column($timeline, 'status'));

        $this->assertSame(
            'completed',
            (string) DemoPayment::query()->where('order_id', $orderId)->latest('id')->value('status'),
        );
    }

    public function test_duplicate_submission_replays_the_first_result_and_conflicting_reuse_is_rejected(): void
    {
        $this->seed(DatabaseSeeder::class);
        $token = $this->signInDemoCustomer();

        $product = $this->productBySku(self::EARBUDS_SKU);
        $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $product->id, 'quantity' => 1])
            ->assertOk();

        $key = (string) Str::uuid();
        $body = ['shippingAddress' => $this->address()];

        $first = $this->placeOrder($token, $body, $key);
        $first->assertCreated();

        // Same key + same body replays the stored result; no second order.
        $replay = $this->placeOrder($token, $body, $key);
        $replay->assertCreated()->assertJsonPath('data.id', $first->json('data.id'));
        $this->assertSame(1, Order::query()->count());

        // Same key + different body is a documented conflict.
        $conflicting = $this->placeOrder($token, ['shippingAddress' => $this->address(), 'notes' => 'different'], $key);
        $this->assertApiError($conflicting, 409, 'IDEMPOTENCY_KEY_REUSED');
        $this->assertSame(1, Order::query()->count());
    }

    public function test_simulated_failure_keeps_the_order_retryable_and_leaves_stock_unchanged(): void
    {
        $this->seed(DatabaseSeeder::class);
        $token = $this->signInDemoCustomer();

        $product = $this->productBySku(self::EARBUDS_SKU);
        $stockBefore = $this->stockOf($product->id);

        $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $product->id, 'quantity' => 1])
            ->assertOk();

        $orderId = (int) $this->placeOrder($token, ['shippingAddress' => $this->address()], (string) Str::uuid())
            ->assertCreated()
            ->json('data.id');

        $this->withToken($token)
            ->withHeader('Idempotency-Key', (string) Str::uuid())
            ->postJson('/api/v1/orders/'.$orderId.'/payment', ['outcome' => 'failure'])
            ->assertOk()
            ->assertJsonPath('data.orderStatus', 'pending_payment')
            ->assertJsonPath('data.paymentStatus', 'failed');

        // Stock untouched, order still retryable, failure never shown as paid.
        $this->assertSame($stockBefore, $this->stockOf($product->id));
        $this->assertSame('pending_payment', (string) Order::query()->whereKey($orderId)->value('status'));
        $this->assertSame(
            'failed',
            (string) DemoPayment::query()->where('order_id', $orderId)->latest('id')->value('status'),
        );

        // A retry with a new key and a successful outcome completes it.
        $this->withToken($token)
            ->withHeader('Idempotency-Key', (string) Str::uuid())
            ->postJson('/api/v1/orders/'.$orderId.'/payment', ['outcome' => 'success'])
            ->assertOk()
            ->assertJsonPath('data.orderStatus', 'confirmed');
        $this->assertSame($stockBefore - 1, $this->stockOf($product->id));
    }

    public function test_another_customer_cannot_read_the_order_object_level_authorization_boundary(): void
    {
        $this->seed(DatabaseSeeder::class);
        $token = $this->signInDemoCustomer();

        $product = $this->productBySku(self::EARBUDS_SKU);
        $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $product->id, 'quantity' => 1])
            ->assertOk();
        $orderId = (int) $this->placeOrder($token, ['shippingAddress' => $this->address()], (string) Str::uuid())
            ->assertCreated()
            ->json('data.id');

        [, $intruderToken] = $this->registerCustomer(['email' => 'intruder@example.com']);

        // A different signed-in customer answers 404, never 403 or the data.
        $this->withToken($intruderToken)->getJson('/api/v1/orders/'.$orderId)
            ->assertNotFound()
            ->assertJsonPath('error.code', 'NOT_FOUND');

        // The owner still reads it normally.
        $this->withToken($token)->getJson('/api/v1/orders/'.$orderId)->assertOk();
    }
}
