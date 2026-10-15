<?php

namespace Tests\Feature\Auth;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class ProfileTest extends TestCase
{
    use RefreshDatabase;

    public function test_profile_requires_authentication(): void
    {
        $this->assertApiError($this->getJson('/api/v1/auth/profile'), 401, 'UNAUTHENTICATED');
        $this->assertApiError(
            $this->patchJson('/api/v1/auth/profile', ['firstName' => 'X']),
            401,
            'UNAUTHENTICATED',
        );
    }

    public function test_profile_returns_the_authenticated_user(): void
    {
        [$userId, $token] = $this->registerCustomer();

        $this->withToken($token)
            ->getJson('/api/v1/auth/profile')
            ->assertOk()
            ->assertJsonStructure([
                'data' => ['id', 'email', 'firstName', 'lastName', 'role', 'status', 'createdAt'],
            ])
            ->assertJsonPath('data.id', $userId)
            ->assertJsonPath('data.email', 'customer@example.com')
            ->assertJsonPath('data.firstName', 'Ada')
            ->assertJsonPath('data.role', 'customer')
            ->assertJsonMissingPath('data.password')
            ->assertJsonMissingPath('data.tokens');
    }

    public function test_profile_update_changes_only_first_and_last_name(): void
    {
        [, $token] = $this->registerCustomer();

        $this->withToken($token)
            ->patchJson('/api/v1/auth/profile', [
                'firstName' => 'Augusta Ada',
                'lastName' => 'King-Lovelace',
            ])
            ->assertOk()
            ->assertJsonPath('data.firstName', 'Augusta Ada')
            ->assertJsonPath('data.lastName', 'King-Lovelace')
            ->assertJsonPath('data.email', 'customer@example.com')
            ->assertJsonPath('data.role', 'customer');

        $this->assertDatabaseHas('users', [
            'email' => 'customer@example.com',
            'first_name' => 'Augusta Ada',
            'last_name' => 'King-Lovelace',
            'role' => 'customer',
        ]);
    }

    public function test_profile_update_can_change_a_single_field(): void
    {
        [, $token] = $this->registerCustomer();

        $this->withToken($token)
            ->patchJson('/api/v1/auth/profile', ['lastName' => 'Byron'])
            ->assertOk()
            ->assertJsonPath('data.lastName', 'Byron')
            ->assertJsonPath('data.firstName', 'Ada');
    }

    public function test_profile_update_rejects_email_role_and_status_fields(): void
    {
        [, $token] = $this->registerCustomer();

        $response = $this->withToken($token)->patchJson('/api/v1/auth/profile', [
            'email' => 'attacker@example.com',
            'role' => 'admin',
            'status' => 'suspended',
        ]);

        $this->assertApiError($response, 422, 'VALIDATION_ERROR');

        $this->assertDatabaseHas('users', [
            'email' => 'customer@example.com',
            'role' => 'customer',
            'status' => 'active',
        ]);
    }

    public function test_profile_update_rejects_empty_body(): void
    {
        [, $token] = $this->registerCustomer();

        $response = $this->withToken($token)->patchJson('/api/v1/auth/profile', []);

        $this->assertApiError($response, 422, 'VALIDATION_ERROR');
    }

    public function test_profile_update_rejects_invalid_name_lengths(): void
    {
        [, $token] = $this->registerCustomer();

        $response = $this->withToken($token)->patchJson('/api/v1/auth/profile', [
            'firstName' => str_repeat('x', 81),
        ]);

        $this->assertApiError($response, 422, 'VALIDATION_ERROR');
    }

    public function test_profile_is_not_reachable_with_another_users_token_shape(): void
    {
        // Ownership is implicit for a token-authenticated profile: the token
        // alone selects the user; no user identifier is accepted from clients.
        [, $token] = $this->registerCustomer();

        $this->withToken($token)
            ->getJson('/api/v1/auth/profile?userId=2')
            ->assertOk()
            ->assertJsonPath('data.email', 'customer@example.com');

        $this->assertSame(1, User::query()->count());
    }
}
