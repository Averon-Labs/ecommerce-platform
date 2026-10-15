<?php

namespace Tests\Feature\Catalog;

use App\Models\Category;
use App\Models\Inventory;
use App\Models\Product;
use App\Models\ProductImage;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Carbon;
use Tests\TestCase;

class CatalogProductListTest extends TestCase
{
    use RefreshDatabase;

    /**
     * Create a product with inventory and one image, created at a known time.
     */
    private function makeProduct(array $productAttributes = [], array $inventoryAttributes = [], int $imageCount = 1): Product
    {
        Carbon::setTestNow(now()->subMinute());

        $product = Product::factory()->create($productAttributes);

        Carbon::setTestNow();

        Inventory::factory()->create(array_merge(['product_id' => $product->id], $inventoryAttributes));

        ProductImage::factory()->count($imageCount)->create(['product_id' => $product->id]);

        return $product->fresh(['category', 'inventory', 'images']);
    }

    public function test_products_returns_active_products_newest_first_with_pagination(): void
    {
        $older = $this->makeProduct(['name' => 'Older Product']);
        $newer = $this->makeProduct(['name' => 'Newer Product']);

        $response = $this->getJson('/api/v1/products');

        $response->assertOk()
            ->assertJsonStructure([
                'data' => [['id', 'name', 'price', 'currency', 'categoryId', 'categoryName', 'imageUrl', 'inStock', 'status']],
                'pagination' => ['currentPage', 'perPage', 'totalItems', 'totalPages'],
            ])
            ->assertJsonPath('data.0.id', $newer->id)
            ->assertJsonPath('data.1.id', $older->id)
            ->assertJsonPath('data.0.currency', 'USD')
            ->assertJsonPath('data.0.status', 'active')
            ->assertJsonPath('pagination.currentPage', 1)
            ->assertJsonPath('pagination.perPage', 20)
            ->assertJsonPath('pagination.totalItems', 2)
            ->assertJsonPath('pagination.totalPages', 1);
    }

    public function test_products_money_is_a_two_fraction_digit_decimal_string_not_a_number(): void
    {
        $this->makeProduct(['price' => '19.90']);

        $price = $this->getJson('/api/v1/products')->json('data.0.price');

        $this->assertSame('19.90', $price);
        $this->assertIsString($price);
        $this->assertMatchesRegularExpression('/^\d{1,9}\.\d{2}$/', $price);
    }

    public function test_products_summary_exposes_in_stock_flag_but_not_exact_quantity(): void
    {
        $this->makeProduct(['name' => 'Stocked'], ['quantity' => 5]);
        $this->makeProduct(['name' => 'Empty'], ['quantity' => 0], 0);

        $response = $this->getJson('/api/v1/products');

        $byName = collect($response->json('data'))->keyBy('name');

        $this->assertTrue($byName['Stocked']['inStock']);
        $this->assertFalse($byName['Empty']['inStock']);
        $this->assertArrayNotHasKey('stockQuantity', $byName['Stocked']);
    }

    public function test_products_search_matches_name_and_description_case_insensitively(): void
    {
        $this->makeProduct(['name' => 'Wireless Headphones', 'description' => 'Over-ear comfort.']);
        $this->makeProduct(['name' => 'Desk Lamp', 'description' => 'Warm WIRELESS charging pad support.']);
        $this->makeProduct(['name' => 'Notebook', 'description' => 'Plain paper.']);

        $response = $this->getJson('/api/v1/products?q=wireless&sort=name_asc');

        $this->assertSame(
            ['Desk Lamp', 'Wireless Headphones'],
            array_column($response->json('data'), 'name'),
        );
        $this->assertSame(2, $response->json('pagination.totalItems'));
    }

    public function test_products_filters_by_active_category(): void
    {
        $audio = Category::factory()->create(['name' => 'Audio']);
        $desk = Category::factory()->create(['name' => 'Desk']);

        $this->makeProduct(['name' => 'Headphones', 'category_id' => $audio->id]);
        $this->makeProduct(['name' => 'Lamp', 'category_id' => $desk->id]);

        $response = $this->getJson("/api/v1/products?categoryId={$audio->id}");

        $this->assertSame(['Headphones'], array_column($response->json('data'), 'name'));
        $this->assertSame('Audio', $response->json('data.0.categoryName'));
    }

    public function test_products_rejects_unknown_or_archived_category(): void
    {
        $archived = Category::factory()->archived()->create();

        $unknown = $this->getJson('/api/v1/products?categoryId=999999');
        $this->assertApiError($unknown, 422, 'CATEGORY_NOT_FOUND');

        $archivedResponse = $this->getJson("/api/v1/products?categoryId={$archived->id}");
        $this->assertApiError($archivedResponse, 422, 'CATEGORY_NOT_FOUND');
    }

