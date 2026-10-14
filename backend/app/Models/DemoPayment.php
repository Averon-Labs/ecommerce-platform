<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * ORD-001: one row per simulated payment attempt (docs/DATABASE.md Demo
 * Payments). The transaction_id is a fictional DEMO- reference, never a
 * real payment credential; no provider is ever contacted.
 */
#[Fillable(['order_id', 'amount', 'method', 'status', 'transaction_id', 'created_at'])]
class DemoPayment extends Model
{
    public $timestamps = false;

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'amount' => 'decimal:2',
            'status' => 'string',
            'method' => 'string',
            // Timestamps are off, so cast explicitly: the contract requires
            // payment.createdAt to serialize as an RFC 3339 date-time.
            'created_at' => 'datetime',
        ];
    }

    /** @return BelongsTo<Order, $this> */
    public function order(): BelongsTo
    {
        return $this->belongsTo(Order::class);
    }
}
