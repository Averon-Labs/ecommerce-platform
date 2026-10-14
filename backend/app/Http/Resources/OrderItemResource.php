<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * docs/openapi.yaml OrderItem: immutable name/price snapshots frozen at
 * creation time; money stays a two-fraction-digit decimal string.
 */
class OrderItemResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return [
            'productId' => $this->product_id,
            'productName' => $this->product_name,
            'quantity' => $this->quantity,
            'unitPrice' => $this->unit_price,
            'lineTotal' => $this->total_price,
        ];
    }
}
