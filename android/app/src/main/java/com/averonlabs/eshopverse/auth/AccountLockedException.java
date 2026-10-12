package com.averonlabs.eshopverse.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.averonlabs.eshopverse.core.net.ApiException;

import java.util.Map;

/**
 * Thrown when an account is locked (HTTP 429 with ACCOUNT_LOCKED),
 * preserving the server-specified Retry-After duration.
 */
public final class AccountLockedException extends ApiException {

    private final int retryAfterSeconds;

    public AccountLockedException(
            int httpStatus,
            @NonNull String code,
            @NonNull String message,
            @Nullable Map<String, String> fieldErrors,
            int retryAfterSeconds
    ) {
        super(httpStatus, code, message, fieldErrors);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
