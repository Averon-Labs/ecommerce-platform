<?php

namespace App\Http\Controllers;

use App\Exceptions\ApiException;
use App\Services\ProductImageStorage;
use Illuminate\Http\Response;
use Illuminate\Support\Facades\Storage;

/**
 * CAT-003: serves admin-uploaded product images from storage.
 *
 * The files live outside the web root, organised per product in
 * `storage/app/product-images/{productId}/{filename}` subdirectories,
 * so every host serves them through this operation regardless of its
 * document-root layout. Reads are public (product images are catalog
 * data) and are governed by the document-level `global` rate-limit class.
 */
class MediaController extends Controller
{
    public function __construct(private readonly ProductImageStorage $images) {}

    /**
     * GET /media/product-images/{productId}/{filename} — one stored image.
     */
    public function productImage(int $productId, string $filename): Response
    {
        // Route constraint already rejects traversal; keep the same check on
        // the value that reaches the disk so only a plain name is ever read.
        if (! $this->images->isSafeFilename($filename)) {
            throw new ApiException(404, 'NOT_FOUND', 'The requested image was not found.');
        }

        $disk = Storage::disk(ProductImageStorage::DISK);
        $path = $productId.'/'.$filename;

        if (! $disk->exists($path)) {
            throw new ApiException(404, 'NOT_FOUND', 'The requested image was not found.');
        }

        $contents = $disk->get($path);

        if (! is_string($contents)) {
            throw new ApiException(404, 'NOT_FOUND', 'The requested image was not found.');
        }

        return response($contents, 200, [
            'Content-Type' => $this->images->mimeTypeFor($filename),
            'Content-Length' => (string) strlen($contents),
            'Content-Disposition' => 'inline; filename="'.$filename.'"',
            // Images are immutable (generated names), so they are safe to cache.
            'Cache-Control' => 'public, max-age=604800',
        ]);
    }
}
