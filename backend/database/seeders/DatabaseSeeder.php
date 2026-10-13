<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Console\Seeds\WithoutModelEvents;
use Illuminate\Database\Seeder;

/**
 * Demo customer for manual login testing (AUTH-001). Seeding is
 * idempotent, so it is safe to re-run against an existing development
 * database.
 */
class DatabaseSeeder extends Seeder
{
    use WithoutModelEvents;

    /**
     * Run the database seeders.
     */
    public function run(): void
    {
        $this->seedTestCustomer();
    }

    /**
     * A demo customer for manual login testing; not part of the catalog.
     */
    private function seedTestCustomer(): void
    {
        User::query()->firstOrCreate(
            ['email' => 'demo@example.com'],
            [
                'first_name' => 'Demo',
                'last_name' => 'Customer',
                'password' => 'Password123',
                'email_verified_at' => now(),
            ],
        );
    }
}
