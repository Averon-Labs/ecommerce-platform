<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/**
 * docs/openapi.yaml GET /admin/products query parameters: q, categoryId,
 * and status (any of the three product statuses) plus page/perPage. The
 * admin list has no sort parameter (unlike the public catalog) and
 * unknown query keys are ignored (additionalProperties applies to JSON
 * bodies, not query strings).
 */
class AdminProductQueryRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
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
            'status' => ['sometimes', 'string', 'in:active,inactive,archived'],
        ];
    }
}
