package com.averonlabs.eshopverse.auth;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.averonlabs.eshopverse.core.net.ApiClient;
import com.averonlabs.eshopverse.core.storage.KeystoreTokenStore;
import com.averonlabs.eshopverse.core.storage.TokenStore;

/**
 * Factory for creating {@link AuthViewModel} and {@link SessionViewModel} instances.
 */
public class AuthViewModelFactory implements ViewModelProvider.Factory {

    private static volatile AuthRepository sDefaultRepository;

    private final AuthRepository authRepository;

    public static synchronized AuthRepository getDefaultRepository(@NonNull Context context) {
        if (sDefaultRepository == null) {
            Context appContext = context.getApplicationContext();
            TokenStore tokenStore = new KeystoreTokenStore(appContext);
            ApiClient apiClient = new ApiClient(tokenStore, null);
            sDefaultRepository = new AuthRepository(apiClient.getRoutes(), tokenStore);
        }
        return sDefaultRepository;
    }

    @VisibleForTesting
    public static synchronized void setDefaultRepository(@Nullable AuthRepository repository) {
        sDefaultRepository = repository;
    }

    public AuthViewModelFactory(@NonNull Context context) {
        this(getDefaultRepository(context));
    }

    public AuthViewModelFactory(@NonNull AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AuthViewModel.class)) {
            return (T) new AuthViewModel(authRepository);
        } else if (modelClass.isAssignableFrom(SessionViewModel.class)) {
            return (T) new SessionViewModel(authRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
