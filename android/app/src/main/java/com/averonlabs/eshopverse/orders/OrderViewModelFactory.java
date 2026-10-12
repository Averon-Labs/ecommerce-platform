package com.averonlabs.eshopverse.orders;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/**
 * Factory for creating OrderListViewModel and OrderDetailViewModel with dependencies.
 */
public class OrderViewModelFactory implements ViewModelProvider.Factory {

    private final OrderRepository repository;

    public OrderViewModelFactory() {
        this(new OrderRepository());
    }

    public OrderViewModelFactory(@NonNull OrderRepository repository) {
        this.repository = repository;
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
