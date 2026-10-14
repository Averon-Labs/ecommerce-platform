<?php

namespace Tests\Feature\Quality;

use App\Models\Inventory;
use App\Models\Product;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * QUAL-002 (backend contribution): the API behaviors the shared quality
 * requirements depend on. Every failure path returns the one documented
 * error envelope (`{error:{code,message[,details]}}`) so a client can render
 * a single error state, and every empty result returns a well-formed page
 * (correct `pagination` with `totalPages` 0) so a screen can show an empty
 * state instead of a dead end. This proves the server supports consistent
 * loading, empty, error, success, and retry states at the contract layer.
 */
class ApiQualityStateTest extends TestCase
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
     * Every documented failure uses one envelope the UI can render generically.
     */
    public function test_every_error_path_uses_the_single_documented_envelope(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 5]);
        [, $token] = $this->registerCustomer();

        $cases = [
            // 401 — unauthenticated read.
            [$this->getJson('/api/v1/cart'), 401, 'UNAUTHENTICATED'],
            // 404 — unknown resource.
            [$this->getJson('/api/v1/products/999999'), 404, 'NOT_FOUND'],
            // 422 — field validation on a cart mutation.
            [$this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $product->id, 'quantity' => 999]), 422, 'VALIDATION_ERROR'],
            // 409 — business conflict (checkout with an empty cart).
            [$this->withToken($token)->postJson('/api/v1/checkout/quote', ['shippingAddress' => $this->address()]), 409, 'CART_EMPTY'],
        ];

        foreach ($cases as [$response, $status, $code]) {
            $this->assertApiError($response, $status, $code);
        }
    }

    /**
     * The cart-less customer read is a well-formed empty state, not an error.
     */
    public function test_empty_cart_read_is_a_documented_empty_state(): void
    {
        [, $token] = $this->registerCustomer();

        $this->withToken($token)->getJson('/api/v1/cart')
            ->assertOk()
            ->assertJsonPath('data.items', [])
            ->assertJsonPath('data.itemCount', 0)
            ->assertJsonPath('data.subtotal', '0.00')
            ->assertJsonPath('data.shipping', '0.00')
            ->assertJsonPath('data.total', '0.00')
            ->assertJsonPath('data.hasIssues', false);
    }

    /**
     * An empty order list is a well-formed zero-page, so screens show an
     * empty state rather than a broken page.
     */
    public function test_empty_order_list_returns_a_zero_page(): void
    {
        [, $token] = $this->registerCustomer();

        $this->withToken($token)->getJson('/api/v1/orders')
            ->assertOk()
            ->assertJsonPath('data', [])
            ->assertJsonPath('pagination.totalItems', 0)
            ->assertJsonPath('pagination.totalPages', 0)
            ->assertJsonPath('pagination.currentPage', 1);
    }

    /**
     * A search that matches nothing is a well-formed empty catalog page.
     */
    public function test_catalog_search_with_no_matches_returns_a_zero_page(): void
    {
        $this->makeProduct(['name' => 'Aurora Desk Lamp'], ['quantity' => 3]);

        $this->getJson('/api/v1/products?q=definitely-not-a-product')
            ->assertOk()
            ->assertJsonPath('data', [])
            ->assertJsonPath('pagination.totalItems', 0)
            ->assertJsonPath('pagination.totalPages', 0);
    }

    /**
     * A page requested past the last page is an empty page, not an error,
     * so infinite-scroll/retry loops never hit a hard failure.
     */
    public function test_orders_page_beyond_the_end_is_an_empty_page_not_an_error(): void
    {
        [, $token] = $this->registerCustomer();

        $this->withToken($token)->getJson('/api/v1/orders?page=99')
            ->assertOk()
            ->assertJsonPath('data', [])
            ->assertJsonPath('pagination.currentPage', 99);
    }

    /**
     * A valid contract address for the quote conflict case.
     *
     * @return array<string, mixed>
     */
    private function address(): array
    {
        return [
            'recipientName' => 'Ada Lovelace',
            'line1' => '1 Analytical Row',
            'city' => 'London',
            'region' => 'LDN',
            'postalCode' => 'N1 1AA',
            'country' => 'GB',
        ];
    }
}
