<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\Relations\HasOne;

/**
 * ORD-001: an order is an immutable snapshot of the cart at creation time
 * (docs/openapi.yaml Order). paymentStatus is derived from the latest demo
 * payment so it can never drift from the payment rows; timeline records
 * every status transition, oldest first.
 */
#[Fillable([
    'user_id',
    'status',
    'subtotal_amount',
    'shipping_amount',
    'total_amount',
    'shipping_address_snapshot',
    'notes',
    'timeline',
])]
class Order extends Model
{
    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'subtotal_amount' => 'decimal:2',
            'shipping_amount' => 'decimal:2',
            'total_amount' => 'decimal:2',
            'shipping_address_snapshot' => 'array',
            'timeline' => 'array',
            'notes' => 'string',
        ];
    }

    /**
     * Contract PaymentStatus: the latest payment attempt, else `pending`.
     */
    public function getPaymentStatusAttribute(): string
    {
        return $this->latestPayment?->status ?? 'pending';
    }

    /** @return BelongsTo<User, $this> */
    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    /** @return HasMany<OrderItem, $this> */
    public function items(): HasMany
    {
        // Stable display order for every read.
        return $this->hasMany(OrderItem::class)->orderBy('id');
    }

    /** @return HasMany<DemoPayment, $this> */
    public function payments(): HasMany
    {
        return $this->hasMany(DemoPayment::class);
    }

    /** @return HasOne<DemoPayment, $this> */
    public function latestPayment(): HasOne
    {
        return $this->hasOne(DemoPayment::class)->latestOfMany('id');
    }
}
