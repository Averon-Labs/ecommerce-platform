<?php

namespace App\Http\Requests;

/**
 * docs/openapi.yaml CategoryInput — body of POST /admin/categories and
 * PUT /admin/categories/{categoryId} (additionalProperties: false).
 * Status is not part of the payload: categories move between active and
 * archived only through the archive/restore endpoints.
 */
class AdminCategoryInputRequest extends ApiFormRequest
{
    /**
     * @return array<int, string>
     */
    protected function allowedFields(): array
    {
        return ['name', 'sortOrder'];
    }

    /**
     * @return array<string, mixed>
     */
    public function rules(): array
    {
        return [
            'name' => ['required', 'string', 'min:1', 'max:80'],
            'sortOrder' => ['required', 'integer', 'min:0', 'max:9999'],
        ];
    }
}
