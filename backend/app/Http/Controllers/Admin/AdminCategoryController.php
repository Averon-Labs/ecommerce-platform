<?php

namespace App\Http\Controllers\Admin;

use App\Exceptions\ApiException;
use App\Http\Controllers\Controller;
use App\Http\Requests\AdminCategoryInputRequest;
use App\Http\Requests\AdminCategoryQueryRequest;
use App\Http\Resources\AdminCategoryResource;
use App\Models\Category;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Http\JsonResponse;

/**
 * docs/openapi.yaml Admin catalog category operations (CAT-003): list
 * (archived included), create, full-replace update, archive, restore.
 * OPEN-2 resolution: a category archives only while it holds no
 * non-archived products (409 CATEGORY_NOT_EMPTY reports the blocking
 * count) — nothing is ever hard-deleted.
 */
class AdminCategoryController extends Controller
{
    /**
     * Categories ordered by sortOrder then name; omit status for all.
     */
    public function index(AdminCategoryQueryRequest $request): JsonResponse
    {
        $data = $request->validated();

        $query = $this->categoryQuery();

        if (isset($data['status'])) {
            $query->where('status', $data['status']);
        }

        $query->orderBy('sort_order')->orderBy('name');

        return response()->json([
            'data' => AdminCategoryResource::collection($query->get()),
        ]);
    }

    /**
     * Create an active category (status is not part of CategoryInput).
     */
    public function store(AdminCategoryInputRequest $request): JsonResponse
    {
        $data = $request->validated();
        $this->rejectTakenName($data['name']);

        $category = Category::query()->create([
            'name' => $data['name'],
            'sort_order' => $data['sortOrder'],
            'status' => 'active',
        ]);
        $category->loadCount($this->productCount());

        return response()->json([
            'data' => new AdminCategoryResource($category),
        ], 201);
    }

    /**
     * Full replacement of name and sortOrder.
     */
    public function update(int $categoryId, AdminCategoryInputRequest $request): JsonResponse
    {
        $category = $this->findCategory($categoryId);
        $data = $request->validated();

        $this->rejectTakenName($data['name'], $category->id);

        $category->name = $data['name'];
        $category->sort_order = $data['sortOrder'];
        $category->save();

        return response()->json([
            'data' => new AdminCategoryResource($category),
        ]);
    }

    /**
     * Archive an empty category. State is checked first, then the
     * non-archived product count that guards the reference data.
     */
    public function archive(int $categoryId): JsonResponse
    {
        $category = $this->findCategory($categoryId);

        if ($category->status === 'archived') {
            throw new ApiException(409, 'INVALID_CATEGORY_STATE', 'The category is already archived.');
        }

        $blockingProducts = $category->products()
            ->where('status', '!=', 'archived')
            ->count();

        if ($blockingProducts > 0) {
            throw new ApiException(
                409,
                'CATEGORY_NOT_EMPTY',
                'The category still holds products.',
                [[
                    'field' => 'products',
                    'message' => "{$blockingProducts} product(s) still use this category.",
                ]],
            );
        }

        $category->status = 'archived';
        $category->save();

        return response()->json([
            'data' => new AdminCategoryResource($category),
        ]);
    }

    /**
     * Restore an archived category to active.
     */
    public function restore(int $categoryId): JsonResponse
    {
        $category = $this->findCategory($categoryId);

        if ($category->status !== 'archived') {
            throw new ApiException(409, 'INVALID_CATEGORY_STATE', 'The category is not archived.');
        }

        $category->status = 'active';
        $category->save();

        return response()->json([
            'data' => new AdminCategoryResource($category),
        ]);
    }

    private function findCategory(int $categoryId): Category
    {
        $category = $this->categoryQuery()->find($categoryId);

        if ($category === null) {
            throw new ApiException(404, 'NOT_FOUND', 'The requested category was not found.');
        }

        return $category;
    }

    /**
     * Base query carrying AdminCategory.productCount for every read.
     */
    private function categoryQuery(): Builder
    {
        return Category::query()->withCount($this->productCount());
    }

    /**
     * productCount = products whose status is not archived.
     *
     * @return array<string, \Closure>
     */
    private function productCount(): array
    {
        return [
            'products as productCount' => fn ($builder) => $builder->where('status', '!=', 'archived'),
        ];
    }

    /**
     * Contract: category names are unique (409 CATEGORY_NAME_TAKEN);
     * the column's unique index backs the check.
     */
    private function rejectTakenName(string $name, ?int $exceptCategoryId = null): void
    {
        $taken = Category::query()
            ->where('name', $name)
            ->when($exceptCategoryId !== null, fn ($builder) => $builder->whereKeyNot($exceptCategoryId))
            ->exists();

        if ($taken) {
            throw new ApiException(409, 'CATEGORY_NAME_TAKEN', 'The category name is already used.');
        }
    }
}
