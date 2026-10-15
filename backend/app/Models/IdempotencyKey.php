<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * ORD-001: stored Idempotency-Key records. The same key with the same
 * request hash replays the stored response; a conflicting reuse answers
 * 409 IDEMPOTENCY_KEY_REUSED (docs/openapi.yaml).
 */
#[Fillable(['user_id', 'endpoint', 'key', 'hash', 'response_code', 'response_body', 'created_at'])]
class IdempotencyKey extends Model
{
    public $timestamps = false;

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'response_body' => 'array',
            'response_code' => 'integer',
        ];
    }

    /** @return BelongsTo<User, $this> */
    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }
}
