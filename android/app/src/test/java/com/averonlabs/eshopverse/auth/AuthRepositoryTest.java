package com.averonlabs.eshopverse.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.averonlabs.eshopverse.ApiFixtureLoader;
import com.averonlabs.eshopverse.core.error.ApiErrorCodes;
import com.averonlabs.eshopverse.core.model.AuthSession;
import com.averonlabs.eshopverse.core.model.User;
import com.averonlabs.eshopverse.core.net.ApiClient;
import com.averonlabs.eshopverse.core.net.ApiException;
import com.averonlabs.eshopverse.core.storage.InMemoryTokenStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * JVM unit tests for AuthRepository per docs/android/PHASE-1-SPEC.md §D.7.
 * Covers success parsing for all seven operations, 401/409/422/429 error mapping,
 * token extraction and persistence, and sign-out / expired-token behavior.
 */
public class AuthRepositoryTest {

    private MockWebServer mockWebServer;
    private InMemoryTokenStore tokenStore;
    private AuthRepository authRepository;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        tokenStore = new InMemoryTokenStore();
        ApiClient apiClient = new ApiClient(
                mockWebServer.url("/api/v1/").toString(),
                tokenStore,
                null,
                false
        );
        authRepository = new AuthRepository(apiClient.getRoutes(), tokenStore);
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testLoginSuccessAndTokenPersistence() throws Exception {
        String fixture = ApiFixtureLoader.load("loginCustomer", "success");
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(fixture));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<AuthSession> resultRef = new AtomicReference<>();

