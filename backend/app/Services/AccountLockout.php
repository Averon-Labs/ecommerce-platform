<?php

namespace App\Services;

use Illuminate\Support\Facades\Cache;

/**
 * Counts consecutive failed sign-in attempts per email address
 * (docs/openapi.yaml login operation: 5 consecutive failures lock the
 * account for 15 minutes; a successful sign-in resets the counter).
 */
class AccountLockout
{
    private const MAX_FAILURES = 5;

    private const LOCK_SECONDS = 900;

    private function countKey(string $email): string
    {
        return 'auth:failures:'.strtolower($email);
    }

    private function lockKey(string $email): string
    {
        return 'auth:lock:'.strtolower($email);
    }

    public function isLocked(string $email): bool
    {
        return $this->lockedUntil($email) !== null;
    }

    public function registerFailure(string $email): void
    {
        $email = strtolower($email);
        $count = (int) Cache::get($this->countKey($email), 0) + 1;

        if ($count >= self::MAX_FAILURES) {
            Cache::forget($this->countKey($email));
            Cache::put($this->lockKey($email), now()->addSeconds(self::LOCK_SECONDS)->getTimestamp(), self::LOCK_SECONDS);

            return;
        }

        Cache::put($this->countKey($email), $count, self::LOCK_SECONDS);
    }

    public function clear(string $email): void
    {
        Cache::forget($this->countKey($email));
        Cache::forget($this->lockKey($email));
    }

    /**
     * Seconds the client must wait before the account unlocks (0 when unlocked).
     */
    public function retryAfter(string $email): int
    {
        $until = $this->lockedUntil($email);

        return $until === null ? 0 : max(1, $until - now()->getTimestamp());
    }

    private function lockedUntil(string $email): ?int
    {
        $until = Cache::get($this->lockKey($email));

        return is_int($until) && $until > now()->getTimestamp() ? $until : null;
    }
}
