<?php

namespace App\Http\Requests;

/**
 * docs/openapi.yaml POST /auth/password-reset/confirm: email + reset
 * token + new password following the shared Password schema.
 */
class ConfirmPasswordResetRequest extends ApiFormRequest
{
    protected function allowedFields(): array
    {
        return ['email', 'token', 'password'];
    }

    /**
     * @return array<string, mixed>
     */
    public function rules(): array
    {
        return [
            'email' => ['required', 'email', 'max:254'],
            'token' => ['required', 'string', 'min:16', 'max:128'],
            'password' => ['required', 'string', 'min:8', 'max:72', 'regex:/[A-Za-z]/', 'regex:/[0-9]/'],
        ];
    }
}
