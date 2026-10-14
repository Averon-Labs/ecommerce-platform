<?php

namespace App\Http\Requests;

/**
 * docs/openapi.yaml POST /checkout/quote (createCheckoutQuote): exactly one
 * ShippingAddress object; additionalProperties: false via ApiFormRequest.
 * Address rules mirror the ShippingAddress schema (uppercase ISO 3166-1
 * alpha-2 country, bounded fields).
 */
class CheckoutQuoteRequest extends ApiFormRequest
{
    protected function allowedFields(): array
    {
        return ['shippingAddress'];
    }

    /**
     * @return array<string, mixed>
     */
    public function rules(): array
    {
        return [
            'shippingAddress' => ['required', 'array'],
            'shippingAddress.recipientName' => ['required', 'string', 'min:1', 'max:120'],
            'shippingAddress.line1' => ['required', 'string', 'min:1', 'max:180'],
            'shippingAddress.line2' => ['nullable', 'string', 'max:180'],
            'shippingAddress.city' => ['required', 'string', 'min:1', 'max:80'],
            'shippingAddress.region' => ['required', 'string', 'min:1', 'max:80'],
            'shippingAddress.postalCode' => ['required', 'string', 'min:1', 'max:20'],
            'shippingAddress.country' => ['required', 'string', 'regex:/^[A-Z]{2}$/'],
        ];
    }
}
