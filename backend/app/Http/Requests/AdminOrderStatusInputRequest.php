<?php

namespace App\Http\Requests;

/**
 * docs/openapi.yaml PATCH /admin/orders/{orderId}/status
 * (adminUpdateOrderStatus): the requested target status. The enum accepts
 * every OrderStatus; illegal transitions are answered with the contract's
 * 409 INVALID_TRANSITION by the controller (never by validation).
 */
class AdminOrderStatusInputRequest extends ApiFormRequest
{
    protected function allowedFields(): array
    {
        return ['status'];
    }

    /**
     * @return array<string, mixed>
     */
    public function rules(): array
    {
        return [
            'status' => ['required', 'string', 'in:pending_payment,confirmed,processing,shipped,delivered,cancelled'],
        ];
    }
}
