<?php

namespace App\Http\Resources;

use App\Support\OrderStateMachine;
use Illuminate\Http\Request;

/**
 * docs/openapi.yaml AdminOrder: the full Order plus the customer and the
 * transitions the current status permits, so the dashboard never offers a
 * move the server will reject.
 */
class AdminOrderResource extends OrderResource
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
            'allowedTransitions' => OrderStateMachine::allowed($this->status),
        ];
    }
}