    public function test_products_in_stock_filter_excludes_out_of_stock_products(): void
    {
        $this->makeProduct(['name' => 'Stocked'], ['quantity' => 3]);
        $this->makeProduct(['name' => 'Empty'], ['quantity' => 0]);

        $response = $this->getJson('/api/v1/products?inStock=true');

        $this->assertSame(['Stocked'], array_column($response->json('data'), 'name'));

        // "1" is an accepted boolean spelling and must behave like "true".
        $response = $this->getJson('/api/v1/products?inStock=1');

        $this->assertSame(['Stocked'], array_column($response->json('data'), 'name'));

        // "0"/"false" apply no filter on the public list ("When true, ...").
        $response = $this->getJson('/api/v1/products?inStock=0');

        $this->assertCount(2, $response->json('data'));
    }

    public function test_products_sorts_by_price_ascending_and_descending(): void
    {
        $this->makeProduct(['name' => 'Mid', 'price' => '50.00']);
        $this->makeProduct(['name' => 'High', 'price' => '90.00']);
        $this->makeProduct(['name' => 'Low', 'price' => '10.00']);

        $asc = $this->getJson('/api/v1/products?sort=price_asc');
        $this->assertSame(['Low', 'Mid', 'High'], array_column($asc->json('data'), 'name'));

        $desc = $this->getJson('/api/v1/products?sort=price_desc');
        $this->assertSame(['High', 'Mid', 'Low'], array_column($desc->json('data'), 'name'));
    }

    public function test_products_sorts_by_name_ascending(): void
    {
        $this->makeProduct(['name' => 'Cherry']);
        $this->makeProduct(['name' => 'Apple']);
        $this->makeProduct(['name' => 'Banana']);

        $response = $this->getJson('/api/v1/products?sort=name_asc');

        $this->assertSame(['Apple', 'Banana', 'Cherry'], array_column($response->json('data'), 'name'));
    }

    public function test_products_never_leaks_inactive_or_archived_products(): void
    {
        $this->makeProduct(['name' => 'Visible']);
        $this->makeProduct(['name' => 'Hidden', 'status' => 'inactive']);
        $this->makeProduct(['name' => 'Gone', 'status' => 'archived']);

        $response = $this->getJson('/api/v1/products');

        $this->assertSame(['Visible'], array_column($response->json('data'), 'name'));
        $this->assertSame(1, $response->json('pagination.totalItems'));
    }

    public function test_products_paginates_with_requested_window(): void
    {
        foreach (['A', 'B', 'C', 'D', 'E'] as $name) {
            $this->makeProduct(['name' => $name.' Product']);
        }

        $page2 = $this->getJson('/api/v1/products?sort=name_asc&page=2&perPage=2');

        $page2->assertOk()
            ->assertJsonPath('data.0.name', 'C Product')
            ->assertJsonPath('data.1.name', 'D Product')
            ->assertJsonPath('pagination.currentPage', 2)
            ->assertJsonPath('pagination.perPage', 2)
            ->assertJsonPath('pagination.totalItems', 5)
            ->assertJsonPath('pagination.totalPages', 3);
    }

    public function test_products_returns_empty_page_when_nothing_matches(): void
    {
        $this->makeProduct(['name' => 'Headphones']);

        $response = $this->getJson('/api/v1/products?q=nonexistent-term');

        $response->assertOk()
            ->assertJson(['data' => []])
            ->assertJsonPath('pagination.totalItems', 0)
            ->assertJsonPath('pagination.totalPages', 0);
    }

    public function test_products_rejects_invalid_filter_and_pagination_values(): void
    {
        $this->makeProduct();

        $invalidQueries = [
            'zero page' => 'page=0',
            'negative page' => 'page=-1',
            'non-numeric page' => 'page=abc',
            'zero perPage' => 'perPage=0',
            'perPage above limit' => 'perPage=101',
            'negative categoryId' => 'categoryId=-1',
            'unknown sort' => 'sort=price_up',
            'overlong q' => 'q='.str_repeat('a', 121),
        ];

        foreach ($invalidQueries as $scenario => $query) {
            $response = $this->getJson('/api/v1/products?'.$query);
            $this->assertApiError($response, 422, 'VALIDATION_ERROR');
            $this->assertNotEmpty($response->json('error.details'), "Expected field details for $scenario.");
        }
    }
}
