package com.averonlabs.eshopverse.shell;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.catalog.CatalogListViewModel;
import com.averonlabs.eshopverse.catalog.CatalogViewModelFactory;
import com.averonlabs.eshopverse.catalog.ProductGridAdapter;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.state.UiState;
import com.averonlabs.eshopverse.core.ui.ThemeManager;
import com.averonlabs.eshopverse.databinding.FragmentHomeBinding;

import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private CatalogListViewModel catalogViewModel;
    private ProductGridAdapter trendingAdapter;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CatalogViewModelFactory factory = new CatalogViewModelFactory(requireContext());
        catalogViewModel = new ViewModelProvider(this, factory).get(CatalogListViewModel.class);

        setupTrendingGrid();
        setupClickListeners();
        updateThemeIcon();
        observeViewModel();

        catalogViewModel.loadFirstPage();
    }

    private void updateThemeIcon() {
        if (binding == null || !isAdded()) return;
        boolean isDark = ThemeManager.isDarkMode(requireContext());
        binding.ivThemeToggleIcon.setImageResource(isDark ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode);
        binding.btnProfileAvatar.setContentDescription(getString(isDark ? R.string.theme_dark_desc : R.string.theme_light_desc));
    }

    private void setupTrendingGrid() {
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        binding.rvTrendingProducts.setLayoutManager(layoutManager);

        trendingAdapter = new ProductGridAdapter(this::onProductClicked);
        binding.rvTrendingProducts.setAdapter(trendingAdapter);
    }

    private void setupClickListeners() {
        // Pill search bar: navigate to Explore
        binding.barPillSearch.setOnClickListener(v -> navigateToExplore());

        // Hero CTA button: navigate to Explore
        binding.btnHeroCta.setOnClickListener(v -> navigateToExplore());

        // Trending See All button: navigate to Explore
        binding.btnTrendingViewAll.setOnClickListener(v -> navigateToExplore());

        // Theme toggle button in circular frame
        binding.btnProfileAvatar.setOnClickListener(v -> {
            ThemeManager.toggleTheme(requireContext());
            updateThemeIcon();
        });

        // Category circular chips:
        binding.chipCatAll.setOnClickListener(v -> navigateToExplore());
        binding.chipCatHome.setOnClickListener(v -> navigateToCategory(1, getString(R.string.home_category_home)));
        binding.chipCatFashion.setOnClickListener(v -> navigateToCategory(2, getString(R.string.home_category_fashion)));
        binding.chipCatObjects.setOnClickListener(v -> navigateToCategory(3, getString(R.string.home_category_objects)));
        binding.chipCatBeauty.setOnClickListener(v -> navigateToCategory(4, getString(R.string.home_category_beauty)));
    }

    private void observeViewModel() {
        catalogViewModel.getProductsState().observe(getViewLifecycleOwner(), state -> {
            if (state != null && state.getStatus() == UiState.Status.CONTENT && state.getData() != null) {
                List<ProductSummary> products = state.getData();
                trendingAdapter.submitList(products);
            }
        });
    }

    private void onProductClicked(@NonNull ProductSummary product) {
        if (!isAdded()) {
            return;
        }
        Bundle args = new Bundle();
        args.putLong("productId", product.getId());
        Navigation.findNavController(requireView()).navigate(
                R.id.action_homeFragment_to_productDetailFragment,
                args
        );
    }

    private void navigateToExplore() {
        if (!isAdded()) {
            return;
        }
        Navigation.findNavController(requireView()).navigate(
                R.id.action_homeFragment_to_exploreFragment
        );
    }

    private void navigateToCategory(long categoryId, String categoryName) {
        if (!isAdded()) {
            return;
        }
        Bundle args = new Bundle();
        args.putLong("categoryId", categoryId);
        args.putString("categoryName", categoryName);
        Navigation.findNavController(requireView()).navigate(
                R.id.action_homeFragment_to_categoryProductsFragment,
                args
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
