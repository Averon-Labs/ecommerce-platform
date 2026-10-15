<?php

namespace Tests\Feature\Catalog;

use App\Models\Inventory;
use App\Models\Product;
use App\Models\ProductImage;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class CatalogProductDetailTest extends TestCase
{
    use RefreshDatabase;

    public function test_product_detail_returns_ordered_images_and_exact_stock(): void
    {
        $product = Product::factory()->create([
            'name' => 'Aurora Headphones',
            'description' => 'Over-ear wireless headphones.',
            'sku' => 'AH-9001',
        ]);
        Inventory::factory()->create(['product_id' => $product->id, 'quantity' => 7]);

        // Created out of order; the API must honor sortOrder, then id.
        ProductImage::query()->create(['product_id' => $product->id, 'url' => 'https://img.test/second.jpg', 'sort_order' => 1]);
        ProductImage::query()->create(['product_id' => $product->id, 'url' => 'https://img.test/first.jpg', 'sort_order' => 0]);
        ProductImage::query()->create(['product_id' => $product->id, 'url' => 'https://img.test/third.jpg', 'sort_order' => 2]);

        $response = $this->getJson("/api/v1/products/{$product->id}");

        $response->assertOk()
            ->assertJsonStructure([
                'data' => [
                    'id', 'name', 'price', 'currency', 'categoryId', 'categoryName',
                    'imageUrl', 'inStock', 'status', 'description', 'sku',
                    'stockQuantity', 'images' => [['url', 'sortOrder']],
                ],
            ])
            ->assertJsonPath('data.name', 'Aurora Headphones')
            ->assertJsonPath('data.sku', 'AH-9001')
            ->assertJsonPath('data.stockQuantity', 7)
            ->assertJsonPath('data.inStock', true)
            ->assertJsonPath('data.images.0.url', 'https://img.test/first.jpg')
            ->assertJsonPath('data.images.0.sortOrder', 0)
            ->assertJsonPath('data.images.1.url', 'https://img.test/second.jpg')
            ->assertJsonPath('data.images.2.url', 'https://img.test/third.jpg');
    }

    public function test_product_detail_of_out_of_stock_product_reports_zero_stock(): void
    {
        $product = Product::factory()->create();
        Inventory::factory()->outOfStock()->create(['product_id' => $product->id]);

        $this->getJson("/api/v1/products/{$product->id}")
            ->assertOk()
            ->assertJsonPath('data.stockQuantity', 0)
            ->assertJsonPath('data.inStock', false);
    }

    public function test_product_detail_allows_empty_image_set_and_null_category(): void
    {
        $product = Product::factory()->create(['category_id' => null]);

        $this->getJson("/api/v1/products/{$product->id}")
            ->assertOk()
            ->assertJson(['data' => ['images' => []]])
            ->assertJsonPath('data.categoryId', null)
            ->assertJsonPath('data.categoryName', null)
            ->assertJsonPath('data.imageUrl', null);
    }

    public function test_product_detail_returns404_for_unknown_product(): void
    {
        $this->assertApiError($this->getJson('/api/v1/products/999999'), 404, 'NOT_FOUND');
    }

    public function test_product_detail_returns404_for_non_numeric_id(): void
    {
        // The route only binds numeric ids; anything else never reaches a handler.
        $this->assertApiError($this->getJson('/api/v1/products/not-a-number'), 404, 'NOT_FOUND');
    }

    public function test_product_detail_never_shows_inactive_or_archived_products(): void
    {
        $inactive = Product::factory()->inactive()->create();
        $archived = Product::factory()->archived()->create();

        $this->assertApiError($this->getJson("/api/v1/products/{$inactive->id}"), 404, 'NOT_FOUND');
        $this->assertApiError($this->getJson("/api/v1/products/{$archived->id}"), 404, 'NOT_FOUND');
    }
}
