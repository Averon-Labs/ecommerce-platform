<?php

namespace App\Http\Requests;

/**
 * docs/openapi.yaml AdminProductInput — body of POST /admin/products and
 * the full-replacement PUT /admin/products/{productId}
 * (additionalProperties: false):
 *
 * - `price` is the Money decimal string '^\d{1,9}\.\d{2}$'; a JSON
 *   number is a type error and fails the string rule.
 * - `description` and `categoryId` are required keys that may hold null
 *   (`present`, because Laravel's `required` rule rejects null).
 * - `status` accepts active|inactive only; `archived` is not assignable —
 *   it is reached through POST /admin/products/{productId}/archive.
 * - `imageUrls` is the ordered uri-reference list (max 8, max 2048
 *   chars, no whitespace); binary upload is out of scope.
 */
class AdminProductInputRequest extends ApiFormRequest
{
    /**
     * @return array<int, string>
     */
    protected function allowedFields(): array
    {
        return ['name', 'description', 'price', 'sku', 'categoryId', 'status', 'imageUrls'];
    }

    /**
     * @return array<string, mixed>
     */
    public function rules(): array
    {
        return [
            'name' => ['required', 'string', 'min:1', 'max:160'],
            'description' => ['present', 'nullable', 'string', 'max:5000'],
            'price' => ['required', 'string', 'regex:/^\d{1,9}\.\d{2}$/'],
            'sku' => ['sometimes', 'nullable', 'string', 'max:64'],
            'categoryId' => ['present', 'nullable', 'integer', 'min:1'],
            'status' => ['required', 'string', 'in:active,inactive'],
            'imageUrls' => ['present', 'array', 'max:8'],
            'imageUrls.*' => ['string', 'max:2048', 'regex:/^\S+$/'],
        ];
    }
}
