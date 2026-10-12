package com.averonlabs.eshopverse.checkout;

import androidx.annotation.NonNull;

import java.util.UUID;

/**
 * Factory for creating unique, UUID-based idempotency keys per logical attempt
 * according to PHASE-2-SPEC.md §E.4.
 */
public final class IdempotencyKeyFactory {

    public IdempotencyKeyFactory() {
    }

    /**
     * Generates a new unique idempotency key for a logical attempt.
     */
    @NonNull
    public String newKey() {
        return UUID.randomUUID().toString();
    }
}
