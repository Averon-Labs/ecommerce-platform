<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/**
 * docs/openapi.yaml GET /admin/inventory query parameters: q, inStock,
 * plus page/perPage. Query strings carry booleans as "true"/"false" (or
 * "1"/"0", which Laravel's boolean rule also accepts as strings), so
 * every accepted spelling is normalized before validation.
 */
class AdminInventoryQueryRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /**
     * Normalize the boolean spellings the "boolean" rule accepts as
     * strings, so the controller's strict bool comparison cannot silently
     * misread "1" as "false".
     *
     * @return array<string, mixed>
     */
    protected function prepareForValidation(): void
    {
        $inStock = $this->query('inStock');

        if (is_string($inStock) && in_array($inStock, ['true', 'false', '1', '0'], true)) {
            $this->merge(['inStock' => in_array($inStock, ['true', '1'], true)]);
        }
    }

    /**
     * @return array<string, mixed>
     */
    public function rules(): array
    {
        return [
            'page' => ['sometimes', 'integer', 'min:1'],
            'perPage' => ['sometimes', 'integer', 'min:1', 'max:100'],
            'q' => ['sometimes', 'string', 'max:120'],
            'inStock' => ['sometimes', 'boolean'],
        ];
    }
}
