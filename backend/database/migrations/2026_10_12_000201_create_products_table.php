<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * CAT-001: products with fixed-precision USD money (docs/DATABASE.md:
 * money is never floating point; the API exposes a two-fraction-digit
 * decimal string plus a separate currency field).
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('products', function (Blueprint $table): void {
            $table->id();
            $table->string('name', 160);
            $table->text('description')->nullable();
            // Valid prices: fixed precision, unsigned (>= 0) at the DB level.
            // Precision 11 (8 integer digits) matches the contract Money
            // pattern '^\d{1,9}\.\d{2}$' — decimal(10,2) would overflow it.
            $table->decimal('price', 11, 2)->unsigned();
            $table->string('sku', 64)->nullable()->unique();
            $table->foreignId('category_id')->nullable()->constrained('categories')->nullOnDelete();
            $table->enum('status', ['active', 'inactive', 'archived'])->default('active');
            $table->timestamps();

            $table->index(['status', 'created_at']);
            $table->index(['status', 'price']);
            $table->index(['status', 'name']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('products');
    }
};
