<?php

namespace Tests\Feature\Admin;

use App\Models\Category;
use App\Models\Inventory;
use App\Models\Product;
use App\Models\ProductImage;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * CAT-003: admin product create/read/update/archive per
 * docs/openapi.yaml — validation, 409 codes, pagination, and the public
 * catalog reflecting persisted admin changes.
 */
class AdminProductTest extends TestCase
{
    use InteractsWithAdminSession, RefreshDatabase;

    public function test_admin_product_list_includes_every_status_filters_and_paginates(): void
    {
        $this->actingAsAdmin();

        $first = Product::factory()->create(['name' => 'Zebra Desk Fan']);
        $second = Product::factory()->inactive()->create(['name' => 'Yak Test Lamp']);
        $third = Product::factory()->archived()->create(['name' => 'X-Ray Mount']);
        Inventory::factory()->create(['product_id' => $first->id, 'quantity' => 5]);

        $response = $this->adminJson('GET', '/api/v1/admin/products');

        $response->assertOk()
            ->assertJsonStructure(['data' => [[
                'id', 'name', 'price', 'currency', 'categoryId', 'categoryName',
                'imageUrl', 'inStock', 'status', 'description', 'sku',
                'stockQuantity', 'images', 'updatedAt',
            ]], 'pagination' => ['currentPage', 'perPage', 'totalItems', 'totalPages']])
            ->assertJsonPath('pagination.totalItems', 3)
            ->assertJsonPath('pagination.totalPages', 1)
            ->assertJsonPath('pagination.currentPage', 1);

        // Newest-first by id (all created within the same second).
        $this->assertSame(
            [$third->id, $second->id, $first->id],
            collect($response->json('data'))->pluck('id')->all(),
        );

        $row = collect($response->json('data'))->firstWhere('id', $first->id);
        $this->assertTrue($row['inStock']);
        $this->assertSame(5, $row['stockQuantity']);
        $this->assertNotEmpty($row['updatedAt']);

        $this->adminJson('GET', '/api/v1/admin/products?status=inactive')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.name', 'Yak Test Lamp');

        $this->adminJson('GET', '/api/v1/admin/products?q=desk')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.name', 'Zebra Desk Fan');

        $this->adminJson('GET', '/api/v1/admin/products?perPage=2&page=2')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('pagination.totalPages', 2);

        $this->adminJson('GET', '/api/v1/admin/products?status=bogus')
            ->assertStatus(422)
            ->assertJsonPath('error.code', 'VALIDATION_ERROR');
    }

    public function test_admin_product_create_persists_product_and_images_without_inventory(): void
    {
        $this->actingAsAdmin();
        $category = Category::factory()->create(['name' => 'Lighting']);

        $response = $this->adminJson('POST', '/api/v1/admin/products', $this->payload($category->id));

        $response->assertCreated()
            ->assertJsonPath('data.name', 'Nimbus Desk Light')
            ->assertJsonPath('data.price', '49.90')
            ->assertJsonPath('data.currency', 'USD')
            ->assertJsonPath('data.categoryName', 'Lighting')
            ->assertJsonPath('data.status', 'active')
            ->assertJsonPath('data.sku', 'DL-9001')
            ->assertJsonPath('data.stockQuantity', 0)
            ->assertJsonPath('data.inStock', false)
            ->assertJsonPath('data.imageUrl', 'https://images.example/nimbus/1.jpg')
            ->assertJsonCount(2, 'data.images')
            ->assertJsonPath('data.images.0.url', 'https://images.example/nimbus/1.jpg')
            ->assertJsonPath('data.images.0.sortOrder', 0)
            ->assertJsonPath('data.images.1.sortOrder', 1)
            ->assertJsonStructure(['data' => ['updatedAt']]);

        $this->assertNotEmpty($response->json('data.updatedAt'));

        $productId = $response->json('data.id');

        $this->assertDatabaseHas('products', [
            'id' => $productId,
            'name' => 'Nimbus Desk Light',
            'price' => '49.90',
            'status' => 'active',
        ]);
        $this->assertDatabaseHas('product_images', [
            'product_id' => $productId,
            'url' => 'https://images.example/nimbus/2.jpg',
            'sort_order' => 1,
        ]);
        // Contract: stock is owned by PATCH /admin/inventory/{id} —
        // creating a product must not fabricate an inventory row.
        $this->assertDatabaseMissing('inventories', ['product_id' => $productId]);

        // The public catalog reflects the persisted row immediately.
        $this->getJson("/api/v1/products/{$productId}")
            ->assertOk()
            ->assertJsonPath('data.price', '49.90')
            ->assertJsonPath('data.inStock', false)
            ->assertJsonPath('data.stockQuantity', 0);
    }

