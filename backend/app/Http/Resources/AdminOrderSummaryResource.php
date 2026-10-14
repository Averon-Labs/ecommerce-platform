<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;

/**
 * docs/openapi.yaml AdminOrderSummary: OrderSummary plus the owning
 * customer (controllers eager-load user).
 */
class AdminOrderSummaryResource extends OrderSummaryResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return parent::toArray($request) + [
            'customer' => [
                'id' => $this->user->id,
                'email' => $this->user->email,
                'firstName' => $this->user->first_name,
                'lastName' => $this->user->last_name,
            ],
        ];
    }
}
