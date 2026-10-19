package com.averonlabs.eshopverse.core.net;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * Provides comprehensive offline fallback dummy data when the backend or local mock server is not reachable.
 * Active in debug builds to guarantee a rich, interactive, realistic demo out-of-the-box.
 */
public class OfflineMockInterceptor implements Interceptor {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    public static class MockCartItem {
        public final long productId;
        public final String name;
        public final String imageUrl;
        public final double unitPrice;
        public int quantity;
        public final int availableQuantity;

        public MockCartItem(long productId, String name, String imageUrl, double unitPrice, int quantity, int availableQuantity) {
            this.productId = productId;
            this.name = name;
            this.imageUrl = imageUrl;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
            this.availableQuantity = availableQuantity;
        }

        public String toJson() {
            double lineTotal = unitPrice * quantity;
            String unitPriceStr = String.format(Locale.US, "%.2f", unitPrice);
            String lineTotalStr = String.format(Locale.US, "%.2f", lineTotal);
            return "{\"productId\":" + productId + ",\"name\":\"" + name + "\",\"imageUrl\":\"" + imageUrl + "\","
                    + "\"unitPrice\":\"" + unitPriceStr + "\",\"currency\":\"USD\",\"quantity\":" + quantity + ",\"lineTotal\":\"" + lineTotalStr + "\","
                    + "\"inStock\":true,\"availableQuantity\":" + availableQuantity + ",\"priceChanged\":false,\"unavailable\":false}";
        }
    }

    private static final List<MockCartItem> MOCK_CART_ITEMS = new ArrayList<>(Arrays.asList(
            new MockCartItem(101, "Classic White Oxford Shirt", "file:///android_asset/products/shirt_001.jpg", 45.99, 1, 25)
    ));

