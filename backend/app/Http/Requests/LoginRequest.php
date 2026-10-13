<?php

namespace App\Http\Requests;

/**
 * docs/openapi.yaml LoginRequest: email + password only. Credential
 * validity is judged by the controller so failures stay deliberately vague.
 */
class LoginRequest extends ApiFormRequest
{
    protected function allowedFields(): array
    {
        return ['email', 'password'];
    }

    /**
     * @return array<string, mixed>
     */
    public function rules(): array
    {
        return [
            'email' => ['required', 'email', 'max:254'],
            'password' => ['required', 'string', 'max:72'],
        ];
    }
}
