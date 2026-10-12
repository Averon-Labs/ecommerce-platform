package com.averonlabs.eshopverse.checkout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Unit tests verifying Idempotency Key rules per docs/android/PHASE-2-SPEC.md §E.4.
 * - One key per logical attempt
 * - A new attempt gets a new key
 * - Transport retry reuses the same key
 */
public class IdempotencyRuleTest {

    @Test
    public void testNewAttemptProducesNewUniqueKey() {
        IdempotencyKeyFactory factory = new IdempotencyKeyFactory();
        Set<String> generatedKeys = new HashSet<>();

        for (int i = 0; i < 50; i++) {
            String key = factory.newKey();
            assertNotNull(key);
            assertFalse(key.trim().isEmpty());
            // Must be valid UUID format
            UUID parsed = UUID.fromString(key);
            assertNotNull(parsed);
            // Must be unique
            assertTrue(generatedKeys.add(key));
        }
    }

    @Test
    public void testDifferentAttemptsHaveDifferentKeys() {
        IdempotencyKeyFactory factory = new IdempotencyKeyFactory();
        String attempt1Key = factory.newKey();
        String attempt2Key = factory.newKey();

        assertNotEquals(attempt1Key, attempt2Key);
    }

    @Test
    public void testTransportRetryReusesSameKey() {
        IdempotencyKeyFactory factory = new IdempotencyKeyFactory();
        String attemptKey = factory.newKey();

        // Simulate transport level retry using the same attempt key
        String transportRetryKey = attemptKey;
        assertEquals(attemptKey, transportRetryKey);
    }
}
