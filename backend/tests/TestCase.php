<?php

namespace Tests;

use Illuminate\Foundation\Testing\TestCase as BaseTestCase;
use Illuminate\Testing\TestResponse;

abstract class TestCase extends BaseTestCase
{
    /**
     * Laravel caches resolved auth guards across multiple HTTP calls made
     * inside one test process. Production requests are separate processes,
     * so forget the cached guards after every call to keep token
     * revocation (logout, password reset) honestly observable.
     *
     * @param  array<string, mixed>  $parameters
     * @param  array<string, mixed>  $cookies
     * @param  array<string, mixed>  $files
     * @param  array<string, mixed>  $server
     */
    public function call($method, $uri, $parameters = [], $cookies = [], $files = [], $server = [], $content = null)
    {
        $response = parent::call($method, $uri, $parameters, $cookies, $files, $server, $content);

        $this->app->make('auth')->forgetGuards();

        return $response;
    }

    /**
     * Assert the shared contract error envelope:
     * { "error": { "code": "...", "message": "...", "details": [...] } }.
     */
    protected function assertApiError(TestResponse $response, int $status, string $code): void
    {
        $response->assertStatus($status);
        $response->assertJsonStructure(['error' => ['code', 'message']]);
        $response->assertJsonPath('error.code', $code);
    }

    /**
     * Register a customer through the API and return [userId, plainTextToken].
     *
     * @return array{int, string}
     */
    protected function registerCustomer(array $overrides = []): array
    {
        $payload = array_merge([
            'email' => 'customer@example.com',
            'password' => 'Password123',
            'firstName' => 'Ada',
            'lastName' => 'Lovelace',
        ], $overrides);

        $response = $this->postJson('/api/v1/auth/register', $payload);
        $response->assertStatus(201);

        return [$response->json('data.user.id'), $response->json('data.token')];
    }
}
