<?php

namespace App\Services;

use App\Exceptions\ApiException;
use App\Support\GoogleIdentity;
use Illuminate\Http\Client\ConnectionException;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Str;

/**
 * AUTH-004: verifies a Google Sign-In ID token without an SDK dependency.
 *
 * The RS256 signature is checked against Google's published JWKS, then the
 * issuer, audience, expiry, and verified-email claims are enforced. The raw
 * token and its signature are never stored, cached, logged, or returned.
 */
class GoogleAuthService
{
    private const JWKS_URL = 'https://www.googleapis.com/oauth2/v3/certs';

    private const JWKS_CACHE_KEY = 'auth-google:jwks';

    private const JWKS_CACHE_SECONDS = 3600;

    /** @var array<int, string> */
    private const ALLOWED_ISSUERS = ['https://accounts.google.com', 'accounts.google.com'];

    /** Tolerance for client/server clock differences when checking `exp` and `nbf`. */
    private const CLOCK_SKEW_SECONDS = 60;

    /**
     * Verify the ID token and return the identity it asserts.
     *
     * @throws ApiException 401 when the token fails any verification step;
     *                      500 when the endpoint is unconfigured or Google's
     *                      JWKS cannot be retrieved.
     */
    public function verify(string $idToken): GoogleIdentity
    {
        $audiences = $this->configuredAudiences();

        if ($audiences === []) {
            // Fail closed: without an expected audience no token can be trusted.
            $this->serverError();
        }

        $segments = explode('.', $idToken);

        if (count($segments) !== 3) {
            $this->reject();
        }

        [$encodedHeader, $encodedPayload, $encodedSignature] = $segments;

        $header = $this->decodeJson($encodedHeader);
        $claims = $this->decodeJson($encodedPayload);
        $signature = $this->decodeBase64Url($encodedSignature);

        if ($header === null || $claims === null || $signature === false) {
            $this->reject();
        }

        if (($header['alg'] ?? null) !== 'RS256' || ! is_string($header['kid'] ?? null)) {
            $this->reject();
        }

        $this->verifyClaims($claims, $audiences);
        $this->verifySignature($encodedHeader.'.'.$encodedPayload, $signature, $header['kid']);

        [$firstName, $lastName] = $this->extractName($claims);

        return new GoogleIdentity(
            subject: $claims['sub'],
            email: strtolower($claims['email']),
            firstName: $firstName,
            lastName: $lastName,
        );
    }

    /**
     * Every first-party OAuth client ID this deployment trusts.
     *
     * `services.google.client_id` may be a single string or an array, and
     * `services.google.client_ids` may be an array or a comma-separated
     * string, so Android, web, and iOS clients can share one deployment.
     * Blank entries are ignored and duplicates keep their first position.
     *
     * @return array<int, string>
     */
    private function configuredAudiences(): array
    {
        $primary = config('services.google.client_id');
        $additional = config('services.google.client_ids', '');

        $candidates = array_merge(
            is_array($primary) ? $primary : [$primary],
            is_array($additional) ? $additional : explode(',', (string) $additional),
        );

        $audiences = [];

        foreach ($candidates as $candidate) {
            if (! is_string($candidate)) {
                continue;
            }

            $candidate = trim($candidate);

            if ($candidate !== '' && ! in_array($candidate, $audiences, true)) {
                $audiences[] = $candidate;
            }
        }

        return $audiences;
    }

    /**
     * @param  array<string, mixed>  $claims
     * @param  array<int, string>  $audiences
     */
    private function verifyClaims(array $claims, array $audiences): void
    {
        $now = now()->getTimestamp();

        $issuer = $claims['iss'] ?? null;

        if (! is_string($issuer) || ! in_array($issuer, self::ALLOWED_ISSUERS, true)) {
            $this->reject();
        }

        $audience = $claims['aud'] ?? null;
        $claimedAudiences = is_string($audience) ? [$audience] : (is_array($audience) ? $audience : []);

        $audienceMatches = false;

        foreach ($claimedAudiences as $claimed) {
            if (! is_string($claimed)) {
                continue;
            }

            foreach ($audiences as $trusted) {
                if (hash_equals($trusted, $claimed)) {
                    $audienceMatches = true;

                    break 2;
                }
            }
        }

        if (! $audienceMatches) {
            $this->reject();
        }

        $expiresAt = $claims['exp'] ?? null;

        if (! is_int($expiresAt) || $expiresAt < $now - self::CLOCK_SKEW_SECONDS) {
            $this->reject();
        }

        $notBefore = $claims['nbf'] ?? null;

        if ($notBefore !== null && (! is_int($notBefore) || $notBefore > $now + self::CLOCK_SKEW_SECONDS)) {
            $this->reject();
        }

        if (($claims['email_verified'] ?? null) !== true) {
            $this->reject();
        }

        if (! is_string($claims['email'] ?? null) || $claims['email'] === '') {
            $this->reject();
        }

        if (! is_string($claims['sub'] ?? null) || $claims['sub'] === '') {
            $this->reject();
        }
    }

