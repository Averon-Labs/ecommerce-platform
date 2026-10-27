<?php

namespace Tests\Feature\Admin;

use App\Models\Inventory;
use App\Models\Product;
use App\Models\ProductImage;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Storage;
use Illuminate\Testing\TestResponse;
use Tests\TestCase;

/**
 * CAT-003 image file handling: upload through POST
 * /admin/products/{productId}/images, deletion through the dedicated image
 * operation, product deletion removing its stored files, and the public read
 * of a stored file. Files live on the (faked) product-images disk, so no test
 * touches real storage.
 */
class AdminProductImageTest extends TestCase
{
    use InteractsWithAdminSession, RefreshDatabase;

    /** Minimal valid 1x1 PNG; fileinfo detects it from the real bytes. */
    private const PNG_BYTES = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==';

    protected function setUp(): void
    {
        parent::setUp();

        Storage::fake('product-images');
    }

    public function test_product_image_upload_stores_the_file_and_appends_it_to_the_set(): void
    {
        $this->actingAsAdmin();
        $product = Product::factory()->create();

        $response = $this->upload($product->id, $this->png('lamp.png'));

        $response->assertCreated()
            ->assertJsonStructure(['data' => ['id', 'url', 'sortOrder']])
            ->assertJsonPath('data.sortOrder', 0);

        $url = (string) $response->json('data.url');
        $filename = basename($url);

        $this->assertStringContainsString('/api/v1/media/product-images/'.$product->id.'/', $url);
        $this->assertDatabaseHas('product_images', [
            'product_id' => $product->id,
            'url' => $url,
            'sort_order' => 0,
        ]);

        // The stored file is readable through the public media operation.
        Storage::disk('product-images')->assertExists($product->id.'/'.$filename);
        $this->get('/api/v1/media/product-images/'.$product->id.'/'.$filename)
            ->assertOk()
            ->assertHeader('Content-Type', 'image/png');
    }

    public function test_product_image_upload_requires_a_valid_image_file(): void
    {
        $this->actingAsAdmin();
        $product = Product::factory()->create();

        // A script renamed to .jpg: the content type decides, not the name.
        $fake = $this->tempFile('evil.jpg', "<?php echo 'not an image';");

        $this->assertApiError($this->upload($product->id, $fake), 422, 'VALIDATION_ERROR');
        $this->assertDatabaseMissing('product_images', ['product_id' => $product->id]);

        // Missing file field is reported against the image field.
        $this->assertApiError(
            $this->post(
                "/api/v1/admin/products/{$product->id}/images",
                [],
                $this->requestHeaders(),
            ),
            422,
            'VALIDATION_ERROR',
        );
    }

    public function test_product_image_upload_requires_an_administrator(): void
    {
        $product = Product::factory()->create();

        $this->postJson("/api/v1/admin/products/{$product->id}/images")
            ->assertStatus(401);

        $token = $this->registerCustomer()[1];

        // Contract: every non-admin credential on an admin route answers
        // 401 UNAUTHENTICATED, never a field-level validation error.
        $this->withToken($token)
            ->postJson("/api/v1/admin/products/{$product->id}/images")
            ->assertStatus(401)
            ->assertJsonPath('error.code', 'UNAUTHENTICATED');

        $this->assertDatabaseCount('product_images', 0);
    }

    public function test_product_image_upload_enforces_the_five_megabyte_ceiling(): void
    {
        $this->actingAsAdmin();
        $product = Product::factory()->create();

        $oversized = $this->tempFile(
            'huge.png',
            base64_decode(self::PNG_BYTES).str_repeat("\0", (6 * 1024 * 1024)),
        );

        $this->assertApiError($this->upload($product->id, $oversized), 422, 'VALIDATION_ERROR');
        $this->assertDatabaseMissing('product_images', ['product_id' => $product->id]);
        $this->assertCount(0, Storage::disk('product-images')->allFiles(''));
    }

    public function test_product_image_upload_is_capped_at_eight_images(): void
    {
        $this->actingAsAdmin();
        $product = Product::factory()->create();

        foreach (range(0, 7) as $index) {
            ProductImage::query()->create([
                'product_id' => $product->id,
                'url' => 'https://images.example/seed/'.$index.'.jpg',
                'sort_order' => $index,
            ]);
        }

        $this->assertApiError($this->upload($product->id, $this->png('ninth.png')), 409, 'PRODUCT_IMAGE_LIMIT');
        $this->assertDatabaseCount('product_images', 8);
    }

