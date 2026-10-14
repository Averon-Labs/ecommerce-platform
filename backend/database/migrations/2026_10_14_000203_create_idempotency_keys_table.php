<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * ORD-001: the contract requires POST /orders and POST /orders/{id}/payment
 * to carry an Idempotency-Key. One row per (user, endpoint, key): the same
 * key with the same request hash replays the stored response, and a
 * conflicting reuse answers 409 IDEMPOTENCY_KEY_REUSED.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('idempotency_keys', function (Blueprint $table): void {
            $table->id();
            $table->foreignId('user_id')->constrained('users')->cascadeOnDelete();
            // Scoped per operation ('orders.create', 'orders.payment:<id>').
            $table->string('endpoint', 64);
            $table->string('key', 64);
            $table->string('hash', 64);
            $table->unsignedSmallInteger('response_code');
            $table->json('response_body');
            $table->timestamp('created_at');
            $table->unique(['user_id', 'endpoint', 'key']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('idempotency_keys');
    }
};
