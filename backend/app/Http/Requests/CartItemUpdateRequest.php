<?php

namespace App\Http\Requests;

/**
 * docs/openapi.yaml PATCH /cart/items/{productId} (updateCartItem): sets
 * the line quantity (CartQuantity, integer 1..99); removing a line is the
 * DELETE operation. Unknown fields are rejected by the base class.
 */
class CartItemUpdateRequest extends ApiFormRequest
{
    protected function allowedFields(): array
    {
        return ['quantity'];
    }

    /**
     * @return array<string, mixed>
     */
    public function rules(): array
    {
        return [
            'quantity' => ['required', 'integer', 'min:1', 'max:99'],
        ];
    }
}
