<?php

namespace App\Support;

/**
 * The verified claims AUTH-004 needs from a Google ID token. Nothing else
 * from the token is retained, and the raw token itself is never stored.
 */
final readonly class GoogleIdentity
{
    public function __construct(
        public string $subject,
        public string $email,
        public string $firstName,
        public string $lastName,
    ) {}
}