    public function test_product_image_delete_removes_the_row_and_the_stored_file(): void
    {
        $this->actingAsAdmin();
        $product = Product::factory()->create();

        $imageId = $this->upload($product->id, $this->png('first.png'))->json('data.id');
        $url = $this->upload($product->id, $this->png('second.png'))->json('data.url');
        $filename = basename((string) $url);

        $this->adminJson('DELETE', "/api/v1/admin/products/{$product->id}/images/{$imageId}")
            ->assertNoContent();

        $this->assertDatabaseMissing('product_images', ['id' => $imageId]);
        $this->assertDatabaseCount('product_images', 1);

        // Deleting the row of a stored file also removes it from the server.
        $secondId = ProductImage::query()->where('product_id', $product->id)->value('id');
        $this->adminJson('DELETE', "/api/v1/admin/products/{$product->id}/images/{$secondId}")
            ->assertNoContent();

        Storage::disk('product-images')->assertMissing($product->id.'/'.$filename);
        $this->get('/api/v1/media/product-images/'.$product->id.'/'.$filename)->assertStatus(404);

        // Unknown ids answer the shared not-found envelope.
        $this->assertApiError(
            $this->adminJson('DELETE', "/api/v1/admin/products/{$product->id}/images/999999"),
            404,
            'NOT_FOUND',
        );
        $this->assertApiError(
            $this->adminJson('DELETE', "/api/v1/admin/products/999999/images/{$secondId}"),
            404,
            'NOT_FOUND',
        );
    }

    public function test_product_image_delete_keeps_seeded_external_urls_untouched_on_disk(): void
    {
        $this->actingAsAdmin();
        $product = Product::factory()->create();

        $image = ProductImage::query()->create([
            'product_id' => $product->id,
            'url' => 'https://images.example/seed/external.jpg',
            'sort_order' => 0,
        ]);

        $this->adminJson('DELETE', "/api/v1/admin/products/{$product->id}/images/{$image->id}")
            ->assertNoContent();

        $this->assertDatabaseMissing('product_images', ['id' => $image->id]);
        // No local file ever existed for an external URL; the disk is untouched.
        $this->assertCount(0, Storage::disk('product-images')->allFiles(''));
    }

    public function test_product_delete_removes_the_product_its_rows_and_its_stored_files(): void
    {
        $this->actingAsAdmin();
        $product = Product::factory()->create();
        Inventory::factory()->create(['product_id' => $product->id, 'quantity' => 4]);

        $url = $this->upload($product->id, $this->png('cover.png'))->json('data.url');
        $filename = basename((string) $url);

        // A cart line referencing the product must go with it.
        [$customerId] = $this->registerCustomer();
        $cartId = DB::table('carts')->insertGetId([
            'user_id' => $customerId,
            'created_at' => now(),
            'updated_at' => now(),
        ]);
        DB::table('cart_items')->insert([
            'cart_id' => $cartId,
            'product_id' => $product->id,
            'quantity' => 1,
            'unit_price' => $product->price,
            'added_at' => now(),
        ]);

        $this->adminJson('DELETE', "/api/v1/admin/products/{$product->id}")->assertNoContent();

        $this->assertDatabaseMissing('products', ['id' => $product->id]);
        $this->assertDatabaseMissing('product_images', ['product_id' => $product->id]);
        $this->assertDatabaseMissing('inventories', ['product_id' => $product->id]);
        $this->assertDatabaseMissing('cart_items', ['product_id' => $product->id]);
        Storage::disk('product-images')->assertMissing($product->id.'/'.$filename);
        $this->get('/api/v1/media/product-images/'.$product->id.'/'.$filename)->assertStatus(404);
    }

    public function test_product_delete_is_refused_while_an_order_references_the_product(): void
    {
        $this->actingAsAdmin();
        $product = Product::factory()->create();
        $url = $this->upload($product->id, $this->png('keep.png'))->json('data.url');
        $filename = basename((string) $url);

        $this->seedOrderFor($product);

        $this->assertApiError(
            $this->adminJson('DELETE', "/api/v1/admin/products/{$product->id}"),
            409,
            'PRODUCT_HAS_ORDERS',
        );

        // Nothing was removed: row, image row, and file all survive.
        $this->assertDatabaseHas('products', ['id' => $product->id]);
        $this->assertDatabaseCount('product_images', 1);
        Storage::disk('product-images')->assertExists($product->id.'/'.$filename);
    }

