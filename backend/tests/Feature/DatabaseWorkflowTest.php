<?php

namespace Tests\Feature;

use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Schema;
use Tests\TestCase;

class DatabaseWorkflowTest extends TestCase
{
    use RefreshDatabase;

    /**
     * Tests must never run migrations against the development database:
     * phpunit.xml points the mysql connection at a disposable *_test database.
     */
    public function test_test_database_is_the_isolated_test_database(): void
    {
        $this->assertSame('mysql', config('database.default'));
        $this->assertStringEndsWith(
            '_test',
            (string) config('database.connections.mysql.database'),
        );
    }

    /**
     * The baseline migration set created by FND-002 must apply cleanly on
     * the hosted test database.
     */
    public function test_migrations_create_foundation_tables(): void
    {
        $this->assertTrue(Schema::hasTable('users'));
        $this->assertTrue(Schema::hasTable('cache'));
        $this->assertTrue(Schema::hasTable('jobs'));
    }
}
