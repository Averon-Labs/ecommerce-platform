package com.averonlabs.eshopverse.catalog.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import androidx.appcompat.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.catalog.CatalogListViewModel;
import com.averonlabs.eshopverse.catalog.CatalogViewModelFactory;
import com.averonlabs.eshopverse.catalog.CategoryIconResolver;
import com.averonlabs.eshopverse.catalog.CategoryViewModel;
import com.averonlabs.eshopverse.catalog.ProductGridAdapter;
import com.averonlabs.eshopverse.catalog.ProductParams;
import com.averonlabs.eshopverse.core.model.Category;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.state.UiState;
import com.averonlabs.eshopverse.core.ui.view.StateContainerView;
import com.averonlabs.eshopverse.databinding.FragmentExploreBinding;
import com.google.android.material.chip.Chip;

import java.util.List;

/**
 * M-02 Explore / All products screen per docs/android/PHASE-1-SPEC.md §E.
 */
public class ExploreFragment extends Fragment {

    private FragmentExploreBinding binding;
    private CatalogListViewModel catalogViewModel;
    private CategoryViewModel categoryViewModel;
    private ProductGridAdapter gridAdapter;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentExploreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CatalogViewModelFactory factory = new CatalogViewModelFactory(requireContext());
        catalogViewModel = new ViewModelProvider(this, factory).get(CatalogListViewModel.class);
        categoryViewModel = new ViewModelProvider(this, factory).get(CategoryViewModel.class);

        binding.stateContainer.setContentView(binding.swipeRefreshExplore);
        setupProductGrid();
        setupSearchAndSort();
        setupCategoryChips();
        setupSwipeRefresh();

        observeViewModel();

        handleIncomingArguments();

