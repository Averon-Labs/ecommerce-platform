package com.averonlabs.eshopverse.checkout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.averonlabs.eshopverse.ApiFixtureLoader;
import com.averonlabs.eshopverse.core.error.ApiErrorCodes;
import com.averonlabs.eshopverse.core.model.CheckoutQuote;
import com.averonlabs.eshopverse.core.model.Order;
import com.averonlabs.eshopverse.core.model.OrderStatus;
import com.averonlabs.eshopverse.core.model.PaymentResult;
import com.averonlabs.eshopverse.core.model.PaymentStatus;
import com.averonlabs.eshopverse.core.model.ShippingAddress;
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
 * JVM unit tests for CheckoutRepository per docs/android/PHASE-2-SPEC.md §E.7.
 */
public class CheckoutRepositoryTest {

    private MockWebServer mockWebServer;
    private CheckoutRepository repository;
    private ShippingAddress testAddress;

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
        repository = new CheckoutRepository(apiClient.getRoutes());
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

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testGetQuoteSuccess() throws Exception {
        String fixture = ApiFixtureLoader.load("checkout", "quote");
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(fixture));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CheckoutQuote> quoteRef = new AtomicReference<>();

        repository.getQuote(testAddress, new CheckoutRepository.CheckoutCallback<CheckoutQuote>() {
            @Override
            public void onSuccess(CheckoutQuote result) {
                quoteRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        CheckoutQuote quote = quoteRef.get();
        assertNotNull(quote);
        assertEquals("89.99", quote.getSubtotal());
        assertEquals("5.00", quote.getShipping());
        assertEquals("94.99", quote.getTotal());
        assertEquals(1, quote.getItems().size());
        assertEquals("addr_fingerprint_12345", quote.getShippingAddressId());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/checkout/quote", request.getPath());
    }

    @Test
    public void testCreateOrderSuccessWithIdempotencyKey() throws Exception {
        String fixture = ApiFixtureLoader.load("createOrder", "success");
        mockWebServer.enqueue(new MockResponse().setResponseCode(201).setBody(fixture));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Order> orderRef = new AtomicReference<>();
        String testKey = "test-idem-uuid-1234";

        repository.createOrder(testKey, testAddress, "Notes", new CheckoutRepository.CheckoutCallback<Order>() {
            @Override
            public void onSuccess(Order result) {
                orderRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        Order order = orderRef.get();
        assertNotNull(order);
        assertEquals(5001, order.getId());
        assertEquals("ESV-005001", order.getReference());
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/orders", request.getPath());
        assertEquals(testKey, request.getHeader("Idempotency-Key"));
    }

    @Test
    public void testSubmitPaymentSuccessOutcome() throws Exception {
        String fixture = ApiFixtureLoader.load("submitSimulatedPayment", "success");
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(fixture));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PaymentResult> resultRef = new AtomicReference<>();
        String testKey = "test-idem-uuid-5678";

        repository.submitPayment(testKey, 5001L, "success", new CheckoutRepository.CheckoutCallback<PaymentResult>() {
            @Override
            public void onSuccess(PaymentResult result) {
                resultRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        PaymentResult result = resultRef.get();
        assertNotNull(result);
        assertEquals(5001, result.getOrderId());
        assertEquals(PaymentStatus.COMPLETED, result.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, result.getOrderStatus());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/orders/5001/payment", request.getPath());
        assertEquals(testKey, request.getHeader("Idempotency-Key"));
    }

    @Test
    public void testSubmitPaymentFailureOutcome409() throws Exception {
        String fixture = ApiFixtureLoader.load("submitSimulatedPayment", "payment-failure");
        mockWebServer.enqueue(new MockResponse().setResponseCode(409).setBody(fixture));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        repository.submitPayment("key-1", 5001L, "failure", new CheckoutRepository.CheckoutCallback<PaymentResult>() {
            @Override
            public void onSuccess(PaymentResult result) {
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
        assertEquals("PAYMENT_FAILED", apiException.getCode());
    }

    @Test
    public void testGetOrderSuccess() throws Exception {
        String fixture = ApiFixtureLoader.load("createOrder", "success");
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(fixture));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Order> orderRef = new AtomicReference<>();

        repository.getOrder(5001L, new CheckoutRepository.CheckoutCallback<Order>() {
            @Override
            public void onSuccess(Order result) {
                orderRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        Order order = orderRef.get();
        assertNotNull(order);
        assertEquals(5001, order.getId());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/orders/5001", request.getPath());
    }

    @Test
    public void testCartEmptyConflict409() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(409)
                .setBody("{\"error\":{\"code\":\"CART_EMPTY\",\"message\":\"Cart is empty\"}}"));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        repository.getQuote(testAddress, new CheckoutRepository.CheckoutCallback<CheckoutQuote>() {
            @Override
            public void onSuccess(CheckoutQuote result) {
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
        assertEquals(ApiErrorCodes.CART_EMPTY, apiException.getCode());
    }

    @Test
    public void testUnauthenticated401() throws Exception {
        String fixture = ApiFixtureLoader.load("common", "unauthenticated");
        mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody(fixture));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        repository.createOrder("key", testAddress, null, new CheckoutRepository.CheckoutCallback<Order>() {
            @Override
            public void onSuccess(Order result) {
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
        assertEquals(401, ((ApiException) t).getHttpStatus());
        assertEquals(ApiErrorCodes.UNAUTHENTICATED, ((ApiException) t).getCode());
    }

    @Test
    public void testMutationsDoNotAutoRetry() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("{\"error\":{\"code\":\"INTERNAL_ERROR\"}}"));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        repository.createOrder("test-key", testAddress, null, new CheckoutRepository.CheckoutCallback<Order>() {
            @Override
            public void onSuccess(Order result) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                errorRef.set(throwable);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertNotNull(errorRef.get());
        assertEquals(1, mockWebServer.getRequestCount());
    }
}
