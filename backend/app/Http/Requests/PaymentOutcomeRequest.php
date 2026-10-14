<?php

namespace App\Http\Requests;

/**
 * docs/openapi.yaml POST /orders/{orderId}/payment (submitSimulatedPayment):
 * an explicit outcome flag only — the schema never accepts card data and no
 * payment provider is ever contacted.
 */
class PaymentOutcomeRequest extends ApiFormRequest
{
    protected function allowedFields(): array
    {
        return ['outcome'];
    }

    /**
     * @return array<string, mixed>
     */
    public function rules(): array
    {
        return [
            'outcome' => ['required', 'string', 'in:success,failure'],
        ];
    }
}
