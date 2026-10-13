<?php

namespace Tests\Feature\Auth;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Carbon;
use Tests\TestCase;

class LoginTest extends TestCase
{
    use RefreshDatabase;

    public function test_login_returns_bearer_token_for_valid_credentials(): void
    {
        [$userId] = $this->registerCustomer();

        $response = $this->postJson('/api/v1/auth/login', [
            'email' => 'customer@example.com',
            'password' => 'Password123',
        ]);

        $response->assertOk()
            ->assertJsonStructure(['data' => ['token', 'expiresAt', 'user' => ['id', 'email']]])
            ->assertJsonPath('data.user.id', $userId);

        $this->withToken($response->json('data.token'))
            ->getJson('/api/v1/auth/profile')
            ->assertOk();
    }

    public function test_login_wrong_password_and_unknown_email_return_identical_vague_error(): void
    {
        $this->registerCustomer();

        $wrongPassword = $this->postJson('/api/v1/auth/login', [
            'email' => 'customer@example.com',
            'password' => 'WrongPass123',
        ]);
        $unknownEmail = $this->postJson('/api/v1/auth/login', [
            'email' => 'nobody@example.com',
            'password' => 'WrongPass123',
        ]);

        $this->assertApiError($wrongPassword, 401, 'INVALID_CREDENTIALS');
        $this->assertApiError($unknownEmail, 401, 'INVALID_CREDENTIALS');
        $this->assertSame($wrongPassword->json(), $unknownEmail->json());
    }

    public function test_login_does_not_disclose_suspended_account_with_correct_password(): void
    {
        User::factory()->create([
            'email' => 'suspended@example.com',
            'password' => 'Password123',
            'status' => 'suspended',
        ]);

        $response = $this->postJson('/api/v1/auth/login', [
            'email' => 'suspended@example.com',
            'password' => 'Password123',
        ]);

        $this->assertApiError($response, 401, 'INVALID_CREDENTIALS');
    }

    public function test_login_locks_account_after_five_consecutive_failures(): void
    {
        $this->registerCustomer();

        for ($i = 1; $i <= 5; $i++) {
            $this->postJson('/api/v1/auth/login', [
                'email' => 'customer@example.com',
                'password' => "WrongPass{$i}x",
            ])->assertStatus(401);
        }

        // Even the correct password must not disclose the lockout state.
        $locked = $this->postJson('/api/v1/auth/login', [
            'email' => 'customer@example.com',
            'password' => 'Password123',
        ]);

        $this->assertApiError($locked, 429, 'ACCOUNT_LOCKED');
        $this->assertTrue($locked->headers->has('Retry-After'));
        $this->assertGreaterThanOrEqual(1, (int) $locked->headers->get('Retry-After'));
    }

    public function test_account_unlocks_after_fifteen_minutes(): void
    {
        $this->registerCustomer();

        for ($i = 1; $i <= 5; $i++) {
            $this->postJson('/api/v1/auth/login', [
                'email' => 'customer@example.com',
                'password' => "WrongPass{$i}x",
            ])->assertStatus(401);
        }

        Carbon::setTestNow(now()->addMinutes(16));

        $this->postJson('/api/v1/auth/login', [
            'email' => 'customer@example.com',
            'password' => 'Password123',
        ])->assertOk();

        Carbon::setTestNow();
    }

    public function test_successful_login_clears_the_failure_counter(): void
    {
        $this->registerCustomer();

        for ($i = 1; $i <= 4; $i++) {
            $this->postJson('/api/v1/auth/login', [
                'email' => 'customer@example.com',
                'password' => "WrongPass{$i}x",
            ])->assertStatus(401);
        }

        $this->postJson('/api/v1/auth/login', [
            'email' => 'customer@example.com',
            'password' => 'Password123',
        ])->assertOk();

        // A fresh streak of four failures must not trip the lockout.
        for ($i = 1; $i <= 4; $i++) {
            $this->postJson('/api/v1/auth/login', [
                'email' => 'customer@example.com',
                'password' => "WrongPass{$i}y",
            ])->assertStatus(401);
        }

        $this->postJson('/api/v1/auth/login', [
            'email' => 'customer@example.com',
            'password' => 'Password123',
        ])->assertOk();
    }

    public function test_login_is_rate_limited_per_ip_after_ten_requests_per_minute(): void
    {
        $blocked = null;

        for ($i = 1; $i <= 11; $i++) {
            $response = $this->postJson('/api/v1/auth/login', [
                'email' => "user{$i}@example.com",
                'password' => 'WrongPass123',
            ]);

            if ($i <= 10) {
                $response->assertStatus(401);
            } else {
                $blocked = $response;
            }
        }

        $this->assertApiError($blocked, 429, 'RATE_LIMITED');
        $this->assertTrue($blocked->headers->has('Retry-After'));
    }
}
