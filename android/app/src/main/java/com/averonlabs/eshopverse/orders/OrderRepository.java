package com.averonlabs.eshopverse.orders;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.averonlabs.eshopverse.core.model.Order;
import com.averonlabs.eshopverse.core.model.OrderSummary;
import com.averonlabs.eshopverse.core.net.ApiResponse;
import com.averonlabs.eshopverse.core.net.ApiRoutes;
import com.averonlabs.eshopverse.core.net.PagedResponse;

import java.io.IOException;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class OrderRepository {

    public interface OrderCallback<T> {
        void onSuccess(@NonNull T result);
        void onError(int httpCode, @Nullable String errorCode, @NonNull String message);
    }

    private final ApiRoutes routes;



    public OrderRepository(@NonNull ApiRoutes routes) {
        this.routes = Objects.requireNonNull(routes, "routes cannot be null");
    }

    public void listOrders(
            @Nullable Integer page,
            @Nullable Integer perPage,
            @NonNull OrderCallback<PagedResponse<OrderSummary>> callback
    ) {
        routes.listOrders(page, perPage).enqueue(new Callback<PagedResponse<OrderSummary>>() {
            @Override
            public void onResponse(
                    @NonNull Call<PagedResponse<OrderSummary>> call,
                    @NonNull Response<PagedResponse<OrderSummary>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    String errCode = null;
                    if (response.code() == 401) {
                        errCode = "UNAUTHENTICATED";
                    }
                    callback.onError(response.code(), errCode, "Failed to load orders: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PagedResponse<OrderSummary>> call, @NonNull Throwable t) {
                callback.onError(0, null, t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void getOrder(long orderId, @NonNull OrderCallback<Order> callback) {
        routes.getOrder(orderId).enqueue(new Callback<ApiResponse<Order>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<Order>> call,
                    @NonNull Response<ApiResponse<Order>> response
            ) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    String errCode = null;
                    if (response.code() == 404) {
                        errCode = "NOT_FOUND";
                    } else if (response.code() == 401) {
                        errCode = "UNAUTHENTICATED";
                    }
                    callback.onError(response.code(), errCode, "Order retrieval failed: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Order>> call, @NonNull Throwable t) {
                callback.onError(0, null, t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }
}
