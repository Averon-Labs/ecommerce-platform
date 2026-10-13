<?php

namespace App\Http\Requests;

/**
 * docs/openapi.yaml PATCH /admin/inventory/{productId}
 * (additionalProperties: false): the client sends the resulting absolute
 * quantity — never a delta, so a retry cannot double-apply. Zero is valid
 * and means out of stock.
 */
class AdminInventoryUpdateRequest extends ApiFormRequest
{
    /**
     * @return array<int, string>
     */
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
            'quantity' => ['required', 'integer', 'min:0', 'max:1000000'],
        ];
    }
}
