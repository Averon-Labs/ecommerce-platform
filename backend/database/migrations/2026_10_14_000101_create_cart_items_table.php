<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * CART-001: at most one row per product per cart (docs/DATABASE.md).
 * unit_price snapshots the price at add time so every read can flag
 * CartItem.priceChanged when the current product price has drifted
 * (docs/openapi.yaml).
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('cart_items', function (Blueprint $table): void {
            $table->id();
            $table->foreignId('cart_id')->constrained('carts')->cascadeOnDelete();
            $table->foreignId('product_id')->constrained('products')->cascadeOnDelete();
            // One line per product per cart.
            $table->unique(['cart_id', 'product_id']);
            // The 1..99 bound is enforced at the API boundary; unsigned here
            // so the column can never hold a negative quantity.
            $table->unsignedSmallInteger('quantity');
            // Same fixed-precision money rule as products.price.
            $table->decimal('unit_price', 11, 2);
            $table->timestamp('added_at');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('cart_items');
    }
};
