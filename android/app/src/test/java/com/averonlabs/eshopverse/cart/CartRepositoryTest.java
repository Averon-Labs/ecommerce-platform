package com.averonlabs.eshopverse.cart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.averonlabs.eshopverse.ApiFixtureLoader;
import com.averonlabs.eshopverse.core.error.ApiErrorCodes;
import com.averonlabs.eshopverse.core.model.Cart;
import com.averonlabs.eshopverse.core.model.CartItem;
import com.averonlabs.eshopverse.core.net.ApiClient;
import com.averonlabs.eshopverse.core.net.ApiException;
import com.averonlabs.eshopverse.core.storage.InMemoryTokenStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * JVM unit tests for {@link CartRepository} per docs/android/PHASE-2-SPEC.md §D.5.
 * Proves parsing of every cart state, mapping of 409 INSUFFICIENT_STOCK, 404 CART_ITEM_NOT_FOUND,
 * 401 UNAUTHENTICATED, and that mutations are never automatically retried.
 */
public class CartRepositoryTest {

    private MockWebServer mockWebServer;
    private CartRepository repository;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        ApiClient apiClient = new ApiClient(
                mockWebServer.url("/api/v1/").toString(),
                new InMemoryTokenStore(),
                null,
                false
        );
        repository = new CartRepository(apiClient.getRoutes());
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testLoadPopulatedCartSuccess() throws Exception {
        String fixture = ApiFixtureLoader.load("cart", "populated");
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(fixture));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Cart> resultRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        repository.getCart(new CartRepository.CartCallback<Cart>() {
            @Override
            public void onSuccess(Cart result) {
                resultRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                errorRef.set(throwable);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNull(errorRef.get());
        Cart cart = resultRef.get();
        assertNotNull(cart);

        assertEquals(3, cart.getItemCount());
        assertEquals("204.48", cart.getSubtotal());
        assertEquals("5.00", cart.getShipping());
        assertEquals("209.48", cart.getTotal());
        assertFalse(cart.hasIssues());
        assertEquals(2, cart.getItems().size());

        CartItem item1 = cart.getItems().get(0);
        assertEquals(1, item1.getProductId());
        assertEquals("Classic Leather Sneaker", item1.getName());
        assertEquals("89.99", item1.getUnitPrice());
        assertEquals(2, item1.getQuantity());
        assertEquals("179.98", item1.getLineTotal());
        assertTrue(item1.isInStock());
        assertEquals(10, item1.getAvailableQuantity());
        assertFalse(item1.isPriceChanged());
        assertFalse(item1.isUnavailable());
    }

    @Test
    public void testLoadEmptyCartSuccess() throws Exception {
        String fixture = ApiFixtureLoader.load("cart", "empty");
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(fixture));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Cart> resultRef = new AtomicReference<>();

        repository.getCart(new CartRepository.CartCallback<Cart>() {
            @Override
            public void onSuccess(Cart result) {
                resultRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        Cart cart = resultRef.get();
        assertNotNull(cart);
        assertEquals(0, cart.getItemCount());
        assertEquals("0.00", cart.getTotal());
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    public void testLoadCartWithIssuesSuccess() throws Exception {
        String fixture = ApiFixtureLoader.load("cart", "with-issues");
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(fixture));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Cart> resultRef = new AtomicReference<>();

        repository.getCart(new CartRepository.CartCallback<Cart>() {
            @Override
            public void onSuccess(Cart result) {
                resultRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        Cart cart = resultRef.get();
        assertNotNull(cart);
        assertTrue(cart.hasIssues());
        assertEquals(2, cart.getItems().size());

        CartItem item1 = cart.getItems().get(0);
        assertTrue(item1.isPriceChanged());
        assertFalse(item1.isUnavailable());

        CartItem item2 = cart.getItems().get(1);
        assertFalse(item2.isPriceChanged());
        assertTrue(item2.isUnavailable());
    }

    @Test
    public void testUpdateQuantitySuccess() throws Exception {
        String fixture = ApiFixtureLoader.load("cart", "populated");
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(fixture));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Cart> resultRef = new AtomicReference<>();

        repository.updateQuantity(1L, 3, new CartRepository.CartCallback<Cart>() {
            @Override
            public void onSuccess(Cart result) {
                resultRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(resultRef.get());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("PATCH", request.getMethod());
        assertEquals("/api/v1/cart/items/1", request.getPath());
        assertTrue(request.getBody().readUtf8().contains("\"quantity\":3"));
    }

    @Test
    public void testUpdateQuantityInsufficientStock409() throws Exception {
        String fixture = ApiFixtureLoader.load("cart", "insufficient-stock");
        mockWebServer.enqueue(new MockResponse().setResponseCode(409).setBody(fixture));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        repository.updateQuantity(1L, 99, new CartRepository.CartCallback<Cart>() {
            @Override
            public void onSuccess(Cart result) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                errorRef.set(throwable);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        Throwable t = errorRef.get();
        assertNotNull(t);
        assertTrue(t instanceof ApiException);
        ApiException apiException = (ApiException) t;
        assertEquals(409, apiException.getHttpStatus());
        assertEquals(ApiErrorCodes.INSUFFICIENT_STOCK, apiException.getCode());
    }

    @Test
    public void testUpdateQuantityItemNotFound404() throws Exception {
        String fixture = ApiFixtureLoader.load("cart", "cart-item-not-found");
        mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody(fixture));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        repository.updateQuantity(999L, 2, new CartRepository.CartCallback<Cart>() {
            @Override
            public void onSuccess(Cart result) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                errorRef.set(throwable);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        Throwable t = errorRef.get();
        assertNotNull(t);
        assertTrue(t instanceof ApiException);
        ApiException apiException = (ApiException) t;
        assertEquals(404, apiException.getHttpStatus());
        assertEquals(ApiErrorCodes.CART_ITEM_NOT_FOUND, apiException.getCode());
    }

    @Test
    public void testRemoveItemSuccess() throws Exception {
        String fixture = ApiFixtureLoader.load("cart", "empty");
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(fixture));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Cart> resultRef = new AtomicReference<>();

        repository.removeItem(1L, new CartRepository.CartCallback<Cart>() {
            @Override
            public void onSuccess(Cart result) {
                resultRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(resultRef.get());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("DELETE", request.getMethod());
        assertEquals("/api/v1/cart/items/1", request.getPath());
    }

    @Test
    public void testCartUnauthenticated401() throws Exception {
        String fixture = ApiFixtureLoader.load("common", "unauthenticated");
        mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody(fixture));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        repository.getCart(new CartRepository.CartCallback<Cart>() {
            @Override
            public void onSuccess(Cart result) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                errorRef.set(throwable);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        Throwable t = errorRef.get();
        assertNotNull(t);
        assertTrue(t instanceof ApiException);
        ApiException apiException = (ApiException) t;
        assertEquals(401, apiException.getHttpStatus());
        assertEquals(ApiErrorCodes.UNAUTHENTICATED, apiException.getCode());
    }

    @Test
    public void testMutationsDoNotAutoRetry() throws Exception {
        // Enqueue single 500 error response
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("{\"error\":{\"code\":\"INTERNAL_ERROR\"}}"));

        CountDownLatch latch = new CountDownLatch(1);
        repository.updateQuantity(1L, 2, new CartRepository.CartCallback<Cart>() {
            @Override
            public void onSuccess(Cart result) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        // Verify only ONE request was sent to the server (no retry)
        assertEquals(1, mockWebServer.getRequestCount());
    }
}
