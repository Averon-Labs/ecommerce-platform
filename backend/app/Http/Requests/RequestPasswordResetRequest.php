<?php

namespace App\Http\Requests;

/**
 * docs/openapi.yaml POST /auth/password-reset: email only; the endpoint
 * must not disclose whether the account exists.
 */
class RequestPasswordResetRequest extends ApiFormRequest
{
    protected function allowedFields(): array
    {
        return ['email'];
    }

    /**
     * @return array<string, mixed>
     */
    public function rules(): array
    {
        return [
            'email' => ['required', 'email', 'max:254'],
        ];
    }
}
