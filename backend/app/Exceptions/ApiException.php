<?php

namespace App\Exceptions;

use App\Support\ApiError;
use Exception;
use Illuminate\Http\JsonResponse;

/**
 * Domain error with a stable machine-readable code from the contract's
 * x-error-codes list. Renders itself in the shared error envelope.
 */
class ApiException extends Exception
{
    /**
     * @param  array<int, array{field: string, message: string}>  $details
     * @param  array<string, int|string>  $headers
     */
    public function __construct(
        private readonly int $status,
        private readonly string $errorCode,
        string $message,
        private readonly array $details = [],
        private readonly array $headers = [],
    ) {
        parent::__construct($message);
    }

    public function render(): JsonResponse
    {
        return ApiError::response($this->status, $this->errorCode, $this->getMessage(), $this->details, $this->headers);
    }
}
