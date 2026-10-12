package com.averonlabs.eshopverse.checkout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.averonlabs.eshopverse.core.model.CheckoutQuote;
import com.averonlabs.eshopverse.core.model.CheckoutQuoteRequest;
import com.averonlabs.eshopverse.core.model.CreateOrderRequest;
import com.averonlabs.eshopverse.core.model.Order;
import com.averonlabs.eshopverse.core.model.PaymentMethod;
import com.averonlabs.eshopverse.core.model.PaymentResult;
import com.averonlabs.eshopverse.core.model.ShippingAddress;
import com.averonlabs.eshopverse.core.model.SimulatedPaymentRequest;
import com.averonlabs.eshopverse.core.net.ApiErrorMapper;
import com.averonlabs.eshopverse.core.net.ApiException;
import com.averonlabs.eshopverse.core.net.ApiResponse;
import com.averonlabs.eshopverse.core.net.ApiRoutes;

import java.io.IOException;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository coordinating checkout operations per docs/android/PHASE-2-SPEC.md §E.
 * Consumes:
 * - POST /checkout/quote (M-12)
 * - POST /orders with Idempotency-Key (M-13)
 * - POST /orders/{orderId}/payment with Idempotency-Key (M-13)
 * - GET /orders/{orderId} (M-14)
 * Mutations carry no automatic retry and return server-authoritative state.
 */
public class CheckoutRepository {

    public interface CheckoutCallback<T> {
        void onSuccess(T result);
        void onError(Throwable throwable);
    }

    private final ApiRoutes apiRoutes;

    public CheckoutRepository(@NonNull ApiRoutes apiRoutes) {
        this.apiRoutes = Objects.requireNonNull(apiRoutes, "apiRoutes cannot be null");
    }

    /**
     * Obtains server-calculated checkout quote (POST /checkout/quote).
     */
    public void getQuote(@NonNull ShippingAddress address, @NonNull CheckoutCallback<CheckoutQuote> callback) {
        apiRoutes.getCheckoutQuote(new CheckoutQuoteRequest(address))
                .enqueue(new Callback<ApiResponse<CheckoutQuote>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<CheckoutQuote>> call,
                            @NonNull Response<ApiResponse<CheckoutQuote>> response
                    ) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body().getData());
                        } else {
                            callback.onError(parseError(response));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<CheckoutQuote>> call, @NonNull Throwable t) {
                        callback.onError(t);
                    }
                });
    }

    /**
     * Creates an order with an attempt-scoped Idempotency-Key (POST /orders).
     */
    public void createOrder(
            @NonNull String idempotencyKey,
            @NonNull ShippingAddress address,
            @Nullable String notes,
            @NonNull CheckoutCallback<Order> callback
    ) {
        apiRoutes.createOrder(idempotencyKey, new CreateOrderRequest(address, notes))
                .enqueue(new Callback<ApiResponse<Order>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<Order>> call,
                            @NonNull Response<ApiResponse<Order>> response
                    ) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body().getData());
                        } else {
                            callback.onError(parseError(response));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Order>> call, @NonNull Throwable t) {
                        callback.onError(t);
                    }
                });
    }

    /**
     * Submits simulated payment outcome with attempt-scoped Idempotency-Key (POST /orders/{orderId}/payment).
     */
    public void submitPayment(
            @NonNull String idempotencyKey,
            long orderId,
            @NonNull String outcome,
            @NonNull CheckoutCallback<PaymentResult> callback
    ) {
        apiRoutes.submitSimulatedPayment(
                idempotencyKey,
                orderId,
                new SimulatedPaymentRequest(PaymentMethod.DEMO, outcome)
        ).enqueue(new Callback<ApiResponse<PaymentResult>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<PaymentResult>> call,
                    @NonNull Response<ApiResponse<PaymentResult>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PaymentResult>> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    /**
     * Fetches current order state by ID (GET /orders/{orderId}) for M-14 recovery.
     */
    public void getOrder(long orderId, @NonNull CheckoutCallback<Order> callback) {
        apiRoutes.getOrder(orderId).enqueue(new Callback<ApiResponse<Order>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<Order>> call,
                    @NonNull Response<ApiResponse<Order>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Order>> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    @NonNull
    private ApiException parseError(@NonNull Response<?> response) {
        String body = null;
        if (response.errorBody() != null) {
            try {
                body = response.errorBody().string();
            } catch (IOException ignored) {
            }
        }
        return ApiErrorMapper.map(response.code(), body);
    }
}
