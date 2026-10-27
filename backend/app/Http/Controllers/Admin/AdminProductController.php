<?php

namespace App\Http\Controllers\Admin;

use App\Exceptions\ApiException;
use App\Http\Controllers\Controller;
use App\Http\Requests\AdminProductImageRequest;
use App\Http\Requests\AdminProductInputRequest;
use App\Http\Requests\AdminProductQueryRequest;
use App\Http\Resources\AdminProductResource;
use App\Http\Resources\ProductImageResource;
use App\Models\CartItem;
use App\Models\Category;
use App\Models\Inventory;
use App\Models\OrderItem;
use App\Models\Product;
use App\Models\ProductImage;
use App\Services\ProductImageStorage;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\QueryException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Response;
use Illuminate\Support\Facades\DB;

/**
 * docs/openapi.yaml Admin catalog product operations (CAT-003): list
 * every status, create, read, full-replace update, archive, delete, and
 * the upload/delete of its image files. Stock is deliberately absent — it
 * is owned by PATCH /admin/inventory/{productId}. Products are hard-deleted
 * only while no order references them; otherwise the answer is
 * 409 PRODUCT_HAS_ORDERS and archiving keeps the order snapshots intact.
 */
class AdminProductController extends Controller
{
    /** Maximum images per product (contract: AdminProductInput maxItems). */
    public const MAX_IMAGES = 8;

    public function __construct(private readonly ProductImageStorage $imageStorage) {}

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

    /**
     * Upload one image file for a product and append it to the ordered set.
     * The file is stored on the server; the returned URL is what the catalog
     * and the customer app load.
     */
    public function storeImage(AdminProductImageRequest $request, int $productId): JsonResponse
    {
        $product = $this->findProduct($productId);
        $sortOrder = ProductImage::query()->where('product_id', $product->id)->count();

        if ($sortOrder >= self::MAX_IMAGES) {
            throw new ApiException(
                409,
                'PRODUCT_IMAGE_LIMIT',
                'A product can hold at most '.self::MAX_IMAGES.' images. Remove one before adding another.',
            );
        }

        $url = $this->imageStorage->store($request->file('image'), $product->id);

        if ($url === null) {
            throw new ApiException(500, 'INTERNAL_ERROR', 'The image could not be stored.');
        }

        $image = ProductImage::query()->create([
            'product_id' => $product->id,
            'url' => $url,
            'sort_order' => $sortOrder,
        ]);

        return response()->json(['data' => new ProductImageResource($image)], 201);
    }

    /**
     * Remove one image row and its stored file from the server.
     */
    public function deleteImage(int $productId, int $imageId): Response
    {
        $product = $this->findProduct($productId);

        $image = ProductImage::query()
            ->where('product_id', $product->id)
            ->whereKey($imageId)
            ->first();

        if ($image === null) {
            throw new ApiException(404, 'NOT_FOUND', 'The requested image was not found.');
        }

        $url = $image->url;
        $image->delete();
        $this->imageStorage->deleteByUrl($url);

        return response()->noContent();
    }

    /**
     * Delete a product together with its image files, inventory row, and
     * cart lines. Orders own immutable snapshots referencing the product,
     * so a product that appears in any order cannot be deleted (409) and
     * must be archived instead.
     */
    public function destroy(int $productId): Response
    {
        $product = $this->findProduct($productId);

        $hasOrders = OrderItem::query()->where('product_id', $product->id)->exists();

        if ($hasOrders) {
            throw new ApiException(
                409,
                'PRODUCT_HAS_ORDERS',
                'This product belongs to an existing order and cannot be deleted. Archive it instead.',
            );
        }

        $urls = ProductImage::query()->where('product_id', $product->id)->pluck('url')->all();

        // Explicit deletes keep the behaviour identical on every supported
        // database instead of relying on per-engine foreign-key cascades.
        DB::transaction(function () use ($product): void {
            ProductImage::query()->where('product_id', $product->id)->delete();
            Inventory::query()->where('product_id', $product->id)->delete();
            CartItem::query()->where('product_id', $product->id)->delete();
            $product->delete();
        });

        // Only files stored by this server are removed; seeded/external URLs
        // resolve to no local file and are skipped.
        foreach ($urls as $url) {
            $this->imageStorage->deleteByUrl($url);
        }

        return response()->noContent();
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
     * Rows are deleted by product_id — the images relation carries an
     * ORDER BY, which a DELETE statement does not accept on every database.
     * Files that no longer appear in the new list are removed from the
     * server; external URLs are left alone.
     */
    private function replaceImages(Product $product, array $urls): void
    {
        $previousUrls = ProductImage::query()
            ->where('product_id', $product->id)
            ->pluck('url')
            ->all();

        ProductImage::query()->where('product_id', $product->id)->delete();

        $kept = [];

        foreach (array_values($urls) as $index => $url) {
            $kept[] = $url;

            ProductImage::query()->create([
                'product_id' => $product->id,
                'url' => $url,
                'sort_order' => $index,
            ]);
        }

        foreach ($previousUrls as $previousUrl) {
            if (! in_array($previousUrl, $kept, true)) {
                $this->imageStorage->deleteByUrl($previousUrl);
            }
        }
    }
}
