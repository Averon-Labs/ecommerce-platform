<?php

namespace Tests\Feature\Auth;

use Illuminate\Database\Migrations\Migration;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\DB;
use RuntimeException;
use Tests\TestCase;

/**
 * AUTH-001: the data migration rewrites legacy mixed-case emails so the
 * case-insensitive sign-in lookup can find every existing account.
 */
class EmailNormalizationMigrationTest extends TestCase
{
    use RefreshDatabase;

    public function test_migration_lowercases_existing_email_rows(): void
    {
        $this->insertUser('Legacy.User@Example.COM');

        $this->migration()->up();

        // Compare the raw value: MySQL's utf8mb4_unicode_ci collation would
        // also match the mixed-case original in a normal WHERE clause.
        $this->assertSame(
            'legacy.user@example.com',
            (string) DB::table('users')->value('email'),
        );
    }

    public function test_migration_fails_loudly_when_case_variants_collide(): void
    {
        // MySQL's documented utf8mb4_unicode_ci collation already treats
        // case-only variants as duplicates, so this state cannot be created
        // on the project database. The guard protects case-sensitive hosts.
        if (DB::connection()->getDriverName() === 'mysql') {
            $this->markTestSkipped('MySQL case-insensitive collation rejects case-only duplicate emails.');
        }

        $this->insertUser('case@example.com');
        $this->insertUser('CASE@example.com');

        $this->expectException(RuntimeException::class);
        $this->expectExceptionMessage('differ only by letter case');

        $this->migration()->up();
    }

    private function migration(): Migration
    {
        return require database_path('migrations/2026_10_23_000100_normalize_user_emails.php');
    }

    private function insertUser(string $email): void
    {
        DB::table('users')->insert([
            'first_name' => 'Legacy',
            'last_name' => 'User',
            'email' => $email,
            'password' => '$2y$04$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
            'created_at' => now(),
            'updated_at' => now(),
        ]);
    }
}
