<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * docs/openapi.yaml OrderSummary: the customer-facing list row. paymentStatus
 * is derived from the latest demo payment; itemCount is the sum of the
 * snapshot quantities (controllers eager-load items and latestPayment).
 */
class OrderSummaryResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return [
            'id' => $this->id,
            'reference' => $this->reference,
            'status' => $this->status,
            'paymentStatus' => $this->payment_status,
            'total' => $this->total_amount,
            'currency' => 'USD',
            'itemCount' => (int) $this->items->sum('quantity'),
            'createdAt' => $this->created_at,
        ];
    }
}
