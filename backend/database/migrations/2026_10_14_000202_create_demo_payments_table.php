<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * ORD-001: explicitly simulated demo payments (docs/DATABASE.md). One row
 * per attempt; the transaction_id is a fictional DEMO- reference, never a
 * real payment credential. Order.paymentStatus is derived from the latest
 * row of this table.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('demo_payments', function (Blueprint $table): void {
            $table->id();
            $table->foreignId('order_id')->constrained('orders')->cascadeOnDelete();
            $table->decimal('amount', 11, 2);
            $table->string('method', 16)->default('demo');
            $table->string('status', 16);
            $table->string('transaction_id', 20);
            $table->timestamp('created_at');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('demo_payments');
    }
};
