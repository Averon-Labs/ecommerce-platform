<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * docs/openapi.yaml ProductSummary: money is a decimal string plus a
 * separate currency code (fixed precision, never floating point), and
 * inStock reflects the inventory row (true when stockQuantity > 0).
 * Controllers always eager-load category, images, and inventory.
 */
class ProductSummaryResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return [
            'id' => $this->id,
            'name' => $this->name,
            'price' => $this->price,
            'currency' => 'USD',
            'categoryId' => $this->category_id,
            'categoryName' => $this->category?->name,
            'imageUrl' => $this->images->first()?->url,
            'inStock' => ($this->inventory?->quantity ?? 0) > 0,
            'status' => $this->status,
        ];
    }
}
