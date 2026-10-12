package com.averonlabs.eshopverse.checkout;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.averonlabs.eshopverse.core.net.ApiClient;
import com.averonlabs.eshopverse.core.storage.KeystoreTokenStore;

import java.util.Objects;

/**
 * ViewModelProvider.Factory for CheckoutViewModel.
 */
public class CheckoutViewModelFactory implements ViewModelProvider.Factory {

    private static volatile CheckoutRepository sDefaultRepository;
    private static volatile IdempotencyKeyFactory sDefaultKeyFactory;

    private final CheckoutRepository repository;
    private final IdempotencyKeyFactory idempotencyKeyFactory;

    public static synchronized CheckoutRepository getDefaultRepository(@NonNull Context context) {
        if (sDefaultRepository == null) {
            Context appContext = context.getApplicationContext();
            ApiClient apiClient = new ApiClient(new KeystoreTokenStore(appContext), null);
            sDefaultRepository = new CheckoutRepository(apiClient.getRoutes());
        }
        return sDefaultRepository;
    }

    public static synchronized IdempotencyKeyFactory getDefaultKeyFactory() {
        if (sDefaultKeyFactory == null) {
            sDefaultKeyFactory = new IdempotencyKeyFactory();
        }
        return sDefaultKeyFactory;
    }

    @VisibleForTesting
    public static synchronized void setDefaultRepository(@Nullable CheckoutRepository repository) {
        sDefaultRepository = repository;
    }

    @VisibleForTesting
    public static synchronized void setDefaultKeyFactory(@Nullable IdempotencyKeyFactory keyFactory) {
        sDefaultKeyFactory = keyFactory;
    }

    public CheckoutViewModelFactory(@NonNull Context context) {
        this(getDefaultRepository(context), getDefaultKeyFactory());
    }

    public CheckoutViewModelFactory(
            @NonNull CheckoutRepository repository,
            @NonNull IdempotencyKeyFactory idempotencyKeyFactory
    ) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.idempotencyKeyFactory = Objects.requireNonNull(idempotencyKeyFactory, "idempotencyKeyFactory cannot be null");
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(CheckoutViewModel.class)) {
            return (T) new CheckoutViewModel(repository, idempotencyKeyFactory);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
