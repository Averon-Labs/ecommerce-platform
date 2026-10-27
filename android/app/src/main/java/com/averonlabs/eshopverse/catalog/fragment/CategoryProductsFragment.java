package com.averonlabs.eshopverse.catalog.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.catalog.CatalogListViewModel;
import com.averonlabs.eshopverse.catalog.CatalogViewModelFactory;
import com.averonlabs.eshopverse.catalog.ProductGridAdapter;
import com.averonlabs.eshopverse.catalog.ProductParams;
import com.averonlabs.eshopverse.core.error.ApiErrorCodes;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.state.UiState;
import com.averonlabs.eshopverse.core.ui.SkeletonAdapter;
import com.averonlabs.eshopverse.core.ui.view.StateContainerView;
import com.averonlabs.eshopverse.databinding.FragmentCategoryProductsBinding;

import java.util.List;

/**
 * M-04 Category products screen per docs/android/PHASE-1-SPEC.md §E.3 and §E.6.
 */
public class CategoryProductsFragment extends Fragment {

    private FragmentCategoryProductsBinding binding;
    private CatalogListViewModel viewModel;
    private ProductGridAdapter gridAdapter;
    private long categoryId = -1;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentCategoryProductsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CatalogViewModelFactory factory = new CatalogViewModelFactory(requireContext());
        viewModel = new ViewModelProvider(this, factory).get(CatalogListViewModel.class);

        if (getArguments() != null) {
            categoryId = getArguments().getLong("categoryId", -1);
            String categoryName = getArguments().getString("categoryName", "");
            if (!categoryName.isEmpty()) {
                binding.tvCategoryTitle.setText(categoryName);
            }
        }

        binding.stateContainer.setContentView(binding.rvProducts);
        setupProductGrid();
        setupSort();
        observeViewModel();

        if (categoryId > 0) {
            viewModel.setParams(new ProductParams().withCategoryId(categoryId));
        } else {
            binding.stateContainer.setState(
                    StateContainerView.State.EMPTY,
                    getString(R.string.catalog_category_not_found_description),
                    getString(R.string.catalog_action_explore),
                    this::navigateToExplore
            );
        }
    }

    private void setupProductGrid() {
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        binding.rvProducts.setLayoutManager(layoutManager);

        gridAdapter = new ProductGridAdapter(this::onProductClicked);
        binding.rvProducts.setAdapter(new SkeletonAdapter(R.layout.item_skeleton_product_card, 6));

        binding.rvProducts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (binding.rvProducts.getAdapter() != gridAdapter) {
                    return;
                }
                if (dy > 0) {
                    int visibleCount = layoutManager.getChildCount();
                    int totalCount = layoutManager.getItemCount();
                    int firstVisible = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleCount + firstVisible) >= totalCount - 4) {
                        viewModel.loadNextPage();
                    }
                }
            }
        });
    }

    private void setupSort() {
        binding.btnSort.setOnClickListener(this::showSortMenu);
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
                    viewModel.setSort(ProductParams.SORT_NEWEST);
                    return true;
                case 2:
                    viewModel.setSort(ProductParams.SORT_PRICE_ASC);
                    return true;
                case 3:
                    viewModel.setSort(ProductParams.SORT_PRICE_DESC);
                    return true;
                case 4:
                    viewModel.setSort(ProductParams.SORT_NAME_ASC);
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    private void observeViewModel() {
        viewModel.getProductsState().observe(getViewLifecycleOwner(), this::renderState);
    }

    private void renderState(UiState<List<ProductSummary>> state) {
        if (state == null) {
            return;
        }

        switch (state.getStatus()) {
            case LOADING:
                if (binding.rvProducts.getAdapter() != gridAdapter || gridAdapter.getItemCount() == 0) {
                    binding.rvProducts.setAdapter(new SkeletonAdapter(R.layout.item_skeleton_product_card, 6));
                }
                binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
                break;
            case CONTENT:
                binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
                if (binding.rvProducts.getAdapter() != gridAdapter) {
                    binding.rvProducts.setAdapter(gridAdapter);
                }
                if (state.getData() != null) {
                    gridAdapter.submitList(state.getData());
                }
                break;
            case EMPTY:
                binding.stateContainer.setState(
                        StateContainerView.State.EMPTY,
                        getString(R.string.catalog_empty_category_description),
                        getString(R.string.catalog_action_explore),
                        this::navigateToExplore
                );
                break;
            case ERROR:
                String errorCode = viewModel.getErrorCode().getValue();
                if (ApiErrorCodes.CATEGORY_NOT_FOUND.equals(errorCode)) {
                    // CATEGORY_NOT_FOUND per §E.3: shows empty/error and link back to Explore without retry loop
                    binding.stateContainer.setState(
                            StateContainerView.State.ERROR,
                            getString(R.string.catalog_category_not_found_description),
                            getString(R.string.catalog_action_explore),
                            this::navigateToExplore
                    );
                } else {
                    binding.stateContainer.setState(
                            StateContainerView.State.ERROR,
                            getString(state.getMessageRes() != 0 ? state.getMessageRes() : R.string.catalog_error_load_products),
                            getString(R.string.catalog_action_retry),
                            viewModel::retry
                    );
                }
                break;
        }
    }

    private void navigateToExplore() {
        Navigation.findNavController(requireView()).navigateUp();
    }

    private void onProductClicked(@NonNull ProductSummary product) {
        if (!isAdded()) {
            return;
        }
        Bundle args = ProductDetailFragment.createArgs(product);
        try {
            Navigation.findNavController(requireView()).navigate(
                    R.id.action_categoryProductsFragment_to_productDetailFragment,
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
        super.onDestroyView();
        binding = null;
    }
}
