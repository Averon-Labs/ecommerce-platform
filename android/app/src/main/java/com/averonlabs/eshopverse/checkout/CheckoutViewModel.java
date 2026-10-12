package com.averonlabs.eshopverse.checkout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.error.ApiErrorCodes;
import com.averonlabs.eshopverse.core.model.CheckoutQuote;
import com.averonlabs.eshopverse.core.model.Order;
import com.averonlabs.eshopverse.core.model.OrderStatus;
import com.averonlabs.eshopverse.core.model.PaymentResult;
import com.averonlabs.eshopverse.core.model.PaymentStatus;
import com.averonlabs.eshopverse.core.model.ShippingAddress;
import com.averonlabs.eshopverse.core.net.ApiException;
import com.averonlabs.eshopverse.core.state.Event;
import com.averonlabs.eshopverse.core.state.UiState;

import java.util.Map;
import java.util.Objects;

/**
 * ViewModel managing checkout flow across M-11..M-14 per docs/android/PHASE-2-SPEC.md §E.
 * Enforces in-flight locking, attempt-scoped idempotency keys, and server-authoritative states.
 */
public class CheckoutViewModel extends ViewModel {

    private final CheckoutRepository repository;
    private final IdempotencyKeyFactory idempotencyKeyFactory;

    // Address state (M-11)
    private final MutableLiveData<ShippingAddress> shippingAddress = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Integer>> addressErrors = new MutableLiveData<>();

    // Quote state (M-12)
    private final MutableLiveData<UiState<CheckoutQuote>> quoteState = new MutableLiveData<>(UiState.empty());

    // Order state (M-13 & M-14)
    private final MutableLiveData<Order> currentOrder = new MutableLiveData<>();
    private final MutableLiveData<UiState<PaymentResult>> paymentState = new MutableLiveData<>(UiState.empty());
    private final MutableLiveData<UiState<Order>> orderDetailState = new MutableLiveData<>(UiState.empty());

    // In-flight operation lock
    private final MutableLiveData<Boolean> isOperationInFlight = new MutableLiveData<>(false);
    private boolean inFlight = false;

    // Ephemeral idempotency key for logical attempt
    private String currentAttemptKey;

    // Navigation and error events
    private final MutableLiveData<Event<Boolean>> navigateToReviewEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> navigateToPaymentEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<PaymentResult>> navigateToResultEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> cartConflictEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> authRequiredEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> errorNoticeEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> errorResEvent = new MutableLiveData<>();

