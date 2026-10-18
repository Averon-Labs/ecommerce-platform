package com.averonlabs.eshopverse.core.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OfflineMockInterceptorTest {

    private final Gson gson = new Gson();

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
            assertEquals(50, envelope.getData().size());
            ProductSummary product = envelope.getData().get(0);
            assertEquals("Classic White Oxford Shirt", product.getName());
            assertNotNull(product.getImageUrl());
            assertTrue(product.isInStock());
        }

        // Verify category filter returns 5 items
        Request catRequest = new Request.Builder()
                .url("http://127.0.0.1:54321/api/v1/products?category_id=1")
                .build();
        try (Response catResponse = client.newCall(catRequest).execute()) {
            assertEquals(200, catResponse.code());
            assertNotNull(catResponse.body());
            Type type = new TypeToken<PagedResponse<ProductSummary>>() {}.getType();
            PagedResponse<ProductSummary> envelope = gson.fromJson(catResponse.body().string(), type);
            assertNotNull(envelope.getData());
            assertEquals(5, envelope.getData().size());
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

    @Test
    public void testOfflineUserProfileDeserialization() throws IOException {
        OkHttpClient client = createMockClient();
        Request request = new Request.Builder()
                .url("http://127.0.0.1:54321/api/v1/auth/profile")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(200, response.code());
            assertNotNull(response.body());
            Type type = new TypeToken<ApiResponse<User>>() {}.getType();
            ApiResponse<User> envelope = gson.fromJson(response.body().string(), type);
            assertNotNull(envelope.getData());
            User user = envelope.getData();
            assertEquals("alex.morgan@eshopverse.com", user.getEmail());
            assertEquals("Alex Morgan", user.getFullName());
        }
    }
}