    public function test_admin_product_create_rejects_duplicate_sku(): void
    {
        $this->actingAsAdmin();
        $category = Category::factory()->create();

        Product::factory()->create(['sku' => 'DL-9001', 'category_id' => $category->id]);

        $response = $this->adminJson(
            'POST',
            '/api/v1/admin/products',
            $this->payload($category->id, ['sku' => 'DL-9001']),
        );

        $this->assertApiError($response, 409, 'SKU_TAKEN');
    }

    public function test_admin_product_create_validates_contract_shapes(): void
    {
        $this->actingAsAdmin();
        $category = Category::factory()->create();
        $archivedCategory = Category::factory()->archived()->create();

        $cases = [
            'price as JSON number' => [['price' => 49.9], null, 'VALIDATION_ERROR'],
            'price with one fraction digit' => [['price' => '49.9'], null, 'VALIDATION_ERROR'],
            'price above the Money pattern' => [['price' => '1234567890.00'], null, 'VALIDATION_ERROR'],
            'archived status not assignable' => [['status' => 'archived'], null, 'VALIDATION_ERROR'],
            'more than eight images' => [['imageUrls' => array_fill(0, 9, 'https://images.example/x.jpg')], null, 'VALIDATION_ERROR'],
            'unknown top-level field' => [['stockQuantity' => 5], null, 'VALIDATION_ERROR'],
            'missing description key' => [[], 'description', 'VALIDATION_ERROR'],
            'archived categoryId' => [['categoryId' => $archivedCategory->id], null, 'CATEGORY_NOT_FOUND'],
            'empty name' => [['name' => ''], null, 'VALIDATION_ERROR'],
            'oversized sku' => [['sku' => str_repeat('A', 65)], null, 'VALIDATION_ERROR'],
        ];

        foreach ($cases as $label => [$overrides, $removeKey, $code]) {
            $payload = $this->payload($category->id, $overrides);

            if ($removeKey !== null) {
                unset($payload[$removeKey]);
            }

            $response = $this->adminJson('POST', '/api/v1/admin/products', $payload);

            $response->assertStatus(422);
            $this->assertSame($code, $response->json('error.code'), $label);
        }

        // Rejected payloads must never reach the database.
        $this->assertDatabaseCount('products', 0);
    }

    public function test_admin_product_show_returns_any_status_and_404s_for_missing(): void
    {
        $this->actingAsAdmin();
        $archived = Product::factory()->archived()->create(['sku' => null]);

        $this->adminJson('GET', "/api/v1/admin/products/{$archived->id}")
            ->assertOk()
            ->assertJsonPath('data.status', 'archived')
            ->assertJsonPath('data.sku', null);

        $this->adminJson('GET', '/api/v1/admin/products/999999')->assertStatus(404);
        $this->adminJson('GET', '/api/v1/admin/products/not-a-number')->assertStatus(404);
    }

