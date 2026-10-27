<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * docs/openapi.yaml ProductImage: id + url + sortOrder. The id addresses
 * the row through DELETE /admin/products/{productId}/images/{imageId}.
 */
class ProductImageResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return [
            'id' => $this->id,
            'url' => $this->url,
            'sortOrder' => $this->sort_order,
        ];
    }
}
