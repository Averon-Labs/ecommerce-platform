package com.averonlabs.eshopverse.auth;

import android.app.Activity;
import android.content.Context;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.averonlabs.eshopverse.BuildConfig;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import java.util.Objects;

/**
 * Coordinates Google Sign-In using Android Credential Manager (AUTH-007).
 * Encapsulates option building, token extraction, and cancellation/error routing.
 */
public class GoogleAuthManager {

    public interface GoogleAuthCallback {
        void onSuccess(@NonNull String idToken);
        void onCancelled();
        void onError(@NonNull String errorMessage);
    }

    private final CredentialManager credentialManager;
    private final String serverClientId;

    public GoogleAuthManager(@NonNull Context context) {
        this(CredentialManager.create(context), BuildConfig.GOOGLE_WEB_CLIENT_ID);
    }

    public GoogleAuthManager(@NonNull CredentialManager credentialManager, @NonNull String serverClientId) {
        this.credentialManager = Objects.requireNonNull(credentialManager, "credentialManager cannot be null");
        this.serverClientId = Objects.requireNonNull(serverClientId, "serverClientId cannot be null");
    }

    @NonNull
    public String getServerClientId() {
        return serverClientId;
    }

    @NonNull
    public GetCredentialRequest buildCredentialRequest() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build();

        return new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();
    }

    /**
     * Launches the Google Sign-In dialog via Android Credential Manager.
     */
    public void startSignIn(@NonNull Activity activity, @NonNull GoogleAuthCallback callback) {
        Objects.requireNonNull(activity, "activity cannot be null");
        Objects.requireNonNull(callback, "callback cannot be null");

        GetCredentialRequest request = buildCredentialRequest();
        CancellationSignal cancellationSignal = new CancellationSignal();

        credentialManager.getCredentialAsync(
                activity,
                request,
                cancellationSignal,
                ContextCompat.getMainExecutor(activity),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        handleCredentialResponse(response, callback);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        handleCredentialError(e, callback);
                    }
                }
        );
    }

    public void handleCredentialResponse(
            @Nullable GetCredentialResponse response,
            @NonNull GoogleAuthCallback callback
    ) {
        if (response == null || response.getCredential() == null) {
            callback.onError("No credential returned.");
            return;
        }

        Credential credential = response.getCredential();
        if (credential instanceof CustomCredential
                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
            try {
                String idToken = extractIdToken(credential);
                if (idToken != null && !idToken.trim().isEmpty()) {
                    callback.onSuccess(idToken);
                } else {
                    callback.onError("Empty Google ID token returned.");
                }
            } catch (Exception e) {
                callback.onError("Failed to parse Google ID token: " + e.getMessage());
            }
        } else {
            callback.onError("Unexpected credential type: " + (credential != null ? credential.getType() : "null"));
        }
    }

    protected String extractIdToken(@NonNull Credential credential) throws Exception {
        GoogleIdTokenCredential googleIdToken = GoogleIdTokenCredential.createFrom(credential.getData());
        return googleIdToken.getIdToken();
    }

    public void handleCredentialError(
            @NonNull GetCredentialException e,
            @NonNull GoogleAuthCallback callback
    ) {
        if (e instanceof GetCredentialCancellationException) {
            callback.onCancelled();
        } else if (e instanceof NoCredentialException) {
            callback.onError("No Google accounts found or Google Play services unavailable.");
        } else {
            callback.onError("Google Sign-In failed: " + e.getMessage());
        }
    }
}
