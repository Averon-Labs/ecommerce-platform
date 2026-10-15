<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * CAT-001: one inventory row per product (unique product_id), holding the
 * current stock level that drives inStock/stockQuantity and later the
 * transactional decrement in ORD-001.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('inventories', function (Blueprint $table): void {
            $table->id();
            $table->foreignId('product_id')->unique()->constrained('products')->cascadeOnDelete();
            // Stock can never go negative: unsigned at the DB level.
            $table->unsignedInteger('quantity')->default(0);
            $table->timestamp('updated_at')->nullable();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('inventories');
    }
};
