<?php

namespace App\Http\Requests;

/**
 * docs/openapi.yaml POST /cart/items (addCartItem): productId + quantity
 * (CartQuantity, integer 1..99). Unknown fields are rejected by the
 * ApiFormRequest base (additionalProperties: false).
 */
class CartItemStoreRequest extends ApiFormRequest
{
    protected function allowedFields(): array
    {
        return ['productId', 'quantity'];
    }

    /**
     * @return array<string, mixed>
     */
    public function rules(): array
    {
        return [
            'productId' => ['required', 'integer', 'min:1'],
            'quantity' => ['required', 'integer', 'min:1', 'max:99'],
        ];
    }
}
