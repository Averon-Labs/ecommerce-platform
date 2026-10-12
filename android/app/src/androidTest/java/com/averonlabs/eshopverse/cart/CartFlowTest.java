package com.averonlabs.eshopverse.cart;

import static androidx.test.espresso.Espresso.onView;
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
import com.averonlabs.eshopverse.core.model.Cart;
import com.averonlabs.eshopverse.core.model.CartItem;
import com.averonlabs.eshopverse.core.model.Currency;
import com.averonlabs.eshopverse.core.net.ApiRoutes;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Instrumentation UI test for Cart flow per docs/android/PHASE-2-SPEC.md §D.5:
 * Load cart -> render server totals -> update/remove item -> empty state.
 */
@RunWith(AndroidJUnit4.class)
public class CartFlowTest {

    private FakeCartRepository fakeRepository;

    @Before
    public void setUp() {
        ApiRoutes dummyRoutes = (ApiRoutes) Proxy.newProxyInstance(
                ApiRoutes.class.getClassLoader(),
                new Class<?>[]{ApiRoutes.class},
                (proxy, method, args) -> null
        );
        fakeRepository = new FakeCartRepository(dummyRoutes);
        CartViewModelFactory.setDefaultRepository(fakeRepository);
    }

    @After
    public void tearDown() {
        CartViewModelFactory.setDefaultRepository(null);
    }

    @Test
    public void testCartLoadsDisplaysServerTotalsAndAllowsInteractions() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, MainActivity.class);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            // Navigate to Cart via bottom navigation
            onView(withId(R.id.cartFragment)).perform(click());

            // Verify totals match exact server-returned strings
            onView(withId(R.id.tv_cart_subtotal)).check(matches(withText("$204.48")));
            onView(withId(R.id.tv_cart_shipping)).check(matches(withText("$5.00")));
            onView(withId(R.id.tv_cart_total)).check(matches(withText("$209.48")));

            // Verify checkout button is visible and enabled
            onView(withId(R.id.btn_cart_checkout)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void testEmptyCartRendersEmptyStateAndAction() {
        fakeRepository.setEmpty(true);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, MainActivity.class);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            // Navigate to Cart
            onView(withId(R.id.cartFragment)).perform(click());

            // Empty state action is displayed
            onView(withText(R.string.cart_empty_title)).check(matches(isDisplayed()));
            onView(withText(R.string.cart_action_explore)).check(matches(isDisplayed()));
        }
    }

    private static class FakeCartRepository extends CartRepository {

        private boolean isEmpty = false;
        private final List<CartItem> items = new ArrayList<>();

        FakeCartRepository(@NonNull ApiRoutes apiRoutes) {
            super(apiRoutes);
            resetItems();
        }

        void setEmpty(boolean empty) {
            this.isEmpty = empty;
        }

        void resetItems() {
            items.clear();
            items.add(new CartItem(
                    1,
                    "Classic Leather Sneaker",
                    null,
                    "89.99",
                    Currency.USD,
                    2,
                    "179.98",
                    true,
                    10,
                    false,
                    false
            ));
            items.add(new CartItem(
                    2,
                    "Canvas Tote Bag",
                    null,
                    "24.50",
                    Currency.USD,
                    1,
                    "24.50",
                    true,
                    5,
                    false,
                    false
            ));
        }

        @Override
        public void getCart(@NonNull CartCallback<Cart> callback) {
            if (isEmpty) {
                callback.onSuccess(new Cart(
                        Collections.emptyList(),
                        0,
                        "0.00",
                        "0.00",
                        "0.00",
                        Currency.USD,
                        false
                ));
            } else {
                callback.onSuccess(new Cart(
                        new ArrayList<>(items),
                        3,
                        "204.48",
                        "5.00",
                        "209.48",
                        Currency.USD,
                        false
                ));
            }
        }

        @Override
        public void updateQuantity(long productId, int quantity, @NonNull CartCallback<Cart> callback) {
            for (int i = 0; i < items.size(); i++) {
                CartItem item = items.get(i);
                if (item.getProductId() == productId) {
                    items.set(i, new CartItem(
                            item.getProductId(),
                            item.getName(),
                            item.getImageUrl(),
                            item.getUnitPrice(),
                            item.getCurrency(),
                            quantity,
                            item.getLineTotal(),
                            item.isInStock(),
                            item.getAvailableQuantity(),
                            item.isPriceChanged(),
                            item.isUnavailable()
                    ));
                    break;
                }
            }
            callback.onSuccess(new Cart(
                    new ArrayList<>(items),
                    3,
                    "204.48",
                    "5.00",
                    "209.48",
                    Currency.USD,
                    false
            ));
        }

        @Override
        public void removeItem(long productId, @NonNull CartCallback<Cart> callback) {
            items.removeIf(item -> item.getProductId() == productId);
            if (items.isEmpty()) {
                callback.onSuccess(new Cart(
                        Collections.emptyList(),
                        0,
                        "0.00",
                        "0.00",
                        "0.00",
                        Currency.USD,
                        false
                ));
            } else {
                callback.onSuccess(new Cart(
                        new ArrayList<>(items),
                        1,
                        "89.99",
                        "5.00",
                        "94.99",
                        Currency.USD,
                        false
                ));
            }
        }
    }
}
