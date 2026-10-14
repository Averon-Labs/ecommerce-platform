<?php

namespace App\Http\Resources;

use App\Support\Money;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * docs/openapi.yaml CartItem: every read reprices the line from the current
 * product record. priceChanged compares the add-time unit_price snapshot
 * against the current price; unavailable flags lines whose product left the
 * `active` status. Controllers eager-load product.inventory and
 * product.images.
 */
class CartItemResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        $product = $this->product;
        $available = $product->inventory?->quantity ?? 0;
        $unitCents = Money::toCents((string) $product->price);

        return [
            'productId' => $product->id,
            'name' => $product->name,
            'imageUrl' => $product->images->first()?->url,
            'unitPrice' => Money::fromCents($unitCents),
            'currency' => 'USD',
            'quantity' => $this->quantity,
            'lineTotal' => Money::fromCents($unitCents * $this->quantity),
            'inStock' => $available > 0,
            'availableQuantity' => $available,
            'priceChanged' => Money::toCents((string) $this->unit_price) !== $unitCents,
            'unavailable' => $product->status !== 'active',
        ];
    }
}
