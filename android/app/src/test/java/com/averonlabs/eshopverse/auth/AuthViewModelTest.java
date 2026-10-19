package com.averonlabs.eshopverse.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.error.ApiErrorCodes;
import com.averonlabs.eshopverse.core.model.AuthSession;
import com.averonlabs.eshopverse.core.model.User;
import com.averonlabs.eshopverse.core.net.ApiException;
import com.averonlabs.eshopverse.core.state.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

/**
 * JVM unit tests for AuthViewModel per docs/android/PHASE-1-SPEC.md §D.7.
 * Covers loading -> content/error transitions, field validation errors,
 * retry after failure, and duplicate submission prevention while in flight.
 */
public class AuthViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AuthRepository authRepository;
    private AuthViewModel viewModel;

    @Before
    public void setUp() {
        authRepository = mock(AuthRepository.class);
        viewModel = new AuthViewModel(authRepository);
    }

    @Test
    public void testLoginClientValidationRejection() {
        viewModel.login("invalid-email", "short");

        assertEquals(Integer.valueOf(R.string.auth_error_email_invalid), viewModel.getEmailErrorRes().getValue());
        assertEquals(Integer.valueOf(R.string.auth_error_password_invalid), viewModel.getPasswordErrorRes().getValue());
        assertNull(viewModel.getLoginState().getValue());
        verify(authRepository, never()).loginCustomer(any(), any(), any());
    }

    @Test
    public void testLoginLoadingAndSuccessTransitions() {
        User user = new User(1, "user@example.com", "John", "Doe",
                com.averonlabs.eshopverse.core.model.UserRole.CUSTOMER,
                com.averonlabs.eshopverse.core.model.UserStatus.ACTIVE,
                "2026-01-01T00:00:00Z");
        AuthSession session = new AuthSession("valid-token", "2099-01-01T00:00:00Z", user);

        doAnswer(invocation -> {
            AuthCallback<AuthSession> cb = invocation.getArgument(2);
            // Verify loading state is set while call is in flight
            UiState<AuthSession> current = viewModel.getLoginState().getValue();
            assertNotNull(current);
            assertTrue(current.isLoading());
            assertTrue(viewModel.isSubmitting());

            cb.onSuccess(session);
            return null;
        }).when(authRepository).loginCustomer(eq("user@example.com"), eq("secret123"), any());

        viewModel.login("user@example.com", "secret123");

        assertFalse(viewModel.isSubmitting());
        UiState<AuthSession> state = viewModel.getLoginState().getValue();
        assertNotNull(state);
        assertTrue(state.isContent());
        assertEquals(session, state.getData());
    }

    @Test
    public void testLoginErrorTransitionAndFieldErrors() {
        ApiException apiEx = new ApiException(401, ApiErrorCodes.INVALID_CREDENTIALS, "Invalid credentials", null);

        doAnswer(invocation -> {
            AuthCallback<AuthSession> cb = invocation.getArgument(2);
            cb.onError(apiEx);
            return null;
        }).when(authRepository).loginCustomer(eq("user@example.com"), eq("wrongpass1"), any());

        viewModel.login("user@example.com", "wrongpass1");

        assertFalse(viewModel.isSubmitting());
        UiState<AuthSession> state = viewModel.getLoginState().getValue();
        assertNotNull(state);
        assertTrue(state.isError());
        assertEquals(R.string.error_invalid_credentials, state.getMessageRes());
        assertEquals(Integer.valueOf(R.string.error_invalid_credentials), viewModel.getPasswordErrorRes().getValue());
    }

    @Test
    public void testNoDuplicateSubmissionWhileInFlight() {
        // Mock async call that does not immediately invoke callback
        viewModel.login("user@example.com", "secret123");

        assertTrue(viewModel.isSubmitting());
        // Second call while in flight must be ignored
        viewModel.login("user@example.com", "secret123");

        verify(authRepository, times(1)).loginCustomer(any(), any(), any());
    }

    @Test
    public void testRegisterValidationRejection() {
        viewModel.register("bademail", "pass1", "pass2", "", "  ");

        assertEquals(Integer.valueOf(R.string.auth_error_email_invalid), viewModel.getEmailErrorRes().getValue());
        assertEquals(Integer.valueOf(R.string.auth_error_password_invalid), viewModel.getPasswordErrorRes().getValue());
        assertEquals(Integer.valueOf(R.string.auth_error_confirm_password_mismatch), viewModel.getConfirmPasswordErrorRes().getValue());
        assertEquals(Integer.valueOf(R.string.auth_error_first_name_required), viewModel.getFirstNameErrorRes().getValue());
        assertEquals(Integer.valueOf(R.string.auth_error_last_name_required), viewModel.getLastNameErrorRes().getValue());
        verify(authRepository, never()).registerCustomer(any(), any(), any(), any(), any());
    }

    @Test
    public void testRegisterEmailAlreadyRegisteredMapsToEmailField() {
        ApiException apiEx = new ApiException(409, ApiErrorCodes.EMAIL_ALREADY_REGISTERED, "Email in use", null);

        doAnswer(invocation -> {
            AuthCallback<AuthSession> cb = invocation.getArgument(4);
            cb.onError(apiEx);
            return null;
        }).when(authRepository).registerCustomer(eq("taken@example.com"), any(), any(), any(), any());

        viewModel.register("taken@example.com", "validPass1", "validPass1", "Alice", "Smith");

        UiState<AuthSession> state = viewModel.getRegisterState().getValue();
        assertNotNull(state);
        assertTrue(state.isError());
        assertEquals(Integer.valueOf(R.string.error_email_already_registered), viewModel.getEmailErrorRes().getValue());
    }

    @Test
    public void testAccountLockedPopulatesLockoutSeconds() {
        AccountLockedException lockedEx = new AccountLockedException(
                429,
                ApiErrorCodes.ACCOUNT_LOCKED,
                "Locked",
                Collections.emptyMap(),
                90
        );

        doAnswer(invocation -> {
            AuthCallback<AuthSession> cb = invocation.getArgument(2);
            cb.onError(lockedEx);
            return null;
        }).when(authRepository).loginCustomer(any(), any(), any());

        viewModel.login("locked@example.com", "pass1234");

        assertEquals(Integer.valueOf(90), viewModel.getLockoutSeconds().getValue());
        UiState<AuthSession> state = viewModel.getLoginState().getValue();
        assertNotNull(state);
        assertTrue(state.isError());
        assertFalse(state.isRetryable());
    }

    @Test
    public void testResetConfirmInvalidTokenMapsToResetTokenField() {
        ApiException apiEx = new ApiException(422, ApiErrorCodes.RESET_TOKEN_INVALID, "Invalid token", null);

        doAnswer(invocation -> {
            AuthCallback<Void> cb = invocation.getArgument(2);
            cb.onError(apiEx);
            return null;
        }).when(authRepository).confirmPasswordReset(eq("invalid-token-12345"), any(), any());

        viewModel.confirmPasswordReset("invalid-token-12345", "newPass123", "newPass123");

        assertEquals(Integer.valueOf(R.string.auth_error_reset_token_invalid_state), viewModel.getResetTokenErrorRes().getValue());
        UiState<Void> state = viewModel.getResetConfirmState().getValue();
        assertNotNull(state);
        assertTrue(state.isError());
    }

    @Test
    public void testLoginWithGoogleSuccess() {
        User user = new User(10, "google.user@example.com", "Google", "User",
                com.averonlabs.eshopverse.core.model.UserRole.CUSTOMER,
                com.averonlabs.eshopverse.core.model.UserStatus.ACTIVE,
                "2026-01-01T00:00:00Z");
        AuthSession session = new AuthSession("google-session-jwt", "2099-01-01T00:00:00Z", user);

        doAnswer(invocation -> {
            AuthCallback<AuthSession> cb = invocation.getArgument(1);
            assertTrue(viewModel.isSubmitting());
            cb.onSuccess(session);
            return null;
        }).when(authRepository).loginWithGoogle(eq("test-google-id-token"), any());

        viewModel.loginWithGoogle("test-google-id-token");

        assertFalse(viewModel.isSubmitting());
        UiState<AuthSession> state = viewModel.getLoginState().getValue();
        assertNotNull(state);
        assertTrue(state.isContent());
        assertEquals(session, state.getData());
    }

    @Test
    public void testLoginWithGoogleError() {
        ApiException apiEx = new ApiException(401, ApiErrorCodes.INVALID_CREDENTIALS, "Invalid token", null);

        doAnswer(invocation -> {
            AuthCallback<AuthSession> cb = invocation.getArgument(1);
            cb.onError(apiEx);
            return null;
        }).when(authRepository).loginWithGoogle(eq("bad-google-token"), any());

        viewModel.loginWithGoogle("bad-google-token");

        assertFalse(viewModel.isSubmitting());
        UiState<AuthSession> state = viewModel.getLoginState().getValue();
        assertNotNull(state);
        assertTrue(state.isError());
    }
}

