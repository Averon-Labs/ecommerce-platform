<?php

namespace Tests\Feature\Admin;

use App\Models\Category;
use App\Models\Product;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * CAT-003: admin category list/create/update/archive/restore per
 * docs/openapi.yaml — productCount semantics, the OPEN-2 archive guard
 * (409 CATEGORY_NOT_EMPTY with the blocking count), 409 name codes, and
 * the public catalog reflecting persisted admin changes.
 */
class AdminCategoryTest extends TestCase
{
    use InteractsWithAdminSession, RefreshDatabase;

    public function test_admin_category_list_includes_archived_with_product_counts_and_filters(): void
    {
        $this->actingAsAdmin();

        $populated = Category::factory()->create(['name' => 'Audio Gear', 'sort_order' => 10]);
        Product::factory()->create(['category_id' => $populated->id]);
        Product::factory()->inactive()->create(['category_id' => $populated->id]);
        Product::factory()->archived()->create(['category_id' => $populated->id]);

        Category::factory()->create(['name' => 'Empty Shelves', 'sort_order' => 20]);
        Category::factory()->archived()->create(['name' => 'Retired Line', 'sort_order' => 30]);

        $response = $this->adminJson('GET', '/api/v1/admin/categories');

        $response->assertOk()
            ->assertJsonStructure(['data' => [[
                'id', 'name', 'sortOrder', 'status', 'productCount',
            ]]])
            ->assertJsonCount(3, 'data');

        // Ordered by sortOrder; productCount excludes archived products
        // (one active + one inactive product = 2).
        $this->assertSame(
            ['Audio Gear', 'Empty Shelves', 'Retired Line'],
            collect($response->json('data'))->pluck('name')->all(),
        );
        $this->assertSame(2, collect($response->json('data'))->firstWhere('name', 'Audio Gear')['productCount']);
        $this->assertSame(0, collect($response->json('data'))->firstWhere('name', 'Retired Line')['productCount']);

        $this->adminJson('GET', '/api/v1/admin/categories?status=archived')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.name', 'Retired Line');

        $this->adminJson('GET', '/api/v1/admin/categories?status=bogus')
            ->assertStatus(422)
            ->assertJsonPath('error.code', 'VALIDATION_ERROR');
    }

    public function test_admin_category_create_persists_an_active_category(): void
    {
        $this->actingAsAdmin();

        $response = $this->adminJson('POST', '/api/v1/admin/categories', [
            'name' => 'Kitchen',
            'sortOrder' => 5,
        ]);

        $response->assertCreated()
            ->assertJsonPath('data.name', 'Kitchen')
            ->assertJsonPath('data.sortOrder', 5)
            ->assertJsonPath('data.status', 'active')
            ->assertJsonPath('data.productCount', 0);

        $this->assertDatabaseHas('categories', [
            'id' => $response->json('data.id'),
            'name' => 'Kitchen',
            'status' => 'active',
        ]);
    }

    public function test_admin_category_create_validates_shapes_and_rejects_duplicate_names(): void
    {
        $this->actingAsAdmin();

        $cases = [
            'empty name' => [['name' => '', 'sortOrder' => 1]],
            'name over 80 characters' => [['name' => str_repeat('a', 81), 'sortOrder' => 1]],
            'negative sortOrder' => [['name' => 'Valid', 'sortOrder' => -1]],
            'sortOrder above 9999' => [['name' => 'Valid', 'sortOrder' => 10000]],
            'non-integer sortOrder' => [['name' => 'Valid', 'sortOrder' => 'high']],
            'missing sortOrder key' => [['name' => 'Valid']],
            'unknown status field' => [['name' => 'Valid', 'sortOrder' => 1, 'status' => 'archived']],
        ];

        foreach ($cases as $label => $payload) {
            $response = $this->adminJson('POST', '/api/v1/admin/categories', $payload);

            $response->assertStatus(422);
            $this->assertSame('VALIDATION_ERROR', $response->json('error.code'), $label);
        }

        $this->assertDatabaseCount('categories', 0);

        $this->adminJson('POST', '/api/v1/admin/categories', ['name' => 'Basics', 'sortOrder' => 1])
            ->assertCreated();

        // Case-insensitive duplicate (column collation matches the check).
        $this->assertApiError(
            $this->adminJson('POST', '/api/v1/admin/categories', ['name' => 'basics', 'sortOrder' => 2]),
            409,
            'CATEGORY_NAME_TAKEN',
        );

        $this->assertDatabaseCount('categories', 1);
    }