    public function test_product_update_deletes_files_that_left_the_image_list(): void
    {
        $this->actingAsAdmin();
        $product = Product::factory()->create();

        $keptUrl = $this->upload($product->id, $this->png('kept.png'))->json('data.url');
        $droppedUrl = $this->upload($product->id, $this->png('dropped.png'))->json('data.url');

        $this->adminJson('PUT', "/api/v1/admin/products/{$product->id}", [
            'name' => $product->name,
            'description' => $product->description,
            'price' => $product->price,
            'sku' => $product->sku,
            'categoryId' => $product->category_id,
            'status' => $product->status,
            'imageUrls' => [$keptUrl, 'https://images.example/seed/external.jpg'],
        ])->assertOk();

        Storage::disk('product-images')->assertExists($product->id.'/'.basename((string) $keptUrl));
        Storage::disk('product-images')->assertMissing($product->id.'/'.basename((string) $droppedUrl));
        $this->assertDatabaseMissing('product_images', ['url' => $droppedUrl]);
        $this->assertDatabaseHas('product_images', ['url' => 'https://images.example/seed/external.jpg']);
    }

    public function test_stored_image_reads_reject_a_path_outside_the_storage_directory(): void
    {
        // ProductId must be numeric — path-traversal attempts that do not pass
        // the route constraint return 404 before any controller logic runs.
        $this->get('/api/v1/media/product-images/..%2F..%2F.env')->assertStatus(404);

        // A valid numeric productId with a missing file still returns NOT_FOUND.
        $this->assertApiError(
            $this->getJson('/api/v1/media/product-images/1/missing.png'),
            404,
            'NOT_FOUND',
        );
    }

    /**
     * POST one multipart image upload as the signed-in administrator.
     */
    private function upload(int $productId, UploadedFile $file): TestResponse
    {
        return $this->post(
            "/api/v1/admin/products/{$productId}/images",
            ['image' => $file],
            $this->requestHeaders(),
        );
    }

    /**
     * Stateful admin headers plus the JSON accept header.
     *
     * @return array<string, string>
     */
    private function requestHeaders(): array
    {
        return array_merge($this->statefulHeaders(), ['Accept' => 'application/json']);
    }

    /** A real PNG upload, decoded from the fixture bytes. */
    private function png(string $name): UploadedFile
    {
        return $this->tempFile($name, base64_decode(self::PNG_BYTES));
    }

    /**
     * A real file on disk wrapped as an UploadedFile with test semantics, so
     * validation reads the same content types a live request would.
     */
    private function tempFile(string $name, string $contents): UploadedFile
    {
        $path = tempnam(sys_get_temp_dir(), 'eshop-img');
        file_put_contents($path, $contents);

        return new UploadedFile($path, $name, null, null, true);
    }

    /**
     * A paid order referencing the product, so deletion must be refused.
     */
    private function seedOrderFor(Product $product): void
    {
        $userId = User::factory()->create()->id;

        $orderId = DB::table('orders')->insertGetId([
            'user_id' => $userId,
            'reference' => 'ESV-000001',
            'status' => 'confirmed',
            'subtotal_amount' => '10.00',
            'shipping_amount' => '5.00',
            'total_amount' => '15.00',
            'shipping_address_snapshot' => json_encode([
                'recipientName' => 'Sara Nouri',
                'line1' => '14 Maple Street',
                'city' => 'Seattle',
                'region' => 'WA',
                'postalCode' => '98101',
                'country' => 'US',
            ]),
            'timeline' => json_encode([['status' => 'pending_payment', 'at' => now()->toIso8601String()]]),
            'created_at' => now(),
            'updated_at' => now(),
        ]);

        DB::table('order_items')->insert([
            'order_id' => $orderId,
            'product_id' => $product->id,
            'product_name' => $product->name,
            'quantity' => 1,
            'unit_price' => $product->price,
            'total_price' => $product->price,
        ]);
    }
}