    private function verifySignature(string $signedInput, string $signature, string $keyId): void
    {
        $jwk = $this->findJwk($keyId);
        $certificate = $jwk['x5c'][0] ?? null;

        if (! is_string($certificate) || $certificate === '') {
            $this->reject();
        }

        $publicKey = openssl_pkey_get_public(
            "-----BEGIN CERTIFICATE-----\n".chunk_split($certificate, 64, "\n").'-----END CERTIFICATE-----',
        );

        if ($publicKey === false || openssl_verify($signedInput, $signature, $publicKey, OPENSSL_ALGO_SHA256) !== 1) {
            $this->reject();
        }
    }

    /**
     * @return array<string, mixed>
     */
    private function findJwk(string $keyId): array
    {
        $jwk = $this->findKey($this->fetchJwks(), $keyId);

        if ($jwk === null) {
            // Google may have rotated its keys; refetch once before rejecting.
            Cache::forget(self::JWKS_CACHE_KEY);
            $jwk = $this->findKey($this->fetchJwks(), $keyId);
        }

        if ($jwk === null) {
            $this->reject();
        }

        return $jwk;
    }

    /**
     * @param  array<int, array<string, mixed>>  $keys
     * @return array<string, mixed>|null
     */
    private function findKey(array $keys, string $keyId): ?array
    {
        foreach ($keys as $key) {
            if (($key['kid'] ?? null) === $keyId) {
                return $key;
            }
        }

        return null;
    }

    /**
     * @return array<int, array<string, mixed>>
     */
    private function fetchJwks(): array
    {
        return Cache::remember(self::JWKS_CACHE_KEY, self::JWKS_CACHE_SECONDS, function (): array {
            try {
                $response = Http::acceptJson()->timeout(5)->get(self::JWKS_URL);
            } catch (ConnectionException) {
                $this->serverError();
            }

            $keys = $response->successful() ? $response->json('keys') : null;

            if (! is_array($keys)) {
                $this->serverError();
            }

            return array_values(array_filter($keys, is_array(...)));
        });
    }

    /**
     * @param  array<string, mixed>  $claims
     * @return array{string, string}
     */
    private function extractName(array $claims): array
    {
        $given = is_string($claims['given_name'] ?? null) ? trim($claims['given_name']) : '';
        $family = is_string($claims['family_name'] ?? null) ? trim($claims['family_name']) : '';
        $full = is_string($claims['name'] ?? null) ? trim($claims['name']) : '';
        $pieces = $full === '' ? [] : preg_split('/\s+/', $full);

        $given = $given !== '' ? $given : ($pieces[0] ?? Str::before($claims['email'].'@', '@'));
        $family = $family !== '' ? $family : implode(' ', array_slice($pieces, 1));

        return [
            Str::limit($given, 80, ''),
            Str::limit($family === '' ? '-' : $family, 80, ''),
        ];
    }

    /**
     * @return array<string, mixed>|null
     */
    private function decodeJson(string $encoded): ?array
    {
        $decoded = $this->decodeBase64Url($encoded);

        if ($decoded === false) {
            return null;
        }

        $value = json_decode($decoded, true);

        return is_array($value) ? $value : null;
    }

    private function decodeBase64Url(string $encoded): string|false
    {
        $padding = (4 - strlen($encoded) % 4) % 4;

        return base64_decode(strtr($encoded, '-_', '+/').str_repeat('=', $padding), true);
    }

    private function reject(): never
    {
        throw new ApiException(
            401,
            'INVALID_CREDENTIALS',
            'The Google sign-in credential is invalid or expired.',
        );
    }

    private function serverError(): never
    {
        throw new ApiException(500, 'INTERNAL_ERROR', 'An unexpected error occurred.');
    }
}
