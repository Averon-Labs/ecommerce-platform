<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

/**
 * CART-001: one persistent cart per signed-in customer. Lines carry the
 * add-time price snapshot; every read reprices from the current product
 * record (docs/openapi.yaml Cart).
 */
#[Fillable(['user_id'])]
class Cart extends Model
{
    /** @return BelongsTo<User, $this> */
    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    /** @return HasMany<CartItem, $this> */
    public function items(): HasMany
    {
        // Stable display order for every read.
        return $this->hasMany(CartItem::class)->orderBy('id');
    }
}
