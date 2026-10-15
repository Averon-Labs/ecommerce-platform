<?php

namespace Tests\Feature\Admin;

use App\Models\Inventory;
use App\Models\Product;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * CAT-003: admin inventory list/update per docs/openapi.yaml — the
 * absolute-quantity patch (never a delta), 0..1000000 boundaries, rows
 * absent until stock is set, and the public catalog reflecting persisted
 * stock changes.
 */
class AdminInventoryTest extends TestCase
{
    use InteractsWithAdminSession, RefreshDatabase;

    public function test_inventory_list_reports_rows_filters_and_paginates(): void
    {
        $this->actingAsAdmin();

        $inStock = Product::factory()->create(['name' => 'Alpha Lamp', 'sku' => 'INV-1']);
        Inventory::factory()->create(['product_id' => $inStock->id, 'quantity' => 5]);

        $missing = Product::factory()->create(['name' => 'Beta Lamp', 'sku' => 'INV-2']);

        $outOfStock = Product::factory()->create(['name' => 'Gamma Lamp', 'sku' => 'INV-3']);
        Inventory::factory()->outOfStock()->create(['product_id' => $outOfStock->id]);

        $response = $this->adminJson('GET', '/api/v1/admin/inventory');

        $response->assertOk()
            ->assertJsonStructure(['data' => [[
                'productId', 'name', 'sku', 'quantity', 'inStock', 'updatedAt',
            ]], 'pagination' => ['currentPage', 'perPage', 'totalItems', 'totalPages']])
            ->assertJsonPath('pagination.totalItems', 3);

        $rows = collect($response->json('data'));
        $this->assertSame(5, $rows->firstWhere('productId', $inStock->id)['quantity']);
        $this->assertTrue($rows->firstWhere('productId', $inStock->id)['inStock']);
        $this->assertNotEmpty($rows->firstWhere('productId', $inStock->id)['updatedAt']);

        // No inventory row: quantity 0, out of stock, no timestamp.
        $this->assertSame(0, $rows->firstWhere('productId', $missing->id)['quantity']);
        $this->assertFalse($rows->firstWhere('productId', $missing->id)['inStock']);
        $this->assertNull($rows->firstWhere('productId', $missing->id)['updatedAt']);

        $this->adminJson('GET', '/api/v1/admin/inventory?inStock=true')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.sku', 'INV-1');

        // "1"/"0" are accepted boolean spellings and must behave exactly
        // like "true"/"false" (regression: "1" once fell through to the
        // out-of-stock branch).
        $this->adminJson('GET', '/api/v1/admin/inventory?inStock=1')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.sku', 'INV-1');

        $this->adminJson('GET', '/api/v1/admin/inventory?inStock=false')
            ->assertOk()
            ->assertJsonCount(2, 'data');

        $this->adminJson('GET', '/api/v1/admin/inventory?inStock=0')
            ->assertOk()
            ->assertJsonCount(2, 'data');

        $this->adminJson('GET', '/api/v1/admin/inventory?q=alpha')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.name', 'Alpha Lamp');

        // q also matches the SKU.
        $this->adminJson('GET', '/api/v1/admin/inventory?q=INV-2')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.name', 'Beta Lamp');

        $this->adminJson('GET', '/api/v1/admin/inventory?perPage=2&page=2')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('pagination.totalPages', 2);

        $this->adminJson('GET', '/api/v1/admin/inventory?inStock=bogus')
            ->assertStatus(422)
            ->assertJsonPath('error.code', 'VALIDATION_ERROR');
    }

    public function test_inventory_update_creates_rows_and_sets_absolute_quantities(): void
    {
        $this->actingAsAdmin();
        $product = Product::factory()->create();

        // First set creates the inventory row.
        $created = $this->adminJson('PATCH', "/api/v1/admin/inventory/{$product->id}", ['quantity' => 12]);

        $created->assertOk()
            ->assertJsonPath('data.productId', $product->id)
            ->assertJsonPath('data.quantity', 12)
            ->assertJsonPath('data.inStock', true)
            ->assertJsonStructure(['data' => ['updatedAt']]);
        $this->assertNotEmpty($created->json('data.updatedAt'));

        // Absolute value, not a delta: 7 replaces 12 (a retry cannot
        // double-apply), and zero is a valid out-of-stock answer.
        $this->adminJson('PATCH', "/api/v1/admin/inventory/{$product->id}", ['quantity' => 7])
            ->assertOk()
            ->assertJsonPath('data.quantity', 7);

        $this->assertDatabaseHas('inventories', ['product_id' => $product->id, 'quantity' => 7]);

        $this->adminJson('PATCH', "/api/v1/admin/inventory/{$product->id}", ['quantity' => 0])
            ->assertOk()
            ->assertJsonPath('data.quantity', 0)
            ->assertJsonPath('data.inStock', false);

        // Upper boundary of the contract's integer range.
        $this->adminJson('PATCH', "/api/v1/admin/inventory/{$product->id}", ['quantity' => 1000000])
            ->assertOk()
            ->assertJsonPath('data.quantity', 1000000);

        // The public catalog reflects persisted stock immediately.
        $this->getJson("/api/v1/products/{$product->id}")
            ->assertOk()
            ->assertJsonPath('data.stockQuantity', 1000000)
            ->assertJsonPath('data.inStock', true);
    }

    public function test_inventory_update_rejects_invalid_values_unknown_fields_and_missing_products(): void
    {
        $this->actingAsAdmin();
        $product = Product::factory()->create();
        Inventory::factory()->create(['product_id' => $product->id, 'quantity' => 5]);

        $cases = [
            'negative quantity' => [['quantity' => -1]],
            'quantity above 1000000' => [['quantity' => 1000001]],
            'non-integer quantity' => [['quantity' => 'lots']],
            'fractional quantity' => [['quantity' => 3.5]],
            'missing quantity key' => [[]],
            'unknown delta field' => [['quantity' => 1, 'delta' => 1]],
        ];

        foreach ($cases as $label => $payload) {
            $response = $this->adminJson('PATCH', "/api/v1/admin/inventory/{$product->id}", $payload);

            $response->assertStatus(422);
            $this->assertSame('VALIDATION_ERROR', $response->json('error.code'), $label);
        }

        // Nothing above may have moved the stored stock.
        $this->assertDatabaseHas('inventories', ['product_id' => $product->id, 'quantity' => 5]);

        $this->adminJson('PATCH', '/api/v1/admin/inventory/999999', ['quantity' => 1])
            ->assertStatus(404)
            ->assertJsonPath('error.code', 'NOT_FOUND');
    }
}