    public function test_admin_category_update_replaces_name_and_sort_order(): void
    {
        $this->actingAsAdmin();
        $category = Category::factory()->create(['name' => 'Original', 'sort_order' => 1]);
        $other = Category::factory()->create(['name' => 'Occupied', 'sort_order' => 2]);

        $this->adminJson('PUT', "/api/v1/admin/categories/{$category->id}", [
            'name' => 'Renamed',
            'sortOrder' => 7,
        ])
            ->assertOk()
            ->assertJsonPath('data.name', 'Renamed')
            ->assertJsonPath('data.sortOrder', 7);

        $this->assertDatabaseHas('categories', ['id' => $category->id, 'name' => 'Renamed', 'sort_order' => 7]);

        $this->assertApiError(
            $this->adminJson('PUT', "/api/v1/admin/categories/{$category->id}", [
                'name' => 'Occupied',
                'sortOrder' => 7,
            ]),
            409,
            'CATEGORY_NAME_TAKEN',
        );

        // Keeping its own name must not trip the uniqueness check.
        $this->adminJson('PUT', "/api/v1/admin/categories/{$category->id}", [
            'name' => 'Renamed',
            'sortOrder' => 8,
        ])->assertOk()->assertJsonPath('data.sortOrder', 8);

        $this->adminJson('PUT', '/api/v1/admin/categories/999999', ['name' => 'Nope', 'sortOrder' => 1])
            ->assertStatus(404);

        $this->assertDatabaseHas('categories', ['id' => $other->id, 'name' => 'Occupied']);
    }

    public function test_admin_category_archive_and_restore_follow_the_open2_lifecycle(): void
    {
        $this->actingAsAdmin();

        $inUse = Category::factory()->create(['name' => 'In Use', 'sort_order' => 1]);
        Product::factory()->create(['category_id' => $inUse->id]);

        // Blocked while it still holds non-archived products; details
        // carry the blocking count.
        $blocked = $this->adminJson('POST', "/api/v1/admin/categories/{$inUse->id}/archive");

        $blocked->assertStatus(409);
        $this->assertSame('CATEGORY_NOT_EMPTY', $blocked->json('error.code'));
        $this->assertSame('products', $blocked->json('error.details.0.field'));
        $this->assertStringContainsString('1', $blocked->json('error.details.0.message'));
        $this->assertDatabaseHas('categories', ['id' => $inUse->id, 'status' => 'active']);

        $empty = Category::factory()->create(['name' => 'Seasonal', 'sort_order' => 2]);

        $this->adminJson('POST', "/api/v1/admin/categories/{$empty->id}/archive")
            ->assertOk()
            ->assertJsonPath('data.status', 'archived');

        // The public catalog excludes archived categories immediately.
        $this->getJson('/api/v1/categories')
            ->assertOk()
            ->assertJsonMissing(['id' => $empty->id]);

        // Re-archiving is rejected.
        $this->assertApiError(
            $this->adminJson('POST', "/api/v1/admin/categories/{$empty->id}/archive"),
            409,
            'INVALID_CATEGORY_STATE',
        );

        $this->adminJson('POST', "/api/v1/admin/categories/{$empty->id}/restore")
            ->assertOk()
            ->assertJsonPath('data.status', 'active');

        $this->getJson('/api/v1/categories')
            ->assertOk()
            ->assertJsonFragment(['id' => $empty->id]);

        // Restoring a non-archived category is rejected.
        $this->assertApiError(
            $this->adminJson('POST', "/api/v1/admin/categories/{$empty->id}/restore"),
            409,
            'INVALID_CATEGORY_STATE',
        );

        $this->adminJson('POST', '/api/v1/admin/categories/999999/archive')->assertStatus(404);
        $this->adminJson('POST', '/api/v1/admin/categories/999999/restore')->assertStatus(404);
    }
}
