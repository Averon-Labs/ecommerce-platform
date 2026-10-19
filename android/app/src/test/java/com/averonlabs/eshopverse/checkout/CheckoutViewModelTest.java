package com.averonlabs.eshopverse.checkout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.averonlabs.eshopverse.core.error.ApiErrorCodes;
import com.averonlabs.eshopverse.core.model.CheckoutQuote;
import com.averonlabs.eshopverse.core.model.Currency;
import com.averonlabs.eshopverse.core.model.Order;
import com.averonlabs.eshopverse.core.model.OrderStatus;
import com.averonlabs.eshopverse.core.model.PaymentResult;
import com.averonlabs.eshopverse.core.model.PaymentStatus;
import com.averonlabs.eshopverse.core.model.ShippingAddress;
import com.averonlabs.eshopverse.core.net.ApiException;
import com.averonlabs.eshopverse.core.state.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

/**
 * JVM unit tests for CheckoutViewModel per docs/android/PHASE-2-SPEC.md §E.7.
 */
public class CheckoutViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private FakeCheckoutRepository fakeRepository;
    private IdempotencyKeyFactory keyFactory;
    private CheckoutViewModel viewModel;
    private ShippingAddress testAddress;

    @Before
    public void setUp() {
        fakeRepository = new FakeCheckoutRepository();
        keyFactory = new IdempotencyKeyFactory();
        viewModel = new CheckoutViewModel(fakeRepository, keyFactory);
        testAddress = new ShippingAddress(
                "John Doe",
                "123 Main St",
                "Apt 4B",
                "Metropolis",
                "NY",
                "10001",
                "US"
        );
    }

    @Test
    public void testAddressSubmissionValidTriggersQuoteAndNavigatesToReview() {
        viewModel.submitShippingAddress(
                "John Doe", "123 Main St", "Apt 4B", "Metropolis", "NY", "10001", "US"
        );

        assertNotNull(viewModel.getShippingAddress().getValue());
        assertNull(viewModel.getAddressErrors().getValue());
        assertEquals(UiState.Status.CONTENT, viewModel.getQuoteState().getValue().getStatus());
        assertTrue(viewModel.getNavigateToReviewEvent().getValue().getContentIfNotHandled());
    }

    @Test
    public void testAddressSubmissionInvalidSetsErrorsAndDoesNotNavigate() {
        viewModel.submitShippingAddress("", "", null, "", "", "", "INVALID");

        assertNull(viewModel.getShippingAddress().getValue());
        assertNotNull(viewModel.getAddressErrors().getValue());
        assertFalse(viewModel.getAddressErrors().getValue().isEmpty());
        assertNull(viewModel.getNavigateToReviewEvent().getValue());
    }

    @Test
    public void testInFlightLockPreventsDoubleSubmission() {
        fakeRepository.setHoldExecution(true);

        viewModel.submitShippingAddress(
                "John Doe", "123 Main St", null, "Metropolis", "NY", "10001", "US"
        );

        assertTrue(Boolean.TRUE.equals(viewModel.getIsOperationInFlight().getValue()));

        // Second call while in flight is ignored
        viewModel.loadQuote(testAddress);
        assertEquals(1, fakeRepository.getQuoteCallCount());
    }

    @Test
    public void testPaymentChoiceSuccessCreatesOrderAndNavigatesToResult() {
        viewModel.submitShippingAddress(
                "John Doe", "123 Main St", null, "Metropolis", "NY", "10001", "US"
        );

        viewModel.submitPaymentChoice("success");

        assertNotNull(viewModel.getCurrentAttemptKey());
        assertEquals(1, fakeRepository.getCreateOrderCallCount());
        assertEquals(1, fakeRepository.getSubmitPaymentCallCount());

        PaymentResult result = viewModel.getNavigateToResultEvent().getValue().getContentIfNotHandled();
        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, result.getOrderStatus());
    }

    @Test
    public void testPaymentChoiceDeclinedNavigatesToResultWithFailedStatus() {
        viewModel.submitShippingAddress(
                "John Doe", "123 Main St", null, "Metropolis", "NY", "10001", "US"
        );

        fakeRepository.setNextPaymentException(new ApiException(409, "PAYMENT_FAILED", "Declined", null));

        viewModel.submitPaymentChoice("failure");

        PaymentResult result = viewModel.getNavigateToResultEvent().getValue().getContentIfNotHandled();
        assertNotNull(result);
        assertEquals(PaymentStatus.FAILED, result.getPaymentStatus());
        assertEquals(OrderStatus.PENDING_PAYMENT, result.getOrderStatus());
    }

    @Test
    public void testCartConflict409FiresCartConflictEvent() {
        fakeRepository.setNextQuoteException(new ApiException(409, ApiErrorCodes.CART_EMPTY, "Cart empty", null));

        viewModel.submitShippingAddress(
                "John Doe", "123 Main St", null, "Metropolis", "NY", "10001", "US"
        );

        assertNotNull(viewModel.getCartConflictEvent().getValue().getContentIfNotHandled());
    }

    @Test
    public void testAuthErrorFiresAuthRequiredEvent() {
        fakeRepository.setNextQuoteException(new ApiException(401, ApiErrorCodes.UNAUTHENTICATED, "Auth required", null));

        viewModel.submitShippingAddress(
                "John Doe", "123 Main St", null, "Metropolis", "NY", "10001", "US"
        );

        assertTrue(Boolean.TRUE.equals(viewModel.getAuthRequiredEvent().getValue().getContentIfNotHandled()));
    }

    @Test
    public void testCheckOrderStatusRecoversState() {
        viewModel.checkOrderStatus(5001L);

        assertEquals(UiState.Status.CONTENT, viewModel.getOrderDetailState().getValue().getStatus());
        Order order = viewModel.getOrderDetailState().getValue().getData();
        assertNotNull(order);
        assertEquals(5001, order.getId());
    }

    // --- Fake Repository for ViewModel Testing ---

    private static class FakeCheckoutRepository extends CheckoutRepository {
        private boolean holdExecution = false;
        private int quoteCallCount = 0;
        private int createOrderCallCount = 0;
        private int submitPaymentCallCount = 0;
        private ApiException nextQuoteException = null;
        private ApiException nextPaymentException = null;

        FakeCheckoutRepository() {
            super(new DummyApiRoutes());
        }

        void setHoldExecution(boolean hold) {
            this.holdExecution = hold;
        }

        void setNextQuoteException(ApiException exception) {
            this.nextQuoteException = exception;
        }

        void setNextPaymentException(ApiException exception) {
            this.nextPaymentException = exception;
        }

        int getQuoteCallCount() {
            return quoteCallCount;
        }

        int getCreateOrderCallCount() {
            return createOrderCallCount;
        }

        int getSubmitPaymentCallCount() {
            return submitPaymentCallCount;
        }

        @Override
        public void getQuote(@NonNull ShippingAddress address, @NonNull CheckoutCallback<CheckoutQuote> callback) {
            quoteCallCount++;
            if (holdExecution) return;
            if (nextQuoteException != null) {
                callback.onError(nextQuoteException);
                nextQuoteException = null;
                return;
            }
            callback.onSuccess(new CheckoutQuote(
                    Collections.emptyList(), "89.99", "5.00", "94.99",
                    Currency.USD, address, "test-fingerprint"
            ));
        }

        @Override
        public void createOrder(
                @NonNull String idempotencyKey,
                @NonNull ShippingAddress address,
                @Nullable String notes,
                @NonNull CheckoutCallback<Order> callback
        ) {
            createOrderCallCount++;
            if (holdExecution) return;
            callback.onSuccess(new Order(
                    5001, "ESV-005001", OrderStatus.PENDING_PAYMENT, PaymentStatus.PENDING,
                    Collections.emptyList(), "89.99", "5.00", "94.99", Currency.USD,
                    address, notes, null, Collections.emptyList(), "2026-10-10", "2026-10-10"
            ));
        }

        @Override
        public void submitPayment(
                @NonNull String idempotencyKey,
                long orderId,
                @NonNull String outcome,
                @NonNull CheckoutCallback<PaymentResult> callback
        ) {
            submitPaymentCallCount++;
            if (holdExecution) return;
            if (nextPaymentException != null) {
                callback.onError(nextPaymentException);
                nextPaymentException = null;
                return;
            }
            callback.onSuccess(new PaymentResult(
                    (int) orderId, "ESV-005001", OrderStatus.CONFIRMED, PaymentStatus.COMPLETED, true, null
            ));
        }

        @Override
        public void getOrder(long orderId, @NonNull CheckoutCallback<Order> callback) {
            if (holdExecution) return;
            callback.onSuccess(new Order(
                    (int) orderId, "ESV-005001", OrderStatus.CONFIRMED, PaymentStatus.COMPLETED,
                    Collections.emptyList(), "89.99", "5.00", "94.99", Currency.USD,
                    new ShippingAddress("A", "B", null, "C", "D", "E", "US"),
                    null, null, Collections.emptyList(), "2026-10-10", "2026-10-10"
            ));
        }
    }

    private static class DummyApiRoutes implements com.averonlabs.eshopverse.core.net.ApiRoutes {
        @Override public retrofit2.Call<com.averonlabs.eshopverse.core.net.ApiResponse<java.util.List<com.averonlabs.eshopverse.core.model.Category>>> listCategories() { return null; }
        @Override public retrofit2.Call<com.averonlabs.eshopverse.core.net.ApiResponse<com.averonlabs.eshopverse.core.model.ProductDetail>> getProduct(long id) { return null; }
        @Override public retrofit2.Call<com.averonlabs.eshopverse.core.net.PagedResponse<com.averonlabs.eshopverse.core.model.ProductSummary>> listProducts(Integer page, Integer perPage, Long categoryId, String search, String sort) { return null; }
        @Override public retrofit2.Call<com.averonlabs.eshopverse.core.net.ApiResponse<com.averonlabs.eshopverse.core.model.AuthSession>> registerCustomer(com.averonlabs.eshopverse.core.model.RegisterRequest r) { return null; }
        @Override public retrofit2.Call<com.averonlabs.eshopverse.core.net.ApiResponse<com.averonlabs.eshopverse.core.model.AuthSession>> loginCustomer(com.averonlabs.eshopverse.core.model.LoginRequest r) { return null; }
        @Override public retrofit2.Call<com.averonlabs.eshopverse.core.net.ApiResponse<com.averonlabs.eshopverse.core.model.AuthSession>> loginWithGoogle(com.averonlabs.eshopverse.core.model.GoogleAuthRequest r) { return null; }
        @Override public retrofit2.Call<Void> logoutCustomer() { return null; }
        @Override public retrofit2.Call<com.averonlabs.eshopverse.core.net.ApiResponse<com.averonlabs.eshopverse.core.model.User>> getCustomerProfile() { return null; }
        @Override public retrofit2.Call<com.averonlabs.eshopverse.core.net.ApiResponse<com.averonlabs.eshopverse.core.model.User>> updateCustomerProfile(com.averonlabs.eshopverse.core.model.UpdateProfileRequest r) { return null; }
        @Override public retrofit2.Call<Void> requestPasswordReset(com.averonlabs.eshopverse.core.model.PasswordResetRequest r) { return null; }
        @Override public retrofit2.Call<Void> confirmPasswordReset(com.averonlabs.eshopverse.core.model.PasswordResetConfirmRequest r) { return null; }
        @Override public retrofit2.Call<com.averonlabs.eshopverse.core.net.ApiResponse<com.averonlabs.eshopverse.core.model.Cart>> getCart() { return null; }
        @Override public retrofit2.Call<com.averonlabs.eshopverse.core.net.ApiResponse<com.averonlabs.eshopverse.core.model.Cart>> addItemToCart(com.averonlabs.eshopverse.core.model.AddCartItemRequest r) { return null; }
        @Override public retrofit2.Call<com.averonlabs.eshopverse.core.net.ApiResponse<com.averonlabs.eshopverse.core.model.Cart>> updateCartItemQuantity(long id, com.averonlabs.eshopverse.core.model.UpdateCartItemQuantityRequest r) { return null; }
        @Override public retrofit2.Call<com.averonlabs.eshopverse.core.net.ApiResponse<com.averonlabs.eshopverse.core.model.Cart>> removeCartItem(long id) { return null; }
        @Override public retrofit2.Call<Void> clearCart() { return null; }
        @Override public retrofit2.Call<com.averonlabs.eshopverse.core.net.ApiResponse<CheckoutQuote>> getCheckoutQuote(com.averonlabs.eshopverse.core.model.CheckoutQuoteRequest r) { return null; }
        @Override public retrofit2.Call<com.averonlabs.eshopverse.core.net.ApiResponse<Order>> createOrder(String k, com.averonlabs.eshopverse.core.model.CreateOrderRequest r) { return null; }
        @Override public retrofit2.Call<com.averonlabs.eshopverse.core.net.ApiResponse<Order>> createOrder(com.averonlabs.eshopverse.core.model.CreateOrderRequest r) { return null; }
        @Override public retrofit2.Call<com.averonlabs.eshopverse.core.net.PagedResponse<com.averonlabs.eshopverse.core.model.OrderSummary>> listOrders(Integer p, Integer pp) { return null; }
        @Override public retrofit2.Call<com.averonlabs.eshopverse.core.net.ApiResponse<Order>> getOrder(long id) { return null; }
        @Override public retrofit2.Call<com.averonlabs.eshopverse.core.net.ApiResponse<PaymentResult>> submitSimulatedPayment(String k, long id, com.averonlabs.eshopverse.core.model.SimulatedPaymentRequest r) { return null; }
        @Override public retrofit2.Call<com.averonlabs.eshopverse.core.net.ApiResponse<PaymentResult>> submitSimulatedPayment(long id, com.averonlabs.eshopverse.core.model.SimulatedPaymentRequest r) { return null; }
    }
}
