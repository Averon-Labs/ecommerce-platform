<?php

namespace Tests\Feature\Auth;

use App\Models\User;
use Illuminate\Auth\Notifications\ResetPassword;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\Notification;
use Tests\TestCase;

class PasswordResetTest extends TestCase
{
    use RefreshDatabase;

    /**
     * Capture the raw reset token delivered through the (faked) mail flow.
     */
    private function captureResetToken(User $user): string
    {
        Notification::fake();

        $this->postJson('/api/v1/auth/password-reset', ['email' => $user->email])
            ->assertStatus(202);

        $token = null;
        Notification::assertSentTo($user, ResetPassword::class, function (ResetPassword $notification) use (&$token): bool {
            $token = $notification->token;

            return true;
        });

        $this->assertIsString($token);
        $this->assertGreaterThanOrEqual(16, strlen($token));

        return $token;
    }

    public function test_reset_request_always_returns_accepted_without_account_disclosure(): void
    {
        $this->registerCustomer();

        $existing = $this->postJson('/api/v1/auth/password-reset', ['email' => 'customer@example.com']);
        $unknown = $this->postJson('/api/v1/auth/password-reset', ['email' => 'nobody@example.com']);

        $existing->assertStatus(202);
        $unknown->assertStatus(202);
        $this->assertSame([], $existing->json());
        $this->assertSame([], $unknown->json());
    }

    public function test_reset_request_delivers_token_only_through_mail_flow(): void
    {
        $user = User::factory()->create(['email' => 'customer@example.com']);

        Notification::fake();

        $response = $this->postJson('/api/v1/auth/password-reset', ['email' => 'customer@example.com']);

        $response->assertStatus(202);
        $this->assertStringNotContainsString('token', $response->getContent());

        Notification::assertSentTo($user, ResetPassword::class);
    }

    public function test_reset_request_is_rate_limited_per_email(): void
    {
        User::factory()->create(['email' => 'customer@example.com']);
        Notification::fake();

        $blocked = null;
        for ($i = 1; $i <= 4; $i++) {
            $response = $this->postJson('/api/v1/auth/password-reset', ['email' => 'customer@example.com']);
            if ($i <= 3) {
                $response->assertStatus(202);
            } else {
                $blocked = $response;
            }
        }

        $this->assertApiError($blocked, 429, 'RATE_LIMITED');
        $this->assertTrue($blocked->headers->has('Retry-After'));
    }

    public function test_reset_confirm_changes_password_and_revokes_every_token(): void
    {
        [, $tokenA] = $this->registerCustomer();
        $user = User::query()->where('email', 'customer@example.com')->firstOrFail();
        $tokenB = $user->createToken('second-device')->plainTextToken;

        $resetToken = $this->captureResetToken($user);

        $this->postJson('/api/v1/auth/password-reset/confirm', [
            'email' => 'customer@example.com',
            'token' => $resetToken,
            'password' => 'NewPass456',
        ])->assertNoContent();

        // Both existing tokens are revoked.
        $this->withToken($tokenA)->getJson('/api/v1/auth/profile')->assertStatus(401);
        $this->withToken($tokenB)->getJson('/api/v1/auth/profile')->assertStatus(401);

        // The old password no longer works; the new one does.
        $this->postJson('/api/v1/auth/login', [
            'email' => 'customer@example.com',
            'password' => 'Password123',
        ])->assertStatus(401);

        $this->postJson('/api/v1/auth/login', [
            'email' => 'customer@example.com',
            'password' => 'NewPass456',
        ])->assertOk();
    }

    public function test_reset_confirm_rejects_invalid_token_with_contract_code(): void
    {
        $this->registerCustomer();

        $response = $this->postJson('/api/v1/auth/password-reset/confirm', [
            'email' => 'customer@example.com',
            'token' => str_repeat('f', 32),
            'password' => 'NewPass456',
        ]);

        $this->assertApiError($response, 422, 'RESET_TOKEN_INVALID');
    }

    public function test_reset_confirm_rejects_expired_token(): void
    {
        $user = User::factory()->create(['email' => 'customer@example.com']);
        $resetToken = $this->captureResetToken($user);

        Carbon::setTestNow(now()->addMinutes(61));

        $response = $this->postJson('/api/v1/auth/password-reset/confirm', [
            'email' => 'customer@example.com',
            'token' => $resetToken,
            'password' => 'NewPass456',
        ]);

        Carbon::setTestNow();

        $this->assertApiError($response, 422, 'RESET_TOKEN_INVALID');
    }

    public function test_reset_confirm_rejects_unknown_email(): void
    {
        $this->registerCustomer();

        $response = $this->postJson('/api/v1/auth/password-reset/confirm', [
            'email' => 'nobody@example.com',
            'token' => str_repeat('f', 32),
            'password' => 'NewPass456',
        ]);

        $this->assertApiError($response, 422, 'RESET_TOKEN_INVALID');
    }

    public function test_reset_confirm_rejects_invalid_password_with_validation_error(): void
    {
        $user = User::factory()->create(['email' => 'customer@example.com']);
        $resetToken = $this->captureResetToken($user);

        $response = $this->postJson('/api/v1/auth/password-reset/confirm', [
            'email' => 'customer@example.com',
            'token' => $resetToken,
            'password' => 'weak',
        ]);

        $this->assertApiError($response, 422, 'VALIDATION_ERROR');
    }
}
