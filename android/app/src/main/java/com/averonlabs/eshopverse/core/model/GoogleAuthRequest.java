package com.averonlabs.eshopverse.core.model;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

/**
 * Request payload for Google Sign-In (AUTH-004).
 */
public final class GoogleAuthRequest {

    @SerializedName("idToken")
    private final String idToken;

    public GoogleAuthRequest(@NonNull String idToken) {
        this.idToken = Objects.requireNonNull(idToken, "idToken cannot be null");
    }

    @NonNull
    public String getIdToken() {
        return idToken;
    }
}
