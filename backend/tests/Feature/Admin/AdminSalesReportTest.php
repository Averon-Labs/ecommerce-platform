<?php

namespace Tests\Feature\Admin;

use App\Models\DemoPayment;
use App\Models\Order;
use App\Models\OrderItem;
use App\Models\Product;
use App\Models\User;
use App\Support\Money;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Carbon;
use Tests\TestCase;

/**
 * RPT-001: the read-only admin sales report against docs/openapi.yaml.
 * Revenue is computed from immutable order snapshots for payment-completed
 * orders that were not cancelled; every other order only contributes to the
 * status mix. The date range is inclusive and zero-filled.
 */
class AdminSalesReportTest extends TestCase
{
    use InteractsWithAdminSession, RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        Carbon::setTestNow('2026-10-29 12:00:00');
    }

    protected function tearDown(): void
    {
        Carbon::setTestNow();
        parent::tearDown();
    }

    /**
     * Create an order directly with its immutable snapshots and an optional
     * simulated payment. This keeps the report fixture independent from the
     * checkout flow, which AdminOrderTest already covers.
     *
     * @param  array<int, array{product: Product, quantity: int, unitPrice: string, name?: string}>  $lines
     */
    private function makeOrder(
        array $lines,
        string $createdAt,
        string $status = 'confirmed',
        ?string $paymentStatus = 'completed',
    ): Order {
        $user = User::factory()->create();
        $subtotalCents = 0;

        foreach ($lines as $line) {
            $subtotalCents += Money::toCents($line['unitPrice']) * $line['quantity'];
        }

        $totalCents = $subtotalCents + 500;

        $order = Order::query()->create([
            'user_id' => $user->id,
            'status' => $status,
            'subtotal_amount' => Money::fromCents($subtotalCents),
            'shipping_amount' => '5.00',
            'total_amount' => Money::fromCents($totalCents),
            'shipping_address_snapshot' => [
                'recipientName' => 'Sara Nouri',
                'line1' => '14 Maple Street',
                'line2' => null,
                'city' => 'Seattle',
                'region' => 'WA',
                'postalCode' => '98101',
                'country' => 'US',
            ],
            'notes' => null,
            'timeline' => [['status' => $status, 'at' => $createdAt.'T00:00:00Z']],
        ]);

        $order->reference = sprintf('ESV-%06d', $order->id);
        $order->save();

        Order::query()->whereKey($order->id)->update([
            'created_at' => $createdAt.' 00:00:00',
            'updated_at' => $createdAt.' 00:00:00',
        ]);

        foreach ($lines as $line) {
            OrderItem::query()->create([
                'order_id' => $order->id,
                'product_id' => $line['product']->id,
                'product_name' => $line['name'] ?? $line['product']->name,
                'quantity' => $line['quantity'],
                'unit_price' => $line['unitPrice'],
                'total_price' => Money::fromCents(Money::toCents($line['unitPrice']) * $line['quantity']),
            ]);
        }

        if ($paymentStatus !== null) {
            DemoPayment::query()->create([
                'order_id' => $order->id,
                'amount' => Money::fromCents($totalCents),
                'method' => 'demo',
                'status' => $paymentStatus,
                'transaction_id' => 'DEMO-'.str_pad((string) $order->id, 12, '0', STR_PAD_LEFT),
                'created_at' => $createdAt.' 00:00:01',
            ]);
        }

        return $order->fresh(['items', 'latestPayment']);
    }

    public function test_admin_sales_report_requires_an_admin_session(): void
    {
        $this->assertApiError($this->getJson('/api/v1/admin/reports/sales'), 401, 'UNAUTHENTICATED');

        [, $customerToken] = $this->registerCustomer(['email' => 'customer@example.com']);
        $this->assertApiError(
            $this->withToken($customerToken)->getJson('/api/v1/admin/reports/sales'),
            401,
            'UNAUTHENTICATED',
        );

        $this->actingAsAdmin();
        $this->adminJson('GET', '/api/v1/admin/reports/sales')
            ->assertOk()
            ->assertJsonPath('data.currency', 'USD')
            ->assertJsonPath('data.range.from', '2026-09-30')
            ->assertJsonPath('data.range.to', '2026-10-29');
    }

    public function test_admin_sales_report_aggregates_only_completed_non_cancelled_orders(): void
    {
        $lamp = Product::factory()->create(['name' => 'Aurora Desk Lamp']);
        $chair = Product::factory()->create(['name' => 'Summit Chair']);

        // Two paid orders inside the range: 25.00 (2 items) and 35.00 (1 item).
        $this->makeOrder(
            [['product' => $lamp, 'quantity' => 2, 'unitPrice' => '10.00']],
            '2026-10-28',
        );
        $this->makeOrder(
            [['product' => $chair, 'quantity' => 1, 'unitPrice' => '30.00']],
            '2026-10-29',
        );

        // Not sales: pending, cancelled after payment, and a failed payment.
        $this->makeOrder(
            [['product' => $lamp, 'quantity' => 1, 'unitPrice' => '10.00']],
            '2026-10-28',
            'pending_payment',
            null,
        );
        $this->makeOrder(
            [['product' => $lamp, 'quantity' => 1, 'unitPrice' => '20.00']],
            '2026-10-29',
            'cancelled',
            'completed',
        );
        $this->makeOrder(
            [['product' => $lamp, 'quantity' => 1, 'unitPrice' => '10.00']],
            '2026-10-29',
            'pending_payment',
            'failed',
        );

        // Outside the requested range.
        $this->makeOrder(
            [['product' => $lamp, 'quantity' => 5, 'unitPrice' => '10.00']],
            '2026-09-01',
        );

        $this->actingAsAdmin();

        $response = $this->adminJson('GET', '/api/v1/admin/reports/sales?from=2026-10-28&to=2026-10-29')
            ->assertOk()
            ->assertJsonPath('data.range.from', '2026-10-28')
            ->assertJsonPath('data.range.to', '2026-10-29')
            ->assertJsonPath('data.summary.revenue', '60.00')
            ->assertJsonPath('data.summary.totalOrders', 5)
            ->assertJsonPath('data.summary.paidOrders', 2)
            ->assertJsonPath('data.summary.itemsSold', 3)
            ->assertJsonPath('data.summary.averageOrderValue', '30.00')
            ->assertJsonPath('data.daily.0.date', '2026-10-28')
            ->assertJsonPath('data.daily.0.revenue', '25.00')
            ->assertJsonPath('data.daily.0.orders', 1)
            ->assertJsonPath('data.daily.0.itemsSold', 2)
            ->assertJsonPath('data.daily.1.date', '2026-10-29')
            ->assertJsonPath('data.daily.1.revenue', '35.00')
            ->assertJsonPath('data.daily.1.orders', 1)
            ->assertJsonPath('data.daily.1.itemsSold', 1)
            ->assertJsonPath('data.topProducts.0.name', 'Summit Chair')
            ->assertJsonPath('data.topProducts.0.quantity', 1)
            ->assertJsonPath('data.topProducts.0.revenue', '30.00')
            ->assertJsonPath('data.topProducts.1.name', 'Aurora Desk Lamp')
            ->assertJsonPath('data.topProducts.1.quantity', 2)
            ->assertJsonPath('data.topProducts.1.revenue', '20.00')
            ->assertJsonPath('data.ordersByStatus.0.status', 'pending_payment')
            ->assertJsonPath('data.ordersByStatus.0.orders', 2)
            ->assertJsonPath('data.ordersByStatus.1.status', 'confirmed')
            ->assertJsonPath('data.ordersByStatus.1.orders', 2)
            ->assertJsonPath('data.ordersByStatus.5.status', 'cancelled')
            ->assertJsonPath('data.ordersByStatus.5.orders', 1);

        $this->assertCount(2, $response->json('data.daily'));
    }

    public function test_admin_sales_report_uses_the_order_snapshot_name(): void
    {
        $product = Product::factory()->create(['name' => 'Aurora Desk Lamp']);

        $this->makeOrder(
            [['product' => $product, 'quantity' => 1, 'unitPrice' => '10.00', 'name' => 'Aurora Desk Lamp (old)']],
            '2026-10-29',
        );

        $product->name = 'Aurora Desk Lamp (new)';
        $product->save();

        $this->actingAsAdmin();

        $this->adminJson('GET', '/api/v1/admin/reports/sales?from=2026-10-29&to=2026-10-29')
            ->assertOk()
            ->assertJsonPath('data.topProducts.0.name', 'Aurora Desk Lamp (old)');
    }

    public function test_admin_sales_report_zero_fills_the_default_thirty_day_range(): void
    {
        $this->actingAsAdmin();

        $response = $this->adminJson('GET', '/api/v1/admin/reports/sales')
            ->assertOk()
            ->assertJsonPath('data.range.from', '2026-09-30')
            ->assertJsonPath('data.range.to', '2026-10-29')
            ->assertJsonPath('data.range.timezone', 'UTC')
            ->assertJsonPath('data.summary.revenue', '0.00')
            ->assertJsonPath('data.summary.totalOrders', 0)
            ->assertJsonPath('data.summary.paidOrders', 0)
            ->assertJsonPath('data.summary.itemsSold', 0)
            ->assertJsonPath('data.summary.averageOrderValue', '0.00')
            ->assertJsonPath('data.topProducts', []);

        $this->assertCount(30, $response->json('data.daily'));
        $this->assertSame(
            ['date' => '2026-09-30', 'revenue' => '0.00', 'orders' => 0, 'itemsSold' => 0],
            $response->json('data.daily.0'),
        );
        $this->assertSame(
            ['date' => '2026-10-29', 'revenue' => '0.00', 'orders' => 0, 'itemsSold' => 0],
            $response->json('data.daily.29'),
        );
    }

    public function test_admin_sales_report_honors_the_top_product_limit(): void
    {
        $products = [
            Product::factory()->create(['name' => 'First']),
            Product::factory()->create(['name' => 'Second']),
            Product::factory()->create(['name' => 'Third']),
        ];

        $this->makeOrder(
            [
                ['product' => $products[0], 'quantity' => 1, 'unitPrice' => '30.00'],
                ['product' => $products[1], 'quantity' => 1, 'unitPrice' => '20.00'],
                ['product' => $products[2], 'quantity' => 1, 'unitPrice' => '10.00'],
            ],
            '2026-10-29',
        );

        $this->actingAsAdmin();

        $this->adminJson('GET', '/api/v1/admin/reports/sales?from=2026-10-29&to=2026-10-29&topLimit=2')
            ->assertOk()
            ->assertJsonCount(2, 'data.topProducts')
            ->assertJsonPath('data.topProducts.0.name', 'First')
            ->assertJsonPath('data.topProducts.1.name', 'Second');
    }

    public function test_admin_sales_report_validates_dates_and_limits(): void
    {
        $this->actingAsAdmin();

        $this->assertApiError(
            $this->adminJson('GET', '/api/v1/admin/reports/sales?from=2026-10-30&to=2026-10-29'),
            422,
            'VALIDATION_ERROR',
        );
        $this->assertApiError(
            $this->adminJson('GET', '/api/v1/admin/reports/sales?from=not-a-date'),
            422,
            'VALIDATION_ERROR',
        );
        $this->assertApiError(
            $this->adminJson('GET', '/api/v1/admin/reports/sales?from=2025-10-28&to=2026-10-29'),
            422,
            'VALIDATION_ERROR',
        );
        $this->assertApiError(
            $this->adminJson('GET', '/api/v1/admin/reports/sales?topLimit=0'),
            422,
            'VALIDATION_ERROR',
        );
        $this->assertApiError(
            $this->adminJson('GET', '/api/v1/admin/reports/sales?topLimit=11'),
            422,
            'VALIDATION_ERROR',
        );

        // The 366-day inclusive maximum is accepted.
        $this->adminJson('GET', '/api/v1/admin/reports/sales?from=2025-10-29&to=2026-10-29')
            ->assertOk();
    }
}
