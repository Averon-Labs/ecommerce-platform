<?php

namespace App\Http\Controllers;

use App\Exceptions\ApiException;
use App\Http\Requests\CatalogQueryRequest;
use App\Http\Resources\ProductDetailResource;
use App\Http\Resources\ProductSummaryResource;
use App\Models\Category;
use App\Models\Product;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Http\JsonResponse;

/**
 * docs/openapi.yaml GET /products and GET /products/{productId}:
 * public catalog reads over active products only. Money stays a
 * two-fraction-digit decimal string (fixed precision).
 */
class ProductController extends Controller
{
    /**
     * Search, filter, sort, and paginate the active catalog.
     */
    public function index(CatalogQueryRequest $request): JsonResponse
    {
        $data = $request->validated();
        $perPage = (int) ($data['perPage'] ?? 20);
        $sort = $data['sort'] ?? 'newest';

        $query = Product::query()
            ->where('status', 'active')
            ->with(['category', 'inventory', 'images']);

        if (isset($data['q'])) {
            // MySQL's *_ci collations make LIKE case-insensitive; escape the
            // LIKE wildcards so user input stays literal.
            $needle = '%'.addcslashes($data['q'], '%_\\').'%';

            $query->where(function (Builder $builder) use ($needle): void {
                $builder->where('name', 'like', $needle)
                    ->orWhere('description', 'like', $needle);
            });
        }

        if (isset($data['categoryId'])) {
            $categoryExists = Category::query()
                ->whereKey($data['categoryId'])
                ->where('status', 'active')
                ->exists();

            if (! $categoryExists) {
                throw new ApiException(
                    422,
                    'CATEGORY_NOT_FOUND',
                    'The referenced category does not exist or is not active.',
                    [['field' => 'categoryId', 'message' => 'Unknown or archived category.']],
                );
            }

            $query->where('category_id', $data['categoryId']);
        }

        if ($request->boolean('inStock')) {
            $query->whereHas('inventory', fn (Builder $builder) => $builder->where('quantity', '>', 0));
        }

        match ($sort) {
            'price_asc' => $query->orderBy('price')->orderBy('id'),
            'price_desc' => $query->orderByDesc('price')->orderBy('id'),
            'name_asc' => $query->orderBy('name')->orderBy('id'),
            default => $query->orderByDesc('created_at')->orderByDesc('id'),
        };

        $paginator = $query->paginate($perPage);
        $totalItems = $paginator->total();

        return response()->json([
            'data' => ProductSummaryResource::collection($paginator->items()),
            'pagination' => [
                'currentPage' => $paginator->currentPage(),
                'perPage' => $paginator->perPage(),
                'totalItems' => $totalItems,
                // The contract allows 0 (Pagination.totalPages minimum: 0);
                // Laravel's lastPage() would report 1 for an empty result.
                'totalPages' => $totalItems === 0 ? 0 : (int) ceil($totalItems / $paginator->perPage()),
            ],
        ]);
    }

    /**
     * Product detail with the ordered image set and exact stock.
     */
    public function show(int $productId): JsonResponse
    {
        $product = Product::query()
            ->whereKey($productId)
            ->where('status', 'active')
            ->with(['category', 'inventory', 'images'])
            ->first();

        if ($product === null) {
            throw new ApiException(404, 'NOT_FOUND', 'The requested product was not found.');
        }

        return response()->json([
            'data' => new ProductDetailResource($product),
        ]);
    }
}
