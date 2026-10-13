<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/**
 * docs/openapi.yaml GET /admin/categories query parameter: optional
 * status filter (omit to return every status).
 */
class AdminCategoryQueryRequest extends FormRequest
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
            'status' => ['sometimes', 'string', 'in:active,archived'],
        ];
    }
}
