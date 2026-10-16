<?php

namespace Tests\Feature;

use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * AUTH-003 / CAT-004 integration support: the first-party admin SPA runs on
 * the Vite development server (:5173) and calls the API on :8000, so its
 * requests are cross-origin while authenticated by the stateful Sanctum
 * session cookie. Credentialed CORS must therefore echo the documented SPA
 * origin instead of the framework's wildcard default.
 */
class ApiCorsTest extends TestCase
{
    use RefreshDatabase;

    private const SPA_ORIGIN = 'http://localhost:5173';

    public function test_preflight_from_the_local_admin_spa_origin_allows_credentials(): void
    {
        $response = $this->call('OPTIONS', '/api/v1/admin/auth/csrf', [], [], [], [
            'HTTP_ORIGIN' => self::SPA_ORIGIN,
            'HTTP_ACCESS_CONTROL_REQUEST_METHOD' => 'GET',
            'HTTP_ACCESS_CONTROL_REQUEST_HEADERS' => 'x-xsrf-token',
        ]);

        $response->assertNoContent();
        $response->assertHeader('Access-Control-Allow-Origin', self::SPA_ORIGIN);
        $response->assertHeader('Access-Control-Allow-Credentials', 'true');
    }

    public function test_admin_csrf_response_allows_the_local_admin_spa_origin_with_credentials(): void
    {
        $response = $this->getJson('/api/v1/admin/auth/csrf', ['Origin' => self::SPA_ORIGIN]);

        $response->assertNoContent();
        $response->assertHeader('Access-Control-Allow-Origin', self::SPA_ORIGIN);
        $response->assertHeader('Access-Control-Allow-Credentials', 'true');
    }

    public function test_cors_does_not_grant_credentials_to_unlisted_origins(): void
    {
        $response = $this->getJson('/api/v1/admin/auth/csrf', ['Origin' => 'https://evil.example.test']);

        $response->assertNoContent();
        $response->assertHeaderMissing('Access-Control-Allow-Origin');
        $response->assertHeaderMissing('Access-Control-Allow-Credentials');
    }
}
