<?php

namespace Tests\Feature\Auth;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class LogoutTest extends TestCase
{
    use RefreshDatabase;

    public function test_logout_revokes_only_the_presented_token(): void
    {
        [, $tokenA] = $this->registerCustomer();
        $user = User::query()->where('email', 'customer@example.com')->firstOrFail();
        $tokenB = $user->createToken('second-device')->plainTextToken;

        $this->withToken($tokenA)->postJson('/api/v1/auth/logout')->assertNoContent();

        $this->withToken($tokenA)
            ->getJson('/api/v1/auth/profile')
            ->assertStatus(401);

        $this->withToken($tokenB)
            ->getJson('/api/v1/auth/profile')
            ->assertOk();
    }

    public function test_logout_requires_authentication(): void
    {
        $response = $this->postJson('/api/v1/auth/logout');

        $this->assertApiError($response, 401, 'UNAUTHENTICATED');
    }

    public function test_logout_rejects_revoked_token(): void
    {
        [, $token] = $this->registerCustomer();

        $this->withToken($token)->postJson('/api/v1/auth/logout')->assertNoContent();

        $this->withToken($token)->postJson('/api/v1/auth/logout')->assertStatus(401);
    }
}
