<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;

/**
 * docs/openapi.yaml ProductDetail: ProductSummary plus description, sku,
 * exact stockQuantity, and the ordered image set.
 */
class ProductDetailResource extends ProductSummaryResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return array_merge(parent::toArray($request), [
            'description' => $this->description,
            'sku' => $this->sku,
            'stockQuantity' => $this->inventory?->quantity ?? 0,
            'images' => ProductImageResource::collection($this->images),
        ]);
    }
}
