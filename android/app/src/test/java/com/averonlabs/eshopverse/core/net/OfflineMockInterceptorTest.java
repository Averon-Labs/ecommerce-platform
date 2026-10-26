package com.averonlabs.eshopverse.core.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.averonlabs.eshopverse.core.model.Cart;
import com.averonlabs.eshopverse.core.model.Category;
import com.averonlabs.eshopverse.core.model.Order;
import com.averonlabs.eshopverse.core.model.OrderSummary;
import com.averonlabs.eshopverse.core.model.ProductDetail;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.model.User;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OfflineMockInterceptorTest {

    private final Gson gson = new Gson();

    @Before
    public void setUp() {
        OfflineMockInterceptor.resetMockCart();
    }

    private OkHttpClient createMockClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(new OfflineMockInterceptor())
                .build();
    }

    @Test
    public void testOfflineCategoriesDeserialization() throws IOException {
        OkHttpClient client = createMockClient();
        Request request = new Request.Builder()
                .url("http://127.0.0.1:54321/api/v1/categories")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(200, response.code());
            assertNotNull(response.body());
            Type type = new TypeToken<ApiResponse<List<Category>>>() {}.getType();
            ApiResponse<List<Category>> envelope = gson.fromJson(response.body().string(), type);
            assertNotNull(envelope.getData());
            assertEquals("Shirts & Tops", envelope.getData().get(0).getName());
            assertEquals(10, envelope.getData().size());
        }
    }

    @Test
    public void testOfflineProductsDeserialization() throws IOException {
        OkHttpClient client = createMockClient();
        Request request = new Request.Builder()
                .url("http://127.0.0.1:54321/api/v1/products")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(200, response.code());
            assertNotNull(response.body());
            Type type = new TypeToken<PagedResponse<ProductSummary>>() {}.getType();
            PagedResponse<ProductSummary> envelope = gson.fromJson(response.body().string(), type);
            assertNotNull(envelope.getData());
            assertEquals(60, envelope.getData().size());
            ProductSummary product = envelope.getData().get(0);
            assertEquals("Classic White Oxford Shirt", product.getName());
            assertNotNull(product.getImageUrl());
            assertTrue(product.isInStock());
        }

        // Verify unpaginated category filter returns all 25 items in category
        Request catRequest = new Request.Builder()
                .url("http://127.0.0.1:54321/api/v1/products?category_id=1")
                .build();
        try (Response catResponse = client.newCall(catRequest).execute()) {
            assertEquals(200, catResponse.code());
            assertNotNull(catResponse.body());
            Type type = new TypeToken<PagedResponse<ProductSummary>>() {}.getType();
            PagedResponse<ProductSummary> envelope = gson.fromJson(catResponse.body().string(), type);
            assertNotNull(envelope.getData());
            assertEquals(25, envelope.getData().size());
            assertEquals(25, envelope.getPagination().getTotalItems());
        }

        // Verify paginated category filter returns 20 items for page 1
        Request catPageRequest = new Request.Builder()
                .url("http://127.0.0.1:54321/api/v1/products?category_id=1&page=1&per_page=20")
                .build();
        try (Response catPageResponse = client.newCall(catPageRequest).execute()) {
            assertEquals(200, catPageResponse.code());
            assertNotNull(catPageResponse.body());
            Type type = new TypeToken<PagedResponse<ProductSummary>>() {}.getType();
            PagedResponse<ProductSummary> envelope = gson.fromJson(catPageResponse.body().string(), type);
            assertNotNull(envelope.getData());
            assertEquals(20, envelope.getData().size());
            assertEquals(25, envelope.getPagination().getTotalItems());
        }

        // Verify pagination returns 20 items for page 1
        Request pageRequest = new Request.Builder()
                .url("http://127.0.0.1:54321/api/v1/products?page=1&per_page=20")
                .build();
        try (Response pageResponse = client.newCall(pageRequest).execute()) {
            assertEquals(200, pageResponse.code());
            assertNotNull(pageResponse.body());
            Type type = new TypeToken<PagedResponse<ProductSummary>>() {}.getType();
            PagedResponse<ProductSummary> envelope = gson.fromJson(pageResponse.body().string(), type);
            assertNotNull(envelope.getData());
            assertEquals(20, envelope.getData().size());
            assertEquals(1, envelope.getPagination().getCurrentPage());
            assertEquals(20, envelope.getPagination().getPerPage());
            assertEquals(250, envelope.getPagination().getTotalItems());
            assertEquals(13, envelope.getPagination().getTotalPages());
        }
    }

    @Test
    public void testOfflineProductDetailDeserialization() throws IOException {
        OkHttpClient client = createMockClient();
        Request request = new Request.Builder()
                .url("http://127.0.0.1:54321/api/v1/products/101")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(200, response.code());
            assertNotNull(response.body());
            Type type = new TypeToken<ApiResponse<ProductDetail>>() {}.getType();
            ApiResponse<ProductDetail> envelope = gson.fromJson(response.body().string(), type);
            assertNotNull(envelope.getData());
            ProductDetail detail = envelope.getData();
            assertEquals("Classic White Oxford Shirt", detail.getName());
            assertEquals("shirt_001", detail.getSku());
            assertFalse(detail.getImages().isEmpty());
        }
    }

    @Test
    public void testOfflineCartDeserialization() throws IOException {
        OkHttpClient client = createMockClient();
        Request request = new Request.Builder()
                .url("http://127.0.0.1:54321/api/v1/cart")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(200, response.code());
            assertNotNull(response.body());
            Type type = new TypeToken<ApiResponse<Cart>>() {}.getType();
            ApiResponse<Cart> envelope = gson.fromJson(response.body().string(), type);
            assertNotNull(envelope.getData());
            Cart cart = envelope.getData();
            assertEquals(1, cart.getItemCount());
            assertEquals(1, cart.getItems().size());
            assertEquals("Classic White Oxford Shirt", cart.getItems().get(0).getName());
        }
    }

    @Test
    public void testOfflineCartItemDeletion() throws IOException {
        OkHttpClient client = createMockClient();
        Request deleteRequest = new Request.Builder()
                .url("http://127.0.0.1:54321/api/v1/cart/items/101")
                .delete()
                .build();

        try (Response response = client.newCall(deleteRequest).execute()) {
            assertEquals(200, response.code());
            assertNotNull(response.body());
            Type type = new TypeToken<ApiResponse<Cart>>() {}.getType();
            ApiResponse<Cart> envelope = gson.fromJson(response.body().string(), type);
            assertNotNull(envelope.getData());
            Cart cart = envelope.getData();
            assertEquals(0, cart.getItemCount());
            assertTrue(cart.getItems().isEmpty());
            assertEquals("0.00", cart.getSubtotal());
            assertEquals("0.00", cart.getTotal());
        }
    }

    @Test
    public void testOfflineCartItemQuantityUpdate() throws IOException {
        OkHttpClient client = createMockClient();
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                "{\"quantity\":3}"
        );
        Request patchRequest = new Request.Builder()
                .url("http://127.0.0.1:54321/api/v1/cart/items/101")
                .patch(body)
                .build();

        try (Response response = client.newCall(patchRequest).execute()) {
            assertEquals(200, response.code());
            assertNotNull(response.body());
            Type type = new TypeToken<ApiResponse<Cart>>() {}.getType();
            ApiResponse<Cart> envelope = gson.fromJson(response.body().string(), type);
            assertNotNull(envelope.getData());
            Cart cart = envelope.getData();
            assertEquals(3, cart.getItemCount());
            assertEquals(1, cart.getItems().size());
            assertEquals(3, cart.getItems().get(0).getQuantity());
        }
    }

    @Test
    public void testOfflineOrdersDeserialization() throws IOException {
        OkHttpClient client = createMockClient();
        Request request = new Request.Builder()
                .url("http://127.0.0.1:54321/api/v1/orders")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(200, response.code());
            assertNotNull(response.body());
            Type type = new TypeToken<PagedResponse<OrderSummary>>() {}.getType();
            PagedResponse<OrderSummary> envelope = gson.fromJson(response.body().string(), type);
            assertNotNull(envelope.getData());
            assertEquals(2, envelope.getData().size());
            assertEquals("ORD-2026-0001", envelope.getData().get(0).getReference());
        }
    }

    @Test
    public void testOfflineOrderDetailDeserialization() throws IOException {
        OkHttpClient client = createMockClient();
        Request request = new Request.Builder()
                .url("http://127.0.0.1:54321/api/v1/orders/1")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(200, response.code());
            assertNotNull(response.body());
            Type type = new TypeToken<ApiResponse<Order>>() {}.getType();
            ApiResponse<Order> envelope = gson.fromJson(response.body().string(), type);
            assertNotNull(envelope.getData());
            Order order = envelope.getData();
            assertEquals("ORD-2026-0001", order.getReference());
            assertEquals(1, order.getItems().size());
            assertNotNull(order.getShippingAddress());
            assertNotNull(order.getTimeline());
        }
    }

    @Test(expected = IOException.class)
    public void testOfflineUserProfileFailsWhenServerOffline() throws IOException {
        OkHttpClient client = createMockClient();
        Request request = new Request.Builder()
                .url("http://127.0.0.1:54321/api/v1/auth/profile")
                .build();

        client.newCall(request).execute();
    }

    @Test
    public void testAuthEndpointsHaveNoOfflineMockResponse() {
        OfflineMockInterceptor interceptor = new OfflineMockInterceptor();
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                "{\"email\":\"new@example.com\",\"password\":\"Password123\",\"firstName\":\"New\",\"lastName\":\"User\"}"
        );

        for (String endpoint : new String[]{"login", "register", "google"}) {
            Request request = new Request.Builder()
                    .url("http://127.0.0.1:54321/api/v1/auth/" + endpoint)
                    .post(body)
                    .build();

            assertNull(
                    "Offline auth must not fabricate a session for /auth/" + endpoint,
                    interceptor.getMockResponseFor(request)
            );
        }
    }
}
