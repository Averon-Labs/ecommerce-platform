package com.averonlabs.eshopverse.catalog;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.averonlabs.eshopverse.MainActivity;
import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.model.Category;
import com.averonlabs.eshopverse.core.model.CategoryStatus;
import com.averonlabs.eshopverse.core.model.Currency;
import com.averonlabs.eshopverse.core.model.Pagination;
import com.averonlabs.eshopverse.core.model.ProductDetail;
import com.averonlabs.eshopverse.core.model.ProductImage;
import com.averonlabs.eshopverse.core.model.ProductStatus;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.net.ApiRoutes;
import com.averonlabs.eshopverse.core.net.PagedResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Instrumentation UI test for catalog browse-to-detail flow per docs/android/PHASE-1-SPEC.md §E.7:
 * Explore grid -> card tap -> M-05 shows matching name and price; system back returns to the list.
 */
@RunWith(AndroidJUnit4.class)
public class BrowseToDetailTest {

    private FakeCatalogRepository fakeRepository;

    @Before
    public void setUp() {
        ApiRoutes dummyRoutes = (ApiRoutes) Proxy.newProxyInstance(
                ApiRoutes.class.getClassLoader(),
                new Class<?>[]{ApiRoutes.class},
                (proxy, method, args) -> null
        );
        fakeRepository = new FakeCatalogRepository(dummyRoutes);
        CatalogViewModelFactory.setDefaultRepository(fakeRepository);
    }

    @After
    public void tearDown() {
        CatalogViewModelFactory.setDefaultRepository(null);
    }

    private Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testBrowseToDetailAndBackNavigation() {
        Intent intent = new Intent(context(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            // Step 1: Navigate to Explore tab via bottom navigation
            onView(withId(R.id.exploreFragment)).perform(click());

            // Verify item is visible in the grid
            onView(withText("Classic Leather Sneaker")).check(matches(isDisplayed()));
            onView(withText("$89.99")).check(matches(isDisplayed()));

            // Step 2: Tap the product card to navigate to M-05 Product Details
            onView(withText("Classic Leather Sneaker")).perform(click());

            // Verify M-05 Product Details shows matching name, price, and description
            onView(withId(R.id.tv_product_name)).check(matches(isDisplayed()));
            onView(withId(R.id.tv_product_name)).check(matches(withText("Classic Leather Sneaker")));
            onView(withId(R.id.tv_product_price)).check(matches(isDisplayed()));
            onView(withId(R.id.tv_product_price)).check(matches(withText("$89.99")));
            onView(withId(R.id.tv_product_description)).check(matches(isDisplayed()));
            onView(withId(R.id.tv_product_description)).check(matches(withText("Premium full-grain leather sneaker.")));

            // Step 3: Press system back and verify return to Explore grid
            pressBack();

            onView(withText("Classic Leather Sneaker")).check(matches(isDisplayed()));
        }
    }

    static class FakeCatalogRepository extends CatalogRepository {

        FakeCatalogRepository(@NonNull ApiRoutes routes) {
            super(routes);
        }

        @Override
        public void listCategories(@NonNull CatalogCallback<List<Category>> callback) {
            List<Category> categories = Collections.singletonList(
                    new Category(1, "Footwear", 1, CategoryStatus.ACTIVE)
            );
            callback.onSuccess(categories);
        }

        @Override
        public void listProducts(
                @NonNull ProductParams params,
                @NonNull CatalogCallback<PagedResponse<ProductSummary>> callback
        ) {
            List<ProductSummary> products = new ArrayList<>();
            products.add(new ProductSummary(
                    101,
                    "Classic Leather Sneaker",
                    "89.99",
                    Currency.USD,
                    1,
                    "Footwear",
                    null,
                    true,
                    ProductStatus.ACTIVE
            ));
            products.add(new ProductSummary(
                    102,
                    "Canvas Low-Top",
                    "49.50",
                    Currency.USD,
                    1,
                    "Footwear",
                    null,
                    true,
                    ProductStatus.ACTIVE
            ));
            callback.onSuccess(new PagedResponse<>(products, new Pagination(1, 20, 2, 1)));
        }

        @Override
        public void getProduct(long productId, @NonNull CatalogCallback<ProductDetail> callback) {
            ProductDetail detail = new ProductDetail(
                    (int) productId,
                    "Classic Leather Sneaker",
                    "89.99",
                    Currency.USD,
                    1,
                    "Footwear",
                    null,
                    true,
                    ProductStatus.ACTIVE,
                    "Premium full-grain leather sneaker.",
                    "SKU-101",
                    15,
                    Collections.singletonList(new ProductImage("https://images.example.com/p1.jpg", 1))
            );
            callback.onSuccess(detail);
        }
    }
}