    public CheckoutViewModel(
            @NonNull CheckoutRepository repository,
            @NonNull IdempotencyKeyFactory idempotencyKeyFactory
    ) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.idempotencyKeyFactory = Objects.requireNonNull(idempotencyKeyFactory, "idempotencyKeyFactory cannot be null");
    }

    // --- Getters ---

    public LiveData<ShippingAddress> getShippingAddress() {
        return shippingAddress;
    }

    public LiveData<Map<String, Integer>> getAddressErrors() {
        return addressErrors;
    }

    public LiveData<UiState<CheckoutQuote>> getQuoteState() {
        return quoteState;
    }

    public LiveData<Order> getCurrentOrder() {
        return currentOrder;
    }

    public LiveData<UiState<PaymentResult>> getPaymentState() {
        return paymentState;
    }

    public LiveData<UiState<Order>> getOrderDetailState() {
        return orderDetailState;
    }

    public LiveData<Boolean> getIsOperationInFlight() {
        return isOperationInFlight;
    }

    public LiveData<Event<Boolean>> getNavigateToReviewEvent() {
        return navigateToReviewEvent;
    }

    public LiveData<Event<Boolean>> getNavigateToPaymentEvent() {
        return navigateToPaymentEvent;
    }

    public LiveData<Event<PaymentResult>> getNavigateToResultEvent() {
        return navigateToResultEvent;
    }

    public LiveData<Event<String>> getCartConflictEvent() {
        return cartConflictEvent;
    }

    public LiveData<Event<Boolean>> getAuthRequiredEvent() {
        return authRequiredEvent;
    }

    public LiveData<Event<String>> getErrorNoticeEvent() {
        return errorNoticeEvent;
    }

    public LiveData<Event<Integer>> getErrorResEvent() {
        return errorResEvent;
    }

    @Nullable
    public String getCurrentAttemptKey() {
        return currentAttemptKey;
    }

    // --- Actions ---

    /**
     * Validates and submits shipping address from M-11.
     */
    public void submitShippingAddress(
            @Nullable String recipientName,
            @Nullable String line1,
            @Nullable String line2,
            @Nullable String city,
            @Nullable String region,
            @Nullable String postalCode,
            @Nullable String country
    ) {
        AddressValidator.ValidationResult result = AddressValidator.validate(
                recipientName, line1, line2, city, region, postalCode, country
        );

        if (!result.isValid()) {
            addressErrors.setValue(result.getFieldErrors());
            return;
        }

        addressErrors.setValue(null);
        ShippingAddress address = AddressValidator.buildIfValid(
                recipientName, line1, line2, city, region, postalCode, country
        );
        shippingAddress.setValue(address);

        if (address != null) {
            loadQuote(address);
        }
    }

    /**
     * Loads checkout quote for M-12.
     */
    public void loadQuote(@NonNull ShippingAddress address) {
        if (inFlight) {
            return;
        }
        setInFlight(true);
        quoteState.setValue(UiState.loading());

        repository.getQuote(address, new CheckoutRepository.CheckoutCallback<CheckoutQuote>() {
            @Override
            public void onSuccess(CheckoutQuote quote) {
                setInFlight(false);
                quoteState.setValue(UiState.content(quote));
                navigateToReviewEvent.setValue(new Event<>(true));
            }

            @Override
            public void onError(Throwable throwable) {
                setInFlight(false);
                handleError(throwable, quoteState, R.string.checkout_error_loading_quote);
            }
        });
    }

    /**
     * Proceeds from M-12 (review) to M-13 (payment selection).
     */
    public void proceedToPayment() {
        navigateToPaymentEvent.setValue(new Event<>(true));
    }

    /**
     * Submits simulated payment choice from M-13.
     * Enforces in-flight lock, creates new attempt key, creates order, and simulates payment.
     */
    public void submitPaymentChoice(@NonNull String outcome) {
        if (inFlight) {
            return; // In-flight lock prevents double-tap duplicates
        }

        ShippingAddress address = shippingAddress.getValue();
        if (address == null) {
            errorResEvent.setValue(new Event<>(R.string.checkout_error_generic));
            return;
        }

        setInFlight(true);
        paymentState.setValue(UiState.loading());

        // Each logical user attempt generates a fresh idempotency key (§E.4)
        currentAttemptKey = idempotencyKeyFactory.newKey();

        // Step 1: Create Order if not already created
        Order existingOrder = currentOrder.getValue();
        if (existingOrder != null) {
            executePaymentSimulation(existingOrder.getId(), outcome);
        } else {
            repository.createOrder(currentAttemptKey, address, null, new CheckoutRepository.CheckoutCallback<Order>() {
                @Override
                public void onSuccess(Order order) {
                    currentOrder.setValue(order);
                    executePaymentSimulation(order.getId(), outcome);
                }

                @Override
                public void onError(Throwable throwable) {
                    setInFlight(false);
                    handleError(throwable, paymentState, R.string.checkout_error_generic);
                }
            });
        }
    }

    private void executePaymentSimulation(long orderId, @NonNull String outcome) {
        repository.submitPayment(currentAttemptKey, orderId, outcome, new CheckoutRepository.CheckoutCallback<PaymentResult>() {
            @Override
            public void onSuccess(PaymentResult result) {
                setInFlight(false);
                paymentState.setValue(UiState.content(result));
                navigateToResultEvent.setValue(new Event<>(result));
            }

            @Override
            public void onError(Throwable throwable) {
                setInFlight(false);
                if (throwable instanceof ApiException) {
                    ApiException apiException = (ApiException) throwable;
                    // Contract: payment failure returns 409 PAYMENT_FAILED, order stays PENDING_PAYMENT
                    if ("PAYMENT_FAILED".equals(apiException.getCode()) || apiException.getHttpStatus() == 409) {
                        Order order = currentOrder.getValue();
                        String ref = order != null ? order.getReference() : "ORD-" + orderId;
                        PaymentResult failedResult = new PaymentResult(
                                (int) orderId,
                                ref,
                                OrderStatus.PENDING_PAYMENT,
                                PaymentStatus.FAILED,
                                true,
                                null
                        );
                        paymentState.setValue(UiState.content(failedResult));
                        navigateToResultEvent.setValue(new Event<>(failedResult));
                        return;
                    }
                }
                handleError(throwable, paymentState, R.string.checkout_error_generic);
            }
        });
    }

    /**
     * Resolves order state on M-14 recovery (e.g. after ambiguous result or timeout).
     */
    public void checkOrderStatus(long orderId) {
        if (inFlight) {
            return;
        }
        setInFlight(true);
        orderDetailState.setValue(UiState.loading());

        repository.getOrder(orderId, new CheckoutRepository.CheckoutCallback<Order>() {
            @Override
            public void onSuccess(Order order) {
                setInFlight(false);
                currentOrder.setValue(order);
                orderDetailState.setValue(UiState.content(order));
            }

            @Override
            public void onError(Throwable throwable) {
                setInFlight(false);
                handleError(throwable, orderDetailState, R.string.checkout_error_generic);
            }
        });
    }

    private void setInFlight(boolean inFlight) {
        this.inFlight = inFlight;
        this.isOperationInFlight.setValue(inFlight);
    }

    private <T> void handleError(
            @Nullable Throwable throwable,
            @NonNull MutableLiveData<UiState<T>> targetState,
            int defaultErrorRes
    ) {
        if (throwable instanceof ApiException) {
            ApiException apiException = (ApiException) throwable;
            String code = apiException.getCode();

            if (apiException.getHttpStatus() == 401 || ApiErrorCodes.UNAUTHENTICATED.equals(code)) {
                authRequiredEvent.setValue(new Event<>(true));
                targetState.setValue(UiState.error(R.string.error_unauthenticated, false));
                return;
            }

            if (ApiErrorCodes.CART_EMPTY.equals(code)) {
                cartConflictEvent.setValue(new Event<>(apiException.getMessage()));
                targetState.setValue(UiState.error(R.string.checkout_error_cart_empty, false));
                return;
            }

            if (ApiErrorCodes.INSUFFICIENT_STOCK.equals(code)) {
                cartConflictEvent.setValue(new Event<>(apiException.getMessage()));
                targetState.setValue(UiState.error(R.string.checkout_error_insufficient_stock, false));
                return;
            }

            if (ApiErrorCodes.PRODUCT_UNAVAILABLE.equals(code)) {
                cartConflictEvent.setValue(new Event<>(apiException.getMessage()));
                targetState.setValue(UiState.error(R.string.checkout_error_product_unavailable, false));
                return;
            }

            if (ApiErrorCodes.IDEMPOTENCY_KEY_REUSED.equals(code)) {
                errorResEvent.setValue(new Event<>(R.string.checkout_error_idempotency_conflict));
                targetState.setValue(UiState.error(R.string.checkout_error_idempotency_conflict, true));
                return;
            }

            if (apiException.getMessage() != null && !apiException.getMessage().isEmpty()) {
                errorNoticeEvent.setValue(new Event<>(apiException.getMessage()));
            }
        }

        targetState.setValue(UiState.error(defaultErrorRes, true));
        errorResEvent.setValue(new Event<>(defaultErrorRes));
    }
}
