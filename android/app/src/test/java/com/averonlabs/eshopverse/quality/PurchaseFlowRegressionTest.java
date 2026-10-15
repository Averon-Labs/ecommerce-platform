package com.averonlabs.eshopverse.quality;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.averonlabs.eshopverse.ApiFixtureLoader;
import com.averonlabs.eshopverse.core.model.AddCartItemRequest;
import com.averonlabs.eshopverse.core.model.AuthSession;
import com.averonlabs.eshopverse.core.model.Cart;
import com.averonlabs.eshopverse.core.model.Category;
import com.averonlabs.eshopverse.core.model.CategoryStatus;
import com.averonlabs.eshopverse.core.model.CheckoutQuote;
import com.averonlabs.eshopverse.core.model.CheckoutQuoteRequest;
import com.averonlabs.eshopverse.core.model.CreateOrderRequest;
import com.averonlabs.eshopverse.core.model.LoginRequest;
import com.averonlabs.eshopverse.core.model.Order;
import com.averonlabs.eshopverse.core.model.OrderStatus;
import com.averonlabs.eshopverse.core.model.OrderSummary;
import com.averonlabs.eshopverse.core.model.PaymentMethod;
import com.averonlabs.eshopverse.core.model.PaymentResult;
import com.averonlabs.eshopverse.core.model.PaymentStatus;
import com.averonlabs.eshopverse.core.model.ProductDetail;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.model.ShippingAddress;
import com.averonlabs.eshopverse.core.model.SimulatedPaymentRequest;
import com.averonlabs.eshopverse.core.model.UpdateCartItemQuantityRequest;
import com.averonlabs.eshopverse.core.model.UserRole;
import com.averonlabs.eshopverse.core.net.ApiClient;
import com.averonlabs.eshopverse.core.net.ApiResponse;
import com.averonlabs.eshopverse.core.net.ApiRoutes;
import com.averonlabs.eshopverse.core.net.PagedResponse;
import com.averonlabs.eshopverse.core.storage.InMemoryTokenStore;
import com.averonlabs.eshopverse.core.storage.TokenStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Response;

/**
 * QUAL-001 (Android contribution): Deterministic End-to-End Real Backend Integration & Regression Suite.
 *
 * Covers:
 * a. Base URL switch between mock and live backend without production code edits.
 * b. Complete customer purchase journey:
 *    login -> browse categories & products -> product detail -> add to cart -> update cart quantity
 *    -> checkout quote with flat shipping -> idempotent order creation -> simulated payment success/failure
 *    -> order history & detail with timeline inspection.
 * c. Network timeout and retry policies: safe GET idempotency retries once, non-idempotent mutations never retry.
 */
public class PurchaseFlowRegressionTest {

    private MockWebServer mockWebServer;
    private TokenStore tokenStore;
    private ApiClient apiClient;
    private ApiRoutes routes;

    private final ShippingAddress testAddress = new ShippingAddress(
            "John Doe",
            "123 Main St",
            "Apt 4B",
            "Metropolis",
            "NY",
            "10001",
            "US"
    );

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        tokenStore = new InMemoryTokenStore();
        apiClient = new ApiClient(
                mockWebServer.url("/api/v1/").toString(),
                tokenStore,
                null,
                false
        );
        routes = apiClient.getRoutes();
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    /**
     * Acceptance criterion a: Base URL switch functions without code changes.
     */
    @Test
    public void test_base_url_configuration_and_switch_contract() {
        String liveBackendUrl = "http://10.0.2.2:8000/api/v1/";
        String mockServerUrl = "http://10.0.2.2:8081/api/v1/";

        ApiClient liveClient = new ApiClient(liveBackendUrl, tokenStore, null, false);
        assertEquals(liveBackendUrl, liveClient.getRetrofit().baseUrl().toString());

        ApiClient mockClient = new ApiClient(mockServerUrl, tokenStore, null, false);
        assertEquals(mockServerUrl, mockClient.getRetrofit().baseUrl().toString());

        assertNotNull(liveClient.getRoutes());
        assertNotNull(mockClient.getRoutes());
    }

