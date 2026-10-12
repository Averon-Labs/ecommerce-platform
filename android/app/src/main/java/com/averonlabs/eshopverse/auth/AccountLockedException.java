package com.averonlabs.eshopverse.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.averonlabs.eshopverse.core.net.ApiException;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Thrown when an account is locked (HTTP 429 with ACCOUNT_LOCKED),
 * preserving the server-specified Retry-After duration.
 */
public final class AccountLockedException extends IOException {

    private final int httpStatus;
    private final String code;
    private final Map<String, String> fieldErrors;
    private final int retryAfterSeconds;

    public AccountLockedException(
            int httpStatus,
            @NonNull String code,
            @NonNull String message,
            @Nullable Map<String, String> fieldErrors,
            int retryAfterSeconds
    ) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = Objects.requireNonNull(code, "code cannot be null");
        this.fieldErrors = fieldErrors != null ? Collections.unmodifiableMap(fieldErrors) : Collections.emptyMap();
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    @NonNull
    public String getCode() {
        return code;
    }

    @NonNull
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
