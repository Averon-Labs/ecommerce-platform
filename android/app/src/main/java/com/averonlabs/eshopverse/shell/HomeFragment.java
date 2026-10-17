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
import com.averonlabs.eshopverse.catalog.CategoryViewModel;
import com.averonlabs.eshopverse.catalog.ProductGridAdapter;
import com.averonlabs.eshopverse.core.model.Category;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.state.UiState;
import com.averonlabs.eshopverse.core.ui.ThemeManager;
import com.averonlabs.eshopverse.databinding.FragmentHomeBinding;

import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private CatalogListViewModel catalogViewModel;
    private CategoryViewModel categoryViewModel;
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
        categoryViewModel = new ViewModelProvider(this, factory).get(CategoryViewModel.class);

        setupTrendingGrid();
        setupCategories();
        setupClickListeners();
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

    private void setupTrendingGrid() {
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        binding.rvTrendingProducts.setLayoutManager(layoutManager);

        trendingAdapter = new ProductGridAdapter(this::onProductClicked);
        binding.rvTrendingProducts.setAdapter(trendingAdapter);
    }

    private void setupCategories() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.rvHomeCategories.setLayoutManager(layoutManager);

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
                    nameView.setText(category.getName());
                    iconView.setImageResource(getCategoryIcon(category.getName()));
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
        if (categoryName == null) return R.drawable.ic_category_all;
        String lower = categoryName.toLowerCase();
        if (lower.contains("home") || lower.contains("decor") || lower.contains("furn")) {
            return R.drawable.ic_home;
        } else if (lower.contains("fash") || lower.contains("cloth") || lower.contains("apparel") || lower.contains("shoe")) {
            return R.drawable.ic_category_fashion;
        } else if (lower.contains("beaut") || lower.contains("care") || lower.contains("cosmet")) {
            return R.drawable.ic_category_beauty;
        } else if (lower.contains("object") || lower.contains("access") || lower.contains("art")) {
            return R.drawable.ic_category_objects;
        } else {
            return R.drawable.ic_explore;
        }
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
