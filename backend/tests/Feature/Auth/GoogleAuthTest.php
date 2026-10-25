<?php

namespace Tests\Feature\Auth;

use App\Models\User;
use Illuminate\Database\QueryException;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Tests\TestCase;

/**
 * AUTH-004: POST /auth/google. Tokens are signed with a locally generated
 * RSA key and Google's JWKS endpoint is faked, so the suite is hermetic
 * and never contacts Google or uses a real credential.
 */
class GoogleAuthTest extends TestCase
{
    use RefreshDatabase;

    private const CLIENT_ID = 'test-google-client-id';

    private const KEY_ID = 'test-key';

    /** @var array{0: \OpenSSLAsymmetricKey, 1: string}|null */
    private static ?array $keyPair = null;

    private bool $googleKeysAvailable = true;

    protected function setUp(): void
    {
        parent::setUp();

        config(['services.google.client_id' => self::CLIENT_ID]);
        Cache::flush();

        Http::fake([
            'https://www.googleapis.com/oauth2/v3/certs' => function () {
                if (! $this->googleKeysAvailable) {
                    return Http::response('', 503);
                }

                return Http::response([
                    'keys' => [[
                        'kty' => 'RSA',
                        'alg' => 'RS256',
                        'use' => 'sig',
                        'kid' => self::KEY_ID,
                        'x5c' => [self::keyPair()[1]],
                    ]],
                ]);
            },
        ]);
    }

    public function test_google_sign_in_creates_customer_account_without_password(): void
    {
        $response = $this->postJson('/api/v1/auth/google', ['idToken' => $this->token()]);

        $response->assertCreated()
            ->assertJsonStructure(['data' => ['token', 'expiresAt', 'user' => ['id', 'email', 'firstName', 'lastName', 'role', 'status']]])
            ->assertJsonPath('data.user.email', 'grace@example.com')
            ->assertJsonPath('data.user.firstName', 'Grace')
            ->assertJsonPath('data.user.lastName', 'Hopper')
            ->assertJsonPath('data.user.role', 'customer')
            ->assertJsonPath('data.user.status', 'active');

        $user = User::where('email', 'grace@example.com')->firstOrFail();

        $this->assertSame('google-subject-001', $user->google_id);
        $this->assertNull($user->password);
        $this->assertNotNull($user->email_verified_at);

        $this->withToken($response->json('data.token'))
            ->getJson('/api/v1/auth/profile')
            ->assertOk();
    }

    public function test_google_sign_in_logs_in_existing_linked_customer(): void
    {
        $user = User::factory()->create([
            'email' => 'grace@example.com',
            'google_id' => 'google-subject-001',
        ]);

        $response = $this->postJson('/api/v1/auth/google', ['idToken' => $this->token()]);

        $response->assertOk()
            ->assertJsonPath('data.user.id', $user->id)
            ->assertJsonStructure(['data' => ['token', 'expiresAt', 'user']]);

        $this->assertSame(1, User::count());
    }

    public function test_google_sign_in_links_existing_password_account(): void
    {
        $user = User::factory()->create(['email' => 'grace@example.com']);

        $response = $this->postJson('/api/v1/auth/google', ['idToken' => $this->token()]);

        $response->assertOk()
            ->assertJsonPath('data.user.id', $user->id);

        $user->refresh();

        $this->assertSame('google-subject-001', $user->google_id);
        $this->assertNotNull($user->password);
        $this->assertSame('customer', $user->role);
    }

    public function test_google_sign_in_refuses_conflicting_google_identity(): void
    {
        User::factory()->create([
            'email' => 'grace@example.com',
            'google_id' => 'a-different-google-subject',
        ]);

        $response = $this->postJson('/api/v1/auth/google', ['idToken' => $this->token()]);

        $this->assertApiError($response, 409, 'GOOGLE_ACCOUNT_CONFLICT');
    }

    public function test_google_sign_in_rejects_suspended_account_without_disclosure(): void
    {
        User::factory()->create([
            'email' => 'grace@example.com',
            'google_id' => 'google-subject-001',
            'status' => 'suspended',
        ]);

        $response = $this->postJson('/api/v1/auth/google', ['idToken' => $this->token()]);

        $this->assertApiError($response, 401, 'INVALID_CREDENTIALS');
    }

    public function test_google_sign_in_rejects_expired_token(): void
    {
        $response = $this->postJson('/api/v1/auth/google', [
            'idToken' => $this->token(['exp' => now()->subHour()->getTimestamp()]),
        ]);

        $this->assertApiError($response, 401, 'INVALID_CREDENTIALS');
    }

    public function test_google_sign_in_rejects_wrong_audience(): void
    {
        $response = $this->postJson('/api/v1/auth/google', [
            'idToken' => $this->token(['aud' => 'a-different-client-id']),
        ]);

        $this->assertApiError($response, 401, 'INVALID_CREDENTIALS');
    }

    public function test_google_sign_in_rejects_wrong_issuer(): void
    {
        $response = $this->postJson('/api/v1/auth/google', [
            'idToken' => $this->token(['iss' => 'https://evil.example']),
        ]);

        $this->assertApiError($response, 401, 'INVALID_CREDENTIALS');
    }

    public function test_google_sign_in_rejects_unverified_email(): void
    {
        $response = $this->postJson('/api/v1/auth/google', [
            'idToken' => $this->token(['email_verified' => false]),
        ]);

        $this->assertApiError($response, 401, 'INVALID_CREDENTIALS');
    }

