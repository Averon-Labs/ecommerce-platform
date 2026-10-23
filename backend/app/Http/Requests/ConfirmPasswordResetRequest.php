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
     * The reset token is stored per lowercased email; normalize the input
     * so confirmation matches regardless of how the client sends it.
     */
    protected function prepareForValidation(): void
    {
        if (is_string($this->input('email'))) {
            $this->merge(['email' => strtolower(trim($this->input('email')))]);
        }
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
