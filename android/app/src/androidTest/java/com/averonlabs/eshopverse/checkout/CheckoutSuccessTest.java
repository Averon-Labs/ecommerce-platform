package com.averonlabs.eshopverse.checkout;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.averonlabs.eshopverse.MainActivity;
import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.model.CartItem;
import com.averonlabs.eshopverse.core.model.CheckoutQuote;
import com.averonlabs.eshopverse.core.model.Currency;
import com.averonlabs.eshopverse.core.model.Order;
import com.averonlabs.eshopverse.core.model.OrderStatus;
import com.averonlabs.eshopverse.core.model.PaymentResult;
import com.averonlabs.eshopverse.core.model.PaymentStatus;
import com.averonlabs.eshopverse.core.model.ShippingAddress;
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
 * Instrumentation UI test for Checkout success flow per docs/android/PHASE-2-SPEC.md §E.7:
 * Address -> review totals -> simulate success -> M-14 shows returned reference and confirmed status.
 */
@RunWith(AndroidJUnit4.class)
public class CheckoutSuccessTest {

    private FakeCheckoutRepository fakeRepository;

    @Before
    public void setUp() {
        ApiRoutes dummyRoutes = (ApiRoutes) Proxy.newProxyInstance(
                ApiRoutes.class.getClassLoader(),
                new Class<?>[]{ApiRoutes.class},
                (proxy, method, args) -> null
        );
        fakeRepository = new FakeCheckoutRepository(dummyRoutes);
        CheckoutViewModelFactory.setDefaultRepository(fakeRepository);
    }

    @After
    public void tearDown() {
        CheckoutViewModelFactory.setDefaultRepository(null);
    }

    @Test
    public void testCheckoutSuccessFlow() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, MainActivity.class);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            // Navigate to Cart -> Checkout
            onView(withId(R.id.cartFragment)).perform(click());
            onView(withId(R.id.btn_cart_checkout)).perform(click());

            // M-11: Enter shipping address
            onView(withId(R.id.et_recipient_name)).perform(typeText("Jane Smith"), closeSoftKeyboard());
            onView(withId(R.id.et_line1)).perform(typeText("456 Market St"), closeSoftKeyboard());
            onView(withId(R.id.et_city)).perform(typeText("San Francisco"), closeSoftKeyboard());
            onView(withId(R.id.et_region)).perform(typeText("CA"), closeSoftKeyboard());
            onView(withId(R.id.et_postal_code)).perform(typeText("94105"), closeSoftKeyboard());
            onView(withId(R.id.et_country)).perform(typeText("US"), closeSoftKeyboard());

            onView(withId(R.id.btn_continue_to_review)).perform(click());

            // M-12: Review screen shows quote server totals
            onView(withId(R.id.tv_review_subtotal)).check(matches(withText("$89.99")));
            onView(withId(R.id.tv_review_shipping)).check(matches(withText("$5.00")));
            onView(withId(R.id.tv_review_total)).check(matches(withText("$94.99")));

            onView(withId(R.id.btn_proceed_to_payment)).perform(click());

            // M-13: Payment choice screen shows amount due
            onView(withId(R.id.tv_payment_amount_due)).check(matches(withText("$94.99")));
            onView(withId(R.id.btn_simulate_success)).perform(click());

            // M-14: Order confirmation screen shows confirmed status and order reference
            onView(withId(R.id.tv_result_order_reference)).check(matches(withText("ESV-005001")));
            onView(withId(R.id.tv_result_heading)).check(matches(withText(R.string.checkout_result_success_heading)));
            onView(withId(R.id.btn_continue_shopping)).check(matches(isDisplayed()));
            onView(withId(R.id.btn_view_orders)).check(matches(isDisplayed()));
        }
    }

    private static class FakeCheckoutRepository extends CheckoutRepository {
        FakeCheckoutRepository(@NonNull ApiRoutes routes) {
            super(routes);
        }

        @Override
        public void getQuote(@NonNull ShippingAddress address, @NonNull CheckoutCallback<CheckoutQuote> callback) {
            List<CartItem> items = new ArrayList<>();
            items.add(new CartItem(
                    101, "Classic Leather Sneaker", null,
                    "89.99", Currency.USD, 1, "89.99",
                    true, 10, false, false
            ));
            callback.onSuccess(new CheckoutQuote(
                    items, "89.99", "5.00", "94.99",
                    Currency.USD, address, "fingerprint_123"
            ));
        }

        @Override
        public void createOrder(
                @NonNull String idempotencyKey,
                @NonNull ShippingAddress address,
                @Nullable String notes,
                @NonNull CheckoutCallback<Order> callback
        ) {
            callback.onSuccess(new Order(
                    5001, "ESV-005001", OrderStatus.PENDING_PAYMENT, PaymentStatus.PENDING,
                    Collections.emptyList(), "89.99", "5.00", "94.99", Currency.USD,
                    address, notes, null, Collections.emptyList(), "2026-10-10", "2026-10-10"
            ));
        }

        @Override
        public void submitPayment(
                @NonNull String idempotencyKey,
                long orderId,
                @NonNull String outcome,
                @NonNull CheckoutCallback<PaymentResult> callback
        ) {
            callback.onSuccess(new PaymentResult(
                    (int) orderId, "ESV-005001", OrderStatus.CONFIRMED, PaymentStatus.COMPLETED, true, null
            ));
        }
    }
}
