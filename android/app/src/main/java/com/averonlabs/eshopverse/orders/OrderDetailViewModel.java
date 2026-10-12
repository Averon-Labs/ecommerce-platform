package com.averonlabs.eshopverse.orders;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.model.Order;
import com.averonlabs.eshopverse.core.state.Event;
import com.averonlabs.eshopverse.core.state.UiState;

/**
 * ViewModel for customer order details (Screen M-16).
 * Handles immutable order snapshot presentation, 404 not-found, and 401 redirection.
 */
public class OrderDetailViewModel extends ViewModel {

    private final OrderRepository repository;

    private final MutableLiveData<UiState<Order>> orderState = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> authRequiredEvent = new MutableLiveData<>();

    public OrderDetailViewModel(@NonNull OrderRepository repository) {
        this.repository = repository;
    }

    public LiveData<UiState<Order>> getOrderState() {
        return orderState;
    }

    public LiveData<Event<Boolean>> getAuthRequiredEvent() {
        return authRequiredEvent;
    }

    public void loadOrder(long orderId) {
        orderState.setValue(UiState.loading());

        repository.getOrder(orderId, new OrderRepository.OrderCallback<Order>() {
            @Override
            public void onSuccess(@NonNull Order order) {
                orderState.setValue(UiState.content(order));
            }

            @Override
            public void onError(int httpCode, @Nullable String errorCode, @NonNull String message) {
                if (httpCode == 404 || "NOT_FOUND".equals(errorCode)) {
                    // Deliberately renders not-found state without distinguishing missing from not-owned
                    orderState.setValue(UiState.empty());
                } else if (httpCode == 401 || "UNAUTHENTICATED".equals(errorCode)) {
                    authRequiredEvent.setValue(new Event<>(true));
                    orderState.setValue(UiState.error(R.string.error_unauthenticated, false));
                } else {
                    orderState.setValue(UiState.error(R.string.orders_error_heading, true));
                }
            }
        });
    }
}