    public static synchronized void resetMockCart() {
        MOCK_CART_ITEMS.clear();
        MOCK_CART_ITEMS.add(new MockCartItem(101, "Classic White Oxford Shirt", "file:///android_asset/products/shirt_001.jpg", 45.99, 1, 25));
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        try {
            // Attempt fast connection check (1s) so the app doesn't hang if no local server is active
            return chain.withConnectTimeout(1, TimeUnit.SECONDS).proceed(request);
        } catch (IOException e) {
            // Server is not running or unreachable -> deliver rich offline mock data
            String jsonResponse = getMockResponseFor(request);

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

    public String getMockResponseFor(Request request) {
        String method = request.method();
        HttpUrl url = request.url();
        String path = url.encodedPath();

        if (path.contains("/cart")) {
            return handleCartRequest(method, path, request);
        }

        return getMockResponseFor(method, url);
    }

    private String getMockResponseFor(String method, HttpUrl url) {
        String path = url.encodedPath();

        if (path.endsWith("/categories")) {
            return "{\"data\":["
                    + "{\"id\":1,\"name\":\"Shirts & Tops\",\"sortOrder\":1,\"status\":\"active\"},"
                    + "{\"id\":2,\"name\":\"Pants & Jeans\",\"sortOrder\":2,\"status\":\"active\"},"
                    + "{\"id\":3,\"name\":\"Dresses\",\"sortOrder\":3,\"status\":\"active\"},"
                    + "{\"id\":4,\"name\":\"Jackets & Coats\",\"sortOrder\":4,\"status\":\"active\"},"
                    + "{\"id\":5,\"name\":\"T-Shirts\",\"sortOrder\":5,\"status\":\"active\"},"
                    + "{\"id\":6,\"name\":\"Activewear\",\"sortOrder\":6,\"status\":\"active\"},"
                    + "{\"id\":7,\"name\":\"Shoes\",\"sortOrder\":7,\"status\":\"active\"},"
                    + "{\"id\":8,\"name\":\"Accessories\",\"sortOrder\":8,\"status\":\"active\"},"
                    + "{\"id\":9,\"name\":\"Leather & Suede\",\"sortOrder\":9,\"status\":\"active\"},"
                    + "{\"id\":10,\"name\":\"Bags & Backpacks\",\"sortOrder\":10,\"status\":\"active\"}"
                    + "]}";
        }

        if (path.endsWith("/products")) {
            String categoryIdParam = url.queryParameter("categoryId");
            if (categoryIdParam == null) {
                categoryIdParam = url.queryParameter("category_id");
            }
            String queryParam = url.queryParameter("q");
            if (queryParam == null) {
                queryParam = url.queryParameter("search");
            }
            String pageParam = url.queryParameter("page");
            String perPageParam = url.queryParameter("perPage");
            if (perPageParam == null) {
                perPageParam = url.queryParameter("per_page");
            }
            int page = 1;
            int perPage = 60; // Default when unpaginated to cover full set for tests
            if (pageParam != null) {
                try {
                    page = Math.max(1, Integer.parseInt(pageParam));
                    perPage = 20; // Default to 20 when pagination is active
                } catch (Exception ignored) {}
            }
            if (perPageParam != null) {
                try { perPage = Math.max(1, Integer.parseInt(perPageParam)); } catch (Exception ignored) {}
            }

            List<MockProduct> filtered = new ArrayList<>();
            for (MockProduct p : ALL_MOCK_PRODUCTS) {
                if (categoryIdParam != null && !categoryIdParam.isEmpty()) {
                    try {
                        if (p.categoryId != Integer.parseInt(categoryIdParam)) {
                            continue;
                        }
                    } catch (Exception ignored) {}
                }
                if (queryParam != null && !queryParam.trim().isEmpty()) {
                    String qLower = queryParam.trim().toLowerCase();
                    if (!p.name.toLowerCase().contains(qLower)
                            && !p.description.toLowerCase().contains(qLower)
                            && !p.categoryName.toLowerCase().contains(qLower)) {
                        continue;
                    }
                }
                filtered.add(p);
            }

            int totalItems = filtered.size();
            int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / perPage));
            int startIndex = (page - 1) * perPage;
            int endIndex = Math.min(startIndex + perPage, totalItems);

            StringBuilder itemsJson = new StringBuilder();
            if (startIndex < totalItems) {
                for (int i = startIndex; i < endIndex; i++) {
                    if (itemsJson.length() > 0) {
                        itemsJson.append(",");
                    }
                    itemsJson.append(filtered.get(i).toSummaryJson());
                }
            }

            return "{\"data\":[" + itemsJson.toString() + "],"
                    + "\"pagination\":{\"currentPage\":" + page + ",\"perPage\":" + perPage + ",\"totalItems\":" + totalItems + ",\"totalPages\":" + totalPages + "}}";
        }

        if (path.contains("/products/")) {
            String idStr = path.substring(path.lastIndexOf('/') + 1);
            MockProduct match = ALL_MOCK_PRODUCTS[0];
            for (MockProduct p : ALL_MOCK_PRODUCTS) {
                if (String.valueOf(p.id).equals(idStr) || p.sku.equalsIgnoreCase(idStr)) {
                    match = p;
                    break;
                }
            }

            return "{\"data\":" + match.toDetailJson() + "}";
        }

        if (path.contains("/cart")) {
            return handleCartRequest(method, path, null);
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

        if (path.endsWith("/auth/login") || path.endsWith("/auth/register") || path.endsWith("/auth/google")) {
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
            // Shirts & Tops (Category 1)
            new MockProduct(101, "Classic White Oxford Shirt", "45.99", 1, "Shirts & Tops", "shirt_001",
                    "file:///android_asset/products/shirt_001.jpg",
                    "Tailored from crisp, breathable 100% organic cotton oxford fabric. Features a neat button-down collar and curved hem.", 25),
            new MockProduct(102, "Silk Navy Blouse", "68.50", 1, "Shirts & Tops", "shirt_002",
                    "file:///android_asset/products/shirt_002.jpg",
                    "Luxurious mulberry silk blouse with an elegant fluid drape, hidden front placket, and mother-of-pearl buttons.", 18),
            new MockProduct(103, "Striped Casual Shirt", "39.99", 1, "Shirts & Tops", "shirt_003",
                    "file:///android_asset/products/shirt_003.jpg",
                    "Relaxed-fit yarn-dyed stripe shirt cut from lightweight washed cotton for effortless weekend styling.", 30),
            new MockProduct(104, "Oversized Cream Linen Top", "52.00", 1, "Shirts & Tops", "shirt_004",
                    "file:///android_asset/products/shirt_004.jpg",
                    "Airy pure European linen shirt designed with a dropped shoulder silhouette and breathable natural texture.", 22),
            new MockProduct(105, "Burgundy Velvet Blouse", "72.99", 1, "Shirts & Tops", "shirt_005",
                    "file:///android_asset/products/shirt_005.jpg",
                    "Sumptuous plush velvet in rich burgundy with gathered cuff detailing and a subtle keyhole neckline.", 15),

            // Pants & Jeans (Category 2)
            new MockProduct(106, "Classic Blue Denim Jeans", "59.99", 2, "Pants & Jeans", "pants_001",
                    "file:///android_asset/products/pants_001.jpg",
                    "Timeless straight-leg five-pocket jeans crafted from authentic 12.5oz ring-spun cotton denim with vintage rinse.", 35),
            new MockProduct(107, "Black Skinny Jeans", "49.50", 2, "Pants & Jeans", "pants_002",
                    "file:///android_asset/products/pants_002.jpg",
                    "Form-flattering high-rise skinny jeans with high-recovery stretch denim that holds shape all day.", 28),
            new MockProduct(108, "Beige Linen Trousers", "65.00", 2, "Pants & Jeans", "pants_003",
                    "file:///android_asset/products/pants_003.jpg",
                    "Relaxed wide-leg trousers tailored in premium breathable beige linen with pleat front and elasticized back waist.", 20),
            new MockProduct(109, "Dark Navy Chinos", "54.99", 2, "Pants & Jeans", "pants_004",
                    "file:///android_asset/products/pants_004.jpg",
                    "Versatile smart-casual slim-fit chinos crafted from stretch cotton twill with welted back pockets.", 32),
            new MockProduct(110, "White Wide-Leg Pants", "62.50", 2, "Pants & Jeans", "pants_005",
                    "file:///android_asset/products/pants_005.jpg",
                    "Sophisticated high-waisted fluid trousers featuring clean front creases and generous wide hems.", 19),

            // Dresses (Category 3)
            new MockProduct(111, "Black Midi Wrap Dress", "79.99", 3, "Dresses", "dress_001",
                    "file:///android_asset/products/dress_001.jpg",
                    "Classic flattering wrap silhouette in premium matte jersey with adjustable tie waist and flutter hem.", 24),
            new MockProduct(112, "Floral Summer Dress", "54.00", 3, "Dresses", "dress_002",
                    "file:///android_asset/products/dress_002.jpg",
                    "Breezy lightweight tiered sundress decorated with delicate botanical watercolor florals and sweetheart neckline.", 30),
            new MockProduct(113, "Navy Blue Shift Dress", "68.50", 3, "Dresses", "dress_003",
                    "file:///android_asset/products/dress_003.jpg",
                    "Understated minimalist shift dress with clean architectural seams and jewel neckline in smooth crepe.", 20),
            new MockProduct(114, "Emerald Green Satin Dress", "89.99", 3, "Dresses", "dress_004",
                    "file:///android_asset/products/dress_004.jpg",
                    "Sensational emerald satin slip dress featuring cowl neckline, bias cut for graceful drape, and side slit.", 14),
            new MockProduct(115, "Striped Casual Shirt Dress", "59.50", 3, "Dresses", "dress_005",
                    "file:///android_asset/products/dress_005.jpg",
                    "Crisp cotton poplin shirt dress featuring classic pinstripes, fabric sash belt, and chest patch pocket.", 25),

            // Jackets & Coats (Category 4)
            new MockProduct(116, "Classic Black Leather Jacket", "145.00", 4, "Jackets & Coats", "jacket_001",
                    "file:///android_asset/products/jacket_001.jpg",
                    "Iconic asymmetrical biker jacket handcrafted from supple full-grain lambskin with heavy brushed silver hardware.", 12),
            new MockProduct(117, "Camel Wool Coat", "125.50", 4, "Jackets & Coats", "jacket_002",
                    "file:///android_asset/products/jacket_002.jpg",
                    "Tailored double-breasted longline coat crafted from a warm wool-blend in sophisticated camel tan.", 16),
            new MockProduct(118, "Denim Jacket Blue", "69.99", 4, "Jackets & Coats", "jacket_003",
                    "file:///android_asset/products/jacket_003.jpg",
                    "Heritage trucker denim jacket with vintage wash fading, button flap pockets, and adjustable waist tabs.", 26),
            new MockProduct(119, "Quilted Puffer Jacket", "99.00", 4, "Jackets & Coats", "jacket_004",
                    "file:///android_asset/products/jacket_004.jpg",
                    "Ultralight water-repellent ripstop shell insulated with high-loft thermal down alternative for cold weather.", 20),
            new MockProduct(120, "Suede Biker Jacket", "135.99", 4, "Jackets & Coats", "jacket_005",
                    "file:///android_asset/products/jacket_005.jpg",
                    "Sumptuous cognac genuine suede leather jacket with moto collar, zipper cuffs, and satin lining.", 14),

            // T-Shirts (Category 5)
            new MockProduct(121, "Classic White T-Shirt", "24.99", 5, "T-Shirts", "tshirt_001",
                    "file:///android_asset/products/tshirt_001.jpg",
                    "The essential everyday crewneck t-shirt made of 100% combed ringspun cotton with smooth handfeel.", 50),
            new MockProduct(122, "Graphic Print T-Shirt", "34.50", 5, "T-Shirts", "tshirt_002",
                    "file:///android_asset/products/tshirt_002.jpg",
                    "Contemporary artistic screen-printed graphic tee with relaxed streetwear fit and vintage enzyme wash.", 35),
            new MockProduct(123, "Soft Gray Melange Tee", "28.99", 5, "T-Shirts", "tshirt_003",
                    "file:///android_asset/products/tshirt_003.jpg",
                    "Heathered gray tri-blend tee offering incredible softness, shape retention, and breathable drape.", 40),
            new MockProduct(124, "Navy Striped T-Shirt", "31.00", 5, "T-Shirts", "tshirt_004",
                    "file:///android_asset/products/tshirt_004.jpg",
                    "French Breton-inspired nautical stripe crewneck knit from premium compact jersey cotton.", 30),
            new MockProduct(125, "Oversized Vintage Tee", "38.50", 5, "T-Shirts", "tshirt_005",
                    "file:///android_asset/products/tshirt_005.jpg",
                    "Heavyweight 220gsm vintage-washed boxy t-shirt with dropped shoulders and thick ribbed collar.", 28),

            // Activewear (Category 6)
            new MockProduct(126, "Black Yoga Leggings", "64.99", 6, "Activewear", "active_001",
                    "file:///android_asset/products/active_001.jpg",
                    "High-waisted buttery-soft moisture-wicking compression leggings with 4-way stretch and hidden pocket.", 45),
            new MockProduct(127, "Sports Performance Bra", "52.00", 6, "Activewear", "active_002",
                    "file:///android_asset/products/active_002.jpg",
                    "Medium-impact racerback sports bra with removable padding and breathable mesh zone ventilation.", 38),
            new MockProduct(128, "Gray Joggers", "54.50", 6, "Activewear", "active_003",
                    "file:///android_asset/products/active_003.jpg",
                    "Cozy french terry athletic sweatpants with tapered cuffs, deep zip pockets, and drawstring waistband.", 35),
            new MockProduct(129, "White Tank Top", "35.99", 6, "Activewear", "active_004",
                    "file:///android_asset/products/active_004.jpg",
                    "Ultra-breathable seamless athletic tank with scoop neck and quick-dry sweat-wicking technology.", 40),
            new MockProduct(130, "Teal Running Shorts", "42.00", 6, "Activewear", "active_005",
                    "file:///android_asset/products/active_005.jpg",
                    "Lightweight athletic running shorts with built-in antimicrobial liner and reflective visibility accents.", 30),

            // Shoes (Category 7)
            new MockProduct(131, "White Leather Sneakers", "89.99", 7, "Shoes", "shoe_001",
                    "file:///android_asset/products/shoe_001.jpg",
                    "Minimalist low-top sneakers in crisp white nappa leather with cushioned memory foam insole.", 25),
            new MockProduct(132, "Black Leather Loafers", "99.50", 7, "Shoes", "shoe_002",
                    "file:///android_asset/products/shoe_002.jpg",
                    "Modern penny loafers crafted in polished black calfskin with flexible rubber sole for effortless refinement.", 20),
            new MockProduct(133, "Brown Chelsea Boots", "125.00", 7, "Shoes", "shoe_003",
                    "file:///android_asset/products/shoe_003.jpg",
                    "Rich oiled brown leather Chelsea boots with durable elastic side panels and Goodyear-welted rubber soles.", 18),
            new MockProduct(134, "Nude Ballet Flats", "65.99", 7, "Shoes", "shoe_004",
                    "file:///android_asset/products/shoe_004.jpg",
                    "Ultra-flexible glove leather ballet flats with rounded toe, cushioned arch support, and tonal trim.", 22),
            new MockProduct(135, "Red Stiletto Heels", "110.00", 7, "Shoes", "shoe_005",
                    "file:///android_asset/products/shoe_005.jpg",
                    "Head-turning scarlet pointed-toe pumps crafted in smooth leather with elegant 85mm slim heels.", 15),

            // Accessories (Category 8)
            new MockProduct(136, "Black Leather Belt", "42.50", 8, "Accessories", "acc_001",
                    "file:///android_asset/products/acc_001.jpg",
                    "Vegetable-tanned full-grain leather belt finished with a brushed silver buckle and beveled burnished edges.", 40),
            new MockProduct(137, "Cashmere Scarf", "78.99", 8, "Accessories", "acc_002",
                    "file:///android_asset/products/acc_002.jpg",
                    "Ultra-warm 100% pure Mongolian cashmere scarf woven with soft fringed ends in versatile oat heather.", 25),
            new MockProduct(138, "Wool Gloves", "35.00", 8, "Accessories", "acc_003",
                    "file:///android_asset/products/acc_003.jpg",
                    "Knit merino wool winter gloves equipped with touchscreen-sensitive conductive fingertips.", 35),
            new MockProduct(139, "Silk Hair Scarf", "29.99", 8, "Accessories", "acc_004",
                    "file:///android_asset/products/acc_004.jpg",
                    "Lightweight printed silk twill square scarf featuring refined hand-rolled edges and hand-drawn motif.", 30),
            new MockProduct(140, "Leather Cuff Bracelet", "48.50", 8, "Accessories", "acc_005",
                    "file:///android_asset/products/acc_005.jpg",
                    "Artisanal double-wrap braided leather wristband with magnetic matte-black stainless steel closure.", 25),

            // Leather & Suede (Category 9)
            new MockProduct(141, "Premium Black Leather Pants", "145.99", 9, "Leather & Suede", "leather_001",
                    "file:///android_asset/products/leather_001.jpg",
                    "Statement high-rise straight-leg trousers cut from supple genuine leather with smooth satin lining.", 12),
            new MockProduct(142, "Brown Suede Jacket", "135.00", 9, "Leather & Suede", "leather_002",
                    "file:///android_asset/products/leather_002.jpg",
                    "Western-inspired zip trucker jacket crafted in velvety brown goat suede with tailored point collar.", 15),
            new MockProduct(143, "Cognac Leather Skirt", "129.50", 9, "Leather & Suede", "leather_003",
                    "file:///android_asset/products/leather_003.jpg",
                    "A-line midi skirt fashioned from butter-soft cognac lambskin leather with hidden rear zip.", 14),
            new MockProduct(144, "Black Suede Dress", "158.99", 9, "Leather & Suede", "leather_004",
                    "file:///android_asset/products/leather_004.jpg",
                    "Exquisite sleeveless mini dress tailored from fine black suede with flattering seam contours.", 10),
            new MockProduct(145, "Tan Leather Shirt", "119.99", 9, "Leather & Suede", "leather_005",
                    "file:///android_asset/products/leather_005.jpg",
                    "Oversized leather overshirt in warm tan with snap front closures and large chest flap pockets.", 16),

            // Bags & Backpacks (Category 10)
            new MockProduct(146, "Black Leather Tote Bag", "99.99", 10, "Bags & Backpacks", "bag_001",
                    "file:///android_asset/products/bag_001.jpg",
                    "Spacious structured everyday tote bag crafted from pebble leather with dedicated laptop divider.", 24),
            new MockProduct(147, "Cognac Leather Shoulder Bag", "124.50", 10, "Bags & Backpacks", "bag_002",
                    "file:///android_asset/products/bag_002.jpg",
                    "Chic crescent baguette bag in warm cognac leather with gleaming hardware and comfortable wide strap.", 18),
            new MockProduct(148, "Navy Canvas Backpack", "64.99", 10, "Bags & Backpacks", "bag_003",
                    "file:///android_asset/products/bag_003.jpg",
                    "Durable 16oz waxed cotton canvas daypack with leather strap accents, laptop sleeve, and brass hardware.", 28),
            new MockProduct(149, "Beige Crossbody Bag", "79.00", 10, "Bags & Backpacks", "bag_004",
                    "file:///android_asset/products/bag_004.jpg",
                    "Compact hands-free crossbody bag crafted in neutral beige pebble leather with adjustable guitar strap.", 22),
            new MockProduct(150, "Luxury Designer Clutch", "189.99", 10, "Bags & Backpacks", "bag_005",
                    "file:///android_asset/products/bag_005.jpg",
                    "Opulent evening pouch clutch in pleated nappa leather with magnetic frame closure and removable chain.", 12)
    };

    private synchronized String handleCartRequest(String method, String path, Request request) {
        // DELETE /cart/items/{productId} -> remove line item
        if ("DELETE".equalsIgnoreCase(method) && path.contains("/cart/items/")) {
            try {
                long productId = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
                Iterator<MockCartItem> it = MOCK_CART_ITEMS.iterator();
                while (it.hasNext()) {
                    if (it.next().productId == productId) {
                        it.remove();
                        break;
                    }
                }
            } catch (Exception ignored) {}
            return buildCartResponse();
        }

        // PATCH /cart/items/{productId} -> update quantity
        if ("PATCH".equalsIgnoreCase(method) && path.contains("/cart/items/")) {
            try {
                long productId = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
                String body = readRequestBody(request);
                int qty = extractIntFromJson(body, "quantity", 1);
                for (MockCartItem item : MOCK_CART_ITEMS) {
                    if (item.productId == productId) {
                        item.quantity = Math.max(1, qty);
                        break;
                    }
                }
            } catch (Exception ignored) {}
            return buildCartResponse();
        }

        // POST /cart/items -> add to cart
        if ("POST".equalsIgnoreCase(method) && path.endsWith("/cart/items")) {
            try {
                String body = readRequestBody(request);
                long productId = extractLongFromJson(body, "productId", 101);
                int qty = extractIntFromJson(body, "quantity", 1);

                boolean exists = false;
                for (MockCartItem item : MOCK_CART_ITEMS) {
                    if (item.productId == productId) {
                        item.quantity += Math.max(1, qty);
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    MockProduct match = null;
                    for (MockProduct p : ALL_MOCK_PRODUCTS) {
                        if (p.id == productId) {
                            match = p;
                            break;
                        }
                    }
                    if (match != null) {
                        double price = 49.99;
                        try {
                            price = Double.parseDouble(match.price);
                        } catch (Exception ignored) {}
                        MOCK_CART_ITEMS.add(new MockCartItem(match.id, match.name, match.imageUrl, price, Math.max(1, qty), match.stockQuantity));
                    }
                }
            } catch (Exception ignored) {}
            return buildCartResponse();
        }

        // DELETE /cart -> clear cart
        if ("DELETE".equalsIgnoreCase(method) && path.endsWith("/cart")) {
            MOCK_CART_ITEMS.clear();
            return "{\"data\":null}";
        }

        // Default: GET /cart
        return buildCartResponse();
    }

    private static synchronized String buildCartResponse() {
        StringBuilder itemsJson = new StringBuilder();
        double subtotal = 0.0;
        int totalItems = 0;
        for (int i = 0; i < MOCK_CART_ITEMS.size(); i++) {
            MockCartItem item = MOCK_CART_ITEMS.get(i);
            if (i > 0) {
                itemsJson.append(",");
            }
            itemsJson.append(item.toJson());
            subtotal += (item.unitPrice * item.quantity);
            totalItems += item.quantity;
        }

        double shipping = MOCK_CART_ITEMS.isEmpty() ? 0.0 : 5.00;
        double total = subtotal + shipping;
        String subtotalStr = String.format(Locale.US, "%.2f", subtotal);
        String shippingStr = String.format(Locale.US, "%.2f", shipping);
        String totalStr = String.format(Locale.US, "%.2f", total);

        return "{\"data\":{"
                + "\"items\":[" + itemsJson.toString() + "],"
                + "\"itemCount\":" + totalItems + ",\"subtotal\":\"" + subtotalStr + "\",\"shipping\":\"" + shippingStr + "\",\"total\":\"" + totalStr + "\",\"currency\":\"USD\",\"hasIssues\":false}}";
    }

    private String readRequestBody(Request request) {
        if (request == null || request.body() == null) {
            return "";
        }
        try {
            Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            return buffer.readUtf8();
        } catch (Exception e) {
            return "";
        }
    }

    private static int extractIntFromJson(String json, String key, int defaultValue) {
        if (json == null || json.isEmpty()) {
            return defaultValue;
        }
        try {
            Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
            Matcher m = p.matcher(json);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Exception ignored) {}
        return defaultValue;
    }

    private static long extractLongFromJson(String json, String key, long defaultValue) {
        if (json == null || json.isEmpty()) {
            return defaultValue;
        }
        try {
            Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
            Matcher m = p.matcher(json);
            if (m.find()) {
                return Long.parseLong(m.group(1));
            }
        } catch (Exception ignored) {}
        return defaultValue;
    }
}
