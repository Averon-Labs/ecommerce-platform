package com.averonlabs.eshopverse.cart;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.error.ApiErrorCodes;
import com.averonlabs.eshopverse.core.model.Cart;
import com.averonlabs.eshopverse.core.net.ApiException;
import com.averonlabs.eshopverse.core.state.Event;
import com.averonlabs.eshopverse.core.state.UiState;

import java.util.Objects;

/**
 * ViewModel managing cart screen state, in-flight mutation locking, and error recovery
 * per docs/android/PHASE-2-SPEC.md §D.
 */
public class CartViewModel extends ViewModel {

    private final CartRepository repository;

    private final MutableLiveData<UiState<Cart>> cartState = new MutableLiveData<>();
    private final MutableLiveData<Long> mutatingProductId = new MutableLiveData<>(null);
    private final MutableLiveData<Event<String>> noticeEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> errorResEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> authRequiredEvent = new MutableLiveData<>();
    private final MutableLiveData<Integer> itemCount = new MutableLiveData<>(0);

    private boolean isMutationInFlight = false;

    public CartViewModel(@NonNull CartRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
    }

    @NonNull
    public LiveData<UiState<Cart>> getCartState() {
        return cartState;
    }

    @NonNull
    public LiveData<Long> getMutatingProductId() {
        return mutatingProductId;
    }

    @NonNull
    public LiveData<Event<String>> getNoticeEvent() {
        return noticeEvent;
    }

    @NonNull
    public LiveData<Event<Integer>> getErrorResEvent() {
        return errorResEvent;
    }

    @NonNull
    public LiveData<Event<Boolean>> getAuthRequiredEvent() {
        return authRequiredEvent;
    }

    @NonNull
    public LiveData<Integer> getItemCount() {
        return itemCount;
    }

    /**
     * Loads the customer's cart from the server (GET /cart).
     */
    public void loadCart() {
        cartState.setValue(UiState.loading());
        repository.getCart(new CartRepository.CartCallback<Cart>() {
            @Override
            public void onSuccess(Cart cart) {
                handleCartLoaded(cart);
            }

            @Override
            public void onError(Throwable throwable) {
                if (isAuthError(throwable)) {
                    authRequiredEvent.setValue(new Event<>(true));
                }
                cartState.setValue(UiState.error(R.string.cart_error_loading, true));
            }
        });
    }

    /**
     * Updates the quantity of one item line with in-flight lock protection.
     */
    public void updateQuantity(long productId, int quantity) {
        if (isMutationInFlight) {
            return; // In-flight lock prevents double mutation
        }
        isMutationInFlight = true;
        mutatingProductId.setValue(productId);

        repository.updateQuantity(productId, quantity, new CartRepository.CartCallback<Cart>() {
            @Override
            public void onSuccess(Cart cart) {
                isMutationInFlight = false;
                mutatingProductId.setValue(null);
                handleCartLoaded(cart);
            }

            @Override
            public void onError(Throwable throwable) {
                isMutationInFlight = false;
                mutatingProductId.setValue(null);
                if (isAuthError(throwable)) {
                    authRequiredEvent.setValue(new Event<>(true));
                    return;
                }
                if (throwable instanceof ApiException) {
                    ApiException apiException = (ApiException) throwable;
                    String code = apiException.getCode();
                    if (ApiErrorCodes.INSUFFICIENT_STOCK.equals(code)) {
                        noticeEvent.setValue(new Event<>(apiException.getMessage() != null
                                ? apiException.getMessage()
                                : "Requested quantity exceeds available stock."));
                        return;
                    } else if (ApiErrorCodes.CART_ITEM_NOT_FOUND.equals(code)) {
                        errorResEvent.setValue(new Event<>(R.string.cart_error_item_not_found));
                        loadCart();
                        return;
                    }
                }
                errorResEvent.setValue(new Event<>(R.string.cart_error_updating));
            }
        });
    }

    /**
     * Removes an item line from the cart with in-flight lock protection.
     */
    public void removeItem(long productId) {
        if (isMutationInFlight) {
            return; // In-flight lock prevents double mutation
        }
        isMutationInFlight = true;
        mutatingProductId.setValue(productId);

        repository.removeItem(productId, new CartRepository.CartCallback<Cart>() {
            @Override
            public void onSuccess(Cart cart) {
                isMutationInFlight = false;
                mutatingProductId.setValue(null);
                handleCartLoaded(cart);
            }

            @Override
            public void onError(Throwable throwable) {
                isMutationInFlight = false;
                mutatingProductId.setValue(null);
                if (isAuthError(throwable)) {
                    authRequiredEvent.setValue(new Event<>(true));
                    return;
                }
                if (throwable instanceof ApiException) {
                    ApiException apiException = (ApiException) throwable;
                    if (ApiErrorCodes.CART_ITEM_NOT_FOUND.equals(apiException.getCode())) {
                        loadCart();
                        return;
                    }
                }
                errorResEvent.setValue(new Event<>(R.string.cart_error_removing));
            }
        });
    }

    public void retry() {
        loadCart();
    }

    private void handleCartLoaded(@NonNull Cart cart) {
        itemCount.setValue(cart.getItemCount());
        if (cart.getItems().isEmpty()) {
            cartState.setValue(UiState.empty());
        } else {
            cartState.setValue(UiState.content(cart));
        }
    }

    private boolean isAuthError(@Nullable Throwable throwable) {
        if (throwable instanceof ApiException) {
            ApiException apiException = (ApiException) throwable;
            return apiException.getHttpStatus() == 401
                    || ApiErrorCodes.UNAUTHENTICATED.equals(apiException.getCode());
        }
        return false;
    }
}
