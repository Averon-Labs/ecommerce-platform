<?php

namespace Tests\Feature\Admin;

use App\Models\Category;
use App\Models\Product;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * AUTH-003 backend portion: admin session lifecycle (csrf, login,
 * session, logout), CSRF enforcement, role rejection, and the
 * admin-login rate-limit class from docs/openapi.yaml.
 */
class AdminAuthTest extends TestCase
{
    use InteractsWithAdminSession, RefreshDatabase;

    public function test_csrf_endpoint_issues_a_readable_xsrf_cookie_and_httponly_session_cookie(): void
    {
        $response = $this->getJson('/api/v1/admin/auth/csrf', ['Origin' => $this->statefulOrigin()]);
        $response->assertNoContent();

        $cookies = collect($response->headers->getCookies())->keyBy->getName();

        // The SPA must be able to read XSRF-TOKEN from JavaScript...
        $this->assertTrue($cookies->has('XSRF-TOKEN'));
        $this->assertFalse($cookies['XSRF-TOKEN']->isHttpOnly());

        // ...while the session cookie stays script-invisible, unnamed
        // after the framework default, and non-Secure on local HTTP.
        $this->assertTrue($cookies->has('eshop_admin_session'));
        $this->assertTrue($cookies['eshop_admin_session']->isHttpOnly());
        $this->assertFalse($cookies['eshop_admin_session']->isSecure());
    }

    public function test_admin_login_returns_the_admin_session_user_without_password_material(): void
    {
        $admin = User::factory()->create([
            'role' => 'admin',
            'status' => 'active',
            'password' => 'Password123',
        ]);

        $this->withCredentials();
        $csrf = $this->getJson('/api/v1/admin/auth/csrf', ['Origin' => $this->statefulOrigin()]);
        $this->captureResponseCookies($csrf);

        $login = $this->postJson('/api/v1/admin/auth/login', [
            'email' => $admin->email,
            'password' => 'Password123',
        ], $this->statefulHeaders());

        $login->assertOk()
            ->assertJsonStructure(['data' => ['user' => [
                'id', 'email', 'firstName', 'lastName', 'role', 'status', 'createdAt',
            ]]])
            ->assertJsonPath('data.user.id', $admin->id)
            ->assertJsonPath('data.user.role', 'admin');
        $login->assertJsonMissingPath('data.token');
        $login->assertJsonMissingPath('data.user.password');

        $sessionCookie = collect($login->headers->getCookies())
            ->first(fn ($cookie) => $cookie->getName() === 'eshop_admin_session');
        $this->assertNotNull($sessionCookie);
        $this->assertTrue($sessionCookie->isHttpOnly());
    }

    public function test_admin_login_answers_wrong_password_and_customer_account_identically(): void
    {
        User::factory()->create([
            'email' => 'admin-auth-test@eshopverse.test',
            'role' => 'admin',
            'status' => 'active',
            'password' => 'Password123',
        ]);
        User::factory()->create([
            'email' => 'customer-auth-test@example.com',
            'role' => 'customer',
            'status' => 'active',
            'password' => 'Password123',
        ]);

        $headers = ['Origin' => $this->statefulOrigin()];

        $adminWrongPassword = $this->postJson('/api/v1/admin/auth/login', [
            'email' => 'admin-auth-test@eshopverse.test',
            'password' => 'WrongPass123',
        ], $headers);

        $customerSignIn = $this->postJson('/api/v1/admin/auth/login', [
            'email' => 'customer-auth-test@example.com',
            'password' => 'Password123',
        ], $headers);

        $this->assertApiError($adminWrongPassword, 401, 'INVALID_CREDENTIALS');
        $this->assertApiError($customerSignIn, 401, 'INVALID_CREDENTIALS');
        $this->assertSame($adminWrongPassword->json(), $customerSignIn->json());

        // A rejected sign-in must not leave a usable session behind.
        $this->getJson('/api/v1/admin/auth/session', $headers)->assertStatus(401);
    }

    public function test_admin_login_does_not_disclose_suspended_administrators(): void
    {
        User::factory()->create([
            'email' => 'suspended-admin@eshopverse.test',
            'role' => 'admin',
            'status' => 'suspended',
            'password' => 'Password123',
        ]);

        $response = $this->postJson('/api/v1/admin/auth/login', [
            'email' => 'suspended-admin@eshopverse.test',
            'password' => 'Password123',
        ], ['Origin' => $this->statefulOrigin()]);

        $this->assertApiError($response, 401, 'INVALID_CREDENTIALS');
    }

    public function test_session_endpoint_answers_guest_demoted_and_valid_admin_states(): void
    {
        // Guest: no credential at all.
        $this->getJson('/api/v1/admin/auth/session')
            ->assertStatus(401)
            ->assertJsonPath('error.code', 'UNAUTHENTICATED');

        $admin = $this->actingAsAdmin();

        $this->adminJson('GET', '/api/v1/admin/auth/session')
            ->assertOk()
            ->assertJsonPath('data.user.id', $admin->id)
            ->assertJsonPath('data.user.role', 'admin');

        // Demote while the session cookie is still alive.
        User::query()->whereKey($admin->id)->update(['role' => 'customer']);

        // Contract Forbidden: session exists, account no longer admin.
        $this->adminJson('GET', '/api/v1/admin/auth/session')
            ->assertStatus(403)
            ->assertJsonPath('error.code', 'FORBIDDEN');

        // Admin catalog routes answer the same demoted session with 401
        // ("not a valid administrator session").
        $this->adminJson('GET', '/api/v1/admin/products')
            ->assertStatus(401)
            ->assertJsonPath('error.code', 'UNAUTHENTICATED');
    }

