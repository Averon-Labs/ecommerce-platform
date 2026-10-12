package com.averonlabs.eshopverse.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.averonlabs.eshopverse.ApiFixtureLoader;
import com.averonlabs.eshopverse.core.error.ApiErrorCodes;
import com.averonlabs.eshopverse.core.model.Category;
import com.averonlabs.eshopverse.core.model.ProductDetail;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.money.Money;
import com.averonlabs.eshopverse.core.money.MoneyFormat;
import com.averonlabs.eshopverse.core.net.ApiClient;
import com.averonlabs.eshopverse.core.net.ApiException;
import com.averonlabs.eshopverse.core.net.PagedResponse;
import com.averonlabs.eshopverse.core.storage.InMemoryTokenStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * JVM unit tests for {@link CatalogRepository} per docs/android/PHASE-1-SPEC.md §E.7.
 * Covers summary and detail parsing, image ordering, 422 CATEGORY_NOT_FOUND, 404, 429,
 * pagination metadata, and verifies that prices render cleanly without binary floating-point conversions.
 */
public class CatalogRepositoryTest {

    private MockWebServer mockWebServer;
    private CatalogRepository repository;

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
        repository = new CatalogRepository(apiClient.getRoutes());
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testListProductsSuccessAndMoneyAssertion() throws Exception {
        String fixture = ApiFixtureLoader.load("listProducts", "success");
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(fixture));

        AtomicReference<PagedResponse<ProductSummary>> resultRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        repository.listProducts(new ProductParams(), new CatalogRepository.CatalogCallback<PagedResponse<ProductSummary>>() {
            @Override
            public void onSuccess(PagedResponse<ProductSummary> result) {
                resultRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        PagedResponse<ProductSummary> response = resultRef.get();
        assertNotNull(response);

        List<ProductSummary> products = response.getData();
        assertEquals(2, products.size());

        ProductSummary p1 = products.get(0);
        assertEquals(101, p1.getId());
        assertEquals("Classic Leather Sneaker", p1.getName());
        assertEquals("89.99", p1.getPrice());
        assertEquals("$89.99", MoneyFormat.format(p1.getPriceMoney()));
        assertEquals("$89.99", MoneyFormat.format(p1.getPrice()));

        // Verification of strict decimal money representation (no binary float/double)
        Money money = p1.getPriceMoney();
        assertEquals("89.99", money.toString());
        assertEquals(8999L, money.getAmountInCents());

        // Pagination metadata
        assertNotNull(response.getPagination());
        assertEquals(1, response.getPagination().getCurrentPage());
        assertEquals(20, response.getPagination().getPerPage());
        assertEquals(2, response.getPagination().getTotalItems());
        assertEquals(1, response.getPagination().getTotalPages());
        assertFalse(response.hasMore());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("GET", request.getMethod());
        assertTrue(request.getPath().contains("/api/v1/products"));
    }

    @Test
    public void testGetProductDetailSuccessAndImageOrdering() throws Exception {
        String fixture = ApiFixtureLoader.load("getProduct", "success");
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(fixture));

        AtomicReference<ProductDetail> resultRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        repository.getProduct(101L, new CatalogRepository.CatalogCallback<ProductDetail>() {
            @Override
            public void onSuccess(ProductDetail result) {
                resultRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        ProductDetail detail = resultRef.get();
        assertNotNull(detail);

        assertEquals(101, detail.getId());
        assertEquals("Classic Leather Sneaker", detail.getName());
        assertNotNull(detail.getDescription());
        assertTrue(detail.getDescription().contains("full-grain leather"));
        assertEquals("89.99", detail.getPrice());
        assertEquals("$89.99", MoneyFormat.format(detail.getPriceMoney()));

        // Image ordering
        assertNotNull(detail.getImages());
        assertFalse(detail.getImages().isEmpty());
        assertEquals(1, detail.getImages().get(0).getSortOrder());
        assertEquals("https://images.example.com/products/sneaker-1.jpg", detail.getImages().get(0).getUrl());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/api/v1/products/101", request.getPath());
    }

    @Test
    public void testListCategoriesSuccess() throws Exception {
        String fixture = ApiFixtureLoader.load("listCategories", "success");
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(fixture));

        AtomicReference<List<Category>> resultRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        repository.listCategories(new CatalogRepository.CatalogCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> result) {
                resultRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        List<Category> categories = resultRef.get();
        assertNotNull(categories);
        assertEquals(1, categories.size());
        assertEquals("Footwear", categories.get(0).getName());
        assertEquals(1, categories.get(0).getId());
    }

    @Test
    public void testCategoryNotFound422Mapping() throws Exception {
        String errorJson = "{\n" +
                "  \"code\": \"CATEGORY_NOT_FOUND\",\n" +
                "  \"message\": \"Category not found or archived\"\n" +
                "}";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(422)
                .setHeader("Content-Type", "application/json")
                .setBody(errorJson));

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        ProductParams params = new ProductParams().withCategoryId(999L);
        repository.listProducts(params, new CatalogRepository.CatalogCallback<PagedResponse<ProductSummary>>() {
            @Override
            public void onSuccess(PagedResponse<ProductSummary> result) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                errorRef.set(throwable);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(errorRef.get() instanceof ApiException);
        ApiException apiException = (ApiException) errorRef.get();
        assertEquals(422, apiException.getHttpStatus());
        assertEquals(ApiErrorCodes.CATEGORY_NOT_FOUND, apiException.getCode());
    }

    @Test
    public void testProductNotFound404Mapping() throws Exception {
        String errorJson = "{\n" +
                "  \"code\": \"RESOURCE_NOT_FOUND\",\n" +
                "  \"message\": \"Product not found\"\n" +
                "}";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody(errorJson));

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        repository.getProduct(9999L, new CatalogRepository.CatalogCallback<ProductDetail>() {
            @Override
            public void onSuccess(ProductDetail result) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                errorRef.set(throwable);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(errorRef.get() instanceof ApiException);
        ApiException apiException = (ApiException) errorRef.get();
        assertEquals(404, apiException.getHttpStatus());
        assertEquals(ApiErrorCodes.RESOURCE_NOT_FOUND, apiException.getCode());
    }

    @Test
    public void testRateLimited429Mapping() throws Exception {
        String fixture = ApiFixtureLoader.load("common", "rate-limited");
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "30")
                .setHeader("Content-Type", "application/json")
                .setBody(fixture));

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        repository.listProducts(new ProductParams(), new CatalogRepository.CatalogCallback<PagedResponse<ProductSummary>>() {
            @Override
            public void onSuccess(PagedResponse<ProductSummary> result) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                errorRef.set(throwable);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(errorRef.get() instanceof ApiException);
        ApiException apiException = (ApiException) errorRef.get();
        assertEquals(429, apiException.getHttpStatus());
        assertEquals(ApiErrorCodes.RATE_LIMITED, apiException.getCode());
    }
}
