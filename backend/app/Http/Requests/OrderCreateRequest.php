<?php

namespace App\Http\Requests;

/**
 * docs/openapi.yaml POST /orders (createOrder): the validated ShippingAddress
 * plus optional notes (max 500). The server recalculates every amount, so no
 * client-supplied money field exists in the schema at all.
 */
class OrderCreateRequest extends ApiFormRequest
{
    protected function allowedFields(): array
    {
        return ['shippingAddress', 'notes'];
    }

    /**
     * @return array<string, mixed>
     */
    public function rules(): array
    {
        return [
            'shippingAddress' => ['required', 'array'],
            'shippingAddress.recipientName' => ['required', 'string', 'min:1', 'max:120'],
            'shippingAddress.line1' => ['required', 'string', 'min:1', 'max:180'],
            'shippingAddress.line2' => ['nullable', 'string', 'max:180'],
            'shippingAddress.city' => ['required', 'string', 'min:1', 'max:80'],
            'shippingAddress.region' => ['required', 'string', 'min:1', 'max:80'],
            'shippingAddress.postalCode' => ['required', 'string', 'min:1', 'max:20'],
            'shippingAddress.country' => ['required', 'string', 'regex:/^[A-Z]{2}$/'],
            'notes' => ['nullable', 'string', 'max:500'],
        ];
    }
}
