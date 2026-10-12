package com.averonlabs.eshopverse.cart;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.averonlabs.eshopverse.core.net.ApiClient;
import com.averonlabs.eshopverse.core.storage.KeystoreTokenStore;

/**
 * Factory for creating {@link CartViewModel} instances.
 */
public class CartViewModelFactory implements ViewModelProvider.Factory {

    private static volatile CartRepository sDefaultRepository;

    private final CartRepository repository;

    public static synchronized CartRepository getDefaultRepository(@NonNull Context context) {
        if (sDefaultRepository == null) {
            Context appContext = context.getApplicationContext();
            ApiClient apiClient = new ApiClient(new KeystoreTokenStore(appContext), null);
            sDefaultRepository = new CartRepository(apiClient.getRoutes());
        }
        return sDefaultRepository;
    }

    @VisibleForTesting
    public static synchronized void setDefaultRepository(@Nullable CartRepository repository) {
        sDefaultRepository = repository;
    }

    public CartViewModelFactory(@NonNull Context context) {
        this(getDefaultRepository(context));
    }

    public CartViewModelFactory(@NonNull CartRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(CartViewModel.class)) {
            return (T) new CartViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
