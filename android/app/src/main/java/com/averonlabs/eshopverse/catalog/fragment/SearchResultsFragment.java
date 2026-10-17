package com.averonlabs.eshopverse.catalog.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.catalog.CatalogListViewModel;
import com.averonlabs.eshopverse.catalog.CatalogViewModelFactory;
import com.averonlabs.eshopverse.catalog.ProductGridAdapter;
import com.averonlabs.eshopverse.catalog.ProductParams;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.state.UiState;
import com.averonlabs.eshopverse.core.ui.view.StateContainerView;
import com.averonlabs.eshopverse.databinding.FragmentSearchResultsBinding;

import java.util.List;

/**
 * M-03 Search results screen per docs/android/PHASE-1-SPEC.md §E.3 and §E.6.
 */
public class SearchResultsFragment extends Fragment {

    private FragmentSearchResultsBinding binding;
    private CatalogListViewModel viewModel;
    private ProductGridAdapter gridAdapter;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentSearchResultsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CatalogViewModelFactory factory = new CatalogViewModelFactory(requireContext());
        viewModel = new ViewModelProvider(this, factory).get(CatalogListViewModel.class);

        String initialQuery = getArguments() != null ? getArguments().getString("query", "") : "";
        if (binding.etSearch.getText() == null || binding.etSearch.getText().toString().isEmpty()) {
            binding.etSearch.setText(initialQuery);
        }

        setupProductGrid();
        setupSearchAndSort();
        observeViewModel();

        viewModel.setParams(new ProductParams().withQuery(initialQuery));
    }

    private void setupProductGrid() {
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        binding.rvProducts.setLayoutManager(layoutManager);

        gridAdapter = new ProductGridAdapter(this::onProductClicked);
        binding.rvProducts.setAdapter(gridAdapter);

        binding.rvProducts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    int visibleCount = layoutManager.getChildCount();
                    int totalCount = layoutManager.getItemCount();
                    int[] firstVisibleItems = layoutManager.findFirstVisibleItemPositions(null);
                    int firstVisible = (firstVisibleItems != null && firstVisibleItems.length > 0) ? firstVisibleItems[0] : 0;

                    if ((visibleCount + firstVisible) >= totalCount - 4) {
                        viewModel.loadNextPage();
                    }
                }
            }
        });
    }

    private void setupSearchAndSort() {
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = binding.etSearch.getText() != null ? binding.etSearch.getText().toString().trim() : "";
                viewModel.setQuery(query);
                return true;
            }
            return false;
        });

        binding.btnClearSearch.setOnClickListener(v -> {
            binding.etSearch.setText("");
            viewModel.setQuery(null);
        });

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

        viewModel.getTotalCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > 0) {
                binding.tvResultsCount.setVisibility(View.VISIBLE);
                binding.tvResultsCount.setText(getString(R.string.catalog_search_results_count, count));
            } else {
                binding.tvResultsCount.setVisibility(View.GONE);
            }
        });

        viewModel.getParams().observe(getViewLifecycleOwner(), params -> {
            boolean hasQuery = params != null && params.getQ() != null && !params.getQ().isEmpty();
            binding.btnClearSearch.setVisibility(hasQuery ? View.VISIBLE : View.GONE);
        });
    }

    private void renderState(UiState<List<ProductSummary>> state) {
        if (state == null) {
            return;
        }

        switch (state.getStatus()) {
            case LOADING:
                binding.stateContainer.setState(StateContainerView.State.LOADING, null, null, null);
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
                        getString(R.string.catalog_empty_search_description),
                        getString(R.string.catalog_action_clear_search),
                        () -> {
                            binding.etSearch.setText("");
                            viewModel.setQuery(null);
                        }
                );
                break;
            case ERROR:
                binding.stateContainer.setState(
                        StateContainerView.State.ERROR,
                        getString(state.getMessageRes() != 0 ? state.getMessageRes() : R.string.catalog_error_load_products),
                        getString(R.string.catalog_action_retry),
                        viewModel::retry
                );
                break;
        }
    }

    private void onProductClicked(@NonNull ProductSummary product) {
        Bundle args = new Bundle();
        args.putLong("productId", product.getId());
        Navigation.findNavController(requireView()).navigate(R.id.productDetailFragment, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
