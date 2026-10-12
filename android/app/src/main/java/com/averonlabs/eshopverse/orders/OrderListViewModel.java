package com.averonlabs.eshopverse.orders;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.model.OrderSummary;
import com.averonlabs.eshopverse.core.model.Pagination;
import com.averonlabs.eshopverse.core.net.PagedResponse;
import com.averonlabs.eshopverse.core.state.SingleLiveEvent;
import com.averonlabs.eshopverse.core.state.UiState;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for customer order list (Screen M-15).
 * Supports pagination, pull-to-refresh, empty and error state management.
 */
public class OrderListViewModel extends ViewModel {

    private final OrderRepository repository;

    private final MutableLiveData<UiState<List<OrderSummary>>> ordersState = new MutableLiveData<>();
    private final SingleLiveEvent<Void> authRequiredEvent = new SingleLiveEvent<>();

    private final List<OrderSummary> currentOrders = new ArrayList<>();
    private int currentPage = 1;
    private int totalPages = 1;
    private boolean isLoading = false;

    public OrderListViewModel(@NonNull OrderRepository repository) {
        this.repository = repository;
    }

    public LiveData<UiState<List<OrderSummary>>> getOrdersState() {
        return ordersState;
    }

    public LiveData<Void> getAuthRequiredEvent() {
        return authRequiredEvent;
    }

    public void loadOrders() {
        if (currentOrders.isEmpty()) {
            ordersState.setValue(UiState.loading());
        }
        fetchPage(1, false);
    }

    public void refresh() {
        fetchPage(1, true);
    }

    public void loadNextPage() {
        if (!isLoading && currentPage < totalPages) {
            fetchPage(currentPage + 1, false);
        }
    }

    private void fetchPage(int page, boolean isRefresh) {
        if (isLoading) return;
        isLoading = true;

        repository.listOrders(page, 20, new OrderRepository.OrderCallback<PagedResponse<OrderSummary>>() {
            @Override
            public void onSuccess(@NonNull PagedResponse<OrderSummary> response) {
                isLoading = false;
                currentPage = page;
                Pagination pagination = response.getPagination();
                if (pagination != null) {
                    totalPages = pagination.getTotalPages();
                }

                if (isRefresh || page == 1) {
                    currentOrders.clear();
                }
                currentOrders.addAll(response.getData());

                if (currentOrders.isEmpty()) {
                    ordersState.setValue(UiState.empty());
                } else {
                    ordersState.setValue(UiState.success(new ArrayList<>(currentOrders)));
                }
            }

            @Override
            public void onError(int httpCode, @Nullable String errorCode, @NonNull String message) {
                isLoading = false;
                if (httpCode == 401 || "UNAUTHENTICATED".equals(errorCode)) {
                    authRequiredEvent.call();
                    ordersState.setValue(UiState.error(R.string.error_unauthorized, false));
                } else {
                    ordersState.setValue(UiState.error(R.string.orders_error_heading, true));
                }
            }
        });
    }
}