        authRepository.loginCustomer("customer@example.com", "secret123", new AuthCallback<AuthSession>() {
            @Override
            public void onSuccess(AuthSession data) {
                resultRef.set(data);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/api/v1/auth/login", request.getPath());

        AuthSession session = resultRef.get();
        assertNotNull(session);
        assertEquals("test-jwt-bearer-token-12345", session.getToken());
        assertEquals("customer@example.com", session.getUser().getEmail());

        // Token must be saved in storage
        assertTrue(authRepository.isSignedIn());
        assertEquals("test-jwt-bearer-token-12345", tokenStore.getToken());
    }

    @Test
    public void testLoginInvalidCredentials401() throws Exception {
        String fixture = ApiFixtureLoader.load("loginCustomer", "invalid-credentials");
        mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody(fixture));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        authRepository.loginCustomer("wrong@example.com", "badpassword", new AuthCallback<AuthSession>() {
            @Override
            public void onSuccess(AuthSession data) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                errorRef.set(throwable);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(errorRef.get() instanceof ApiException);
        ApiException ex = (ApiException) errorRef.get();
        assertEquals(401, ex.getHttpStatus());
        assertEquals(ApiErrorCodes.INVALID_CREDENTIALS, ex.getCode());
        assertFalse(authRepository.isSignedIn());
    }

    @Test
    public void testRegisterSuccessAndTokenPersistence() throws Exception {
        String fixture = ApiFixtureLoader.load("loginCustomer", "success");
        mockWebServer.enqueue(new MockResponse().setResponseCode(201).setBody(fixture));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<AuthSession> resultRef = new AtomicReference<>();

        authRepository.registerCustomer("new@example.com", "secret123", "Jane", "Doe", new AuthCallback<AuthSession>() {
            @Override
            public void onSuccess(AuthSession data) {
                resultRef.set(data);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/api/v1/auth/register", request.getPath());
        assertNotNull(resultRef.get());
        assertTrue(authRepository.isSignedIn());
    }

    @Test
    public void testRegisterEmailAlreadyRegistered409() throws Exception {
        String body = "{\"error\":{\"code\":\"EMAIL_ALREADY_REGISTERED\",\"message\":\"Email already in use\"}}";
        mockWebServer.enqueue(new MockResponse().setResponseCode(409).setBody(body));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        authRepository.registerCustomer("dup@example.com", "secret123", "Jane", "Doe", new AuthCallback<AuthSession>() {
            @Override
            public void onSuccess(AuthSession data) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                errorRef.set(throwable);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(errorRef.get() instanceof ApiException);
        ApiException ex = (ApiException) errorRef.get();
        assertEquals(409, ex.getHttpStatus());
        assertEquals(ApiErrorCodes.EMAIL_ALREADY_REGISTERED, ex.getCode());
    }

    @Test
    public void testAccountLocked429WithRetryAfter() throws Exception {
        String body = "{\"error\":{\"code\":\"ACCOUNT_LOCKED\",\"message\":\"Account locked due to excessive failed attempts\"}}";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "120")
                .setBody(body));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        authRepository.loginCustomer("locked@example.com", "pass1234", new AuthCallback<AuthSession>() {
            @Override
            public void onSuccess(AuthSession data) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                errorRef.set(throwable);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(errorRef.get() instanceof AccountLockedException);
        AccountLockedException lockedEx = (AccountLockedException) errorRef.get();
        assertEquals(429, lockedEx.getHttpStatus());
        assertEquals(ApiErrorCodes.ACCOUNT_LOCKED, lockedEx.getCode());
        assertEquals(120, lockedEx.getRetryAfterSeconds());
    }

    @Test
    public void testSignOutClearsTokenEvenWhenApiFails() throws Exception {
        tokenStore.save("active-token", "2099-01-01T00:00:00Z");
        assertTrue(authRepository.isSignedIn());

        // API returns 500 error
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("{\"error\":{\"code\":\"INTERNAL_ERROR\",\"message\":\"Server error\"}}"));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean successCalled = new AtomicBoolean(false);

        authRepository.logoutCustomer(new AuthCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                successCalled.set(true);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(successCalled.get());
        // Token must be wiped even when server logout failed
        assertFalse(authRepository.isSignedIn());
        assertNull(tokenStore.getToken());
    }

    @Test
    public void testGetProfileShortCircuitsWhenExpired() throws Exception {
        tokenStore.save("expired-token", "2020-01-01T00:00:00Z"); // expired in past
        assertFalse(authRepository.isSignedIn());

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        authRepository.getProfile(new AuthCallback<User>() {
            @Override
            public void onSuccess(User data) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                errorRef.set(throwable);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        // Server should not have received any request!
        assertEquals(0, mockWebServer.getRequestCount());
        assertTrue(errorRef.get() instanceof ApiException);
        assertEquals(401, ((ApiException) errorRef.get()).getHttpStatus());
    }

    @Test
    public void testGetProfileSuccessWhenSignedIn() throws Exception {
        tokenStore.save("valid-token", "2099-01-01T00:00:00Z");
        String body = "{\"data\":{\"id\":1,\"email\":\"user@example.com\",\"firstName\":\"Alice\",\"lastName\":\"Smith\",\"role\":\"customer\",\"status\":\"active\",\"createdAt\":\"2026-01-01T00:00:00Z\"}}";
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(body));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<User> userRef = new AtomicReference<>();

        authRepository.getProfile(new AuthCallback<User>() {
            @Override
            public void onSuccess(User data) {
                userRef.set(data);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(userRef.get());
        assertEquals("Alice", userRef.get().getFirstName());
        assertEquals("user@example.com", userRef.get().getEmail());
    }

    @Test
    public void testUpdateProfileSuccess() throws Exception {
        tokenStore.save("valid-token", "2099-01-01T00:00:00Z");
        String body = "{\"data\":{\"id\":1,\"email\":\"user@example.com\",\"firstName\":\"AliceUpdated\",\"lastName\":\"SmithUpdated\",\"role\":\"customer\",\"status\":\"active\",\"createdAt\":\"2026-01-01T00:00:00Z\"}}";
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(body));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<User> userRef = new AtomicReference<>();

        authRepository.updateProfile("AliceUpdated", "SmithUpdated", new AuthCallback<User>() {
            @Override
            public void onSuccess(User data) {
                userRef.set(data);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("PATCH", request.getMethod());
        assertEquals("/api/v1/auth/profile", request.getPath());
        assertNotNull(userRef.get());
        assertEquals("AliceUpdated", userRef.get().getFirstName());
    }

    @Test
    public void testRequestPasswordResetSuccess() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(202));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        authRepository.requestPasswordReset("reset@example.com", new AuthCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                success.set(true);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(success.get());
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/api/v1/auth/password-reset", request.getPath());
    }

    @Test
    public void testConfirmPasswordResetSuccessInvalidatesLocalTokens() throws Exception {
        tokenStore.save("old-token", "2099-01-01T00:00:00Z");
        assertTrue(authRepository.isSignedIn());

        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        authRepository.confirmPasswordReset("sample-token-123456", "newSecret1", new AuthCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                success.set(true);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(success.get());
        // Local tokens must be invalidated after password reset
        assertFalse(authRepository.isSignedIn());
    }

    @Test
    public void testConfirmPasswordResetInvalidToken422() throws Exception {
        String body = "{\"error\":{\"code\":\"RESET_TOKEN_INVALID\",\"message\":\"Reset token has expired or is invalid\"}}";
        mockWebServer.enqueue(new MockResponse().setResponseCode(422).setBody(body));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        authRepository.confirmPasswordReset("invalid-token-12345", "newSecret1", new AuthCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                errorRef.set(throwable);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(errorRef.get() instanceof ApiException);
        assertEquals(ApiErrorCodes.RESET_TOKEN_INVALID, ((ApiException) errorRef.get()).getCode());
    }

    @Test
    public void testGoogleLoginSuccessAndTokenPersistence() throws Exception {
        String fixture = ApiFixtureLoader.load("loginCustomer", "success");
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(fixture));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<AuthSession> resultRef = new AtomicReference<>();

        authRepository.loginWithGoogle("valid-mock-google-token", new AuthCallback<AuthSession>() {
            @Override
            public void onSuccess(AuthSession data) {
                resultRef.set(data);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/api/v1/auth/google", request.getPath());
        assertTrue(request.getBody().readUtf8().contains("valid-mock-google-token"));
        assertNotNull(resultRef.get());
        assertTrue(authRepository.isSignedIn());
    }

    @Test
    public void testGoogleLoginFailureInvalidToken() throws Exception {
        String fixture = ApiFixtureLoader.load("loginCustomer", "invalid-credentials");
        mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody(fixture));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        authRepository.loginWithGoogle("expired-google-token", new AuthCallback<AuthSession>() {
            @Override
            public void onSuccess(AuthSession data) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                errorRef.set(throwable);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(errorRef.get() instanceof ApiException);
        ApiException ex = (ApiException) errorRef.get();
        assertEquals(401, ex.getHttpStatus());
        assertFalse(authRepository.isSignedIn());
    }

    @Test
    public void testRegisterSuccessWithMissingExpiresAtDoesNotCrash() throws Exception {
        String jsonWithoutExpiresAt = "{\"data\":{\"token\":\"test-register-token\",\"user\":{\"id\":10,\"email\":\"new@example.com\",\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"role\":\"customer\",\"status\":\"active\",\"createdAt\":\"2026-01-15T10:00:00Z\"}}}";
        mockWebServer.enqueue(new MockResponse().setResponseCode(201).setBody(jsonWithoutExpiresAt));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<AuthSession> resultRef = new AtomicReference<>();

        authRepository.registerCustomer("new@example.com", "password123", "Jane", "Doe", new AuthCallback<AuthSession>() {
            @Override
            public void onSuccess(AuthSession data) {
                resultRef.set(data);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(resultRef.get());
        assertEquals("test-register-token", resultRef.get().getToken());
        assertTrue(authRepository.isSignedIn());
        assertEquals("test-register-token", tokenStore.getToken());
    }
}