        catalogViewModel.loadFirstPage();
        categoryViewModel.loadCategories();
    }

    private void handleIncomingArguments() {
        Bundle args = getArguments();
        if (args != null && args.containsKey("categoryId")) {
            long categoryId = args.getLong("categoryId", -1);
            if (categoryId != -1) {
                categoryViewModel.selectCategory(categoryId);
                catalogViewModel.setCategoryId(categoryId);
            } else {
                categoryViewModel.selectCategory(null);
                catalogViewModel.setCategoryId(null);
            }
        }
    }

    private void setupProductGrid() {
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        binding.rvProducts.setLayoutManager(layoutManager);

        gridAdapter = new ProductGridAdapter(this::onProductClicked);
        binding.rvProducts.setAdapter(gridAdapter);

        binding.rvProducts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    int visibleCount = layoutManager.getChildCount();
                    int totalCount = layoutManager.getItemCount();
                    int firstVisible = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleCount + firstVisible) >= totalCount - 4) {
                        catalogViewModel.loadNextPage();
                    }
                }
            }
        });
    }

    private void setupSearchAndSort() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                String query = s != null ? s.toString().trim() : "";
                searchRunnable = () -> {
                    if (isAdded() && binding != null) {
                        if (query.length() >= 2) {
                            catalogViewModel.setQuery(query);
                        } else if (query.isEmpty()) {
                            catalogViewModel.setQuery(null);
                        }
                    }
                };
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                String query = binding.etSearch.getText() != null ? binding.etSearch.getText().toString().trim() : "";
                catalogViewModel.setQuery(query.isEmpty() ? null : query);

                if (getActivity() != null) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
                return true;
            }
            return false;
        });

        binding.btnSort.setOnClickListener(v -> showSortMenu(v));
    }

    private void showSortMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenu().add(0, 1, 0, R.string.catalog_sort_newest);
        popup.getMenu().add(0, 2, 1, R.string.catalog_sort_price_asc);
        popup.getMenu().add(0, 3, 2, R.string.catalog_sort_price_desc);
        popup.getMenu().add(0, 4, 3, R.string.catalog_sort_name_asc);

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    catalogViewModel.setSort(ProductParams.SORT_NEWEST);
                    return true;
                case 2:
                    catalogViewModel.setSort(ProductParams.SORT_PRICE_ASC);
                    return true;
                case 3:
                    catalogViewModel.setSort(ProductParams.SORT_PRICE_DESC);
                    return true;
                case 4:
                    catalogViewModel.setSort(ProductParams.SORT_NAME_ASC);
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    private void setupCategoryChips() {
        categoryViewModel.getCategoriesState().observe(getViewLifecycleOwner(), state -> {
            if (state != null && state.isContent() && state.getData() != null) {
                renderCategoryChips(state.getData());
            }
        });
        categoryViewModel.getSelectedCategoryId().observe(getViewLifecycleOwner(), selectedId -> {
            if (binding.rvCategories.getAdapter() != null) {
                binding.rvCategories.getAdapter().notifyDataSetChanged();
            }
        });
    }

    private void renderCategoryChips(List<Category> categories) {
        binding.rvCategories.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View chipView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_category_chip, parent, false);
                return new RecyclerView.ViewHolder(chipView) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                Chip chip = (Chip) holder.itemView;
                if (position == 0) {
                    String label = getString(R.string.catalog_category_all);
                    chip.setText(label);
                    chip.setContentDescription(getString(R.string.catalog_cd_category_chip, label));
                    chip.setChipIconResource(R.drawable.ic_category_all);
                    chip.setChipIconVisible(true);
                    chip.setCheckedIconVisible(false);
                    chip.setChipIconTintResource(R.color.chip_filter_text);
                    chip.setChecked(categoryViewModel.getSelectedCategoryId().getValue() == null);
                    chip.setOnClickListener(v -> {
                        categoryViewModel.selectCategory(null);
                        catalogViewModel.setCategoryId(null);
                    });
                } else {
                    Category category = categories.get(position - 1);
                    chip.setText(category.getName());
                    chip.setContentDescription(getString(R.string.catalog_cd_category_chip, category.getName()));
                    chip.setChipIconResource(CategoryIconResolver.getCategoryIcon(category.getName()));
                    chip.setChipIconVisible(true);
                    chip.setCheckedIconVisible(false);
                    chip.setChipIconTintResource(R.color.chip_filter_text);
                    Long currentSelected = categoryViewModel.getSelectedCategoryId().getValue();
                    long catId = (long) category.getId();
                    boolean isSelected = currentSelected != null && currentSelected == catId;
                    chip.setChecked(isSelected);
                    if (isSelected) {
                        binding.rvCategories.post(() -> binding.rvCategories.smoothScrollToPosition(position));
                    }
                    chip.setOnClickListener(v -> {
                        if (isSelected) {
                            categoryViewModel.selectCategory(null);
                            catalogViewModel.setCategoryId(null);
                        } else {
                            categoryViewModel.selectCategory(catId);
                            catalogViewModel.setCategoryId(catId);
                        }
                    });
                }
            }

            @Override
            public int getItemCount() {
                return categories.size() + 1;
            }
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshExplore.setColorSchemeResources(R.color.color_botanical_green);
        binding.swipeRefreshExplore.setOnRefreshListener(() -> {
            catalogViewModel.reload();
            categoryViewModel.loadCategories();
        });
    }

    private void observeViewModel() {
        catalogViewModel.getProductsState().observe(getViewLifecycleOwner(), this::renderProductsState);
    }

    private void renderProductsState(UiState<List<ProductSummary>> state) {
        if (state == null) {
            return;
        }

        if (binding != null && state.getStatus() != UiState.Status.LOADING) {
            binding.swipeRefreshExplore.setRefreshing(false);
        }

        switch (state.getStatus()) {
            case LOADING:
                if (!binding.swipeRefreshExplore.isRefreshing()) {
                    binding.stateContainer.setState(StateContainerView.State.LOADING, null, null, null);
                }
                break;
            case CONTENT:
                binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
                if (state.getData() != null) {
                    gridAdapter.submitList(state.getData());
                }
                break;
            case EMPTY:
                binding.stateContainer.setState(
                        StateContainerView.State.EMPTY,
                        getString(R.string.catalog_empty_description),
                        null,
                        null
                );
                break;
            case ERROR:
                binding.stateContainer.setState(
                        StateContainerView.State.ERROR,
                        getString(state.getMessageRes() != 0 ? state.getMessageRes() : R.string.catalog_error_load_products),
                        getString(R.string.catalog_action_retry),
                        catalogViewModel::retry
                );
                break;
        }
    }

    private void onProductClicked(@NonNull ProductSummary product) {
        if (!isAdded()) {
            return;
        }
        Bundle args = new Bundle();
        args.putLong("productId", product.getId());
        try {
            Navigation.findNavController(requireView()).navigate(
                    R.id.action_exploreFragment_to_productDetailFragment,
                    args
            );
        } catch (Exception e) {
            try {
                Navigation.findNavController(requireView()).navigate(R.id.nav_catalog, args);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onDestroyView() {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        super.onDestroyView();
        binding = null;
    }
}
