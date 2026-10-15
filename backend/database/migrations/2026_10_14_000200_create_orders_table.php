<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * ORD-001: orders snapshot the money and address at creation time
 * (docs/DATABASE.md). payment_status is deliberately not a column: it is
 * derived from the latest demo payment row, so it can never drift.
 * timeline records every status transition, oldest first, as required by
 * the contract's Order schema.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('orders', function (Blueprint $table): void {
            $table->id();
            $table->foreignId('user_id')->constrained('users')->cascadeOnDelete();
            // Contract reference ESV-NNNNNN derived from the primary key;
            // set immediately after creation inside the creating transaction.
            $table->string('reference', 16)->nullable()->unique();
            $table->string('status', 24)->default('pending_payment');
            $table->decimal('subtotal_amount', 11, 2);
            $table->decimal('shipping_amount', 11, 2);
            $table->decimal('total_amount', 11, 2);
            // Immutable copy of the validated delivery address.
            $table->json('shipping_address_snapshot');
            $table->text('notes')->nullable();
            // Recorded status transitions, oldest first.
            $table->json('timeline');
            $table->timestamps();
            $table->index(['user_id', 'created_at']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('orders');
    }
};