    public function test_admin_product_update_full_replaces_editable_fields_and_images(): void
    {
        $this->actingAsAdmin();
        $newCategory = Category::factory()->create(['name' => 'Lighting']);
        $product = Product::factory()->create([
            'name' => 'Old Name',
            'price' => '10.00',
            'sku' => 'OLD-1',
        ]);
        Inventory::factory()->create(['product_id' => $product->id, 'quantity' => 7]);
        ProductImage::query()->create([
            'product_id' => $product->id,
            'url' => 'https://images.example/old.jpg',
            'sort_order' => 0,
        ]);

        $response = $this->adminJson('PUT', "/api/v1/admin/products/{$product->id}", [
            'name' => 'Fresh Name',
            'description' => 'Rewritten description.',
            'price' => '88.00',
            'sku' => 'NEW-1',
            'categoryId' => $newCategory->id,
            'status' => 'inactive',
            'imageUrls' => [
                'https://images.example/new-a.jpg',
                'https://images.example/new-b.jpg',
            ],
        ]);

        $response->assertOk()
            ->assertJsonPath('data.name', 'Fresh Name')
            ->assertJsonPath('data.price', '88.00')
            ->assertJsonPath('data.sku', 'NEW-1')
            ->assertJsonPath('data.categoryName', 'Lighting')
            ->assertJsonPath('data.status', 'inactive')
            ->assertJsonCount(2, 'data.images')
            // Stock is not part of the payload and must not change.
            ->assertJsonPath('data.stockQuantity', 7)
            ->assertJsonPath('data.inStock', true);

        $this->assertDatabaseHas('products', ['id' => $product->id, 'name' => 'Fresh Name', 'price' => '88.00']);
        $this->assertDatabaseMissing('product_images', ['product_id' => $product->id, 'url' => 'https://images.example/old.jpg']);
        $this->assertDatabaseHas('inventories', ['product_id' => $product->id, 'quantity' => 7]);

        // Full replacement: an omitted optional sku clears to null.
        $this->adminJson('PUT', "/api/v1/admin/products/{$product->id}", [
            'name' => 'Fresh Name',
            'description' => 'Rewritten description.',
            'price' => '88.00',
            'categoryId' => $newCategory->id,
            'status' => 'inactive',
            'imageUrls' => ['https://images.example/new-a.jpg'],
        ])->assertOk()->assertJsonPath('data.sku', null);

        $this->assertDatabaseHas('products', ['id' => $product->id, 'sku' => null]);
    }

    public function test_admin_product_update_rejects_duplicate_sku_of_another_product(): void
    {
        $this->actingAsAdmin();
        Product::factory()->create(['sku' => 'TAKEN-1']);
        $product = Product::factory()->create(['sku' => 'OWN-1']);

        $this->assertApiError(
            $this->adminJson('PUT', "/api/v1/admin/products/{$product->id}", $this->payload($product->category_id, ['sku' => 'TAKEN-1'])),
            409,
            'SKU_TAKEN',
        );

        // Keeping its own SKU must not trip the uniqueness check.
        $this->adminJson('PUT', "/api/v1/admin/products/{$product->id}", [
            'name' => $product->name,
            'description' => $product->description,
            'price' => '19.99',
            'sku' => 'OWN-1',
            'categoryId' => $product->category_id,
            'status' => 'active',
            'imageUrls' => [],
        ])->assertOk()->assertJsonPath('data.sku', 'OWN-1');
    }

    public function test_admin_product_archive_hides_product_from_public_catalog(): void
    {
        $this->actingAsAdmin();
        $product = Product::factory()->create(['name' => 'Retire Me']);

        // Visible while active.
        $this->getJson('/api/v1/products')
            ->assertOk()
            ->assertJsonFragment(['id' => $product->id]);

        $this->adminJson('POST', "/api/v1/admin/products/{$product->id}/archive")
            ->assertOk()
            ->assertJsonPath('data.status', 'archived');

        // Row survives (order snapshots), public reads exclude it.
        $this->assertDatabaseHas('products', ['id' => $product->id, 'status' => 'archived']);
        $this->getJson('/api/v1/products')
            ->assertOk()
            ->assertJsonMissing(['id' => $product->id]);
        $this->getJson("/api/v1/products/{$product->id}")->assertStatus(404);

        // Re-archiving is rejected; missing ids answer 404.
        $this->assertApiError(
            $this->adminJson('POST', "/api/v1/admin/products/{$product->id}/archive"),
            409,
            'INVALID_PRODUCT_STATE',
        );
        $this->adminJson('POST', '/api/v1/admin/products/999999/archive')->assertStatus(404);
    }

    /**
     * Contract-shaped AdminProductInput body.
     *
     * @return array<string, mixed>
     */
    private function payload(int $categoryId, array $overrides = []): array
    {
        return array_merge([
            'name' => 'Nimbus Desk Light',
            'description' => 'Warm dimmable desk light with USB-C.',
            'price' => '49.90',
            'sku' => 'DL-9001',
            'categoryId' => $categoryId,
            'status' => 'active',
            'imageUrls' => [
                'https://images.example/nimbus/1.jpg',
                'https://images.example/nimbus/2.jpg',
            ],
        ], $overrides);
    }
}
