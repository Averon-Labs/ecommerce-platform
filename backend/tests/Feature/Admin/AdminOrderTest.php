<?php

namespace Tests\Feature\Admin;

use App\Models\DemoPayment;
use App\Models\Inventory;
use App\Models\Product;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Carbon;
use Illuminate\Support\Str;
use Tests\TestCase;

/**
 * ORD-001 admin portion: the three admin order operations against
 * docs/openapi.yaml — filtered listing with the owning customer, detail
 * with allowedTransitions, and the x-order-state-machine transitions,
 * including CSRF and role enforcement.
 */
class AdminOrderTest extends TestCase
{
    use InteractsWithAdminSession, RefreshDatabase;

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

    /**
     * Place one order as a signed-in customer through the real cart and
     * checkout endpoints, returning the token, order id, and reference.
     *
     * @return array{token: string, id: int, reference: string}
     */
    private function customerOrder(string $email, Product $product, int $quantity): array
    {
        [, $token] = $this->registerCustomer(['email' => $email]);

        $this->withToken($token)->postJson('/api/v1/cart/items', [
            'productId' => $product->id,
            'quantity' => $quantity,
        ])->assertOk();

        $data = $this->withToken($token)
            ->withHeader('Idempotency-Key', (string) Str::uuid())
            ->postJson('/api/v1/orders', ['shippingAddress' => $this->seedAddress()])
            ->assertCreated()
            ->json('data');

        return ['token' => $token, 'id' => $data['id'], 'reference' => $data['reference']];
    }

    private function pay(string $token, int $orderId, string $outcome = 'success'): void
    {
        $this->withToken($token)
            ->withHeader('Idempotency-Key', (string) Str::uuid())
            ->postJson('/api/v1/orders/'.$orderId.'/payment', ['outcome' => $outcome])
            ->assertOk();
    }

