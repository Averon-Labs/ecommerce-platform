package com.averonlabs.eshopverse.cart;

import androidx.annotation.NonNull;

import com.averonlabs.eshopverse.core.model.AddCartItemRequest;
import com.averonlabs.eshopverse.core.model.Cart;
import com.averonlabs.eshopverse.core.model.UpdateCartItemQuantityRequest;
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
 * Repository coordinating customer cart operations per docs/android/PHASE-2-SPEC.md §D.
 * Mutations carry no automatic retry and return server-authoritative Cart state.
 */
public class CartRepository {

    public interface CartCallback<T> {
        void onSuccess(T result);
        void onError(Throwable throwable);
    }

    private final ApiRoutes apiRoutes;

    public CartRepository(@NonNull ApiRoutes apiRoutes) {
        this.apiRoutes = Objects.requireNonNull(apiRoutes, "apiRoutes cannot be null");
    }

    /**
     * Retrieves the current signed-in customer cart (GET /cart).
     */
    public void getCart(@NonNull CartCallback<Cart> callback) {
        apiRoutes.getCart().enqueue(new Callback<ApiResponse<Cart>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<Cart>> call,
                    @NonNull Response<ApiResponse<Cart>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Cart>> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    /**
     * Updates the quantity of one cart line (PATCH /cart/items/{productId}).
     */
    public void updateQuantity(long productId, int quantity, @NonNull CartCallback<Cart> callback) {
        apiRoutes.updateCartItemQuantity(productId, new UpdateCartItemQuantityRequest(quantity))
                .enqueue(new Callback<ApiResponse<Cart>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<Cart>> call,
                            @NonNull Response<ApiResponse<Cart>> response
                    ) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body().getData());
                        } else {
                            callback.onError(parseError(response));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Cart>> call, @NonNull Throwable t) {
                        callback.onError(t);
                    }
                });
    }

    /**
     * Removes one item line from the cart (DELETE /cart/items/{productId}).
     */
    public void removeItem(long productId, @NonNull CartCallback<Cart> callback) {
        apiRoutes.removeCartItem(productId).enqueue(new Callback<ApiResponse<Cart>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<Cart>> call,
                    @NonNull Response<ApiResponse<Cart>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Cart>> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    /**
     * Adds an item to the cart or increases its quantity (POST /cart/items).
     */
    public void addItem(long productId, int quantity, @NonNull CartCallback<Cart> callback) {
        apiRoutes.addItemToCart(new AddCartItemRequest((int) productId, quantity))
                .enqueue(new Callback<ApiResponse<Cart>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<ApiResponse<Cart>> call,
                            @NonNull Response<ApiResponse<Cart>> response
                    ) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body().getData());
                        } else {
                            callback.onError(parseError(response));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Cart>> call, @NonNull Throwable t) {
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
