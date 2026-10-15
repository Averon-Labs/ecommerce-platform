<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\Attributes\FailOnUnknownFields;
use Illuminate\Foundation\Http\FormRequest;

/**
 * Base request for JSON bodies that follow contract schemas declaring
 * `additionalProperties: false`: unknown top-level fields are rejected
 * with the standard 422 VALIDATION_ERROR envelope.
 */
#[FailOnUnknownFields]
abstract class ApiFormRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }
}