    public function test_session_endpoint_answers_401_json_even_without_an_accept_header(): void
    {
        // Plain browser requests that do not send `Accept: application/json`
        // must still receive the contracted 401 JSON. Without the guest
        // redirect override these failed with a 500 because the framework
        // tried to redirect to a `login` route that does not exist.
        $response = $this->withHeaders(['Origin' => $this->statefulOrigin()])
            ->get('/api/v1/admin/auth/session');

        $response->assertStatus(401)
            ->assertJsonPath('error.code', 'UNAUTHENTICATED');
    }

    public function test_logout_destroys_the_admin_session(): void
    {
        $this->actingAsAdmin();

        $this->adminJson('POST', '/api/v1/admin/auth/logout')->assertNoContent();

        $this->adminJson('GET', '/api/v1/admin/auth/session')
            ->assertStatus(401)
            ->assertJsonPath('error.code', 'UNAUTHENTICATED');
    }

    public function test_admin_login_requires_a_stateful_origin(): void
    {
        User::factory()->create(['role' => 'admin', 'status' => 'active', 'password' => 'Password123']);

        // Without Origin/Referer there is no session context and no CSRF
        // context: the contract answers 403 CSRF_TOKEN_MISMATCH.
        $response = $this->postJson('/api/v1/admin/auth/login', [
            'email' => User::query()->where('role', 'admin')->firstOrFail()->email,
            'password' => 'Password123',
        ]);

        $this->assertApiError($response, 403, 'CSRF_TOKEN_MISMATCH');
    }

    public function test_admin_login_enforces_the_xsrf_header_when_csrf_verification_runs(): void
    {
        $admin = User::factory()->create(['role' => 'admin', 'status' => 'active', 'password' => 'Password123']);

        $this->withCredentials();
        $csrf = $this->getJson('/api/v1/admin/auth/csrf', ['Origin' => $this->statefulOrigin()]);
        $csrf->assertNoContent();
        $this->captureResponseCookies($csrf);

        // The test environment normally bypasses CSRF verification, so
        // enforce it explicitly for these two requests to prove the real
        // X-XSRF-TOKEN check behaves per contract.
        $this->app->instance('env', 'local');

        $missing = $this->postJson('/api/v1/admin/auth/login', [
            'email' => $admin->email,
            'password' => 'Password123',
        ], ['Origin' => $this->statefulOrigin()]);

        $this->app->instance('env', 'testing');

        $this->assertApiError($missing, 403, 'CSRF_TOKEN_MISMATCH');

        // The echoed cookie value passes the very same check.
        $this->app->instance('env', 'local');

        $valid = $this->postJson('/api/v1/admin/auth/login', [
            'email' => $admin->email,
            'password' => 'Password123',
        ], $this->statefulHeaders());

        $this->app->instance('env', 'testing');

        $valid->assertOk()->assertJsonPath('data.user.role', 'admin');
    }

    public function test_admin_catalog_mutation_rejects_missing_xsrf_header_when_csrf_runs(): void
    {
        $category = Category::factory()->create();
        $this->actingAsAdmin();

        $this->app->instance('env', 'local');

        $response = $this->json('POST', '/api/v1/admin/products', [
            'name' => 'No Token Lamp',
            'description' => null,
            'price' => '10.00',
            'sku' => null,
            'categoryId' => $category->id,
            'status' => 'active',
            'imageUrls' => [],
        ], ['Origin' => $this->statefulOrigin()]);

        $this->app->instance('env', 'testing');

        $this->assertApiError($response, 403, 'CSRF_TOKEN_MISMATCH');
        $this->assertDatabaseMissing('products', ['name' => 'No Token Lamp']);
    }

    public function test_admin_login_is_rate_limited_after_ten_requests_per_minute(): void
    {
        $blocked = null;

        for ($i = 1; $i <= 11; $i++) {
            $response = $this->postJson('/api/v1/admin/auth/login', [
                'email' => "unknown{$i}@example.com",
                'password' => 'WrongPass123',
            ], ['Origin' => $this->statefulOrigin()]);

            if ($i <= 10) {
                $response->assertStatus(401);
            } else {
                $blocked = $response;
            }
        }

        $this->assertApiError($blocked, 429, 'RATE_LIMITED');
        $this->assertTrue($blocked->headers->has('Retry-After'));
    }

    public function test_every_admin_endpoint_rejects_guest_and_customer_credentials(): void
    {
        [, $token] = $this->registerCustomer();
        $id = Product::factory()->create()->id;

        $endpoints = [
            ['GET', '/api/v1/admin/auth/session'],
            ['POST', '/api/v1/admin/auth/logout'],
            ['GET', '/api/v1/admin/products'],
            ['POST', '/api/v1/admin/products'],
            ['GET', "/api/v1/admin/products/{$id}"],
            ['PUT', "/api/v1/admin/products/{$id}"],
            ['POST', "/api/v1/admin/products/{$id}/archive"],
            ['GET', '/api/v1/admin/categories'],
            ['POST', '/api/v1/admin/categories'],
            ['PUT', "/api/v1/admin/categories/{$id}"],
            ['POST', "/api/v1/admin/categories/{$id}/archive"],
            ['POST', "/api/v1/admin/categories/{$id}/restore"],
            ['GET', '/api/v1/admin/inventory'],
            ['PATCH', "/api/v1/admin/inventory/{$id}"],
        ];

        // Guests are rejected before any payload validation runs.
        foreach ($endpoints as [$method, $uri]) {
            $response = $this->json($method, $uri, []);
            $this->assertApiError($response, 401, 'UNAUTHENTICATED');
        }

        // A customer's bearer token is not an administrator session.
        foreach ($endpoints as [$method, $uri]) {
            $response = $this->withToken($token)->json($method, $uri, []);
            $this->assertApiError($response, 401, 'UNAUTHENTICATED');
        }
    }
}
