<?php

namespace Tests\Feature\Auth;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class RegisterTest extends TestCase
{
    use RefreshDatabase;

    private function validPayload(array $overrides = []): array
    {
        return array_merge([
            'email' => 'ada@example.com',
            'password' => 'Password123',
            'firstName' => 'Ada',
            'lastName' => 'Lovelace',
        ], $overrides);
    }

    public function test_register_creates_customer_and_returns_working_bearer_token(): void
    {
        $response = $this->postJson('/api/v1/auth/register', $this->validPayload());

        $response->assertStatus(201)
            ->assertJsonStructure([
                'data' => [
                    'token',
                    'expiresAt',
                    'user' => ['id', 'email', 'firstName', 'lastName', 'role', 'status', 'createdAt'],
                ],
            ])
            ->assertJsonPath('data.user.email', 'ada@example.com')
            ->assertJsonPath('data.user.firstName', 'Ada')
            ->assertJsonPath('data.user.lastName', 'Lovelace')
            ->assertJsonPath('data.user.role', 'customer')
            ->assertJsonPath('data.user.status', 'active');

        $this->assertDatabaseHas('users', ['email' => 'ada@example.com']);

        $this->withToken($response->json('data.token'))
            ->getJson('/api/v1/auth/profile')
            ->assertOk()
            ->assertJsonPath('data.email', 'ada@example.com');
    }

    public function test_register_stores_password_as_hash_never_plaintext(): void
    {
        $this->postJson('/api/v1/auth/register', $this->validPayload())->assertStatus(201);

        $user = User::query()->where('email', 'ada@example.com')->firstOrFail();

        $this->assertTrue(Hash::check('Password123', $user->password));
        $this->assertNotSame('Password123', $user->password);
        $this->assertStringStartsWith('$2y$', $user->password);
    }

    public function test_register_token_lifetime_is_capped_at_thirty_days(): void
    {
        $this->assertSame(43200, (int) config('sanctum.expiration'));

        Carbon::setTestNow('2026-10-12 12:00:00');
        $response = $this->postJson('/api/v1/auth/register', $this->validPayload());
        $expiresAt = Carbon::parse($response->json('data.expiresAt'));
        Carbon::setTestNow();

        $this->assertTrue($expiresAt->equalTo(Carbon::parse('2026-10-12 12:00:00')->addDays(30)));
    }

    public function test_register_normalizes_email_to_lowercase(): void
    {
        $response = $this->postJson('/api/v1/auth/register', $this->validPayload([
            'email' => '  Ada@Example.COM ',
        ]));

        $response->assertStatus(201)
            ->assertJsonPath('data.user.email', 'ada@example.com');

        $this->assertDatabaseHas('users', ['email' => 'ada@example.com']);
    }

    public function test_register_rejects_duplicate_email_regardless_of_letter_case(): void
    {
        $this->registerCustomer(['email' => 'ada@example.com']);

        $response = $this->postJson('/api/v1/auth/register', $this->validPayload([
            'email' => 'ADA@Example.com',
        ]));

        $this->assertApiError($response, 409, 'EMAIL_ALREADY_REGISTERED');
        $this->assertSame(1, User::query()->whereRaw('LOWER(email) = ?', ['ada@example.com'])->count());
    }

    public function test_register_rejects_duplicate_email_with_conflict(): void
    {
        $this->registerCustomer(['email' => 'ada@example.com']);

        $response = $this->postJson('/api/v1/auth/register', $this->validPayload());

        $this->assertApiError($response, 409, 'EMAIL_ALREADY_REGISTERED');
        $this->assertSame(1, User::query()->where('email', 'ada@example.com')->count());
    }

    public function test_register_rejects_invalid_payloads_with_validation_error(): void
    {
        // At most five cases per test: the customer-register limiter allows
        // five requests per hour per IP.
        $invalidPayloads = [
            'missing everything' => [],
            'malformed email' => $this->validPayload(['email' => 'not-an-email']),
            'password too short' => $this->validPayload(['password' => 'Ab1']),
            'password without digit' => $this->validPayload(['password' => 'abcdefghij']),
            'empty firstName' => $this->validPayload(['firstName' => '']),
        ];

        foreach ($invalidPayloads as $scenario => $payload) {
            $response = $this->postJson('/api/v1/auth/register', $payload);
            $this->assertApiError($response, 422, 'VALIDATION_ERROR');
            $this->assertSame(0, User::query()->count(), "A register attempt with $scenario must not create a user.");
        }
    }

    public function test_register_rejects_disallowed_or_boundary_fields_with_validation_error(): void
    {
        $invalidPayloads = [
            'password without letter' => $this->validPayload(['password' => '1234567890']),
            'overlong email' => $this->validPayload(['email' => str_repeat('a', 250).'@example.com']),
            'overlong lastName' => $this->validPayload(['lastName' => str_repeat('x', 81)]),
            'role field is not editable' => $this->validPayload(['role' => 'admin']),
            'status field is not allowed' => $this->validPayload(['status' => 'active']),
        ];

        foreach ($invalidPayloads as $scenario => $payload) {
            $response = $this->postJson('/api/v1/auth/register', $payload);
            $this->assertApiError($response, 422, 'VALIDATION_ERROR');
            $this->assertSame(0, User::query()->count(), "A register attempt with $scenario must not create a user.");
        }
    }

    public function test_register_is_rate_limited_per_ip_after_five_requests_per_hour(): void
    {
        $blocked = null;

        for ($i = 1; $i <= 6; $i++) {
            $response = $this->postJson('/api/v1/auth/register', $this->validPayload([
                'email' => "customer$i@example.com",
            ]));

            if ($i <= 5) {
                $response->assertStatus(201);
            } else {
                $blocked = $response;
            }
        }

        $this->assertApiError($blocked, 429, 'RATE_LIMITED');
        $this->assertTrue($blocked->headers->has('Retry-After'));
    }
}
