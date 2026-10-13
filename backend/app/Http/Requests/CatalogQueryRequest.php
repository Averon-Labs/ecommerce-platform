<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/**
 * docs/openapi.yaml GET /products query parameters. Unknown query keys are
 * ignored (additionalProperties applies to JSON bodies, not query strings).
 */
class CatalogQueryRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /**
     * Query strings carry booleans as "true"/"false" (or "1"/"0", which
     * Laravel's boolean rule also accepts as strings); normalize every
     * accepted spelling so downstream code can rely on a real bool.
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
            'categoryId' => ['sometimes', 'integer', 'min:1'],
            'inStock' => ['sometimes', 'boolean'],
            'sort' => ['sometimes', 'string', 'in:newest,price_asc,price_desc,name_asc'],
        ];
    }
}