    public function test_admin_order_endpoints_require_an_admin_session(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 9]);
        ['id' => $orderId] = $this->customerOrder('a@example.com', $product, 1);

        $this->assertApiError($this->getJson('/api/v1/admin/orders'), 401, 'UNAUTHENTICATED');
        $this->assertApiError($this->getJson('/api/v1/admin/orders/'.$orderId), 401, 'UNAUTHENTICATED');
        $this->assertApiError(
            $this->patchJson('/api/v1/admin/orders/'.$orderId.'/status', ['status' => 'cancelled']),
            401,
            'UNAUTHENTICATED',
        );

        // A customer bearer token is not an administrator either.
        [, $customerToken] = $this->registerCustomer(['email' => 'customer@example.com']);
        $this->assertApiError(
            $this->withToken($customerToken)->getJson('/api/v1/admin/orders'),
            401,
            'UNAUTHENTICATED',
        );

        $this->actingAsAdmin();
        $this->adminJson('GET', '/api/v1/admin/orders')->assertOk();
    }

    public function test_admin_order_list_includes_the_customer_and_honors_filters(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 50]);
        $first = $this->customerOrder('anna@example.com', $product, 1);
        $second = $this->customerOrder('ben@example.com', $product, 2);
        $this->pay($second['token'], $second['id']);

        $this->actingAsAdmin();

        $all = $this->adminJson('GET', '/api/v1/admin/orders');
        $all->assertOk()
            ->assertJsonStructure([
                'data' => [[
                    'id', 'reference', 'status', 'paymentStatus', 'total', 'currency', 'itemCount', 'createdAt',
                    'customer' => ['id', 'email', 'firstName', 'lastName'],
                ]],
                'pagination' => ['currentPage', 'perPage', 'totalItems', 'totalPages'],
            ])
            ->assertJsonPath('pagination.totalItems', 2)
            // Newest first; the paid order was created last.
            ->assertJsonPath('data.0.id', $second['id'])
            ->assertJsonPath('data.0.customer.email', 'ben@example.com')
            ->assertJsonPath('data.0.paymentStatus', 'completed')
            ->assertJsonPath('data.1.customer.email', 'anna@example.com');

        // q matches the order reference.
        $this->adminJson('GET', '/api/v1/admin/orders?q='.$first['reference'])
            ->assertOk()
            ->assertJsonPath('pagination.totalItems', 1)
            ->assertJsonPath('data.0.id', $first['id']);

        // q matches the customer email.
        $this->adminJson('GET', '/api/v1/admin/orders?q=ben@example.com')
            ->assertOk()
            ->assertJsonPath('pagination.totalItems', 1)
            ->assertJsonPath('data.0.id', $second['id']);

        // status filter.
        $this->adminJson('GET', '/api/v1/admin/orders?status=pending_payment')
            ->assertOk()
            ->assertJsonPath('pagination.totalItems', 1)
            ->assertJsonPath('data.0.id', $first['id']);
        $this->adminJson('GET', '/api/v1/admin/orders?status=confirmed')
            ->assertOk()
            ->assertJsonPath('pagination.totalItems', 1)
            ->assertJsonPath('data.0.id', $second['id']);

        // Inclusive UTC date bounds on createdAt.
        $today = Carbon::now()->toDateString();
        $this->adminJson('GET', '/api/v1/admin/orders?from='.$today.'&to='.$today)
            ->assertOk()
            ->assertJsonPath('pagination.totalItems', 2);
        $this->adminJson('GET', '/api/v1/admin/orders?from='.Carbon::tomorrow()->toDateString())
            ->assertOk()
            ->assertJsonPath('pagination.totalItems', 0);
        $this->adminJson('GET', '/api/v1/admin/orders?from=2020-01-01&to=2020-01-02')
            ->assertOk()
            ->assertJsonPath('pagination.totalItems', 0);

        // Pagination parameters are honored.
        $this->adminJson('GET', '/api/v1/admin/orders?perPage=1')
            ->assertOk()
            ->assertJsonPath('pagination.totalItems', 2)
            ->assertJsonPath('pagination.totalPages', 2)
            ->assertJsonPath('pagination.perPage', 1);

        // Unknown status values are a validation error, not a silent match.
        $this->assertApiError(
            $this->adminJson('GET', '/api/v1/admin/orders?status=paid'),
            422,
            'VALIDATION_ERROR',
        );
    }

    public function test_admin_order_detail_reports_allowed_transitions(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 9]);

        // Every customer call happens before actingAsAdmin(): the admin
        // session identity persists in-process and would shadow the
        // customer's bearer token on later requests (Sanctum resolves the
        // session guard before the bearer token).
        $pending = $this->customerOrder('a@example.com', $product, 1);
        $confirmed = $this->customerOrder('b@example.com', $product, 1);
        $this->pay($confirmed['token'], $confirmed['id']);

        $this->actingAsAdmin();

        $this->adminJson('GET', '/api/v1/admin/orders/'.$pending['id'])
            ->assertOk()
            ->assertJsonPath('data.id', $pending['id'])
            ->assertJsonPath('data.reference', $pending['reference'])
            ->assertJsonPath('data.status', 'pending_payment')
            ->assertJsonPath('data.customer.email', 'a@example.com')
            ->assertJsonPath('data.allowedTransitions', ['cancelled'])
            ->assertJsonPath('data.items.0.quantity', 1);

        $this->adminJson('GET', '/api/v1/admin/orders/'.$confirmed['id'])
            ->assertOk()
            ->assertJsonPath('data.status', 'confirmed')
            ->assertJsonPath('data.paymentStatus', 'completed')
            ->assertJsonPath('data.allowedTransitions', ['processing', 'cancelled']);
    }

    public function test_admin_status_transitions_follow_the_state_machine(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 9]);
        $order = $this->customerOrder('a@example.com', $product, 1);
        $this->pay($order['token'], $order['id']);

        $this->actingAsAdmin();

        $this->adminJson('PATCH', '/api/v1/admin/orders/'.$order['id'].'/status', ['status' => 'processing'])
            ->assertOk()
            ->assertJsonPath('data.status', 'processing')
            ->assertJsonPath('data.allowedTransitions', ['shipped', 'cancelled']);

        $this->adminJson('PATCH', '/api/v1/admin/orders/'.$order['id'].'/status', ['status' => 'shipped'])
            ->assertOk()
            ->assertJsonPath('data.status', 'shipped')
            ->assertJsonPath('data.allowedTransitions', ['delivered']);

        $this->adminJson('PATCH', '/api/v1/admin/orders/'.$order['id'].'/status', ['status' => 'delivered'])
            ->assertOk()
            ->assertJsonPath('data.status', 'delivered')
            ->assertJsonPath('data.allowedTransitions', []);

        // Every applied transition joined the timeline, oldest first.
        $timeline = $this->adminJson('GET', '/api/v1/admin/orders/'.$order['id'])
            ->assertOk()
            ->json('data.timeline');
        $this->assertSame(
            ['pending_payment', 'confirmed', 'processing', 'shipped', 'delivered'],
            array_column($timeline, 'status'),
        );

        // Delivered is terminal.
        $this->assertApiError(
            $this->adminJson('PATCH', '/api/v1/admin/orders/'.$order['id'].'/status', ['status' => 'cancelled']),
            409,
            'INVALID_TRANSITION',
        );
    }

    public function test_admin_cannot_confirm_or_revive_orders(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 20]);

        // All customer calls run before actingAsAdmin(): the admin session
        // identity persists in-process and would shadow the customers'
        // bearer tokens on later requests.
        $pending = $this->customerOrder('a@example.com', $product, 1);
        $walked = $this->customerOrder('b@example.com', $product, 1);
        $this->pay($walked['token'], $walked['id']);

        $this->actingAsAdmin();

        // confirmed is reached only through a successful simulated payment.
        $this->assertApiError(
            $this->adminJson('PATCH', '/api/v1/admin/orders/'.$pending['id'].'/status', ['status' => 'confirmed']),
            409,
            'INVALID_TRANSITION',
        );

        // A cancelled order is terminal: nothing revives it.
        $this->adminJson('PATCH', '/api/v1/admin/orders/'.$pending['id'].'/status', ['status' => 'cancelled'])
            ->assertOk()
            ->assertJsonPath('data.status', 'cancelled');
        $this->assertApiError(
            $this->adminJson('PATCH', '/api/v1/admin/orders/'.$pending['id'].'/status', ['status' => 'processing']),
            409,
            'INVALID_TRANSITION',
        );
        $this->assertApiError(
            $this->adminJson('PATCH', '/api/v1/admin/orders/'.$pending['id'].'/status', ['status' => 'pending_payment']),
            409,
            'INVALID_TRANSITION',
        );

        // shipped cannot jump backwards.
        $this->adminJson('PATCH', '/api/v1/admin/orders/'.$walked['id'].'/status', ['status' => 'processing'])->assertOk();
        $this->adminJson('PATCH', '/api/v1/admin/orders/'.$walked['id'].'/status', ['status' => 'shipped'])->assertOk();
        $this->assertApiError(
            $this->adminJson('PATCH', '/api/v1/admin/orders/'.$walked['id'].'/status', ['status' => 'processing']),
            409,
            'INVALID_TRANSITION',
        );

        // The rejected transitions created no payments: only the walked
        // order has one completed payment and the pending one has none.
        $this->assertSame(1, DemoPayment::query()->where('status', 'completed')->count());
        $this->assertSame(0, DemoPayment::query()->where('order_id', $pending['id'])->count());
    }

    public function test_admin_status_update_validates_the_body(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 9]);
        $order = $this->customerOrder('a@example.com', $product, 1);

        $this->actingAsAdmin();

        $this->assertApiError(
            $this->adminJson('PATCH', '/api/v1/admin/orders/'.$order['id'].'/status', ['status' => 'paid']),
            422,
            'VALIDATION_ERROR',
        );
        $this->assertApiError(
            $this->adminJson('PATCH', '/api/v1/admin/orders/'.$order['id'].'/status', []),
            422,
            'VALIDATION_ERROR',
        );
        $this->assertApiError(
            $this->adminJson('PATCH', '/api/v1/admin/orders/'.$order['id'].'/status', [
                'status' => 'cancelled',
                'note' => 'not in the schema',
            ]),
            422,
            'VALIDATION_ERROR',
        );

        // The rejected bodies changed nothing.
        $this->adminJson('GET', '/api/v1/admin/orders/'.$order['id'])
            ->assertJsonPath('data.status', 'pending_payment');
    }

    public function test_admin_status_update_rejects_a_missing_csrf_header(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 9]);
        $order = $this->customerOrder('a@example.com', $product, 1);

        $this->actingAsAdmin();

        // The test environment normally bypasses CSRF verification, so
        // enforce it explicitly (same technique as AdminAuthTest) to prove
        // the real X-XSRF-TOKEN check rejects this admin mutation.
        $this->app->instance('env', 'local');

        $response = $this->json(
            'PATCH',
            '/api/v1/admin/orders/'.$order['id'].'/status',
            ['status' => 'cancelled'],
            ['Origin' => $this->statefulOrigin()],
        );

        $this->app->instance('env', 'testing');

        $this->assertApiError($response, 403, 'CSRF_TOKEN_MISMATCH');

        $this->adminJson('GET', '/api/v1/admin/orders/'.$order['id'])
            ->assertJsonPath('data.status', 'pending_payment');
    }

    public function test_admin_order_detail_answers_404_for_unknown_ids(): void
    {
        $this->actingAsAdmin();

        $this->assertApiError(
            $this->adminJson('GET', '/api/v1/admin/orders/999999'),
            404,
            'NOT_FOUND',
        );
        $this->assertApiError(
            $this->adminJson('PATCH', '/api/v1/admin/orders/999999/status', ['status' => 'cancelled']),
            404,
            'NOT_FOUND',
        );
    }
}
