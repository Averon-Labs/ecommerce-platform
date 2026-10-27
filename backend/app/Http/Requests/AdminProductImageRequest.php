<?php

namespace App\Http\Requests;

use App\Services\ProductImageStorage;

/**
 * docs/openapi.yaml AdminProductImageInput — multipart body of
 * POST /admin/products/{productId}/images.
 *
 * The file is validated on its detected content type (`mimes:`), not on the
 * client-supplied name, so a renamed executable is rejected. Size follows the
 * contract's 5 MB ceiling.
 */
class AdminProductImageRequest extends ApiFormRequest
{
    /**
     * @return array<int, string>
     */
    protected function allowedFields(): array
    {
        return ['image'];
    }

    /**
     * @return array<string, mixed>
     */
    public function rules(): array
    {
        return [
            'image' => [
                'required',
                'file',
                'mimes:jpg,jpeg,png,webp,gif',
                'max:'.ProductImageStorage::MAX_KILOBYTES,
            ],
        ];
    }
}
