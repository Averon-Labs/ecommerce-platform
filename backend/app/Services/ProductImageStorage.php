<?php

namespace App\Services;

use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;

/**
 * CAT-003: server-side storage for admin-uploaded product images.
 *
 * Files live on the `product-images` disk (storage/app/product-images),
 * outside the web root, organised per product in
 * `storage/app/product-images/{productId}/{uuid}.{ext}` subdirectories, and
 * served by GET /api/v1/media/product-images/{productId}/{filename}.
 * Serving them through the API keeps one working path on every supported
 * layout: a published public directory as well as the shared-origin layout
 * where only /api/* reaches the front controller.
 *
 * The stored URL is absolute and built from the named media route, so the
 * customer app can load it directly. URLs that do not point into that route
 * (seeded or third-party demo URLs written through `imageUrls`) are never
 * treated as local files and are never deleted from disk.
 */
class ProductImageStorage
{
    /** Filesystem disk holding the uploaded files (config/filesystems.php). */
    public const DISK = 'product-images';

    /** Path segment identifying a locally stored image inside a URL. */
    public const URL_PATH = '/media/product-images/';

    /** Maximum directory depth for filename-safety checks (productId + filename). */
    private const MAX_DEPTH = 2;

    /** Upload ceiling in kilobytes (matches the contract's 5 MB limit). */
    public const MAX_KILOBYTES = 5120;

    /**
     * Accepted extensions => MIME type. The upload rule validates by MIME
     * content (`mimes:`), this maps the stored name back to a content type
     * when the file is served.
     *
     * @var array<string, string>
     */
    private const MIME_TYPES = [
        'jpg' => 'image/jpeg',
        'png' => 'image/png',
        'webp' => 'image/webp',
        'gif' => 'image/gif',
    ];

    /**
     * Store one validated upload under a generated name inside a product-id
     * subdirectory and return its URL. The client-supplied name is never used,
     * so it cannot influence the path.
     */
    public function store(UploadedFile $file, int $productId): ?string
    {
        $extension = $this->extensionFor($file);

        if ($extension === null) {
            return null;
        }

        $filename = Str::uuid()->toString().'.'.$extension;
        $path = $productId.'/'.$filename;

        $stored = Storage::disk(self::DISK)->putFileAs((string) $productId, $file, $filename);

        if ($stored === false) {
            Log::error('Product image upload failed: storage/app/product-images is not writable.', [
                'product_id' => $productId,
                'extension' => $extension,
            ]);

            return null;
        }

        return route('media.product-images', ['productId' => $productId, 'filename' => $filename]);
    }

    /**
     * Delete the file behind an image URL. The URL may point to a
     * product-id subdirectory or be an external (seeded/demo) URL — the
     * latter resolve to no local file and are left untouched.
     */
    public function deleteByUrl(?string $url): void
    {
        $relativePath = $this->relativePathFor($url);

        if ($relativePath === null) {
            return;
        }

        Storage::disk(self::DISK)->delete($relativePath);
    }

    /**
     * Extract the relative storage path (productId/filename) from a stored
     * image URL, or null when the URL points outside this storage.
     */
    public function relativePathFor(?string $url): ?string
    {
        if (! is_string($url) || $url === '') {
            return null;
        }

        $position = strrpos($url, self::URL_PATH);

        if ($position === false) {
            return null;
        }

        $relativePath = substr($url, $position + strlen(self::URL_PATH));

        // Must match {positive integer}/{safe-filename}
        if (preg_match('#^(\d+)/([A-Za-z0-9][A-Za-z0-9._-]{0,120})$#', $relativePath) !== 1) {
            return null;
        }

        return $relativePath;
    }

    /**
     * Local file name behind a stored URL, or null when the URL points
     * outside this storage (a seeded or third-party image).
     *
     * @deprecated Use relativePathFor() instead to get the full storage path.
     */
    public function filenameFor(?string $url): ?string
    {
        $relativePath = $this->relativePathFor($url);

        if ($relativePath === null) {
            return null;
        }

        // Backward-compatible: return just the filename portion.
        $parts = explode('/', $relativePath);

        return $parts[1] ?? null;
    }

    /** Whether a requested file name may be served (no path traversal). */
    public function isSafeFilename(string $filename): bool
    {
        return preg_match('/^[A-Za-z0-9][A-Za-z0-9._-]{0,120}$/', $filename) === 1;
    }

    /** MIME type for a stored file name, derived from its extension. */
    public function mimeTypeFor(string $filename): string
    {
        $extension = strtolower(pathinfo($filename, PATHINFO_EXTENSION));

        return self::MIME_TYPES[$extension] ?? 'application/octet-stream';
    }

    /** Allowed extension for an upload, normalized to the stored form. */
    private function extensionFor(UploadedFile $file): ?string
    {
        $extension = strtolower((string) $file->guessExtension());

        if ($extension === 'jpeg') {
            $extension = 'jpg';
        }

        return array_key_exists($extension, self::MIME_TYPES) ? $extension : null;
    }
}
