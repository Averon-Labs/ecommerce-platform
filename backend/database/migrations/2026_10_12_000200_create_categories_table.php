<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * CAT-001: flat categories with the archive lifecycle resolved in FND-004
 * (docs/openapi.yaml CategoryStatus: active | archived).
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('categories', function (Blueprint $table): void {
            $table->id();
            $table->string('name', 80)->unique();
            $table->unsignedInteger('sort_order')->default(0);
            $table->enum('status', ['active', 'archived'])->default('active');
            $table->timestamps();

            $table->index(['status', 'sort_order']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('categories');
    }
};
