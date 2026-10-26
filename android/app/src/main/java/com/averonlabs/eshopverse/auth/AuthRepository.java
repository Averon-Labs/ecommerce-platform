package com.averonlabs.eshopverse.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.averonlabs.eshopverse.BuildConfig;
import com.averonlabs.eshopverse.core.error.ApiErrorCodes;
import com.averonlabs.eshopverse.core.model.AuthSession;
import com.averonlabs.eshopverse.core.model.GoogleAuthRequest;
import com.averonlabs.eshopverse.core.model.LoginRequest;
import com.averonlabs.eshopverse.core.model.PasswordResetConfirmRequest;
import com.averonlabs.eshopverse.core.model.PasswordResetRequest;
import com.averonlabs.eshopverse.core.model.RegisterRequest;
import com.averonlabs.eshopverse.core.model.UpdateProfileRequest;
import com.averonlabs.eshopverse.core.model.User;
import com.averonlabs.eshopverse.core.net.ApiErrorMapper;
import com.averonlabs.eshopverse.core.net.ApiException;
import com.averonlabs.eshopverse.core.net.ApiResponse;
import com.averonlabs.eshopverse.core.net.ApiRoutes;
import com.averonlabs.eshopverse.core.storage.TokenStore;

import java.io.IOException;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository coordinating customer authentication, session tokens, and profile operations
 * per docs/android/PHASE-1-SPEC.md §D.
 */
public class AuthRepository {

    private final ApiRoutes apiRoutes;
    private final TokenStore tokenStore;

    public AuthRepository(@NonNull ApiRoutes apiRoutes, @NonNull TokenStore tokenStore) {
        this(apiRoutes, tokenStore, BuildConfig.DEBUG);
    }

    /**
     * @param isDebugMode retained for source compatibility with existing tests.
     *                    Legacy offline mock tokens are rejected in every build now
     *                    that authentication always goes to the real API.
     */
    public AuthRepository(
            @NonNull ApiRoutes apiRoutes,
            @NonNull TokenStore tokenStore,
            boolean isDebugMode
    ) {
        this.apiRoutes = Objects.requireNonNull(apiRoutes, "apiRoutes cannot be null");
        this.tokenStore = Objects.requireNonNull(tokenStore, "tokenStore cannot be null");
    }

    /**
     * Checks if the user has an active, non-expired session token.
     *
     * <p>The old offline mock bearer token is always rejected and purged, even
     * in debug builds. It is not valid against the live API and would otherwise
     * keep a stale demo session (previously "Alex Morgan") alive after an
     * upgrade.</p>
     */
    public boolean isSignedIn() {
        if ("demo-jwt-offline-token-sample".equals(tokenStore.getToken())) {
            tokenStore.clear();
            return false;
        }
        return !tokenStore.isExpired() && tokenStore.getToken() != null;
    }

