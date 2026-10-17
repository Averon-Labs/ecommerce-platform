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
            String categoryIdParam = url.queryParameter("category_id");
            StringBuilder itemsJson = new StringBuilder();
            int count = 0;
            for (MockProduct p : ALL_MOCK_PRODUCTS) {
                if (categoryIdParam != null && !categoryIdParam.isEmpty()) {
                    if (p.categoryId != Integer.parseInt(categoryIdParam)) {
                        continue;
                    }
                }
                if (count > 0) {
                    itemsJson.append(",");
                }
                itemsJson.append(p.toSummaryJson());
                count++;
            }

            return "{\"data\":[" + itemsJson.toString() + "],"
                    + "\"pagination\":{\"currentPage\":1,\"perPage\":20,\"totalItems\":" + count + ",\"totalPages\":1}}";
        }

        if (path.contains("/products/")) {
            long id = 101;
            try {
                String idStr = path.substring(path.lastIndexOf('/') + 1);
                id = Long.parseLong(idStr);
            } catch (Exception ignored) {}

            MockProduct match = ALL_MOCK_PRODUCTS[0];
            for (MockProduct p : ALL_MOCK_PRODUCTS) {
                if (p.id == id) {
                    match = p;
                    break;
                }
            }

            return "{\"data\":" + match.toDetailJson() + "}";
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

    private static class MockProduct {
        final long id;
        final String name;
        final String price;
        final int categoryId;
        final String categoryName;
        final String sku;
        final String imageUrl;
        final String description;
        final int stockQuantity;

        MockProduct(long id, String name, String price, int categoryId, String categoryName, String sku, String imageUrl, String description, int stockQuantity) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.sku = sku;
            this.imageUrl = imageUrl;
            this.description = description;
            this.stockQuantity = stockQuantity;
        }

        String toSummaryJson() {
            return "{\"id\":" + id + ",\"name\":\"" + name + "\",\"price\":\"" + price + "\",\"currency\":\"USD\","
                    + "\"categoryId\":" + categoryId + ",\"categoryName\":\"" + categoryName + "\",\"imageUrl\":\"" + imageUrl + "\","
                    + "\"inStock\":true,\"status\":\"active\"}";
        }

        String toDetailJson() {
            return "{\"id\":" + id + ",\"name\":\"" + name + "\",\"price\":\"" + price + "\",\"currency\":\"USD\","
                    + "\"categoryId\":" + categoryId + ",\"categoryName\":\"" + categoryName + "\",\"imageUrl\":\"" + imageUrl + "\","
                    + "\"inStock\":true,\"status\":\"active\","
                    + "\"description\":\"" + description + "\","
                    + "\"sku\":\"" + sku + "\",\"stockQuantity\":" + stockQuantity + ","
                    + "\"images\":[{\"url\":\"" + imageUrl + "\",\"sortOrder\":1}]}";
        }
    }

    private static final MockProduct[] ALL_MOCK_PRODUCTS = new MockProduct[] {
            // Footwear (Category 1)
            new MockProduct(
                    101, "Classic Leather Sneaker", "89.99", 1, "Footwear", "FTW-SNK-001",
                    "https://images.unsplash.com/photo-1549298916-b41d501d3772?w=800&q=80",
                    "Handcrafted with meticulous detail using premium supple leather. Designed for high durability, effortless street style, and all-day walking comfort.",
                    18
            ),
            new MockProduct(
                    102, "Canvas Low-Top", "49.50", 1, "Footwear", "FTW-CNV-002",
                    "https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?w=800&q=80",
                    "Lightweight breathable vulcanized canvas sneakers with reinforced rubber toe caps and flexible waffle outsoles.",
                    25
            ),
            new MockProduct(
                    107, "Chelsea Rugged Boot", "129.00", 1, "Footwear", "FTW-BOT-007",
                    "https://images.unsplash.com/photo-1608256246200-53e635b5b65f?w=800&q=80",
                    "Premium oiled full-grain leather Chelsea boots with heavy-duty elastic side gussets, Goodyear welt, and lugged all-terrain soles.",
                    12
            ),
            new MockProduct(
                    108, "Aero Velocity Running Shoe", "115.00", 1, "Footwear", "FTW-RUN-008",
                    "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&q=80",
                    "High-performance responsive foam cushioning paired with engineered breathable mesh for marathon training and speed runs.",
                    15
            ),

            // Apparel (Category 2)
            new MockProduct(
                    103, "Urban Tech Hoodie", "75.00", 2, "Apparel", "APP-HOD-003",
                    "https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=800&q=80",
                    "Double-knit technical fleece hoodie with concealed water-resistant zip pockets and tailored athletic silhouette.",
                    20
            ),
            new MockProduct(
                    109, "Vintage Washed Denim Jacket", "89.00", 2, "Apparel", "APP-JKT-009",
                    "https://images.unsplash.com/photo-1576995853123-5a10305d93c0?w=800&q=80",
                    "Classic trucker jacket cut from 100% heavyweight cotton denim with vintage stonewashed patina and antique brass hardware.",
                    14
            ),
            new MockProduct(
                    110, "Organic Cotton Crewneck Tee", "34.00", 2, "Apparel", "APP-TEE-010",
                    "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800&q=80",
                    "Silky-smooth organic combed cotton everyday tee with pre-shrunk rib collar and relaxed drape.",
                    40
            ),
            new MockProduct(
                    111, "Insulated Winter Puffer Jacket", "139.00", 2, "Apparel", "APP-PFJ-011",
                    "https://images.unsplash.com/photo-1544441893-675973e31985?w=800&q=80",
                    "Water-resistant recycled ripstop shell packed with high-loft thermal insulation for freezing temperatures.",
                    10
            ),

            // Accessories (Category 3)
            new MockProduct(
                    104, "Minimalist Daypack", "65.00", 3, "Accessories", "ACC-BCK-004",
                    "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&q=80",
                    "Sleek weather-resistant Cordura commuter backpack featuring a dedicated 16-inch padded laptop sleeve and ergonomic shoulder straps.",
                    22
            ),
            new MockProduct(
                    112, "Polarized Aviator Sunglasses", "58.00", 3, "Accessories", "ACC-SNG-012",
                    "https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=800&q=80",
                    "Timeless lightweight stainless steel wireframe sunglasses with scratch-resistant polarized lenses providing 100% UV400 glare reduction.",
                    30
            ),
            new MockProduct(
                    113, "Full-Grain Leather Wallet", "45.00", 3, "Accessories", "ACC-WLT-013",
                    "https://images.unsplash.com/photo-1627123424574-724758594e93?w=800&q=80",
                    "Handmade slim bifold wallet crafted from vegetable-tanned leather with integrated RFID blocking shield and 8 card slots.",
                    25
            ),
            new MockProduct(
                    114, "Insulated Stainless Steel Tumbler", "29.50", 3, "Accessories", "ACC-BTL-014",
                    "https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=800&q=80",
                    "Double-wall vacuum insulation keeps iced coffee cold for 24 hours or piping hot for 12 hours. Sweat-free textured matte powder coat.",
                    35
            ),

            // Electronics (Category 4)
            new MockProduct(
                    105, "Wireless Noise-Cancelling Headphones", "199.99", 4, "Electronics", "ELC-HDP-005",
                    "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&q=80",
                    "Over-ear studio audio with hybrid active noise cancellation, custom 40mm titanium drivers, and up to 35 hours of lossless battery playback.",
                    16
            ),
            new MockProduct(
                    106, "Smart Fitness Watch", "149.00", 4, "Electronics", "ELC-WTC-006",
                    "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&q=80",
                    "Vibrant AMOLED always-on touchscreen with comprehensive biometric health tracking, dual-band GPS, and 50m water resistance.",
                    19
            ),
            new MockProduct(
                    115, "Mechanical RGB Gaming Keyboard", "129.99", 4, "Electronics", "ELC-KBD-015",
                    "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800&q=80",
                    "Compact tenkeyless mechanical board equipped with hot-swappable linear switches, sound-dampening silicone foam, and per-key RGB backlighting.",
                    11
            ),
            new MockProduct(
                    116, "Rugged Portable Bluetooth Speaker", "69.00", 4, "Electronics", "ELC-SPK-016",
                    "https://images.unsplash.com/photo-1545454675-3531b543be5d?w=800&q=80",
                    "360-degree immersive acoustic profile with dual passive bass radiators, IPX7 waterproof casing, and 16 hours of continuous playtime.",
                    28
            )
    };
}
