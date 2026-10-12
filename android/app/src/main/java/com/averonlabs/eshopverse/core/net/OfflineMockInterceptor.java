package com.averonlabs.eshopverse.core.net;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Provides offline fallback dummy data when the local mock server or backend is not reachable.
 * Active in debug builds to ensure an immediate, functional, interactive demo experience out-of-the-box.
 */
public class OfflineMockInterceptor implements Interceptor {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        try {
            return chain.proceed(request);
        } catch (IOException e) {
            // Server is not running or unreachable -> proceed with offline mock fallback
            String path = request.url().encodedPath();
            String jsonResponse = getMockResponseFor(request.method(), path);

            if (jsonResponse != null) {
                return new Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK (Offline Dummy Fallback)")
                        .body(ResponseBody.create(JSON_MEDIA_TYPE, jsonResponse))
                        .build();
            }

            throw e;
        }
    }

    private String getMockResponseFor(String method, String path) {
        if (path.endsWith("/categories")) {
            return "{\"data\":["
                    + "{\"id\":1,\"name\":\"Footwear\",\"sortOrder\":1,\"status\":\"active\"},"
                    + "{\"id\":2,\"name\":\"Apparel\",\"sortOrder\":2,\"status\":\"active\"},"
                    + "{\"id\":3,\"name\":\"Accessories\",\"sortOrder\":3,\"status\":\"active\"}"
                    + "]}";
        }

        if (path.endsWith("/products")) {
            return "{\"data\":["
                    + "{\"id\":101,\"name\":\"Classic Leather Sneaker\",\"slug\":\"classic-leather-sneaker\",\"price\":\"89.99\",\"primaryImageUrl\":\"https://images.unsplash.com/photo-1549298916-b41d501d3772?w=500\",\"status\":\"active\",\"categoryName\":\"Footwear\"},"
                    + "{\"id\":102,\"name\":\"Canvas Low-Top\",\"slug\":\"canvas-low-top\",\"price\":\"49.50\",\"primaryImageUrl\":\"https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?w=500\",\"status\":\"active\",\"categoryName\":\"Footwear\"},"
                    + "{\"id\":103,\"name\":\"Urban Tech Hoodie\",\"slug\":\"urban-tech-hoodie\",\"price\":\"75.00\",\"primaryImageUrl\":\"https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=500\",\"status\":\"active\",\"categoryName\":\"Apparel\"},"
                    + "{\"id\":104,\"name\":\"Minimalist Daypack\",\"slug\":\"minimalist-daypack\",\"price\":\"65.00\",\"primaryImageUrl\":\"https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=500\",\"status\":\"active\",\"categoryName\":\"Accessories\"}"
                    + "],\"pagination\":{\"currentPage\":1,\"perPage\":20,\"totalItems\":4,\"totalPages\":1}}";
        }

        if (path.contains("/products/")) {
            return "{\"data\":{"
                    + "\"id\":101,\"name\":\"Classic Leather Sneaker\",\"slug\":\"classic-leather-sneaker\","
                    + "\"description\":\"Premium full-grain leather sneaker with cushioned insole and durable rubber outsole for maximum all-day comfort.\","
                    + "\"price\":\"89.99\",\"status\":\"active\",\"categoryName\":\"Footwear\","
                    + "\"images\":[{\"id\":1,\"url\":\"https://images.unsplash.com/photo-1549298916-b41d501d3772?w=500\",\"sortOrder\":1}],"
                    + "\"inventory\":{\"available\":15,\"inStock\":true}}}";
        }

        if (path.contains("/cart")) {
            return "{\"data\":{"
                    + "\"items\":[{"
                    + "\"id\":1,\"productId\":101,\"productName\":\"Classic Leather Sneaker\","
                    + "\"unitPrice\":\"89.99\",\"quantity\":1,\"lineTotal\":\"89.99\","
                    + "\"imageUrl\":\"https://images.unsplash.com/photo-1549298916-b41d501d3772?w=500\"}],"
                    + "\"itemCount\":1,\"subtotal\":\"89.99\",\"shipping\":\"0.00\",\"tax\":\"0.00\",\"total\":\"89.99\"}}";
        }

        if (path.endsWith("/checkout/quote")) {
            return "{\"data\":{\"subtotal\":\"89.99\",\"shipping\":\"5.00\",\"tax\":\"7.20\",\"total\":\"102.19\",\"currency\":\"USD\"}}";
        }

        if (path.contains("/payment")) {
            return "{\"data\":{\"status\":\"COMPLETED\",\"transactionId\":\"TXN-DEMO-9988\",\"paymentMethod\":\"SIMULATED\",\"amount\":\"102.19\"}}";
        }

        if (path.endsWith("/orders")) {
            if ("POST".equalsIgnoreCase(method)) {
                return "{\"data\":{"
                        + "\"id\":1,\"reference\":\"ORD-2026-0001\",\"status\":\"CONFIRMED\",\"paymentStatus\":\"PENDING\","
                        + "\"shippingAddress\":{\"recipientName\":\"Demo Customer\",\"street\":\"123 Main St\",\"city\":\"New York\",\"region\":\"NY\",\"postalCode\":\"10001\",\"country\":\"US\"},"
                        + "\"items\":[{\"id\":1,\"productId\":101,\"productName\":\"Classic Leather Sneaker\",\"unitPrice\":\"89.99\",\"quantity\":1,\"lineTotal\":\"89.99\"}],"
                        + "\"subtotal\":\"89.99\",\"shipping\":\"5.00\",\"tax\":\"7.20\",\"total\":\"102.19\","
                        + "\"timeline\":[{\"status\":\"CONFIRMED\",\"at\":\"2026-10-12 18:00\"}]}}";
            }
            return "{\"data\":[{"
                    + "\"id\":1,\"reference\":\"ORD-2026-0001\",\"status\":\"CONFIRMED\",\"paymentStatus\":\"COMPLETED\","
                    + "\"total\":\"89.99\",\"currency\":\"USD\",\"itemCount\":1,\"createdAt\":\"2026-10-12T18:00:00Z\"}],"
                    + "\"pagination\":{\"currentPage\":1,\"perPage\":20,\"totalItems\":1,\"totalPages\":1}}";
        }

        if (path.contains("/orders/")) {
            return "{\"data\":{"
                    + "\"id\":1,\"reference\":\"ORD-2026-0001\",\"status\":\"CONFIRMED\",\"paymentStatus\":\"COMPLETED\","
                    + "\"shippingAddress\":{\"recipientName\":\"Demo Customer\",\"street\":\"123 Main St\",\"city\":\"New York\",\"region\":\"NY\",\"postalCode\":\"10001\",\"country\":\"US\"},"
                    + "\"items\":[{\"id\":1,\"productId\":101,\"productName\":\"Classic Leather Sneaker\",\"unitPrice\":\"89.99\",\"quantity\":1,\"lineTotal\":\"89.99\"}],"
                    + "\"subtotal\":\"89.99\",\"shipping\":\"0.00\",\"tax\":\"0.00\",\"total\":\"89.99\","
                    + "\"timeline\":[{\"status\":\"CONFIRMED\",\"at\":\"2026-10-12 18:00\"}]}}";
        }

        if (path.endsWith("/auth/profile") || path.endsWith("/auth/me")) {
            return "{\"data\":{\"id\":1,\"email\":\"demo@eshopverse.example.com\",\"firstName\":\"Demo\",\"lastName\":\"Customer\",\"role\":\"CUSTOMER\"}}";
        }

        if (path.endsWith("/auth/login") || path.endsWith("/auth/register")) {
            return "{\"data\":{\"token\":\"demo-offline-token-sample\",\"user\":{\"id\":1,\"email\":\"demo@eshopverse.example.com\",\"firstName\":\"Demo\",\"lastName\":\"Customer\",\"role\":\"CUSTOMER\"}}}";
        }

        return null;
    }
}
