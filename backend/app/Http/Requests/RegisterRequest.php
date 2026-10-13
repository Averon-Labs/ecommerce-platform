<?php

namespace App\Http\Requests;

/**
 * docs/openapi.yaml RegisterRequest: email, password, firstName, lastName.
 * Password follows the shared Password schema: 8-72 chars, at least one
 * letter and one digit.
 */
class RegisterRequest extends ApiFormRequest
{
    protected function allowedFields(): array
    {
        return ['email', 'password', 'firstName', 'lastName'];
    }

    /**
     * @return array<string, mixed>
     */
    public function rules(): array
    {
        return [
            // Uniqueness is not a validation rule here: the contract answers
            // a taken email with 409 EMAIL_ALREADY_REGISTERED (see controller).
            'email' => ['required', 'email', 'max:254'],
            'password' => ['required', 'string', 'min:8', 'max:72', 'regex:/[A-Za-z]/', 'regex:/[0-9]/'],
            'firstName' => ['required', 'string', 'min:1', 'max:80'],
            'lastName' => ['required', 'string', 'min:1', 'max:80'],
        ];
    }
}