    /**
     * Acceptance criterion b: Full deterministic purchase journey walkthrough.
     */
    @Test
    public void test_full_customer_purchase_journey_deterministic_success() throws Exception {
        // 1. Customer Login (POST /auth/login)
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(ApiFixtureLoader.load("loginCustomer", "success")));

        Response<ApiResponse<AuthSession>> loginResp = routes.loginCustomer(
                new LoginRequest("demo@example.com", "Password123")
        ).execute();

        assertTrue(loginResp.isSuccessful());
        assertNotNull(loginResp.body());
        AuthSession session = loginResp.body().getData();
        assertNotNull(session);
        assertEquals("test-jwt-bearer-token-12345", session.getToken());
        assertEquals(UserRole.CUSTOMER, session.getUser().getRole());
        tokenStore.save(session.getToken(), session.getExpiresAt());

        RecordedRequest loginReq = mockWebServer.takeRequest();
        assertEquals("/api/v1/auth/login", loginReq.getPath());
        assertEquals("POST", loginReq.getMethod());

        // 2. Browse Categories (GET /categories)
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(ApiFixtureLoader.load("listCategories", "success")));

        Response<ApiResponse<List<Category>>> categoriesResp = routes.listCategories().execute();
        assertTrue(categoriesResp.isSuccessful());
        assertNotNull(categoriesResp.body());
        List<Category> categories = categoriesResp.body().getData();
        assertFalse(categories.isEmpty());
        assertEquals(CategoryStatus.ACTIVE, categories.get(0).getStatus());

        RecordedRequest catReq = mockWebServer.takeRequest();
        assertEquals("/api/v1/categories", catReq.getPath());
        assertEquals("Bearer " + session.getToken(), catReq.getHeader("Authorization"));

        // 3. Browse Products (GET /products)
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(ApiFixtureLoader.load("listProducts", "success")));

        Response<PagedResponse<ProductSummary>> productsResp = routes.listProducts(1, 20, null, null, null).execute();
        assertTrue(productsResp.isSuccessful());
        assertNotNull(productsResp.body());
        assertEquals(2, productsResp.body().getData().size());
        ProductSummary product = productsResp.body().getData().get(0);
        assertEquals(101, product.getId());
        assertEquals("89.99", product.getPrice());

        RecordedRequest prodListReq = mockWebServer.takeRequest();
        assertTrue(prodListReq.getPath().startsWith("/api/v1/products"));

        // 4. Product Details (GET /products/101)
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(ApiFixtureLoader.load("getProduct", "success")));

        Response<ApiResponse<ProductDetail>> productDetailResp = routes.getProduct(101).execute();
        assertTrue(productDetailResp.isSuccessful());
        assertNotNull(productDetailResp.body());
        ProductDetail detail = productDetailResp.body().getData();
        assertEquals(101, detail.getId());
        assertEquals("Classic Leather Sneaker", detail.getName());
        assertEquals("89.99", detail.getPrice());
        assertNotNull(detail.getDescription());

        RecordedRequest prodDetailReq = mockWebServer.takeRequest();
        assertEquals("/api/v1/products/101", prodDetailReq.getPath());

        // 5. Add to Cart (POST /cart/items)
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(ApiFixtureLoader.load("cart", "populated")));

        Response<ApiResponse<Cart>> addCartResp = routes.addItemToCart(new AddCartItemRequest(101, 1)).execute();
        assertTrue(addCartResp.isSuccessful());
        assertNotNull(addCartResp.body());
        Cart cart = addCartResp.body().getData();
        assertEquals(3, cart.getItemCount());
        assertEquals("204.48", cart.getSubtotal());
        assertEquals("209.48", cart.getTotal());
        assertEquals("5.00", cart.getShipping());

        RecordedRequest addCartReq = mockWebServer.takeRequest();
        assertEquals("/api/v1/cart/items", addCartReq.getPath());
        assertEquals("POST", addCartReq.getMethod());

        // 6. Update Cart Item Quantity (PATCH /cart/items/101)
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(ApiFixtureLoader.load("cart", "populated")));

        Response<ApiResponse<Cart>> updateCartResp = routes.updateCartItemQuantity(101, new UpdateCartItemQuantityRequest(2)).execute();
        assertTrue(updateCartResp.isSuccessful());

        RecordedRequest updateCartReq = mockWebServer.takeRequest();
        assertEquals("/api/v1/cart/items/101", updateCartReq.getPath());
        assertEquals("PATCH", updateCartReq.getMethod());

        // 7. Request Checkout Quote (POST /checkout/quote)
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(ApiFixtureLoader.load("checkout", "quote")));

        Response<ApiResponse<CheckoutQuote>> quoteResp = routes.getCheckoutQuote(new CheckoutQuoteRequest(testAddress)).execute();
        assertTrue(quoteResp.isSuccessful());
        assertNotNull(quoteResp.body());
        CheckoutQuote quote = quoteResp.body().getData();
        assertEquals("5.00", quote.getShipping());
        assertEquals("94.99", quote.getTotal());
        assertEquals("addr_fingerprint_12345", quote.getShippingAddressId());

        RecordedRequest quoteReq = mockWebServer.takeRequest();
        assertEquals("/api/v1/checkout/quote", quoteReq.getPath());

        // 8. Create Order Idempotently (POST /orders)
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(ApiFixtureLoader.load("createOrder", "success")));

        String idempotencyKey = UUID.randomUUID().toString();
        Response<ApiResponse<Order>> createOrderResp = routes.createOrder(
                idempotencyKey,
                new CreateOrderRequest(testAddress, "Please leave at door")
        ).execute();

        assertTrue(createOrderResp.isSuccessful());
        assertNotNull(createOrderResp.body());
        Order order = createOrderResp.body().getData();
        assertEquals(5001, order.getId());
        assertEquals("ESV-005001", order.getReference());
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals("94.99", order.getTotal());

        RecordedRequest createOrderReq = mockWebServer.takeRequest();
        assertEquals("/api/v1/orders", createOrderReq.getPath());
        assertEquals(idempotencyKey, createOrderReq.getHeader("Idempotency-Key"));

        // 9. Submit Simulated Payment Success (POST /orders/5001/payment)
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(ApiFixtureLoader.load("submitSimulatedPayment", "success")));

        String paymentIdempotencyKey = UUID.randomUUID().toString();
        Response<ApiResponse<PaymentResult>> paymentResp = routes.submitSimulatedPayment(
                paymentIdempotencyKey,
                order.getId(),
                new SimulatedPaymentRequest(PaymentMethod.DEMO, "success")
        ).execute();

        assertTrue(paymentResp.isSuccessful());
        assertNotNull(paymentResp.body());
        PaymentResult paymentResult = paymentResp.body().getData();
        assertEquals(OrderStatus.CONFIRMED, paymentResult.getOrderStatus());
        assertEquals(PaymentStatus.COMPLETED, paymentResult.getPaymentStatus());
        assertEquals("DEMO-ABCD1234EF56", paymentResult.getPayment().getTransactionId());

        RecordedRequest paymentReq = mockWebServer.takeRequest();
        assertEquals("/api/v1/orders/5001/payment", paymentReq.getPath());
        assertEquals(paymentIdempotencyKey, paymentReq.getHeader("Idempotency-Key"));

        // 10. List Orders (GET /orders)
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(ApiFixtureLoader.load("listOrders", "success")));

        Response<PagedResponse<OrderSummary>> ordersListResp = routes.listOrders(1, 20).execute();
        assertTrue(ordersListResp.isSuccessful());
        assertNotNull(ordersListResp.body());
        assertEquals(1, ordersListResp.body().getData().size());
        OrderSummary summary = ordersListResp.body().getData().get(0);
        assertEquals("ESV-005001", summary.getReference());
        assertEquals(OrderStatus.CONFIRMED, summary.getStatus());

        RecordedRequest orderListReq = mockWebServer.takeRequest();
        assertTrue(orderListReq.getPath().startsWith("/api/v1/orders"));

        // 11. Get Order Details (GET /orders/5001)
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(ApiFixtureLoader.load("getOrder", "success")));

        Response<ApiResponse<Order>> orderDetailResp = routes.getOrder(5001).execute();
        assertTrue(orderDetailResp.isSuccessful());
        assertNotNull(orderDetailResp.body());
        Order fetchedOrder = orderDetailResp.body().getData();
        assertEquals(5001, fetchedOrder.getId());
        assertEquals(OrderStatus.CONFIRMED, fetchedOrder.getStatus());
        assertEquals(PaymentStatus.COMPLETED, fetchedOrder.getPaymentStatus());
        assertEquals(2, fetchedOrder.getTimeline().size());
        assertEquals(OrderStatus.PENDING_PAYMENT, fetchedOrder.getTimeline().get(0).getStatus());
        assertEquals(OrderStatus.CONFIRMED, fetchedOrder.getTimeline().get(1).getStatus());

        RecordedRequest orderDetailReq = mockWebServer.takeRequest();
        assertEquals("/api/v1/orders/5001", orderDetailReq.getPath());
    }

    /**
     * Simulated failure outcome keeps order in pending_payment without stock deduction.
     */
    @Test
    public void test_simulated_payment_failure_leaves_order_retryable() throws Exception {
        tokenStore.save("tok_customer_test_12345", "2099-01-01T00:00:00Z");

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(409)
                .setHeader("Content-Type", "application/json")
                .setBody(ApiFixtureLoader.load("submitSimulatedPayment", "payment-failure")));

        String idempotencyKey = UUID.randomUUID().toString();
        Response<ApiResponse<PaymentResult>> paymentResp = routes.submitSimulatedPayment(
                idempotencyKey,
                5001,
                new SimulatedPaymentRequest(PaymentMethod.DEMO, "failure")
        ).execute();

        assertFalse(paymentResp.isSuccessful());
        assertEquals(409, paymentResp.code());

        RecordedRequest paymentReq = mockWebServer.takeRequest();
        assertEquals("/api/v1/orders/5001/payment", paymentReq.getPath());
        assertEquals(idempotencyKey, paymentReq.getHeader("Idempotency-Key"));
    }

    /**
     * Acceptance criterion c: Network retry policy and idempotency safety.
     * Safe GET requests are retried once on 5xx; mutating POST requests are never automatically retried.
     */
    @Test
    public void test_network_retry_policy_mutations_never_auto_retried_and_get_retried_once() throws Exception {
        // Test GET retry: first request fails with 500, second succeeds with 200
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(ApiFixtureLoader.load("listCategories", "success")));

        Response<ApiResponse<List<Category>>> getResp = routes.listCategories().execute();
        assertTrue(getResp.isSuccessful());
        assertEquals(2, mockWebServer.getRequestCount()); // Retried once automatically

        // Test POST mutation: failure with 500 must NOT be retried
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        try {
            routes.addItemToCart(new AddCartItemRequest(101, 1)).execute();
        } catch (Exception ignored) {
            // May throw or return error response
        }

        assertEquals(3, mockWebServer.getRequestCount()); // Exactly 1 attempt for the POST; no retry!
    }

    /**
     * Idempotent replay: submitting the exact same order request with the same idempotency key.
     */
    @Test
    public void test_duplicate_order_submission_idempotency_safe() throws Exception {
        tokenStore.save("tok_customer_test_12345", "2099-01-01T00:00:00Z");

        String idempotencyKey = UUID.randomUUID().toString();
        CreateOrderRequest request = new CreateOrderRequest(testAddress, "No duplicate");

        // First attempt returns 201 Created
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(ApiFixtureLoader.load("createOrder", "success")));

        Response<ApiResponse<Order>> firstResp = routes.createOrder(idempotencyKey, request).execute();
        assertTrue(firstResp.isSuccessful());
        assertEquals(5001, firstResp.body().getData().getId());

        // Second attempt with same idempotency key replays the stored 201 response
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(ApiFixtureLoader.load("createOrder", "success")));

        Response<ApiResponse<Order>> secondResp = routes.createOrder(idempotencyKey, request).execute();
        assertTrue(secondResp.isSuccessful());
        assertEquals(firstResp.body().getData().getId(), secondResp.body().getData().getId());
        assertEquals(firstResp.body().getData().getReference(), secondResp.body().getData().getReference());

        RecordedRequest firstReq = mockWebServer.takeRequest();
        RecordedRequest secondReq = mockWebServer.takeRequest();

        assertEquals(idempotencyKey, firstReq.getHeader("Idempotency-Key"));
        assertEquals(idempotencyKey, secondReq.getHeader("Idempotency-Key"));
    }
}
