<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;

/**
 * docs/openapi.yaml GET /admin/orders (adminListOrders) query parameters:
 * q (reference or customer email), status, inclusive UTC date bounds from/to,
 * plus page/perPage.
 */
class AdminOrderQueryRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /**
     * @return array<string, mixed>
     */
    public function rules(): array
    {
        return [
            'page' => ['sometimes', 'integer', 'min:1'],
            'perPage' => ['sometimes', 'integer', 'min:1', 'max:100'],
            'q' => ['sometimes', 'string', 'max:120'],
            'status' => ['sometimes', 'string', 'in:pending_payment,confirmed,processing,shipped,delivered,cancelled'],
            'from' => ['sometimes', 'date_format:Y-m-d'],
            'to' => ['sometimes', 'date_format:Y-m-d'],
        ];
    }
}
