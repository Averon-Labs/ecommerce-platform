<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * ORD-001: immutable line snapshots taken at creation time — the product
 * name and unit price are frozen, so later catalog edits never rewrite an
 * order (docs/DATABASE.md).
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('order_items', function (Blueprint $table): void {
            $table->id();
            $table->foreignId('order_id')->constrained('orders')->cascadeOnDelete();
            // Products are archived, never hard-deleted, so snapshots stay
            // referentially intact; restrict protects that invariant.
            $table->foreignId('product_id')->constrained('products');
            $table->string('product_name');
            $table->unsignedSmallInteger('quantity');
            $table->decimal('unit_price', 11, 2);
            $table->decimal('total_price', 11, 2);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('order_items');
    }
};
