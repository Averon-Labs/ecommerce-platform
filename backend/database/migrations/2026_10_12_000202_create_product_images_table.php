<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * CAT-001: ordered seeded image set per product (binary upload is out of
 * scope for the first release; seeded/demo URLs are sufficient).
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('product_images', function (Blueprint $table): void {
            $table->id();
            $table->foreignId('product_id')->constrained('products')->cascadeOnDelete();
            $table->string('url', 2048);
            $table->unsignedInteger('sort_order')->default(0);

            $table->index(['product_id', 'sort_order']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('product_images');
    }
};
