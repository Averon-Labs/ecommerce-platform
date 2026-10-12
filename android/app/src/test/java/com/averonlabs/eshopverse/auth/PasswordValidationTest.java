package com.averonlabs.eshopverse.auth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * JVM unit tests for password validation bounds, letter+digit requirement,
 * and blank/whitespace handling per docs/android/PHASE-1-SPEC.md §D.7.
 */
public class PasswordValidationTest {

    @Test
    public void testValidPasswords() {
        // Minimum valid: 8 chars, letter + digit
        assertTrue(AuthValidator.isValidPassword("secret01"));
        assertTrue(AuthValidator.isValidPassword("P@ssw0rd"));
        assertTrue(AuthValidator.isValidPassword("1234567a"));
        assertTrue(AuthValidator.isValidPassword("a1234567"));

        // Maximum valid: 72 chars
        String maxValid = "a1" + "x".repeat(70);
        assertTrue(AuthValidator.isValidPassword(maxValid));
    }

    @Test
    public void testLengthBounds() {
        // Less than 8 characters
        assertFalse(AuthValidator.isValidPassword(null));
        assertFalse(AuthValidator.isValidPassword(""));
        assertFalse(AuthValidator.isValidPassword("a1"));
        assertFalse(AuthValidator.isValidPassword("secr3t")); // 6 chars
        assertFalse(AuthValidator.isValidPassword("secret1")); // 7 chars

        // Greater than 72 characters
        String tooLong = "a1" + "x".repeat(71); // 73 chars
        assertFalse(AuthValidator.isValidPassword(tooLong));
    }

    @Test
    public void testRequiresBothLetterAndDigit() {
        // Only letters
        assertFalse(AuthValidator.isValidPassword("secretpass"));
        assertFalse(AuthValidator.isValidPassword("ALLCAPSWORD"));

        // Only digits
        assertFalse(AuthValidator.isValidPassword("12345678"));
        assertFalse(AuthValidator.isValidPassword("0987654321"));

        // Special characters only
        assertFalse(AuthValidator.isValidPassword("!@#$%^&*()"));

        // Letters + special chars, but no digits
        assertFalse(AuthValidator.isValidPassword("secret!@#$"));

        // Digits + special chars, but no letters
        assertFalse(AuthValidator.isValidPassword("12345678!@#"));
    }

    @Test
    public void testBlankAndWhitespaceHandling() {
        assertFalse(AuthValidator.isValidPassword("        "));
        assertFalse(AuthValidator.isValidPassword("\t\n\r    "));
        // Whitespace within bounds but missing letter or digit
        assertFalse(AuthValidator.isValidPassword("   1    "));
        assertFalse(AuthValidator.isValidPassword("   a    "));
    }

    @Test
    public void testPasswordMatchConfirmation() {
        assertTrue(AuthValidator.doPasswordsMatch("secret01", "secret01"));
        assertFalse(AuthValidator.doPasswordsMatch("secret01", "secret02"));
        assertFalse(AuthValidator.doPasswordsMatch(null, "secret01"));
        assertFalse(AuthValidator.doPasswordsMatch("secret01", null));
        assertFalse(AuthValidator.doPasswordsMatch(null, null));
    }
}
