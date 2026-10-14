<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * docs/openapi.yaml OrderPayment: the explicitly simulated demo attempt.
 * transactionId is a fictional DEMO- reference, never a real payment
 * credential.
 */
class OrderPaymentResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return [
            'method' => $this->method,
            'status' => $this->status,
            'amount' => $this->amount,
            'currency' => 'USD',
            'transactionId' => $this->transaction_id,
            'createdAt' => $this->created_at,
        ];
    }
}
