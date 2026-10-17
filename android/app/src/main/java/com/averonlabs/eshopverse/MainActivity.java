package com.averonlabs.eshopverse;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.averonlabs.eshopverse.core.ui.ThemeManager;
import com.averonlabs.eshopverse.databinding.ActivityMainBinding;
import com.google.android.material.badge.BadgeDrawable;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private Insets lastSystemBars = Insets.NONE;
    private Insets lastIme = Insets.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        boolean isDarkMode = ThemeManager.isDarkMode(this);
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(!isDarkMode);
            insetsController.setAppearanceLightNavigationBars(!isDarkMode);
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Insets handling: left/right/top to root, bottom margin to floating card and content
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            lastSystemBars = bars;
            lastIme = ime;

            v.setPadding(bars.left, bars.top, bars.right, 0);

            // Dynamically position floating pill card above system navigation bars
            if (binding.cardBottomNav.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
                CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) binding.cardBottomNav.getLayoutParams();
                int baseMargin = getResources().getDimensionPixelSize(R.dimen.nav_floating_margin_bottom);
                lp.bottomMargin = baseMargin + bars.bottom;
                binding.cardBottomNav.setLayoutParams(lp);
            }

            updateContentBottomPadding();
            return insets;
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNav, navController);

            // Hide bottom navigation on non-root destinations
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destId = destination.getId();
                boolean isTopLevel = destId == R.id.homeFragment
                        || destId == R.id.exploreFragment
                        || destId == R.id.cartFragment
                        || destId == R.id.accountFragment;
                binding.cardBottomNav.setVisibility(isTopLevel ? View.VISIBLE : View.GONE);
                updateContentBottomPadding();
            });
        }

        binding.cardBottomNav.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            updateContentBottomPadding();
        });

        // Initialize cart badge as hidden
        setCartBadgeCount(0);

        // Hierarchical back navigation adhering to Material 3 & UX principles:
        // First pop child/sub-screens in current flow (e.g. Register -> SignIn -> Account).
        // If at the root of a tab other than Home, Back returns to HomeFragment.
        // A subsequent Back press from HomeFragment exits the application directly.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (navController != null) {
                    if (navController.popBackStack()) {
                        return;
                    }
                    if (navController.getCurrentDestination() != null
                            && navController.getCurrentDestination().getId() != R.id.homeFragment) {
                        navController.navigate(R.id.homeFragment);
                        return;
                    }
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    private void updateContentBottomPadding() {
        if (binding == null) {
            return;
        }
        int imeBottom = lastIme != null ? lastIme.bottom : 0;
        int systemBottom = lastSystemBars != null ? lastSystemBars.bottom : 0;
        int floatingNavHeight = 0;
        if (binding.cardBottomNav != null && binding.cardBottomNav.getVisibility() == View.VISIBLE) {
            int cardH = binding.cardBottomNav.getHeight();
            int baseMargin = getResources().getDimensionPixelSize(R.dimen.nav_floating_margin_bottom);
            floatingNavHeight = cardH + baseMargin + systemBottom + getResources().getDimensionPixelSize(R.dimen.spacing_sm);
        }
        int baseBottom = Math.max(floatingNavHeight, systemBottom);
        int finalPadding = Math.max(baseBottom, imeBottom);

        binding.contentMain.navHostFragmentContentMain.setPadding(0, 0, 0, finalPadding);
    }

    /**
     * Updates the cart badge count. If count > 0, the badge is visible with the number.
     * If count <= 0, the badge is hidden.
     *
     * @param count the item count in the cart
     */
    public void setCartBadgeCount(int count) {
        if (binding == null || binding.bottomNav == null) {
            return;
        }
        if (count > 0) {
            BadgeDrawable badge = binding.bottomNav.getOrCreateBadge(R.id.cartFragment);
            badge.setVisible(true);
            badge.setNumber(count);
            badge.setContentDescriptionQuantityStringsResource(R.plurals.cart_badge_content_description);
        } else {
            binding.bottomNav.removeBadge(R.id.cartFragment);
        }
    }
}
