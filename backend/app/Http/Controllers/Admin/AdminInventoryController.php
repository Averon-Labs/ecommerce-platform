<?php

namespace App\Http\Controllers\Admin;

use App\Exceptions\ApiException;
use App\Http\Controllers\Controller;
use App\Http\Requests\AdminInventoryQueryRequest;
use App\Http\Requests\AdminInventoryUpdateRequest;
use App\Http\Resources\InventoryRowResource;
use App\Models\Inventory;
use App\Models\Product;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Http\JsonResponse;

/**
 * docs/openapi.yaml Admin inventory operations (CAT-003): list stock
 * rows and set the absolute quantity for one product. The client always
 * sends the resulting value (never a delta), so a retry cannot
 * double-apply; zero is valid and means out of stock. A product without
 * an inventory row reports quantity 0.
 */
class AdminInventoryController extends Controller
{
    /**
     * Paged stock rows across all products (any status).
     */
    public function index(AdminInventoryQueryRequest $request): JsonResponse
    {
        $data = $request->validated();
        $perPage = (int) ($data['perPage'] ?? 20);

        $query = Product::query()->with('inventory');

        if (isset($data['q'])) {
            $needle = '%'.addcslashes($data['q'], '%_\\').'%';

            $query->where(function (Builder $builder) use ($needle): void {
                $builder->where('name', 'like', $needle)
                    ->orWhere('sku', 'like', $needle);
            });
        }

        if (isset($data['inStock'])) {
            if ($data['inStock'] === true) {
                $query->whereHas('inventory', fn (Builder $builder) => $builder->where('quantity', '>', 0));
            } else {
                $query->where(function (Builder $builder): void {
                    $builder->whereDoesntHave('inventory')
                        ->orWhereHas('inventory', fn (Builder $inner) => $inner->where('quantity', 0));
                });
            }
        }

        $query->orderByDesc('created_at')->orderByDesc('id');

        $paginator = $query->paginate($perPage);
        $totalItems = $paginator->total();

        return response()->json([
            'data' => InventoryRowResource::collection($paginator->items()),
            'pagination' => [
                'currentPage' => $paginator->currentPage(),
                'perPage' => $paginator->perPage(),
                'totalItems' => $totalItems,
                'totalPages' => $totalItems === 0 ? 0 : (int) ceil($totalItems / $paginator->perPage()),
            ],
        ]);
    }

    /**
     * Set the absolute stock quantity (creates the inventory row when
     * the product has none yet). No client-supplied total reaches the
     * database without passing the 0..1000000 integer validation above.
     */
    public function update(int $productId, AdminInventoryUpdateRequest $request): JsonResponse
    {
        $product = Product::query()->find($productId);

        if ($product === null) {
            throw new ApiException(404, 'NOT_FOUND', 'The requested product was not found.');
        }

        Inventory::query()->updateOrCreate(
            ['product_id' => $product->id],
            ['quantity' => $request->validated('quantity')],
        );

        return response()->json([
            'data' => new InventoryRowResource($product->fresh('inventory')),
        ]);
    }
}
