package com.averonlabs.eshopverse.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.error.ApiErrorCodes;
import com.averonlabs.eshopverse.core.error.ErrorMessages;
import com.averonlabs.eshopverse.core.model.AuthSession;
import com.averonlabs.eshopverse.core.model.User;
import com.averonlabs.eshopverse.core.net.ApiException;
import com.averonlabs.eshopverse.core.state.UiState;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * ViewModel managing authentication forms, validation, and submission states
 * per docs/android/PHASE-1-SPEC.md §D.
 */
public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;

    private final MutableLiveData<UiState<AuthSession>> loginState = new MutableLiveData<>();
    private final MutableLiveData<UiState<AuthSession>> registerState = new MutableLiveData<>();
    private final MutableLiveData<UiState<Void>> resetRequestState = new MutableLiveData<>();
    private final MutableLiveData<UiState<Void>> resetConfirmState = new MutableLiveData<>();
    private final MutableLiveData<UiState<User>> profileUpdateState = new MutableLiveData<>();

    // Client-side and server-side field errors
    private final MutableLiveData<Integer> emailErrorRes = new MutableLiveData<>();
    private final MutableLiveData<Integer> passwordErrorRes = new MutableLiveData<>();
    private final MutableLiveData<Integer> confirmPasswordErrorRes = new MutableLiveData<>();
    private final MutableLiveData<Integer> firstNameErrorRes = new MutableLiveData<>();
    private final MutableLiveData<Integer> lastNameErrorRes = new MutableLiveData<>();
    private final MutableLiveData<Integer> resetTokenErrorRes = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> serverFieldErrors = new MutableLiveData<>(Collections.emptyMap());

    // Lockout countdown seconds (for 429 ACCOUNT_LOCKED)
    private final MutableLiveData<Integer> lockoutSeconds = new MutableLiveData<>(0);

    private boolean isSubmitting = false;

    public AuthViewModel(@NonNull AuthRepository authRepository) {
        this.authRepository = Objects.requireNonNull(authRepository, "authRepository cannot be null");
    }

    public LiveData<UiState<AuthSession>> getLoginState() {
        return loginState;
    }

    public LiveData<UiState<AuthSession>> getRegisterState() {
        return registerState;
    }

    public LiveData<UiState<Void>> getResetRequestState() {
        return resetRequestState;
    }

    public LiveData<UiState<Void>> getResetConfirmState() {
        return resetConfirmState;
    }

    public LiveData<UiState<User>> getProfileUpdateState() {
        return profileUpdateState;
    }

    public LiveData<Integer> getEmailErrorRes() {
        return emailErrorRes;
    }

    public LiveData<Integer> getPasswordErrorRes() {
        return passwordErrorRes;
    }

    public LiveData<Integer> getConfirmPasswordErrorRes() {
        return confirmPasswordErrorRes;
    }

    public LiveData<Integer> getFirstNameErrorRes() {
        return firstNameErrorRes;
    }

    public LiveData<Integer> getLastNameErrorRes() {
        return lastNameErrorRes;
    }

    public LiveData<Integer> getResetTokenErrorRes() {
        return resetTokenErrorRes;
    }

    public LiveData<Map<String, String>> getServerFieldErrors() {
        return serverFieldErrors;
    }

    public LiveData<Integer> getLockoutSeconds() {
        return lockoutSeconds;
    }

    public boolean isSubmitting() {
        return isSubmitting;
    }

    public void clearErrors() {
        emailErrorRes.setValue(null);
        passwordErrorRes.setValue(null);
        confirmPasswordErrorRes.setValue(null);
        firstNameErrorRes.setValue(null);
        lastNameErrorRes.setValue(null);
        resetTokenErrorRes.setValue(null);
        serverFieldErrors.setValue(Collections.emptyMap());
    }

    /**
     * Attempts sign in with email and password.
     */
    public void login(@Nullable String email, @Nullable String password) {
        if (isSubmitting) {
            return;
        }

        clearErrors();
        boolean valid = true;

        if (!AuthValidator.isValidEmail(email)) {
            emailErrorRes.setValue(R.string.auth_error_email_invalid);
            valid = false;
        }
        if (!AuthValidator.isValidPassword(password)) {
            passwordErrorRes.setValue(R.string.auth_error_password_invalid);
            valid = false;
        }

        if (!valid) {
            return;
        }

        isSubmitting = true;
        loginState.setValue(UiState.loading());

        authRepository.loginCustomer(email.trim(), password, new AuthCallback<AuthSession>() {
            @Override
            public void onSuccess(AuthSession data) {
                isSubmitting = false;
                loginState.postValue(UiState.content(data));
            }

            @Override
            public void onError(Throwable throwable) {
                isSubmitting = false;
                handleAuthError(throwable, loginState);
            }
        });
    }

    /**
     * Attempts sign in with a Google ID token (AUTH-004).
     */
    public void loginWithGoogle(@NonNull String idToken) {
        if (isSubmitting) {
            return;
        }

        clearErrors();
        isSubmitting = true;
        loginState.setValue(UiState.loading());

        authRepository.loginWithGoogle(idToken, new AuthCallback<AuthSession>() {
            @Override
            public void onSuccess(AuthSession data) {
                isSubmitting = false;
                loginState.postValue(UiState.content(data));
            }

            @Override
            public void onError(Throwable throwable) {
                isSubmitting = false;
                handleAuthError(throwable, loginState);
            }
        });
    }

    /**
     * Attempts registration of new customer account.
     */
    public void register(
            @Nullable String email,
            @Nullable String password,
            @Nullable String confirmPassword,
            @Nullable String firstName,
            @Nullable String lastName
    ) {
        if (isSubmitting) {
            return;
        }

        clearErrors();
        boolean valid = true;

        if (!AuthValidator.isValidEmail(email)) {
            emailErrorRes.setValue(R.string.auth_error_email_invalid);
            valid = false;
        }
        if (!AuthValidator.isValidPassword(password)) {
            passwordErrorRes.setValue(R.string.auth_error_password_invalid);
            valid = false;
        }
        if (!AuthValidator.doPasswordsMatch(password, confirmPassword)) {
            confirmPasswordErrorRes.setValue(R.string.auth_error_confirm_password_mismatch);
            valid = false;
        }
        if (!AuthValidator.isValidName(firstName)) {
            firstNameErrorRes.setValue(R.string.auth_error_first_name_required);
            valid = false;
        }
        if (!AuthValidator.isValidName(lastName)) {
            lastNameErrorRes.setValue(R.string.auth_error_last_name_required);
            valid = false;
        }

        if (!valid) {
            return;
        }

        isSubmitting = true;
        registerState.setValue(UiState.loading());

        authRepository.registerCustomer(
                email.trim(),
                password,
                firstName.trim(),
                lastName.trim(),
                new AuthCallback<AuthSession>() {
                    @Override
                    public void onSuccess(AuthSession data) {
                        isSubmitting = false;
                        registerState.postValue(UiState.content(data));
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        isSubmitting = false;
                        handleAuthError(throwable, registerState);
                    }
                }
        );
    }

    /**
     * Submits a request for password reset link.
     */
    public void requestPasswordReset(@Nullable String email) {
        if (isSubmitting) {
            return;
        }

        clearErrors();
        if (!AuthValidator.isValidEmail(email)) {
            emailErrorRes.setValue(R.string.auth_error_email_invalid);
            return;
        }

        isSubmitting = true;
        resetRequestState.setValue(UiState.loading());

        authRepository.requestPasswordReset(email.trim(), new AuthCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                isSubmitting = false;
                resetRequestState.postValue(UiState.empty()); // neutral confirmation
            }

            @Override
            public void onError(Throwable throwable) {
                isSubmitting = false;
                handleAuthError(throwable, resetRequestState);
            }
        });
    }

    /**
     * Sets new password using reset token.
     */
    public void confirmPasswordReset(
            @Nullable String token,
            @Nullable String newPassword,
            @Nullable String confirmPassword
    ) {
        if (isSubmitting) {
            return;
        }

        clearErrors();
        boolean valid = true;

        if (!AuthValidator.isValidResetToken(token)) {
            resetTokenErrorRes.setValue(R.string.auth_error_reset_token_required);
            valid = false;
        }
        if (!AuthValidator.isValidPassword(newPassword)) {
            passwordErrorRes.setValue(R.string.auth_error_password_invalid);
            valid = false;
        }
        if (!AuthValidator.doPasswordsMatch(newPassword, confirmPassword)) {
            confirmPasswordErrorRes.setValue(R.string.auth_error_confirm_password_mismatch);
            valid = false;
        }

        if (!valid) {
            return;
        }

        isSubmitting = true;
        resetConfirmState.setValue(UiState.loading());

        authRepository.confirmPasswordReset(token.trim(), newPassword, new AuthCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                isSubmitting = false;
                resetConfirmState.postValue(UiState.empty());
            }

            @Override
            public void onError(Throwable throwable) {
                isSubmitting = false;
                handleAuthError(throwable, resetConfirmState);
            }
        });
    }

    /**
     * Updates profile name.
     */
    public void updateProfile(@Nullable String firstName, @Nullable String lastName) {
        if (isSubmitting) {
            return;
        }

        clearErrors();
        boolean valid = true;

        if (!AuthValidator.isValidName(firstName)) {
            firstNameErrorRes.setValue(R.string.auth_error_first_name_required);
            valid = false;
        }
        if (!AuthValidator.isValidName(lastName)) {
            lastNameErrorRes.setValue(R.string.auth_error_last_name_required);
            valid = false;
        }

        if (!valid) {
            return;
        }

        isSubmitting = true;
        profileUpdateState.setValue(UiState.loading());

        authRepository.updateProfile(firstName.trim(), lastName.trim(), new AuthCallback<User>() {
            @Override
            public void onSuccess(User data) {
                isSubmitting = false;
                profileUpdateState.postValue(UiState.content(data));
            }

            @Override
            public void onError(Throwable throwable) {
                isSubmitting = false;
                handleAuthError(throwable, profileUpdateState);
            }
        });
    }

    private <T> void handleAuthError(Throwable throwable, MutableLiveData<UiState<T>> stateLiveData) {
        if (throwable instanceof AccountLockedException) {
            AccountLockedException lockedEx = (AccountLockedException) throwable;
            lockoutSeconds.postValue(lockedEx.getRetryAfterSeconds());
            stateLiveData.postValue(UiState.error(R.string.auth_error_account_locked, false));
            return;
        }

        if (throwable instanceof ApiException) {
            ApiException apiException = (ApiException) throwable;
            Map<String, String> fieldErrors = apiException.getFieldErrors();
            if (!fieldErrors.isEmpty()) {
                serverFieldErrors.postValue(fieldErrors);
            }

            if (ApiErrorCodes.EMAIL_ALREADY_REGISTERED.equals(apiException.getCode())) {
                emailErrorRes.postValue(R.string.error_email_already_registered);
            } else if (ApiErrorCodes.INVALID_CREDENTIALS.equals(apiException.getCode())) {
                passwordErrorRes.postValue(R.string.error_invalid_credentials);
            } else if (ApiErrorCodes.RESET_TOKEN_INVALID.equals(apiException.getCode())) {
                resetTokenErrorRes.postValue(R.string.auth_error_reset_token_invalid_state);
            }

            int messageRes = ErrorMessages.getMessageRes(apiException.getCode());
            stateLiveData.postValue(UiState.error(messageRes, apiException.getHttpStatus() >= 500));
            return;
        }

        // Generic / network timeout error
        stateLiveData.postValue(UiState.error(R.string.error_internal, true));
    }
}
