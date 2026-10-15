<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;

/**
 * docs/openapi.yaml Order: immutable snapshots (items, address, money) plus
 * the derived payment view and the full status timeline, oldest first.
 */
class OrderResource extends OrderSummaryResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return parent::toArray($request) + [
            'items' => OrderItemResource::collection($this->items),
            'subtotal' => $this->subtotal_amount,
            'shipping' => $this->shipping_amount,
            'shippingAddress' => $this->shipping_address_snapshot,
            'notes' => $this->notes,
            'payment' => $this->latestPayment === null
                ? null
                : new OrderPaymentResource($this->latestPayment),
            'timeline' => $this->timeline,
            'updatedAt' => $this->updated_at,
        ];
    }
}
