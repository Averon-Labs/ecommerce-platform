<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * docs/openapi.yaml InventoryRow: one product's stock line. Products
 * without an inventory row report quantity 0 / inStock false and a null
 * updatedAt (the row only appears once stock has been set).
 */
class InventoryRowResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        $quantity = (int) ($this->inventory?->quantity ?? 0);

        return [
            'productId' => $this->id,
            'name' => $this->name,
            'sku' => $this->sku,
            'quantity' => $quantity,
            'inStock' => $quantity > 0,
            'updatedAt' => $this->inventory?->updated_at?->toIso8601String(),
        ];
    }
}
