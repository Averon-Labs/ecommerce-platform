<?php

namespace App\Http\Requests;

/**
 * docs/openapi.yaml GoogleAuthRequest: the Google Sign-In ID token issued
 * to the client. Signature, issuer, audience, expiry, and email claims are
 * verified by GoogleAuthService.
 */
class GoogleAuthRequest extends ApiFormRequest
{
    protected function allowedFields(): array
    {
        return ['idToken'];
    }

    /**
     * @return array<string, mixed>
     */
    public function rules(): array
    {
        return [
            'idToken' => ['required', 'string', 'min:16', 'max:4096'],
        ];
    }
}
