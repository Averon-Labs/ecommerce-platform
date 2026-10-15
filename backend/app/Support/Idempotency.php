<?php

namespace App\Support;

use App\Exceptions\ApiException;
use App\Models\IdempotencyKey;
use Illuminate\Http\Request;

/**
 * ORD-001: Idempotency-Key handling for the mutation endpoints that require
 * it (docs/openapi.yaml MISSING_IDEMPOTENCY_KEY / IDEMPOTENCY_KEY_REUSED).
 * Records are scoped per user and per operation so one key can never leak
 * a response across endpoints or customers.
 */
final class Idempotency
{
    /**
     * Read and validate the header; absent or non-UUID values are a 400.
     */
    public static function key(Request $request): string
    {
        $key = (string) $request->header('Idempotency-Key', '');

        if (preg_match('/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i', $key) !== 1) {
            throw new ApiException(
                400,
                'MISSING_IDEMPOTENCY_KEY',
                'A valid Idempotency-Key header (UUID) is required for this operation.',
            );
        }

        return $key;
    }

    /**
     * Stable hash of the JSON request body for reuse comparison.
     *
     * @param  array<string, mixed>  $body
     */
    public static function hash(array $body): string
    {
        return hash('sha256', json_encode($body, JSON_THROW_ON_ERROR));
    }

    /**
     * The stored response of an already-used key, or null on first use.
     * Reusing a key with a different body answers 409 IDEMPOTENCY_KEY_REUSED.
     *
     * @return array{code: int, body: array<string, mixed>}|null
     */
    public static function replay(int $userId, string $endpoint, string $key, string $hash): ?array
    {
        $record = IdempotencyKey::query()
            ->where('user_id', $userId)
            ->where('endpoint', $endpoint)
            ->where('key', $key)
            ->first();

        if ($record === null) {
            return null;
        }

        if ($record->hash !== $hash) {
            throw new ApiException(
                409,
                'IDEMPOTENCY_KEY_REUSED',
                'This Idempotency-Key was already used with a different request body.',
            );
        }

        return ['code' => (int) $record->response_code, 'body' => $record->response_body];
    }

    /**
     * Persist the response of a completed mutation for later replay. Must
     * run inside the same transaction as the mutation, so a conflicting
     * concurrent insert rolls the mutation back before it can duplicate.
     *
     * @param  array<string, mixed>  $body
     */
    public static function remember(int $userId, string $endpoint, string $key, string $hash, int $code, array $body): void
    {
        IdempotencyKey::query()->create([
            'user_id' => $userId,
            'endpoint' => $endpoint,
            'key' => $key,
            'hash' => $hash,
            'response_code' => $code,
            'response_body' => $body,
            'created_at' => now(),
        ]);
    }
}
