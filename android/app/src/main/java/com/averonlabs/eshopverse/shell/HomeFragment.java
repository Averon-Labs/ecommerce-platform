package com.averonlabs.eshopverse.shell;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.catalog.CatalogListViewModel;
import com.averonlabs.eshopverse.catalog.CatalogViewModelFactory;
import com.averonlabs.eshopverse.catalog.CategoryIconResolver;
import com.averonlabs.eshopverse.catalog.CategoryViewModel;
import com.averonlabs.eshopverse.catalog.ProductGridAdapter;
import com.averonlabs.eshopverse.core.model.Category;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.state.UiState;
import com.averonlabs.eshopverse.core.ui.ImageLoaderHelper;
import com.averonlabs.eshopverse.core.ui.SkeletonAdapter;
import com.averonlabs.eshopverse.core.ui.ThemeManager;
import com.averonlabs.eshopverse.databinding.FragmentHomeBinding;

import java.util.ArrayList;

import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private CatalogListViewModel catalogViewModel;
    private CategoryViewModel categoryViewModel;
    private ProductGridAdapter trendingCarouselAdapter;
    private ProductGridAdapter bestSellersAdapter;
    private ProductGridAdapter specialOffersAdapter;
    private ProductGridAdapter essentialsAdapter;

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
        categoryViewModel = new ViewModelProvider(this, factory).get(CategoryViewModel.class);

        setupSections();
        setupCategories();
        setupClickListeners();
        setupSwipeRefresh();
        updateThemeIcon();
        observeViewModel();

        catalogViewModel.loadFirstPage();
        categoryViewModel.loadCategories();
    }

    private void updateThemeIcon() {
        if (binding == null || !isAdded()) return;
        boolean isDark = ThemeManager.isDarkMode(requireContext());
        binding.ivThemeToggleIcon.setImageResource(isDark ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode);
        binding.btnProfileAvatar.setContentDescription(getString(isDark ? R.string.theme_dark_desc : R.string.theme_light_desc));
    }

    private void setupSections() {
        // Section 1: Trending This Week (Horizontal Carousel)
        trendingCarouselAdapter = new ProductGridAdapter(this::onProductClicked, true);
        binding.rvTrendingCarousel.setAdapter(new SkeletonAdapter(R.layout.item_skeleton_product_card, 4));

        // Section 2: Popular & Best Sellers (2-Column Grid)
        bestSellersAdapter = new ProductGridAdapter(this::onProductClicked, false);
        binding.rvBestsellersGrid.setAdapter(new SkeletonAdapter(R.layout.item_skeleton_product_card, 4));

        // Section 3: Special Offers (Horizontal Carousel)
        specialOffersAdapter = new ProductGridAdapter(this::onProductClicked, true);
        binding.rvSpecialOffersCarousel.setAdapter(new SkeletonAdapter(R.layout.item_skeleton_product_card, 4));

        // Section 4: Curated Essentials (2-Column Grid)
        essentialsAdapter = new ProductGridAdapter(this::onProductClicked, false);
        binding.rvEssentialsGrid.setAdapter(new SkeletonAdapter(R.layout.item_skeleton_product_card, 4));
    }

    private void setupCategories() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.rvHomeCategories.setLayoutManager(layoutManager);

        // Immediately attach 8 skeleton mood circles so category row is never blank on initial render
        binding.rvHomeCategories.setAdapter(new SkeletonAdapter(R.layout.item_skeleton_category_circle, 8));

        categoryViewModel.getCategoriesState().observe(getViewLifecycleOwner(), state -> {
            if (state != null && state.isContent() && state.getData() != null) {
                renderCategoryMoodCircles(state.getData());
            }
        });
    }

    private void renderCategoryMoodCircles(@NonNull List<Category> categories) {
        binding.rvHomeCategories.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_category_mood_circle, parent, false);
                return new RecyclerView.ViewHolder(view) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                ImageView iconView = holder.itemView.findViewById(R.id.iv_category_icon);
                TextView nameView = holder.itemView.findViewById(R.id.tv_category_name);

                if (position == 0) {
                    nameView.setText(R.string.catalog_category_all);
                    iconView.setImageResource(R.drawable.ic_category_all);
                    holder.itemView.setOnClickListener(v -> navigateToExploreCategory(-1, getString(R.string.catalog_category_all)));
                } else {
                    Category category = categories.get(position - 1);
                    nameView.setText(CategoryIconResolver.getStreamlinedName(category.getName()));
                    iconView.setImageResource(CategoryIconResolver.getCategoryIcon(category.getName()));
                    holder.itemView.setOnClickListener(v -> navigateToExploreCategory(category.getId(), category.getName()));
                }
            }

            @Override
            public int getItemCount() {
                return categories.size() + 1;
            }
        });
    }

    private int getCategoryIcon(String categoryName) {
        return CategoryIconResolver.getCategoryIcon(categoryName);
    }

    private void setupClickListeners() {
        // Pill search bar: navigate to Explore
        binding.barPillSearch.setOnClickListener(v -> navigateToExplore());

        // Hero CTA button: navigate to Explore
        binding.btnHeroCta.setOnClickListener(v -> navigateToExplore());

        // Section See All buttons: navigate to Explore
        binding.btnTrendingViewAll.setOnClickListener(v -> navigateToExplore());
        binding.btnBestsellersViewAll.setOnClickListener(v -> navigateToExplore());
        binding.btnSpecialOffersViewAll.setOnClickListener(v -> navigateToExplore());
        binding.btnEssentialsViewAll.setOnClickListener(v -> navigateToExplore());

        // Theme toggle button in circular frame
        binding.btnProfileAvatar.setOnClickListener(v -> {
            ThemeManager.toggleTheme(requireContext());
            updateThemeIcon();
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshHome.setColorSchemeResources(R.color.color_botanical_green);
        binding.swipeRefreshHome.setOnRefreshListener(() -> {
            catalogViewModel.reload();
            categoryViewModel.loadCategories();
        });
    }

    private void observeViewModel() {
        catalogViewModel.getProductsState().observe(getViewLifecycleOwner(), state -> {
            if (binding != null && state != null && state.getStatus() != UiState.Status.LOADING) {
                binding.swipeRefreshHome.setRefreshing(false);
            }
            if (state != null && state.getStatus() == UiState.Status.CONTENT && state.getData() != null) {
                List<ProductSummary> all = state.getData();
                if (!all.isEmpty()) {
                    int total = all.size();
                    if (binding.rvTrendingCarousel.getAdapter() != trendingCarouselAdapter) {
                        binding.rvTrendingCarousel.setAdapter(trendingCarouselAdapter);
                    }
                    trendingCarouselAdapter.submitList(all.subList(0, Math.min(8, total)));

                    if (binding.rvBestsellersGrid.getAdapter() != bestSellersAdapter) {
                        binding.rvBestsellersGrid.setAdapter(bestSellersAdapter);
                    }
                    if (total > 8) {
                        bestSellersAdapter.submitList(all.subList(8, Math.min(12, total)));
                    } else {
                        bestSellersAdapter.submitList(all.subList(0, Math.min(4, total)));
                    }

                    if (binding.rvSpecialOffersCarousel.getAdapter() != specialOffersAdapter) {
                        binding.rvSpecialOffersCarousel.setAdapter(specialOffersAdapter);
                    }
                    if (total > 12) {
                        specialOffersAdapter.submitList(all.subList(12, Math.min(20, total)));
                    } else {
                        specialOffersAdapter.submitList(all.subList(Math.max(0, total - 4), total));
                    }

                    if (binding.rvEssentialsGrid.getAdapter() != essentialsAdapter) {
                        binding.rvEssentialsGrid.setAdapter(essentialsAdapter);
                    }
                    if (total > 20) {
                        essentialsAdapter.submitList(all.subList(20, Math.min(24, total)));
                    } else {
                        essentialsAdapter.submitList(all.subList(0, Math.min(4, total)));
                    }

                    // Proactive background prefetching for top section product cards
                    List<String> preloadUrls = new ArrayList<>();
                    int preloadCount = Math.min(12, total);
                    for (int i = 0; i < preloadCount; i++) {
                        ProductSummary p = all.get(i);
                        if (p != null && p.getImageUrl() != null && !p.getImageUrl().trim().isEmpty()) {
                            preloadUrls.add(p.getImageUrl());
                        }
                    }
                    ImageLoaderHelper.preloadBatch(getContext(), preloadUrls);
                }
            }
        });
    }

    private void onProductClicked(@NonNull ProductSummary product) {
        if (!isAdded()) {
            return;
        }
        Bundle args = new Bundle();
        args.putLong("productId", product.getId());
        try {
            Navigation.findNavController(requireView()).navigate(
                    R.id.action_homeFragment_to_productDetailFragment,
                    args
            );
        } catch (Exception e) {
            try {
                Navigation.findNavController(requireView()).navigate(R.id.nav_catalog, args);
            } catch (Exception ignored) {
            }
        }
    }

    private void navigateToExplore() {
        navigateToExploreCategory(-1, null);
    }

    private void navigateToExploreCategory(long categoryId, @Nullable String categoryName) {
        if (!isAdded()) {
            return;
        }
        Bundle args = new Bundle();
        if (categoryId != -1) {
            args.putLong("categoryId", categoryId);
            args.putString("categoryName", categoryName);
        }
        Navigation.findNavController(requireView()).navigate(
                R.id.action_homeFragment_to_exploreFragment,
                args
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
