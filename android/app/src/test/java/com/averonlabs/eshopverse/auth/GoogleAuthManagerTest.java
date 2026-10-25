package com.averonlabs.eshopverse.auth;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for GoogleAuthManager (AUTH-007).
 */
public class GoogleAuthManagerTest {

    private static final String TEST_CLIENT_ID = "test-server-client-id.apps.googleusercontent.com";
    private CredentialManager mockCredentialManager;
    private GoogleAuthManager googleAuthManager;

    @Before
    public void setUp() {
        mockCredentialManager = Mockito.mock(CredentialManager.class);
        googleAuthManager = new GoogleAuthManager(mockCredentialManager, TEST_CLIENT_ID);
    }

    @Test
    public void testServerClientIdPreserved() {
        assertEquals(TEST_CLIENT_ID, googleAuthManager.getServerClientId());
    }

    @Test
    public void testBuildCredentialRequestBuildsCorrectRequest() {
        GetCredentialRequest request = googleAuthManager.buildCredentialRequest();
        assertNotNull(request);
        assertEquals(1, request.getCredentialOptions().size());
    }

    @Test
    public void testHandleCredentialErrorCancellationInvokesOnCancelled() {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicBoolean errorCalled = new AtomicBoolean(false);

        GetCredentialCancellationException cancelException = new GetCredentialCancellationException("User cancelled");

        googleAuthManager.handleCredentialError(cancelException, new GoogleAuthManager.GoogleAuthCallback() {
            @Override
            public void onSuccess(@NonNull String idToken) {}

            @Override
            public void onCancelled() {
                cancelled.set(true);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                errorCalled.set(true);
            }
        });

        assertTrue(cancelled.get());
        assertTrue(!errorCalled.get());
    }

    @Test
    public void testHandleCredentialErrorNoCredentialInvokesOnError() {
        AtomicReference<String> errorRef = new AtomicReference<>();

        NoCredentialException noCredException = new NoCredentialException("No credential available");

        googleAuthManager.handleCredentialError(noCredException, new GoogleAuthManager.GoogleAuthCallback() {
            @Override
            public void onSuccess(@NonNull String idToken) {}

            @Override
            public void onCancelled() {}

            @Override
            public void onError(@NonNull String errorMessage) {
                errorRef.set(errorMessage);
            }
        });

        assertNotNull(errorRef.get());
        assertTrue(errorRef.get().contains("No Google accounts found"));
    }

    @Test
    public void testHandleCredentialResponseNullInvokesOnError() {
        AtomicReference<String> errorRef = new AtomicReference<>();

        googleAuthManager.handleCredentialResponse(null, new GoogleAuthManager.GoogleAuthCallback() {
            @Override
            public void onSuccess(@NonNull String idToken) {}

            @Override
            public void onCancelled() {}

            @Override
            public void onError(@NonNull String errorMessage) {
                errorRef.set(errorMessage);
            }
        });

        assertNotNull(errorRef.get());
        assertEquals("No credential returned.", errorRef.get());
    }

    @Test
    public void testHandleCredentialResponseUnexpectedTypeInvokesOnError() {
        AtomicReference<String> errorRef = new AtomicReference<>();

        Credential mockCredential = Mockito.mock(Credential.class);
        Mockito.when(mockCredential.getType()).thenReturn("unexpected_credential_type");

        GetCredentialResponse response = new GetCredentialResponse(mockCredential);

        googleAuthManager.handleCredentialResponse(response, new GoogleAuthManager.GoogleAuthCallback() {
            @Override
            public void onSuccess(@NonNull String idToken) {}

            @Override
            public void onCancelled() {}

            @Override
            public void onError(@NonNull String errorMessage) {
                errorRef.set(errorMessage);
            }
        });

        assertNotNull(errorRef.get());
        assertTrue(errorRef.get().contains("Unexpected credential type"));
    }

    @Test
    public void testHandleCredentialResponseValidGoogleIdTokenInvokesOnSuccess() {
        AtomicReference<String> successTokenRef = new AtomicReference<>();

        GoogleAuthManager testManager = new GoogleAuthManager(mockCredentialManager, TEST_CLIENT_ID) {
            @Override
            protected String extractIdToken(@NonNull Credential credential) {
                return "mock.jwt.token";
            }
        };

        CustomCredential customCredential = new CustomCredential(
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
                new Bundle()
        );

        GetCredentialResponse response = new GetCredentialResponse(customCredential);

        testManager.handleCredentialResponse(response, new GoogleAuthManager.GoogleAuthCallback() {
            @Override
            public void onSuccess(@NonNull String idToken) {
                successTokenRef.set(idToken);
            }

            @Override
            public void onCancelled() {}

            @Override
            public void onError(@NonNull String errorMessage) {}
        });

        assertEquals("mock.jwt.token", successTokenRef.get());
    }

    @Test
    public void testHandleCredentialResponseEmptyTokenInvokesOnError() {
        AtomicReference<String> errorRef = new AtomicReference<>();

        GoogleAuthManager testManager = new GoogleAuthManager(mockCredentialManager, TEST_CLIENT_ID) {
            @Override
            protected String extractIdToken(@NonNull Credential credential) {
                return "";
            }
        };

        CustomCredential customCredential = new CustomCredential(
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
                new Bundle()
        );

        GetCredentialResponse response = new GetCredentialResponse(customCredential);

        testManager.handleCredentialResponse(response, new GoogleAuthManager.GoogleAuthCallback() {
            @Override
            public void onSuccess(@NonNull String idToken) {}

            @Override
            public void onCancelled() {}

            @Override
            public void onError(@NonNull String errorMessage) {
                errorRef.set(errorMessage);
            }
        });

        assertNotNull(errorRef.get());
        assertTrue(errorRef.get().contains("Empty Google ID token returned."));
    }

    @Test
    public void testHandleCredentialResponseParsingExceptionInvokesOnError() {
        AtomicReference<String> errorRef = new AtomicReference<>();

        GoogleAuthManager testManager = new GoogleAuthManager(mockCredentialManager, TEST_CLIENT_ID) {
            @Override
            protected String extractIdToken(@NonNull Credential credential) throws Exception {
                throw new RuntimeException("Malformed payload");
            }
        };

        CustomCredential customCredential = new CustomCredential(
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
                new Bundle()
        );

        GetCredentialResponse response = new GetCredentialResponse(customCredential);

        testManager.handleCredentialResponse(response, new GoogleAuthManager.GoogleAuthCallback() {
            @Override
            public void onSuccess(@NonNull String idToken) {}

            @Override
            public void onCancelled() {}

            @Override
            public void onError(@NonNull String errorMessage) {
                errorRef.set(errorMessage);
            }
        });

        assertNotNull(errorRef.get());
        assertTrue(errorRef.get().contains("Failed to parse Google ID token: Malformed payload"));
    }
}
