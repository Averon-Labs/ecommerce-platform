<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * AUTH-001: align the skeleton users table with the logical Users model in
 * docs/DATABASE.md and the accepted User schema in docs/openapi.yaml
 * (first/last name, role, lifecycle status).
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('users', function (Blueprint $table): void {
            $table->dropColumn('name');
            $table->string('first_name', 80);
            $table->string('last_name', 80);
            $table->enum('role', ['customer', 'admin'])->default('customer');
            $table->enum('status', ['active', 'suspended', 'deleted'])->default('active');
            $table->index('status');
        });
    }

    public function down(): void
    {
        Schema::table('users', function (Blueprint $table): void {
            $table->dropIndex(['status']);
            $table->dropColumn(['role', 'status', 'first_name', 'last_name']);
            $table->string('name');
        });
    }
};
