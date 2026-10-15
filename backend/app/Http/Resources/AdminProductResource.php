<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;

/**
 * docs/openapi.yaml AdminProduct: ProductDetail (summary + description,
 * sku, exact stockQuantity, ordered images) plus the row's updatedAt.
 * Reuses the public serializers so both views stay contract-identical.
 */
class AdminProductResource extends ProductDetailResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return array_merge(parent::toArray($request), [
            'updatedAt' => $this->updated_at?->toIso8601String(),
        ]);
    }
}
