package com.averonlabs.eshopverse.orders;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.averonlabs.eshopverse.core.net.ApiClient;
import com.averonlabs.eshopverse.core.storage.KeystoreTokenStore;
import com.averonlabs.eshopverse.orders.OrderDetailViewModel;
import com.averonlabs.eshopverse.orders.OrderListViewModel;
import com.averonlabs.eshopverse.orders.OrderRepository;

import java.util.Objects;

/**
 * Factory for creating OrderListViewModel and OrderDetailViewModel with dependencies.
 */
public class OrderViewModelFactory implements ViewModelProvider.Factory {

    private static volatile OrderRepository sDefaultRepository;

    private final OrderRepository repository;

    public static synchronized OrderRepository getDefaultRepository(@NonNull Context context) {
        if (sDefaultRepository == null) {
            Context appContext = context.getApplicationContext();
            ApiClient apiClient = new ApiClient(new KeystoreTokenStore(appContext), null);
            sDefaultRepository = new OrderRepository(apiClient.getRoutes());
        }
        return sDefaultRepository;
    }

    @VisibleForTesting
    public static synchronized void setDefaultRepository(@Nullable OrderRepository repository) {
        sDefaultRepository = repository;
    }

    public OrderViewModelFactory(@NonNull Context context) {
        this(getDefaultRepository(context));
    }

    public OrderViewModelFactory(@NonNull OrderRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(OrderListViewModel.class)) {
            return (T) new OrderListViewModel(repository);
        } else if (modelClass.isAssignableFrom(OrderDetailViewModel.class)) {
            return (T) new OrderDetailViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
