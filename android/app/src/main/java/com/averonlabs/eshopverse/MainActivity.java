package com.averonlabs.eshopverse;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.averonlabs.eshopverse.databinding.ActivityMainBinding;
import com.google.android.material.badge.BadgeDrawable;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(true);
            insetsController.setAppearanceLightNavigationBars(true);
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Insets handling: left/right to root, top to AppBarLayout, bottom to BottomNavigationView
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, 0, bars.right, 0);
            binding.appBarLayout.setPadding(0, bars.top, 0, 0);
            binding.bottomNav.setPadding(0, 0, 0, bars.bottom);
            return insets;
        });

        setSupportActionBar(binding.toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.homeFragment,
                    R.id.exploreFragment,
                    R.id.cartFragment,
                    R.id.accountFragment
            ).build();

            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(binding.bottomNav, navController);

            // Hide bottom navigation on non-root destinations
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destId = destination.getId();
                boolean isTopLevel = destId == R.id.homeFragment
                        || destId == R.id.exploreFragment
                        || destId == R.id.cartFragment
                        || destId == R.id.accountFragment;
                binding.bottomNav.setVisibility(isTopLevel ? View.VISIBLE : View.GONE);
                updateContentBottomPadding();
            });
        }

        binding.bottomNav.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
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
        int bottomPadding = binding.bottomNav.getVisibility() == View.VISIBLE ? binding.bottomNav.getHeight() : 0;
        binding.contentMain.navHostFragmentContentMain.setPadding(0, 0, 0, bottomPadding);
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
        } else {
            binding.bottomNav.removeBadge(R.id.cartFragment);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) {
            binding.bottomNav.setSelectedItemId(R.id.exploreFragment);
            return true;
        } else if (id == R.id.action_account) {
            binding.bottomNav.setSelectedItemId(R.id.accountFragment);
            return true;
        }
        if (navController != null && NavigationUI.onNavDestinationSelected(item, navController)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (navController != null && navController.getCurrentDestination() != null) {
            int currentId = navController.getCurrentDestination().getId();
            if (currentId != R.id.homeFragment) {
                navController.popBackStack(R.id.homeFragment, false);
                return true;
            }
        }
        return super.onSupportNavigateUp();
    }
}
