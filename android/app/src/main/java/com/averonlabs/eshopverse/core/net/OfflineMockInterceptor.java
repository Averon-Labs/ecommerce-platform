package com.averonlabs.eshopverse.core.net;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Provides comprehensive offline fallback dummy data when the backend or local mock server is not reachable.
 * Active in debug builds to guarantee a rich, interactive, realistic demo out-of-the-box.
 */
public class OfflineMockInterceptor implements Interceptor {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        try {
            // Attempt fast connection check (1s) so the app doesn't hang if no local server is active
            return chain.withConnectTimeout(1, TimeUnit.SECONDS).proceed(request);
        } catch (IOException e) {
            // Server is not running or unreachable -> deliver rich offline mock data
            String jsonResponse = getMockResponseFor(request.method(), request.url());

            if (jsonResponse != null) {
                return new Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK (EShop-Verse Offline Mock Data)")
                        .body(ResponseBody.create(JSON_MEDIA_TYPE, jsonResponse))
                        .build();
            }

            throw e;
        }
    }

    private String getMockResponseFor(String method, HttpUrl url) {
        String path = url.encodedPath();

        if (path.endsWith("/categories")) {
            return "{\"data\":["
                    + "{\"id\":1,\"name\":\"Footwear\",\"sortOrder\":1,\"status\":\"active\"},"
                    + "{\"id\":2,\"name\":\"Apparel\",\"sortOrder\":2,\"status\":\"active\"},"
                    + "{\"id\":3,\"name\":\"Accessories\",\"sortOrder\":3,\"status\":\"active\"},"
                    + "{\"id\":4,\"name\":\"Electronics\",\"sortOrder\":4,\"status\":\"active\"}"
                    + "]}";
        }

        if (path.endsWith("/products")) {
            String categoryId = url.queryParameter("category_id");
            if ("1".equals(categoryId)) {
                return "{\"data\":["
                        + "{\"id\":101,\"name\":\"Classic Leather Sneaker\",\"price\":\"89.99\",\"currency\":\"USD\",\"categoryId\":1,\"categoryName\":\"Footwear\",\"imageUrl\":\"https://images.unsplash.com/photo-1549298916-b41d501d3772?w=500\",\"inStock\":true,\"status\":\"active\"},"
                        + "{\"id\":102,\"name\":\"Canvas Low-Top\",\"price\":\"49.50\",\"currency\":\"USD\",\"categoryId\":1,\"categoryName\":\"Footwear\",\"imageUrl\":\"https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?w=500\",\"inStock\":true,\"status\":\"active\"}"
                        + "],\"pagination\":{\"currentPage\":1,\"perPage\":20,\"totalItems\":2,\"totalPages\":1}}";
            }
            if ("2".equals(categoryId)) {
                return "{\"data\":["
                        + "{\"id\":103,\"name\":\"Urban Tech Hoodie\",\"price\":\"75.00\",\"currency\":\"USD\",\"categoryId\":2,\"categoryName\":\"Apparel\",\"imageUrl\":\"https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=500\",\"inStock\":true,\"status\":\"active\"}"
                        + "],\"pagination\":{\"currentPage\":1,\"perPage\":20,\"totalItems\":1,\"totalPages\":1}}";
            }
            if ("3".equals(categoryId)) {
                return "{\"data\":["
                        + "{\"id\":104,\"name\":\"Minimalist Daypack\",\"price\":\"65.00\",\"currency\":\"USD\",\"categoryId\":3,\"categoryName\":\"Accessories\",\"imageUrl\":\"https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=500\",\"inStock\":true,\"status\":\"active\"}"
                        + "],\"pagination\":{\"currentPage\":1,\"perPage\":20,\"totalItems\":1,\"totalPages\":1}}";
            }
            if ("4".equals(categoryId)) {
                return "{\"data\":["
                        + "{\"id\":105,\"name\":\"Wireless Noise-Cancelling Headphones\",\"price\":\"199.99\",\"currency\":\"USD\",\"categoryId\":4,\"categoryName\":\"Electronics\",\"imageUrl\":\"https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500\",\"inStock\":true,\"status\":\"active\"},"
                        + "{\"id\":106,\"name\":\"Smart Fitness Watch\",\"price\":\"149.00\",\"currency\":\"USD\",\"categoryId\":4,\"categoryName\":\"Electronics\",\"imageUrl\":\"https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500\",\"inStock\":true,\"status\":\"active\"}"
                        + "],\"pagination\":{\"currentPage\":1,\"perPage\":20,\"totalItems\":2,\"totalPages\":1}}";
            }

            return "{\"data\":["
                    + "{\"id\":101,\"name\":\"Classic Leather Sneaker\",\"price\":\"89.99\",\"currency\":\"USD\",\"categoryId\":1,\"categoryName\":\"Footwear\",\"imageUrl\":\"https://images.unsplash.com/photo-1549298916-b41d501d3772?w=500\",\"inStock\":true,\"status\":\"active\"},"
                    + "{\"id\":102,\"name\":\"Canvas Low-Top\",\"price\":\"49.50\",\"currency\":\"USD\",\"categoryId\":1,\"categoryName\":\"Footwear\",\"imageUrl\":\"https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?w=500\",\"inStock\":true,\"status\":\"active\"},"
                    + "{\"id\":103,\"name\":\"Urban Tech Hoodie\",\"price\":\"75.00\",\"currency\":\"USD\",\"categoryId\":2,\"categoryName\":\"Apparel\",\"imageUrl\":\"https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=500\",\"inStock\":true,\"status\":\"active\"},"
                    + "{\"id\":104,\"name\":\"Minimalist Daypack\",\"price\":\"65.00\",\"currency\":\"USD\",\"categoryId\":3,\"categoryName\":\"Accessories\",\"imageUrl\":\"https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=500\",\"inStock\":true,\"status\":\"active\"},"
                    + "{\"id\":105,\"name\":\"Wireless Noise-Cancelling Headphones\",\"price\":\"199.99\",\"currency\":\"USD\",\"categoryId\":4,\"categoryName\":\"Electronics\",\"imageUrl\":\"https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500\",\"inStock\":true,\"status\":\"active\"},"
                    + "{\"id\":106,\"name\":\"Smart Fitness Watch\",\"price\":\"149.00\",\"currency\":\"USD\",\"categoryId\":4,\"categoryName\":\"Electronics\",\"imageUrl\":\"https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500\",\"inStock\":true,\"status\":\"active\"}"
                    + "],\"pagination\":{\"currentPage\":1,\"perPage\":20,\"totalItems\":6,\"totalPages\":1}}";
        }

        if (path.contains("/products/")) {
            long id = 101;
            try {
                String idStr = path.substring(path.lastIndexOf('/') + 1);
                id = Long.parseLong(idStr);
            } catch (Exception ignored) {}

            String name = "Classic Leather Sneaker";
            String price = "89.99";
            String image = "https://images.unsplash.com/photo-1549298916-b41d501d3772?w=500";
            int catId = 1;
            String catName = "Footwear";
            String sku = "FTW-SNK-001";

            if (id == 102) {
                name = "Canvas Low-Top";
                price = "49.50";
                image = "https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?w=500";
                sku = "FTW-CNV-002";
            } else if (id == 103) {
                name = "Urban Tech Hoodie";
                price = "75.00";
                image = "https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=500";
                catId = 2;
                catName = "Apparel";
                sku = "APP-HOD-003";
            } else if (id == 104) {
                name = "Minimalist Daypack";
                price = "65.00";
                image = "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=500";
                catId = 3;
                catName = "Accessories";
                sku = "ACC-BCK-004";
            } else if (id == 105) {
                name = "Wireless Noise-Cancelling Headphones";
                price = "199.99";
                image = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500";
                catId = 4;
                catName = "Electronics";
                sku = "ELC-HDP-005";
            } else if (id == 106) {
                name = "Smart Fitness Watch";
                price = "149.00";
                image = "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500";
                catId = 4;
                catName = "Electronics";
                sku = "ELC-WTC-006";
            }

            return "{\"data\":{"
                    + "\"id\":" + id + ",\"name\":\"" + name + "\",\"price\":\"" + price + "\",\"currency\":\"USD\","
                    + "\"categoryId\":" + catId + ",\"categoryName\":\"" + catName + "\",\"imageUrl\":\"" + image + "\","
                    + "\"inStock\":true,\"status\":\"active\","
                    + "\"description\":\"Handcrafted with meticulous detail using premium materials. Designed for high durability, effortless style, and maximum daily utility.\","
                    + "\"sku\":\"" + sku + "\",\"stockQuantity\":15,"
                    + "\"images\":["
                    + "{\"url\":\"" + image + "\",\"sortOrder\":1}"
                    + "]}}";
        }

        if (path.contains("/cart")) {
            return "{\"data\":{"
                    + "\"items\":[{"
                    + "\"productId\":101,\"name\":\"Classic Leather Sneaker\",\"imageUrl\":\"https://images.unsplash.com/photo-1549298916-b41d501d3772?w=500\","
                    + "\"unitPrice\":\"89.99\",\"currency\":\"USD\",\"quantity\":1,\"lineTotal\":\"89.99\","
                    + "\"inStock\":true,\"availableQuantity\":15,\"priceChanged\":false,\"unavailable\":false"
                    + "}],"
                    + "\"itemCount\":1,\"subtotal\":\"89.99\",\"shipping\":\"5.00\",\"total\":\"94.99\",\"currency\":\"USD\",\"hasIssues\":false}}";
        }

        if (path.endsWith("/checkout/quote")) {
            return "{\"data\":{"
                    + "\"items\":[{"
                    + "\"productId\":101,\"name\":\"Classic Leather Sneaker\",\"imageUrl\":\"https://images.unsplash.com/photo-1549298916-b41d501d3772?w=500\","
                    + "\"unitPrice\":\"89.99\",\"currency\":\"USD\",\"quantity\":1,\"lineTotal\":\"89.99\","
                    + "\"inStock\":true,\"availableQuantity\":15,\"priceChanged\":false,\"unavailable\":false"
                    + "}],"
                    + "\"subtotal\":\"89.99\",\"shipping\":\"5.00\",\"total\":\"94.99\",\"currency\":\"USD\","
                    + "\"shippingAddress\":{\"recipientName\":\"Alex Morgan\",\"line1\":\"123 Main St\",\"city\":\"New York\",\"region\":\"NY\",\"postalCode\":\"10001\",\"country\":\"US\"},"
                    + "\"shippingAddressId\":\"addr-demo-001\"}}";
        }

        if (path.contains("/payment")) {
            return "{\"data\":{"
                    + "\"orderId\":1,\"orderReference\":\"ORD-2026-0001\",\"orderStatus\":\"confirmed\",\"paymentStatus\":\"completed\","
                    + "\"simulated\":true,\"payment\":null}}";
        }

        if (path.endsWith("/orders")) {
            if ("POST".equalsIgnoreCase(method)) {
                return "{\"data\":{"
                        + "\"id\":1,\"reference\":\"ORD-2026-0001\",\"status\":\"confirmed\",\"paymentStatus\":\"completed\","
                        + "\"subtotal\":\"89.99\",\"shipping\":\"5.00\",\"total\":\"94.99\",\"currency\":\"USD\","
                        + "\"createdAt\":\"2026-10-12T18:00:00Z\",\"updatedAt\":\"2026-10-12T18:05:00Z\",\"notes\":\"Express Delivery\","
                        + "\"shippingAddress\":{\"recipientName\":\"Alex Morgan\",\"line1\":\"123 Main St\",\"city\":\"New York\",\"region\":\"NY\",\"postalCode\":\"10001\",\"country\":\"US\"},"
                        + "\"items\":[{\"productId\":101,\"productName\":\"Classic Leather Sneaker\",\"quantity\":1,\"unitPrice\":\"89.99\",\"lineTotal\":\"89.99\"}],"
                        + "\"timeline\":[{\"status\":\"confirmed\",\"at\":\"2026-10-12T18:00:00Z\"}]}}";
            }
            return "{\"data\":["
                    + "{\"id\":1,\"reference\":\"ORD-2026-0001\",\"status\":\"confirmed\",\"paymentStatus\":\"completed\",\"total\":\"94.99\",\"currency\":\"USD\",\"itemCount\":1,\"createdAt\":\"2026-10-12T18:00:00Z\"},"
                    + "{\"id\":2,\"reference\":\"ORD-2026-0002\",\"status\":\"shipped\",\"paymentStatus\":\"completed\",\"total\":\"75.00\",\"currency\":\"USD\",\"itemCount\":1,\"createdAt\":\"2026-10-11T14:30:00Z\"}"
                    + "],\"pagination\":{\"currentPage\":1,\"perPage\":20,\"totalItems\":2,\"totalPages\":1}}";
        }

        if (path.contains("/orders/")) {
            return "{\"data\":{"
                    + "\"id\":1,\"reference\":\"ORD-2026-0001\",\"status\":\"confirmed\",\"paymentStatus\":\"completed\","
                    + "\"subtotal\":\"89.99\",\"shipping\":\"5.00\",\"total\":\"94.99\",\"currency\":\"USD\","
                    + "\"createdAt\":\"2026-10-12T18:00:00Z\",\"updatedAt\":\"2026-10-12T18:05:00Z\",\"notes\":\"Express Delivery\","
                    + "\"shippingAddress\":{\"recipientName\":\"Alex Morgan\",\"line1\":\"123 Main St\",\"city\":\"New York\",\"region\":\"NY\",\"postalCode\":\"10001\",\"country\":\"US\"},"
                    + "\"items\":[{\"productId\":101,\"productName\":\"Classic Leather Sneaker\",\"quantity\":1,\"unitPrice\":\"89.99\",\"lineTotal\":\"89.99\"}],"
                    + "\"timeline\":[{\"status\":\"confirmed\",\"at\":\"2026-10-12T18:00:00Z\"}]}}";
        }

        if (path.endsWith("/auth/profile") || path.endsWith("/auth/me")) {
            return "{\"data\":{\"id\":1,\"email\":\"alex.morgan@eshopverse.com\",\"firstName\":\"Alex\",\"lastName\":\"Morgan\",\"role\":\"customer\",\"status\":\"active\",\"createdAt\":\"2026-01-15T10:00:00Z\"}}";
        }

        if (path.endsWith("/auth/login") || path.endsWith("/auth/register")) {
            return "{\"data\":{\"token\":\"demo-jwt-offline-token-sample\",\"user\":{\"id\":1,\"email\":\"alex.morgan@eshopverse.com\",\"firstName\":\"Alex\",\"lastName\":\"Morgan\",\"role\":\"customer\",\"status\":\"active\",\"createdAt\":\"2026-01-15T10:00:00Z\"}}}";
        }

        return null;
    }
}
