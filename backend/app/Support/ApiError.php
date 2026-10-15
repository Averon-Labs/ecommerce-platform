<?php

namespace App\Support;

use Illuminate\Http\JsonResponse;

/**
 * Builds the shared API error envelope defined in docs/openapi.yaml:
 * { "error": { "code": "...", "message": "...", "details": [...] } }.
 */
class ApiError
{
    /**
     * @param  array<int, array{field: string, message: string}>  $details
     * @param  array<string, int|string>  $headers
     */
    public static function response(int $status, string $code, string $message, array $details = [], array $headers = []): JsonResponse
    {
        $error = [
            'code' => $code,
            'message' => $message,
        ];

        if ($details !== []) {
            $error['details'] = $details;
        }

        return new JsonResponse(['error' => $error], $status, $headers);
    }
}
