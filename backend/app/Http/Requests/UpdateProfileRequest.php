<?php

namespace App\Http\Requests;

use Illuminate\Validation\Validator;

/**
 * docs/openapi.yaml PATCH /auth/profile: only firstName and lastName are
 * editable; email and role are not part of the schema, and at least one
 * property must be present (minProperties: 1).
 */
class UpdateProfileRequest extends ApiFormRequest
{
    /**
     * @return array<string, mixed>
     */
    public function rules(): array
    {
        return [
            'firstName' => ['sometimes', 'string', 'min:1', 'max:80'],
            'lastName' => ['sometimes', 'string', 'min:1', 'max:80'],
        ];
    }

    public function after(Validator $validator): callable
    {
        return function (Validator $validator): void {
            if ($this->json()->all() === []) {
                $validator->errors()->add('body', 'At least one of firstName or lastName is required.');
            }
        };
    }
}
