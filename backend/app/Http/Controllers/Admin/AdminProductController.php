<?php

namespace App\Http\Controllers\Admin;

use App\Exceptions\ApiException;
use App\Http\Controllers\Controller;
use App\Http\Requests\AdminProductInputRequest;
use App\Http\Requests\AdminProductQueryRequest;
use App\Http\Resources\AdminProductResource;
use App\Models\Category;
use App\Models\Product;
use App\Models\ProductImage;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\QueryException;
use Illuminate\Http\JsonResponse;

/**
 * docs/openapi.yaml Admin catalog product operations (CAT-003): list
 * every status, create, read, full-replace update, and archive. Stock is
 * deliberately absent — it is owned by PATCH /admin/inventory/{productId}.
 * Products are never hard-deleted, so order snapshots stay intact.
 */
class AdminProductController extends Controller
{
    /**
     * Paged admin product rows across all statuses.
     */
    public function index(AdminProductQueryRequest $request): JsonResponse
    {
        $data = $request->validated();
        $perPage = (int) ($data['perPage'] ?? 20);

        $query = Product::query()->with(['category', 'inventory', 'images']);

        if (isset($data['q'])) {
            // MySQL's *_ci collations make LIKE case-insensitive; escape
            // the LIKE wildcards so user input stays literal.
            $needle = '%'.addcslashes($data['q'], '%_\\').'%';

            $query->where(function (Builder $builder) use ($needle): void {
                $builder->where('name', 'like', $needle)
                    ->orWhere('description', 'like', $needle);
            });
        }

        if (isset($data['categoryId'])) {
            $query->where('category_id', $data['categoryId']);
        }

        if (isset($data['status'])) {
            $query->where('status', $data['status']);
        }

        $query->orderByDesc('created_at')->orderByDesc('id');

        $paginator = $query->paginate($perPage);
        $totalItems = $paginator->total();

        return response()->json([
            'data' => AdminProductResource::collection($paginator->items()),
            'pagination' => [
                'currentPage' => $paginator->currentPage(),
                'perPage' => $paginator->perPage(),
                'totalItems' => $totalItems,
                'totalPages' => $totalItems === 0 ? 0 : (int) ceil($totalItems / $paginator->perPage()),
            ],
        ]);
    }

    /**
     * Create a product and its image rows. No inventory row is created
     * implicitly; stock starts at 0 until PATCH /admin/inventory/{id}.
     */
    public function store(AdminProductInputRequest $request): JsonResponse
    {
        $data = $request->validated();
        $this->rejectTakenSku($data['sku'] ?? null);
        $this->rejectUnknownCategory($data['categoryId'] ?? null);

        try {
            $product = Product::create([
                'name' => $data['name'],
                'description' => $data['description'],
                'price' => $data['price'],
                'sku' => $data['sku'] ?? null,
                'category_id' => $data['categoryId'] ?? null,
                'status' => $data['status'],
            ]);
        } catch (QueryException $e) {
            // Unique-SKU race behind the existence check above.
            if ((string) $e->getCode() === '23000') {
                throw new ApiException(409, 'SKU_TAKEN', 'The SKU is already used by another product.');
            }

            throw $e;
        }

        $this->replaceImages($product, $data['imageUrls']);

        return response()->json([
            'data' => new AdminProductResource($product->fresh(['category', 'inventory', 'images'])),
        ], 201);
    }

    /**
     * Read one product for administration, whatever its status.
     */
    public function show(int $productId): JsonResponse
    {
        return response()->json([
            'data' => new AdminProductResource($this->findProduct($productId)),
        ]);
    }

    /**
     * Full replacement of the editable fields (contract PUT semantics):
     * a nullable key absent from the payload is replaced with null, and
     * the image list is replaced wholesale. Stock is not part of the
     * payload and never changes here. `archived` stays unassignable —
     * status may only become active/inactive through this endpoint.
     */
    public function update(int $productId, AdminProductInputRequest $request): JsonResponse
    {
        $product = $this->findProduct($productId);
        $data = $request->validated();

        $this->rejectTakenSku($data['sku'] ?? null, $product->id);
        $this->rejectUnknownCategory($data['categoryId'] ?? null);

        try {
            $product->name = $data['name'];
            $product->description = $data['description'];
            $product->price = $data['price'];
            $product->sku = $data['sku'] ?? null;
            $product->category_id = $data['categoryId'] ?? null;
            $product->status = $data['status'];
            $product->save();
        } catch (QueryException $e) {
            // Unique-SKU race behind the existence check above.
            if ((string) $e->getCode() === '23000') {
                throw new ApiException(409, 'SKU_TAKEN', 'The SKU is already used by another product.');
            }

            throw $e;
        }

        $this->replaceImages($product, $data['imageUrls']);

        return response()->json([
            'data' => new AdminProductResource($product->fresh(['category', 'inventory', 'images'])),
        ]);
    }

    /**
     * Archive the product (removes it from public listings). Products
     * are never hard-deleted; re-archiving is rejected.
     */
    public function archive(int $productId): JsonResponse
    {
        $product = $this->findProduct($productId);

        if ($product->status === 'archived') {
            throw new ApiException(409, 'INVALID_PRODUCT_STATE', 'The product is already archived.');
        }

        $product->status = 'archived';
        $product->save();

        return response()->json([
            'data' => new AdminProductResource($product->fresh(['category', 'inventory', 'images'])),
        ]);
    }

    private function findProduct(int $productId): Product
    {
        $product = Product::query()
            ->whereKey($productId)
            ->with(['category', 'inventory', 'images'])
            ->first();

        if ($product === null) {
            throw new ApiException(404, 'NOT_FOUND', 'The requested product was not found.');
        }

        return $product;
    }

    /**
     * Contract: SKU is optional and unique when present (409 SKU_TAKEN).
     */
    private function rejectTakenSku(?string $sku, ?int $exceptProductId = null): void
    {
        if ($sku === null) {
            return;
        }

        $taken = Product::query()
            ->where('sku', $sku)
            ->when($exceptProductId !== null, fn (Builder $builder) => $builder->whereKeyNot($exceptProductId))
            ->exists();

        if ($taken) {
            throw new ApiException(409, 'SKU_TAKEN', 'The SKU is already used by another product.');
        }
    }

    /**
     * Contract: categoryId, when provided, must reference an active
     * category (422 CATEGORY_NOT_FOUND — same answer as the public filter).
     */
    private function rejectUnknownCategory(?int $categoryId): void
    {
        if ($categoryId === null) {
            return;
        }

        $exists = Category::query()
            ->whereKey($categoryId)
            ->where('status', 'active')
            ->exists();

        if (! $exists) {
            throw new ApiException(
                422,
                'CATEGORY_NOT_FOUND',
                'The referenced category does not exist or is not active.',
                [['field' => 'categoryId', 'message' => 'Unknown or archived category.']],
            );
        }
    }

    /**
     * Replace the ordered image set (sort_order = position in imageUrls).
     */
    private function replaceImages(Product $product, array $urls): void
    {
        $product->images()->delete();

        foreach (array_values($urls) as $index => $url) {
            ProductImage::query()->create([
                'product_id' => $product->id,
                'url' => $url,
                'sort_order' => $index,
            ]);
        }
    }
}
