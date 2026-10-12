package com.averonlabs.eshopverse.cart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.error.ApiErrorCodes;
import com.averonlabs.eshopverse.core.model.Cart;
import com.averonlabs.eshopverse.core.model.CartItem;
import com.averonlabs.eshopverse.core.model.Currency;
import com.averonlabs.eshopverse.core.net.ApiException;
import com.averonlabs.eshopverse.core.state.Event;
import com.averonlabs.eshopverse.core.state.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JVM unit tests for {@link CartViewModel} per docs/android/PHASE-2-SPEC.md §D.5.
 * Proves in-flight locking prevents double mutation, state after each failure,
 * stale-line recovery on 404, and auth-required event routing.
 */
public class CartViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private CartRepository repository;
    private CartViewModel viewModel;

    @Before
    public void setUp() {
        repository = mock(CartRepository.class);
        viewModel = new CartViewModel(repository);
    }

    private Cart createTestCart(int itemCount, boolean hasIssues) {
        List<CartItem> items = new ArrayList<>();
        if (itemCount > 0) {
            items.add(new CartItem(
                    1,
                    "Test Item",
                    null,
                    "50.00",
                    Currency.USD,
                    itemCount,
                    "50.00",
                    true,
                    10,
                    false,
                    false
            ));
        }
        return new Cart(
                items,
                itemCount,
                "50.00",
                "5.00",
                "55.00",
                Currency.USD,
                hasIssues
        );
    }

    @Test
    public void testInitialLoadCartSuccess() {
        Cart cart = createTestCart(2, false);
        doAnswer(invocation -> {
            CartRepository.CartCallback<Cart> cb = invocation.getArgument(0);
            cb.onSuccess(cart);
            return null;
        }).when(repository).getCart(any());

        viewModel.loadCart();

        UiState<Cart> state = viewModel.getCartState().getValue();
        assertNotNull(state);
        assertEquals(UiState.Status.CONTENT, state.getStatus());
        assertEquals(cart, state.getData());
        assertEquals(Integer.valueOf(2), viewModel.getItemCount().getValue());
    }

    @Test
    public void testInitialLoadCartEmpty() {
        Cart emptyCart = new Cart(
                Collections.emptyList(),
                0,
                "0.00",
                "0.00",
                "0.00",
                Currency.USD,
                false
        );
        doAnswer(invocation -> {
            CartRepository.CartCallback<Cart> cb = invocation.getArgument(0);
            cb.onSuccess(emptyCart);
            return null;
        }).when(repository).getCart(any());

        viewModel.loadCart();

        UiState<Cart> state = viewModel.getCartState().getValue();
        assertNotNull(state);
        assertEquals(UiState.Status.EMPTY, state.getStatus());
        assertEquals(Integer.valueOf(0), viewModel.getItemCount().getValue());
    }

    @Test
    public void testLoadCartError() {
        doAnswer(invocation -> {
            CartRepository.CartCallback<Cart> cb = invocation.getArgument(0);
            cb.onError(new RuntimeException("Network timeout"));
            return null;
        }).when(repository).getCart(any());

        viewModel.loadCart();

        UiState<Cart> state = viewModel.getCartState().getValue();
        assertNotNull(state);
        assertEquals(UiState.Status.ERROR, state.getStatus());
        assertEquals(R.string.cart_error_loading, state.getMessageRes());
        assertTrue(state.isRetryable());
    }

    @Test
    public void testInFlightLockPreventsDoubleMutation() {
        AtomicReference<CartRepository.CartCallback<Cart>> pendingCallback = new AtomicReference<>();
        doAnswer(invocation -> {
            pendingCallback.set(invocation.getArgument(2));
            return null;
        }).when(repository).updateQuantity(eq(1L), anyInt(), any());

        // First mutation begins and is in flight
        viewModel.updateQuantity(1L, 2);
        assertEquals(Long.valueOf(1L), viewModel.getMutatingProductId().getValue());
        verify(repository, times(1)).updateQuantity(eq(1L), eq(2), any());

        // Second mutation attempted while first is in flight
        viewModel.updateQuantity(1L, 3);
        // Repository should NOT be called a second time
        verify(repository, times(1)).updateQuantity(anyLong(), anyInt(), any());

        // Now first mutation finishes successfully
        Cart updatedCart = createTestCart(2, false);
        assertNotNull(pendingCallback.get());
        pendingCallback.get().onSuccess(updatedCart);

        // Lock is released
        assertNull(viewModel.getMutatingProductId().getValue());
        assertEquals(UiState.Status.CONTENT, viewModel.getCartState().getValue().getStatus());

        // Subsequent mutation is now permitted
        viewModel.updateQuantity(1L, 4);
        verify(repository, times(2)).updateQuantity(anyLong(), anyInt(), any());
    }

    @Test
    public void testInsufficientStockLeavesStateAndSurfacesNotice() {
        doAnswer(invocation -> {
            CartRepository.CartCallback<Cart> cb = invocation.getArgument(2);
            cb.onError(new ApiException(409, ApiErrorCodes.INSUFFICIENT_STOCK, "Only 2 available in stock", null));
            return null;
        }).when(repository).updateQuantity(eq(1L), anyInt(), any());

        viewModel.updateQuantity(1L, 10);

        // In-flight lock is released
        assertNull(viewModel.getMutatingProductId().getValue());

        // Notice event emitted explaining the limit
        Event<String> notice = viewModel.getNoticeEvent().getValue();
        assertNotNull(notice);
        assertEquals("Only 2 available in stock", notice.peekContent());
    }

    @Test
    public void testCartItemNotFoundTriggersStaleLineRecovery() {
        doAnswer(invocation -> {
            CartRepository.CartCallback<Cart> cb = invocation.getArgument(2);
            cb.onError(new ApiException(404, ApiErrorCodes.CART_ITEM_NOT_FOUND, "Item not found", null));
            return null;
        }).when(repository).updateQuantity(eq(99L), anyInt(), any());

        viewModel.updateQuantity(99L, 1);

        // In-flight lock released
        assertNull(viewModel.getMutatingProductId().getValue());

        // Error message posted and cart reloaded automatically (stale-line recovery)
        Event<Integer> errorRes = viewModel.getErrorResEvent().getValue();
        assertNotNull(errorRes);
        assertEquals(Integer.valueOf(R.string.cart_error_item_not_found), errorRes.peekContent());

        verify(repository, times(1)).getCart(any());
    }

    @Test
    public void testAuthErrorEmitsAuthRequiredEvent() {
        doAnswer(invocation -> {
            CartRepository.CartCallback<Cart> cb = invocation.getArgument(0);
            cb.onError(new ApiException(401, ApiErrorCodes.UNAUTHENTICATED, "Session expired", null));
            return null;
        }).when(repository).getCart(any());

        viewModel.loadCart();

        Event<Boolean> authEvent = viewModel.getAuthRequiredEvent().getValue();
        assertNotNull(authEvent);
        assertTrue(authEvent.peekContent());
    }

    @Test
    public void testRemoveItemInFlightLock() {
        AtomicReference<CartRepository.CartCallback<Cart>> pendingCallback = new AtomicReference<>();
        doAnswer(invocation -> {
            pendingCallback.set(invocation.getArgument(1));
            return null;
        }).when(repository).removeItem(eq(1L), any());

        viewModel.removeItem(1L);
        assertEquals(Long.valueOf(1L), viewModel.getMutatingProductId().getValue());
        verify(repository, times(1)).removeItem(eq(1L), any());

        // Second remove call ignored while in flight
        viewModel.removeItem(1L);
        verify(repository, times(1)).removeItem(anyLong(), any());

        // Completion
        pendingCallback.get().onSuccess(createTestCart(0, false));
        assertNull(viewModel.getMutatingProductId().getValue());
        assertEquals(UiState.Status.EMPTY, viewModel.getCartState().getValue().getStatus());
    }
}
