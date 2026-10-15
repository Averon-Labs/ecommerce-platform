<?php

namespace Tests\Feature\Cart;

use App\Models\Inventory;
use App\Models\Product;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * CART-001: the customer cart API against docs/openapi.yaml — server-priced
 * lines, availability and stock validation on every mutation, implicit
 * per-token ownership, and the documented error envelope for every failure.
 */
class CartTest extends TestCase
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

    public function test_cart_endpoints_reject_guests(): void
    {
        $product = $this->makeProduct();

        $this->assertApiError($this->getJson('/api/v1/cart'), 401, 'UNAUTHENTICATED');
        $this->assertApiError(
            $this->postJson('/api/v1/cart/items', ['productId' => $product->id, 'quantity' => 1]),
            401,
            'UNAUTHENTICATED',
        );
        $this->assertApiError(
            $this->patchJson('/api/v1/cart/items/'.$product->id, ['quantity' => 1]),
            401,
            'UNAUTHENTICATED',
        );
        $this->assertApiError($this->deleteJson('/api/v1/cart/items/'.$product->id), 401, 'UNAUTHENTICATED');
        $this->assertApiError($this->deleteJson('/api/v1/cart'), 401, 'UNAUTHENTICATED');
    }

    public function test_cart_read_without_a_cart_returns_the_documented_empty_shape(): void
    {
        [, $token] = $this->registerCustomer();

        $response = $this->withToken($token)->getJson('/api/v1/cart');

        $response->assertOk()
            ->assertJsonStructure([
                'data' => ['items', 'itemCount', 'subtotal', 'shipping', 'total', 'currency', 'hasIssues'],
            ])
            ->assertJsonPath('data.items', [])
            ->assertJsonPath('data.itemCount', 0)
            ->assertJsonPath('data.subtotal', '0.00')
            ->assertJsonPath('data.shipping', '0.00')
            ->assertJsonPath('data.total', '0.00')
            ->assertJsonPath('data.currency', 'USD')
            ->assertJsonPath('data.hasIssues', false);
    }

    public function test_cart_add_creates_a_server_priced_line_and_returns_the_whole_cart(): void
    {
        $product = $this->makeProduct(['name' => 'Aurora Desk Lamp', 'price' => '39.99'], ['quantity' => 12]);
        [, $token] = $this->registerCustomer();

        $response = $this->withToken($token)->postJson('/api/v1/cart/items', [
            'productId' => $product->id,
            'quantity' => 2,
        ]);

        $response->assertOk()
            ->assertJsonStructure([
                'data' => [
                    'items' => [[
                        'productId', 'name', 'imageUrl', 'unitPrice', 'currency', 'quantity', 'lineTotal',
                        'inStock', 'availableQuantity', 'priceChanged', 'unavailable',
                    ]],
                    'itemCount', 'subtotal', 'shipping', 'total', 'currency', 'hasIssues',
                ],
            ])
            ->assertJsonPath('data.items.0.productId', $product->id)
            ->assertJsonPath('data.items.0.name', 'Aurora Desk Lamp')
            ->assertJsonPath('data.items.0.unitPrice', '39.99')
            ->assertJsonPath('data.items.0.currency', 'USD')
            ->assertJsonPath('data.items.0.quantity', 2)
            ->assertJsonPath('data.items.0.lineTotal', '79.98')
            ->assertJsonPath('data.items.0.inStock', true)
            ->assertJsonPath('data.items.0.availableQuantity', 12)
            ->assertJsonPath('data.items.0.priceChanged', false)
            ->assertJsonPath('data.items.0.unavailable', false)
            ->assertJsonPath('data.itemCount', 2)
            ->assertJsonPath('data.subtotal', '79.98')
            ->assertJsonPath('data.shipping', '5.00')
            ->assertJsonPath('data.total', '84.98')
            ->assertJsonPath('data.hasIssues', false);

        // Money stays a decimal string, never a JSON number.
        $total = $response->json('data.total');
        $this->assertIsString($total);
        $this->assertMatchesRegularExpression('/^\d{1,9}\.\d{2}$/', $total);
    }

    public function test_cart_add_increases_the_quantity_of_a_line_already_in_the_cart(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 10]);
        [, $token] = $this->registerCustomer();

        $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $product->id, 'quantity' => 1])
            ->assertOk();

        $response = $this->withToken($token)->postJson('/api/v1/cart/items', [
            'productId' => $product->id,
            'quantity' => 2,
        ]);

        $response->assertOk()
            ->assertJsonPath('data.items.0.quantity', 3)
            ->assertJsonPath('data.itemCount', 3)
            ->assertJsonPath('data.subtotal', '30.00');
    }

    public function test_cart_add_with_an_unknown_product_returns_404(): void
    {
        [, $token] = $this->registerCustomer();

        $response = $this->withToken($token)->postJson('/api/v1/cart/items', [
            'productId' => 9999,
            'quantity' => 1,
        ]);

        $this->assertApiError($response, 404, 'NOT_FOUND');
    }

    public function test_cart_add_with_an_unavailable_product_returns_409(): void
    {
        $inactive = $this->makeProduct(['status' => 'inactive']);
        $archived = $this->makeProduct(['status' => 'archived']);
        [, $token] = $this->registerCustomer();

        $this->assertApiError(
            $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $inactive->id, 'quantity' => 1]),
            409,
            'PRODUCT_UNAVAILABLE',
        );
        $this->assertApiError(
            $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $archived->id, 'quantity' => 1]),
            409,
            'PRODUCT_UNAVAILABLE',
        );
    }

    public function test_cart_add_with_an_invalid_quantity_returns_422(): void
    {
        $product = $this->makeProduct();
        [, $token] = $this->registerCustomer();

        foreach ([0, 100, 'two', null] as $quantity) {
            $response = $this->withToken($token)->postJson('/api/v1/cart/items', [
                'productId' => $product->id,
                'quantity' => $quantity,
            ]);

            $this->assertApiError($response, 422, 'VALIDATION_ERROR');
        }
    }

    public function test_cart_add_rejects_unknown_request_fields(): void
    {
        $product = $this->makeProduct();
        [, $token] = $this->registerCustomer();

        $response = $this->withToken($token)->postJson('/api/v1/cart/items', [
            'productId' => $product->id,
            'quantity' => 1,
            'discount' => 10,
        ]);

        $this->assertApiError($response, 422, 'VALIDATION_ERROR');
    }

    public function test_cart_add_beyond_available_stock_returns_409_reporting_the_stock(): void
    {
        $product = $this->makeProduct(['price' => '20.00'], ['quantity' => 3]);
        [, $token] = $this->registerCustomer();

        $response = $this->withToken($token)->postJson('/api/v1/cart/items', [
            'productId' => $product->id,
            'quantity' => 5,
        ]);

        $this->assertApiError($response, 409, 'INSUFFICIENT_STOCK');
        $response->assertJsonStructure(['error' => ['details' => [['field', 'message']]]]);
        $this->assertStringContainsString('3', $response->json('error.details.0.message'));
    }

    public function test_cart_add_of_an_out_of_stock_product_returns_409(): void
    {
        $product = $this->makeProduct(['price' => '20.00'], ['quantity' => 0]);
        [, $token] = $this->registerCustomer();

        $response = $this->withToken($token)->postJson('/api/v1/cart/items', [
            'productId' => $product->id,
            'quantity' => 1,
        ]);

        $this->assertApiError($response, 409, 'INSUFFICIENT_STOCK');
        $this->assertStringContainsString('0', $response->json('error.details.0.message'));
    }

    public function test_cart_add_increment_may_not_push_a_line_past_99(): void
    {
        $product = $this->makeProduct(['price' => '1.00'], ['quantity' => 500]);
        [, $token] = $this->registerCustomer();

        $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $product->id, 'quantity' => 98])
            ->assertOk();

        $response = $this->withToken($token)->postJson('/api/v1/cart/items', [
            'productId' => $product->id,
            'quantity' => 5,
        ]);

        $this->assertApiError($response, 422, 'VALIDATION_ERROR');
    }

    public function test_cart_add_increment_may_not_exceed_the_stock_of_an_existing_line(): void
    {
        $product = $this->makeProduct(['price' => '1.00'], ['quantity' => 10]);
        [, $token] = $this->registerCustomer();

        $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $product->id, 'quantity' => 8])
            ->assertOk();

        $response = $this->withToken($token)->postJson('/api/v1/cart/items', [
            'productId' => $product->id,
            'quantity' => 5,
        ]);

        $this->assertApiError($response, 409, 'INSUFFICIENT_STOCK');
    }

    public function test_cart_update_sets_the_line_quantity(): void
    {
        $product = $this->makeProduct(['price' => '12.50'], ['quantity' => 9]);
        [, $token] = $this->registerCustomer();

        $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $product->id, 'quantity' => 1])
            ->assertOk();

        $response = $this->withToken($token)->patchJson('/api/v1/cart/items/'.$product->id, ['quantity' => 5]);

        $response->assertOk()
            ->assertJsonPath('data.items.0.quantity', 5)
            ->assertJsonPath('data.subtotal', '62.50')
            ->assertJsonPath('data.total', '67.50');
    }

    public function test_cart_update_of_a_line_not_in_the_cart_returns_404(): void
    {
        $product = $this->makeProduct();
        [, $token] = $this->registerCustomer();

        $response = $this->withToken($token)->patchJson('/api/v1/cart/items/'.$product->id, ['quantity' => 2]);

        $this->assertApiError($response, 404, 'CART_ITEM_NOT_FOUND');
    }

    public function test_cart_update_beyond_available_stock_returns_409(): void
    {
        $product = $this->makeProduct(['price' => '5.00'], ['quantity' => 4]);
        [, $token] = $this->registerCustomer();

        $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $product->id, 'quantity' => 1])
            ->assertOk();

        $response = $this->withToken($token)->patchJson('/api/v1/cart/items/'.$product->id, ['quantity' => 6]);

        $this->assertApiError($response, 409, 'INSUFFICIENT_STOCK');
    }

    public function test_cart_update_of_a_line_whose_product_became_unavailable_returns_409(): void
    {
        $product = $this->makeProduct(['price' => '5.00'], ['quantity' => 9]);
        [, $token] = $this->registerCustomer();

        $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $product->id, 'quantity' => 1])
            ->assertOk();

        $product->update(['status' => 'inactive']);

        $response = $this->withToken($token)->patchJson('/api/v1/cart/items/'.$product->id, ['quantity' => 2]);

        $this->assertApiError($response, 409, 'PRODUCT_UNAVAILABLE');
    }

    public function test_cart_update_with_an_invalid_quantity_returns_422(): void
    {
        $product = $this->makeProduct(['price' => '5.00'], ['quantity' => 9]);
        [, $token] = $this->registerCustomer();

        $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $product->id, 'quantity' => 1])
            ->assertOk();

        foreach ([0, 100] as $quantity) {
            $response = $this->withToken($token)->patchJson('/api/v1/cart/items/'.$product->id, ['quantity' => $quantity]);

            $this->assertApiError($response, 422, 'VALIDATION_ERROR');
        }
    }

    public function test_cart_remove_deletes_one_line_and_unknown_lines_answer_404(): void
    {
        $kept = $this->makeProduct(['name' => 'Kept'], ['quantity' => 5]);
        $removed = $this->makeProduct(['name' => 'Removed'], ['quantity' => 5]);
        [, $token] = $this->registerCustomer();

        $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $kept->id, 'quantity' => 1])
            ->assertOk();
        $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $removed->id, 'quantity' => 2])
            ->assertOk();

        $response = $this->withToken($token)->deleteJson('/api/v1/cart/items/'.$removed->id);

        $response->assertOk()
            ->assertJsonPath('data.itemCount', 1)
            ->assertJsonPath('data.items.0.productId', $kept->id);

        $this->assertApiError(
            $this->withToken($token)->deleteJson('/api/v1/cart/items/'.$removed->id),
            404,
            'CART_ITEM_NOT_FOUND',
        );
    }

    public function test_cart_clear_empties_the_cart(): void
    {
        $first = $this->makeProduct(['price' => '10.00'], ['quantity' => 5]);
        $second = $this->makeProduct(['price' => '20.00'], ['quantity' => 5]);
        [, $token] = $this->registerCustomer();

        $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $first->id, 'quantity' => 1])
            ->assertOk();
        $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $second->id, 'quantity' => 1])
            ->assertOk();

        $this->withToken($token)->deleteJson('/api/v1/cart')
            ->assertOk()
            ->assertJsonPath('data.items', [])
            ->assertJsonPath('data.itemCount', 0)
            ->assertJsonPath('data.total', '0.00');

        $this->withToken($token)->getJson('/api/v1/cart')
            ->assertOk()
            ->assertJsonPath('data.itemCount', 0);
    }

    public function test_cart_read_flags_a_price_change_since_the_line_was_added(): void
    {
        $product = $this->makeProduct(['price' => '39.99'], ['quantity' => 12]);
        [, $token] = $this->registerCustomer();

        $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $product->id, 'quantity' => 2])
            ->assertJsonPath('data.items.0.priceChanged', false);

        $product->update(['price' => '49.99']);

        $response = $this->withToken($token)->getJson('/api/v1/cart');

        $response->assertOk()
            ->assertJsonPath('data.items.0.unitPrice', '49.99')
            ->assertJsonPath('data.items.0.lineTotal', '99.98')
            ->assertJsonPath('data.items.0.priceChanged', true)
            ->assertJsonPath('data.subtotal', '99.98')
            ->assertJsonPath('data.hasIssues', true);
    }

    public function test_cart_read_flags_unavailable_and_short_stock_lines(): void
    {
        $product = $this->makeProduct(['price' => '10.00'], ['quantity' => 5]);
        [, $token] = $this->registerCustomer();

        $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $product->id, 'quantity' => 5])
            ->assertJsonPath('data.hasIssues', false);

        // Stock shrank below the line quantity: the line is flagged.
        $product->inventory->update(['quantity' => 2]);

        $this->withToken($token)->getJson('/api/v1/cart')
            ->assertJsonPath('data.items.0.availableQuantity', 2)
            ->assertJsonPath('data.items.0.inStock', true)
            ->assertJsonPath('data.hasIssues', true);

        // The product left the active catalog: unavailable is flagged too.
        $product->update(['status' => 'archived']);

        $this->withToken($token)->getJson('/api/v1/cart')
            ->assertJsonPath('data.items.0.unavailable', true)
            ->assertJsonPath('data.hasIssues', true);
    }

    public function test_cart_is_owned_per_token_and_never_exposes_another_customer_cart(): void
    {
        $productA = $this->makeProduct(['name' => 'Customer A item'], ['quantity' => 5]);
        $productB = $this->makeProduct(['name' => 'Customer B item'], ['quantity' => 5]);
        [, $tokenA] = $this->registerCustomer(['email' => 'a@example.com']);
        [, $tokenB] = $this->registerCustomer(['email' => 'b@example.com']);

        $this->withToken($tokenA)->postJson('/api/v1/cart/items', ['productId' => $productA->id, 'quantity' => 2])
            ->assertOk();

        // Customer B starts with an empty cart of their own.
        $this->withToken($tokenB)->getJson('/api/v1/cart')
            ->assertOk()
            ->assertJsonPath('data.items', [])
            ->assertJsonPath('data.itemCount', 0);

        $this->withToken($tokenB)->postJson('/api/v1/cart/items', ['productId' => $productB->id, 'quantity' => 1])
            ->assertOk()
            ->assertJsonPath('data.itemCount', 1)
            ->assertJsonPath('data.items.0.productId', $productB->id);

        // Customer B cannot touch a line that is not in B's own cart.
        $this->assertApiError(
            $this->withToken($tokenB)->patchJson('/api/v1/cart/items/'.$productA->id, ['quantity' => 1]),
            404,
            'CART_ITEM_NOT_FOUND',
        );

        // Customer A's cart is untouched by anything B did.
        $this->withToken($tokenA)->getJson('/api/v1/cart')
            ->assertOk()
            ->assertJsonPath('data.items.0.productId', $productA->id)
            ->assertJsonPath('data.items.0.quantity', 2)
            ->assertJsonPath('data.itemCount', 2);
    }

    public function test_cart_totals_use_every_line_plus_the_flat_sample_shipping(): void
    {
        $first = $this->makeProduct(['price' => '39.99'], ['quantity' => 12]);
        $second = $this->makeProduct(['price' => '12.50'], ['quantity' => 9]);
        [, $token] = $this->registerCustomer();

        $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $first->id, 'quantity' => 2])
            ->assertOk();
        $this->withToken($token)->postJson('/api/v1/cart/items', ['productId' => $second->id, 'quantity' => 1])
            ->assertOk();

        $this->withToken($token)->getJson('/api/v1/cart')
            ->assertOk()
            ->assertJsonPath('data.itemCount', 3)
            ->assertJsonPath('data.subtotal', '92.48')
            ->assertJsonPath('data.shipping', '5.00')
            ->assertJsonPath('data.total', '97.48');
    }
}
