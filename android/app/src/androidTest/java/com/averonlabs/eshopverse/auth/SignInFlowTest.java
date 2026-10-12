package com.averonlabs.eshopverse.auth;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
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
import com.averonlabs.eshopverse.core.model.AuthSession;
import com.averonlabs.eshopverse.core.model.User;
import com.averonlabs.eshopverse.core.model.UserRole;
import com.averonlabs.eshopverse.core.model.UserStatus;
import com.averonlabs.eshopverse.core.net.ApiRoutes;
import com.averonlabs.eshopverse.core.storage.InMemoryTokenStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Proxy;

/**
 * Instrumentation test for customer sign-in flow per docs/android/PHASE-1-SPEC.md §D.7:
 * M-06 -> validation error -> valid credentials -> M-17 shows the name; sign-out returns to M-06 / guest state.
 */
@RunWith(AndroidJUnit4.class)
public class SignInFlowTest {

    private InMemoryTokenStore tokenStore;
    private FakeAuthRepository fakeRepository;

    @Before
    public void setUp() {
        tokenStore = new InMemoryTokenStore();
        ApiRoutes dummyRoutes = (ApiRoutes) Proxy.newProxyInstance(
                ApiRoutes.class.getClassLoader(),
                new Class<?>[]{ApiRoutes.class},
                (proxy, method, args) -> null
        );
        fakeRepository = new FakeAuthRepository(dummyRoutes, tokenStore);
        AuthViewModelFactory.setDefaultRepository(fakeRepository);
    }

    @After
    public void tearDown() {
        AuthViewModelFactory.setDefaultRepository(null);
    }

    private Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testSignInValidationSuccessAndSignOut() {
        Intent intent = new Intent(context(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            // Step 1: Navigate to Account tab via bottom navigation
            onView(withId(R.id.accountFragment)).perform(click());

            // Initially signed out: tap Sign In button in guest layout
            onView(withId(R.id.btn_guest_sign_in)).perform(click());

            // Step 2: Trigger M-06 validation error
            onView(withId(R.id.et_email)).perform(typeText("bad-email"), closeSoftKeyboard());
            onView(withId(R.id.btn_sign_in)).perform(click());
            onView(withId(R.id.til_email)).check(matches(hasDescendant(withText(R.string.auth_error_email_invalid))));

            // Step 3: Enter valid credentials
            onView(withId(R.id.et_email)).perform(replaceText("alice@example.com"), closeSoftKeyboard());
            onView(withId(R.id.et_password)).perform(replaceText("Secret123"), closeSoftKeyboard());
            onView(withId(R.id.btn_sign_in)).perform(click());

            // Step 4: Verify customer arrives at Account (M-17) and sees name
            onView(withId(R.id.accountFragment)).perform(click());
            onView(withId(R.id.tv_user_name)).check(matches(withText("Alice Smith")));

            // Step 5: Sign out and verify return to guest state
            onView(withId(R.id.btn_sign_out)).perform(click());
            onView(withId(R.id.layout_guest_state)).check(matches(isDisplayed()));
        }
    }

    static class FakeAuthRepository extends AuthRepository {
        private final InMemoryTokenStore tokenStore;

        public FakeAuthRepository(@NonNull ApiRoutes routes, @NonNull InMemoryTokenStore tokenStore) {
            super(routes, tokenStore);
            this.tokenStore = tokenStore;
        }

        @Override
        public void loginCustomer(
                @NonNull String email,
                @NonNull String password,
                @NonNull AuthCallback<AuthSession> callback
        ) {
            tokenStore.save("mock-token-xyz", "2099-01-01T00:00:00Z");
            User user = new User(1, email, "Alice", "Smith", UserRole.CUSTOMER, UserStatus.ACTIVE, "2026-01-01T00:00:00Z");
            AuthSession session = new AuthSession("mock-token-xyz", "2099-01-01T00:00:00Z", user);
            callback.onSuccess(session);
        }

        @Override
        public void logoutCustomer(@NonNull AuthCallback<Void> callback) {
            tokenStore.clear();
            callback.onSuccess(null);
        }

        @Override
        public void getProfile(@NonNull AuthCallback<User> callback) {
            User user = new User(1, "alice@example.com", "Alice", "Smith", UserRole.CUSTOMER, UserStatus.ACTIVE, "2026-01-01T00:00:00Z");
            callback.onSuccess(user);
        }
    }
}