    public function test_google_sign_in_rejects_token_signed_by_an_unknown_key(): void
    {
        $rogueKey = openssl_pkey_new([
            'private_key_bits' => 2048,
            'private_key_type' => OPENSSL_KEYTYPE_RSA,
        ]);

        $response = $this->postJson('/api/v1/auth/google', [
            'idToken' => $this->token([], $rogueKey),
        ]);

        $this->assertApiError($response, 401, 'INVALID_CREDENTIALS');
    }

    public function test_google_sign_in_rejects_malformed_token(): void
    {
        $response = $this->postJson('/api/v1/auth/google', ['idToken' => 'not-a-valid-jwt-token']);

        $this->assertApiError($response, 401, 'INVALID_CREDENTIALS');
    }

    public function test_google_sign_in_validates_request_body(): void
    {
        $missing = $this->postJson('/api/v1/auth/google', []);
        $this->assertApiError($missing, 422, 'VALIDATION_ERROR');

        $tooShort = $this->postJson('/api/v1/auth/google', ['idToken' => 'short']);
        $this->assertApiError($tooShort, 422, 'VALIDATION_ERROR');

        $oversized = $this->postJson('/api/v1/auth/google', ['idToken' => str_repeat('a', 4097)]);
        $this->assertApiError($oversized, 422, 'VALIDATION_ERROR');

        $unknownField = $this->postJson('/api/v1/auth/google', [
            'idToken' => $this->token(),
            'role' => 'admin',
        ]);
        $this->assertApiError($unknownField, 422, 'VALIDATION_ERROR');
    }

    public function test_google_id_column_is_nullable_and_unique(): void
    {
        User::factory()->create(['google_id' => null]);
        User::factory()->create(['google_id' => null]);

        $this->assertSame(2, User::whereNull('google_id')->count());

        User::factory()->create(['google_id' => 'google-subject-001']);

        $this->expectException(QueryException::class);

        User::factory()->create([
            'email' => 'second@example.com',
            'google_id' => 'google-subject-001',
        ]);
    }

    public function test_google_sign_in_fails_closed_when_client_id_is_not_configured(): void
    {
        config(['services.google.client_id' => null]);

        $response = $this->postJson('/api/v1/auth/google', ['idToken' => $this->token()]);

        $this->assertApiError($response, 500, 'INTERNAL_ERROR');
        $this->assertSame(0, User::count());
    }

    public function test_google_sign_in_fails_closed_when_google_keys_are_unavailable(): void
    {
        $this->googleKeysAvailable = false;

        $response = $this->postJson('/api/v1/auth/google', ['idToken' => $this->token()]);

        $this->assertApiError($response, 500, 'INTERNAL_ERROR');
        $this->assertSame(0, User::count());
    }

    public function test_google_sign_in_is_rate_limited_per_ip_after_ten_requests_per_minute(): void
    {
        $blocked = null;

        for ($i = 1; $i <= 11; $i++) {
            $response = $this->postJson('/api/v1/auth/google', ['idToken' => 'not-a-valid-jwt-token']);

            if ($i <= 10) {
                $response->assertStatus(401);
            } else {
                $blocked = $response;
            }
        }

        $this->assertApiError($blocked, 429, 'RATE_LIMITED');
        $this->assertTrue($blocked->headers->has('Retry-After'));
    }

    /**
     * Sign a Google ID token with the test key.
     *
     * @param  array<string, mixed>  $overrides
     */
    private function token(array $overrides = [], ?\OpenSSLAsymmetricKey $signingKey = null, string $keyId = self::KEY_ID): string
    {
        $header = $this->encode(['alg' => 'RS256', 'kid' => $keyId, 'typ' => 'JWT']);
        $payload = $this->encode(array_merge([
            'iss' => 'https://accounts.google.com',
            'aud' => self::CLIENT_ID,
            'exp' => now()->addHour()->getTimestamp(),
            'iat' => now()->getTimestamp(),
            'sub' => 'google-subject-001',
            'email' => 'grace@example.com',
            'email_verified' => true,
            'name' => 'Grace Hopper',
            'given_name' => 'Grace',
            'family_name' => 'Hopper',
        ], $overrides));

        $signedInput = $header.'.'.$payload;

        openssl_sign($signedInput, $signature, $signingKey ?? self::keyPair()[0], OPENSSL_ALGO_SHA256);

        return $signedInput.'.'.$this->encode($signature);
    }

    /**
     * A fresh RSA key pair and its self-signed certificate (base64 DER,
     * as Google's JWKS `x5c` entries are), shared within the process.
     *
     * @return array{0: \OpenSSLAsymmetricKey, 1: string}
     */
    private static function keyPair(): array
    {
        if (self::$keyPair === null) {
            $resource = openssl_pkey_new([
                'private_key_bits' => 2048,
                'private_key_type' => OPENSSL_KEYTYPE_RSA,
            ]);

            $request = openssl_csr_new(['commonName' => 'eshopverse-test'], $resource, ['digest_alg' => 'sha256']);
            $certificate = openssl_csr_sign($request, null, $resource, 1, ['digest_alg' => 'sha256']);
            openssl_x509_export($certificate, $certificatePem);

            self::$keyPair = [
                $resource,
                preg_replace('/-----(BEGIN|END) CERTIFICATE-----|\s+/', '', $certificatePem),
            ];
        }

        return self::$keyPair;
    }

    private function encode(array|string $value): string
    {
        if (is_array($value)) {
            $value = json_encode($value, JSON_THROW_ON_ERROR);
        }

        return rtrim(strtr(base64_encode($value), '+/', '-_'), '=');
    }
}
