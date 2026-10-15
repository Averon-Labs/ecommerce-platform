<?php

namespace Tests\Feature\Catalog;

use App\Models\Category;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class CatalogCategoryTest extends TestCase
{
    use RefreshDatabase;

    public function test_categories_returns_active_categories_ordered_by_sort_order_then_name(): void
    {
        Category::factory()->create(['name' => 'Zeta', 'sort_order' => 1]);
        Category::factory()->create(['name' => 'Alpha', 'sort_order' => 1]);
        Category::factory()->create(['name' => 'Beta', 'sort_order' => 0]);

        $response = $this->getJson('/api/v1/categories');

        $response->assertOk()
            ->assertJsonStructure(['data' => [['id', 'name', 'sortOrder', 'status']]])
            ->assertJsonPath('data.0.name', 'Beta')
            ->assertJsonPath('data.1.name', 'Alpha')
            ->assertJsonPath('data.2.name', 'Zeta')
            ->assertJsonPath('data.0.sortOrder', 0)
            ->assertJsonPath('data.0.status', 'active');
    }

    public function test_categories_never_returns_archived_categories(): void
    {
        Category::factory()->create(['name' => 'Active One']);
        Category::factory()->archived()->create(['name' => 'Archived One']);

        $response = $this->getJson('/api/v1/categories');

        $response->assertOk();
        $this->assertSame(['Active One'], array_column($response->json('data'), 'name'));
    }

    public function test_categories_returns_empty_list_when_no_active_categories(): void
    {
        Category::factory()->archived()->count(2)->create();

        $this->getJson('/api/v1/categories')
            ->assertOk()
            ->assertJson(['data' => []]);
    }
}
