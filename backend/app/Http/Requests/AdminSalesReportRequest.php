<?php

namespace App\Http\Requests;

use Illuminate\Contracts\Validation\Validator;
use Illuminate\Foundation\Http\FormRequest;

/**
 * docs/openapi.yaml GET /admin/reports/sales (adminSalesReport) query
 * parameters: inclusive UTC date bounds from/to and the number of top
 * products. Dates are validated as plain calendar dates; the controller
 * applies the documented default range and maximum span.
 */
class AdminSalesReportRequest extends FormRequest
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
            'from' => ['sometimes', 'date_format:Y-m-d'],
            'to' => ['sometimes', 'date_format:Y-m-d'],
            'topLimit' => ['sometimes', 'integer', 'min:1', 'max:10'],
        ];
    }

    /**
     * `from` is optional and falls back to the start of the default range,
     * so the ordering check only runs when both bounds are present.
     *
     * @return array<int, callable>
     */
    public function after(): array
    {
        return [
            function (Validator $validator): void {
                if ($validator->errors()->isNotEmpty()) {
                    return;
                }

                $from = $this->input('from');
                $to = $this->input('to');

                if (is_string($from) && is_string($to) && $from > $to) {
                    $validator->errors()->add('from', 'The from date must not be after the to date.');
                }
            },
        ];
    }
}
