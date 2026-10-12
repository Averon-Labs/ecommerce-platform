package com.averonlabs.eshopverse.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.averonlabs.eshopverse.core.model.User;

import java.util.Objects;

/**
 * App-wide session state ViewModel per docs/android/PHASE-1-SPEC.md §D.5.
 * Exposes signed-in status and the current User for observation across customer screens.
 */
public class SessionViewModel extends ViewModel {

    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> isSignedInLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<User> currentUserLiveData = new MutableLiveData<>(null);

    public SessionViewModel(@NonNull AuthRepository authRepository) {
        this.authRepository = Objects.requireNonNull(authRepository, "authRepository cannot be null");
        checkInitialSession();
    }

    public LiveData<Boolean> getIsSignedIn() {
        return isSignedInLiveData;
    }

    public LiveData<User> getCurrentUser() {
        return currentUserLiveData;
    }

    @Nullable
    public User getCurrentUserValue() {
        return currentUserLiveData.getValue();
    }

    public boolean isSignedIn() {
        Boolean value = isSignedInLiveData.getValue();
        return value != null && value;
    }

    /**
     * Checks token store and updates session state. If token is active, refreshes user profile.
     */
    public void checkInitialSession() {
        boolean signedIn = authRepository.isSignedIn();
        isSignedInLiveData.setValue(signedIn);
        if (signedIn) {
            refreshProfile(null);
        } else {
            currentUserLiveData.setValue(null);
        }
    }

    /**
     * Called upon successful sign-in or registration.
     */
    public void onSessionStarted(@NonNull User user) {
        isSignedInLiveData.setValue(true);
        currentUserLiveData.setValue(user);
    }

    public void setSession(@NonNull com.averonlabs.eshopverse.core.model.AuthSession session) {
        onSessionStarted(session.getUser());
    }

    /**
     * Updates locally cached user profile.
     */
    public void setCurrentUser(@Nullable User user) {
        currentUserLiveData.setValue(user);
    }

    public void updateCurrentUser(@NonNull User user) {
        setCurrentUser(user);
    }

    /**
     * Fetches fresh user profile from API.
     */
    public void refreshProfile(@Nullable AuthCallback<User> callback) {
        if (!authRepository.isSignedIn()) {
            onSessionEnded();
            if (callback != null) {
                callback.onError(new IllegalStateException("User not signed in"));
            }
            return;
        }

        authRepository.getProfile(new AuthCallback<User>() {
            @Override
            public void onSuccess(User user) {
                isSignedInLiveData.postValue(true);
                currentUserLiveData.postValue(user);
                if (callback != null) {
                    callback.onSuccess(user);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                // If token expired / unauthenticated, end local session
                if (!authRepository.isSignedIn()) {
                    onSessionEnded();
                }
                if (callback != null) {
                    callback.onError(throwable);
                }
            }
        });
    }

    /**
     * Ends local session immediately (e.g. on 401 unauthenticated hook).
     */
    public void onSessionEnded() {
        authRepository.getTokenStore().clear();
        isSignedInLiveData.postValue(false);
        currentUserLiveData.postValue(null);
    }

    /**
     * Performs customer sign-out via API and clears local session.
     */
    public void signOut() {
        signOut(new AuthCallback<Void>() {
            @Override
            public void onSuccess(Void data) {}

            @Override
            public void onError(Throwable throwable) {}
        });
    }

    /**
     * Performs customer sign-out via API and clears local session.
     */
    public void signOut(@NonNull AuthCallback<Void> callback) {
        authRepository.logoutCustomer(new AuthCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                isSignedInLiveData.postValue(false);
                currentUserLiveData.postValue(null);
                callback.onSuccess(data);
            }

            @Override
            public void onError(Throwable throwable) {
                // Logout clears token even on error
                isSignedInLiveData.postValue(false);
                currentUserLiveData.postValue(null);
                callback.onError(throwable);
            }
        });
    }
}
