<?php

namespace Tests\Feature\Admin;

use App\Models\User;
use Illuminate\Testing\TestResponse;

/**
 * AUTH-003/CAT-003 test support: signs in through the real
 * GET /admin/auth/csrf → POST /admin/auth/login flow and replays the
 * captured stateful cookies (session + XSRF) on later requests, so every
 * admin test exercises the same cookie/CSRF path as the admin SPA.
 */
trait InteractsWithAdminSession
{
    /** First-party origin that puts a request into Sanctum stateful mode. */
    protected function statefulOrigin(): string
    {
        return 'http://localhost';
    }

    /**
     * Create an administrator and sign in through the API.
     */
    protected function actingAsAdmin(array $overrides = []): User
    {
        $user = User::factory()->create(array_merge([
            'role' => 'admin',
            'status' => 'active',
            'password' => 'Password123',
        ], $overrides));

        $this->signInAdmin($user);

        return $user;
    }

    /**
     * Run the csrf + login flow for an existing admin user, keeping the
     * resulting session and XSRF cookies for subsequent requests.
     */
    protected function signInAdmin(User $user): void
    {
        $this->withCredentials();

        $csrf = $this->getJson('/api/v1/admin/auth/csrf', ['Origin' => $this->statefulOrigin()]);
        $csrf->assertNoContent();
        $this->captureResponseCookies($csrf);

        $login = $this->postJson('/api/v1/admin/auth/login', [
            'email' => $user->email,
            'password' => 'Password123',
        ], $this->statefulHeaders());
        $login->assertOk();
        $this->captureResponseCookies($login);
    }

    /**
     * Stateful request headers, echoing the captured XSRF cookie value
     * in X-XSRF-TOKEN exactly as the SPA must.
     *
     * @return array<string, string>
     */
    protected function statefulHeaders(): array
    {
        $headers = ['Origin' => $this->statefulOrigin()];

        if (isset($this->unencryptedCookies['XSRF-TOKEN'])) {
            $headers['X-XSRF-TOKEN'] = $this->unencryptedCookies['XSRF-TOKEN'];
        }

        return $headers;
    }

    /**
     * Persist every Set-Cookie of a response for later requests. Values
     * are already in wire (encrypted) form and are replayed verbatim.
     */
    protected function captureResponseCookies(TestResponse $response): void
    {
        foreach ($response->headers->getCookies() as $cookie) {
            $this->withUnencryptedCookie($cookie->getName(), $cookie->getValue());
        }
    }

    /**
     * Send a stateful authenticated admin request.
     *
     * @param  array<string, mixed>  $data
     */
    protected function adminJson(string $method, string $uri, array $data = []): TestResponse
    {
        return $this->json($method, $uri, $data, $this->statefulHeaders());
    }
}
