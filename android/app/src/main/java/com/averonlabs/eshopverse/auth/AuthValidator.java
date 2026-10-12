package com.averonlabs.eshopverse.auth;

import androidx.annotation.Nullable;

import java.util.regex.Pattern;

/**
 * Pure validation rules for authentication and profile forms per docs/android/PHASE-1-SPEC.md §D.3.
 * Free of Android framework dependencies so it can be verified in standard JVM unit tests.
 */
public final class AuthValidator {

    private static final int EMAIL_MAX_LENGTH = 254;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 72;

    private static final int NAME_MIN_LENGTH = 1;
    private static final int NAME_MAX_LENGTH = 80;

    private static final int RESET_TOKEN_MIN_LENGTH = 16;
    private static final int RESET_TOKEN_MAX_LENGTH = 128;

    private AuthValidator() {
    }

    /**
     * Email must be non-null, trimmed length <= 254, and match email format.
     */
    public static boolean isValidEmail(@Nullable String email) {
        if (email == null) {
            return false;
        }
        String trimmed = email.trim();
        if (trimmed.isEmpty() || trimmed.length() > EMAIL_MAX_LENGTH) {
            return false;
        }
        return EMAIL_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Password must be 8–72 characters, non-blank, containing at least one letter and one digit.
     */
    public static boolean isValidPassword(@Nullable String password) {
        if (password == null) {
            return false;
        }
        int len = password.length();
        if (len < PASSWORD_MIN_LENGTH || len > PASSWORD_MAX_LENGTH) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < len; i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        return hasLetter && hasDigit;
    }

    /**
     * Confirm password must be identical to password.
     */
    public static boolean doPasswordsMatch(@Nullable String password, @Nullable String confirmPassword) {
        if (password == null || confirmPassword == null) {
            return false;
        }
        return password.equals(confirmPassword);
    }

    /**
     * First or last name must be 1–80 characters and non-blank after trim.
     */
    public static boolean isValidName(@Nullable String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        return trimmed.length() >= NAME_MIN_LENGTH && trimmed.length() <= NAME_MAX_LENGTH;
    }

    /**
     * Reset token must be 16–128 characters after trim.
     */
    public static boolean isValidResetToken(@Nullable String token) {
        if (token == null) {
            return false;
        }
        String trimmed = token.trim();
        return trimmed.length() >= RESET_TOKEN_MIN_LENGTH && trimmed.length() <= RESET_TOKEN_MAX_LENGTH;
    }
}