    /**
     * Authenticates a customer with email and password (M-06).
     * On success, persists the returned bearer token into protected storage.
     */
    public void loginCustomer(
            @NonNull String email,
            @NonNull String password,
            @NonNull AuthCallback<AuthSession> callback
    ) {
        LoginRequest request = new LoginRequest(email, password);
        apiRoutes.loginCustomer(request).enqueue(new Callback<ApiResponse<AuthSession>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<AuthSession>> call,
                    @NonNull Response<ApiResponse<AuthSession>> response
            ) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    AuthSession session = response.body().getData();
                    persistSessionSafely(session);
                    callback.onSuccess(session);
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<AuthSession>> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t);
            }
        });
    }

    /**
     * Authenticates a customer using a Google ID token (AUTH-004).
     * On success, persists the returned bearer token into protected storage.
     */
    public void loginWithGoogle(
            @NonNull String idToken,
            @NonNull AuthCallback<AuthSession> callback
    ) {
        GoogleAuthRequest request = new GoogleAuthRequest(idToken);
        apiRoutes.loginWithGoogle(request).enqueue(new Callback<ApiResponse<AuthSession>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<AuthSession>> call,
                    @NonNull Response<ApiResponse<AuthSession>> response
            ) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    AuthSession session = response.body().getData();
                    persistSessionSafely(session);
                    callback.onSuccess(session);
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<AuthSession>> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t);
            }
        });
    }

    /**
     * Registers a new customer account (M-07).
     * On success, persists the returned bearer token into protected storage.
     */
    public void registerCustomer(
            @NonNull String email,
            @NonNull String password,
            @NonNull String firstName,
            @NonNull String lastName,
            @NonNull AuthCallback<AuthSession> callback
    ) {
        RegisterRequest request = new RegisterRequest(email, password, firstName, lastName);
        apiRoutes.registerCustomer(request).enqueue(new Callback<ApiResponse<AuthSession>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<AuthSession>> call,
                    @NonNull Response<ApiResponse<AuthSession>> response
            ) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    AuthSession session = response.body().getData();
                    persistSessionSafely(session);
                    callback.onSuccess(session);
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<AuthSession>> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t);
            }
        });
    }

    /**
     * Signs out the customer (M-17).
     * In accordance with §D.5, tokenStore.clear() executes regardless of the API outcome.
     */
    public void logoutCustomer(@NonNull AuthCallback<Void> callback) {
        apiRoutes.logoutCustomer().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(
                    @NonNull Call<Void> call,
                    @NonNull Response<Void> response
            ) {
                tokenStore.clear();
                callback.onSuccess(null);
            }

            @Override
            public void onFailure(
                    @NonNull Call<Void> call,
                    @NonNull Throwable t
            ) {
                // Failed revoke is recorded as non-fatal; local session must still end.
                tokenStore.clear();
                callback.onSuccess(null);
            }
        });
    }

    /**
     * Fetches current customer profile (M-17).
     * Short-circuits with an unauthenticated error if the token has expired before attempting network.
     */
    public void getProfile(@NonNull AuthCallback<User> callback) {
        if (!isSignedIn()) {
            callback.onError(new ApiException(401, ApiErrorCodes.UNAUTHENTICATED, "Session expired", null));
            return;
        }

        apiRoutes.getCustomerProfile().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<User>> call,
                    @NonNull Response<ApiResponse<User>> response
            ) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<User>> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t);
            }
        });
    }

    /**
     * Updates customer first and last name (M-17).
     */
    public void updateProfile(
            @NonNull String firstName,
            @NonNull String lastName,
            @NonNull AuthCallback<User> callback
    ) {
        if (!isSignedIn()) {
            callback.onError(new ApiException(401, ApiErrorCodes.UNAUTHENTICATED, "Session expired", null));
            return;
        }

        UpdateProfileRequest request = new UpdateProfileRequest(firstName, lastName);
        apiRoutes.updateCustomerProfile(request).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<User>> call,
                    @NonNull Response<ApiResponse<User>> response
            ) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<User>> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t);
            }
        });
    }

    /**
     * Requests a password reset link/instructions (M-08).
     */
    public void requestPasswordReset(
            @NonNull String email,
            @NonNull AuthCallback<Void> callback
    ) {
        PasswordResetRequest request = new PasswordResetRequest(email);
        apiRoutes.requestPasswordReset(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(
                    @NonNull Call<Void> call,
                    @NonNull Response<Void> response
            ) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<Void> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t);
            }
        });
    }

    /**
     * Sets a new password using a reset token (M-09).
     * On success, local session is cleared since server invalidates existing tokens.
     */
    public void confirmPasswordReset(
            @NonNull String email,
            @NonNull String token,
            @NonNull String newPassword,
            @NonNull AuthCallback<Void> callback
    ) {
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(email, token, newPassword);
        apiRoutes.confirmPasswordReset(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(
                    @NonNull Call<Void> call,
                    @NonNull Response<Void> response
            ) {
                if (response.isSuccessful()) {
                    tokenStore.clear();
                    callback.onSuccess(null);
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<Void> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t);
            }
        });
    }

    public void confirmPasswordReset(
            @NonNull String token,
            @NonNull String newPassword,
            @NonNull AuthCallback<Void> callback
    ) {
        confirmPasswordReset("", token, newPassword, callback);
    }

    public TokenStore getTokenStore() {
        return tokenStore;
    }

    @NonNull
    private IOException parseError(@NonNull Response<?> response) {
        String body = null;
        if (response.errorBody() != null) {
            try {
                body = response.errorBody().string();
            } catch (IOException ignored) {
            }
        }
        ApiException apiException = ApiErrorMapper.map(response.code(), body);

        // Check if ACCOUNT_LOCKED (429) to attach Retry-After seconds
        if (response.code() == 429 && ApiErrorCodes.ACCOUNT_LOCKED.equals(apiException.getCode())) {
            int retryAfter = parseRetryAfter(response.headers().get("Retry-After"));
            return new AccountLockedException(
                    apiException.getHttpStatus(),
                    apiException.getCode(),
                    apiException.getMessage(),
                    apiException.getFieldErrors(),
                    retryAfter
            );
        }

        return apiException;
    }

    private int parseRetryAfter(@Nullable String header) {
        if (header != null) {
            try {
                return Math.max(1, Integer.parseInt(header.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return 60; // Safe default lockout seconds
    }

    public static String calculateDefaultExpiresAt() {
        return java.time.Instant.now()
                .plus(30, java.time.temporal.ChronoUnit.DAYS)
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
                .toString();
    }

    private void persistSessionSafely(@NonNull AuthSession session) {
        String token = session.getToken();
        String expiresAt = session.getExpiresAt();
        if (expiresAt == null || expiresAt.trim().isEmpty()) {
            expiresAt = calculateDefaultExpiresAt();
        }
        tokenStore.save(token, expiresAt);
    }
}
