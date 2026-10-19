package com.averonlabs.eshopverse.catalog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.error.ErrorMessages;
import com.averonlabs.eshopverse.core.model.Pagination;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.net.ApiException;
import com.averonlabs.eshopverse.core.net.PagedResponse;
import com.averonlabs.eshopverse.core.state.UiState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ViewModel managing catalog product lists, search queries, filtering, sorting,
 * pagination, and duplicate request prevention per docs/android/PHASE-1-SPEC.md §E.
 */
public class CatalogListViewModel extends ViewModel {

    private final CatalogRepository repository;

    private final MutableLiveData<UiState<List<ProductSummary>>> productsState = new MutableLiveData<>();
    private final MutableLiveData<ProductParams> paramsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalCountLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorCodeLiveData = new MutableLiveData<>();

    private final List<ProductSummary> loadedProducts = new ArrayList<>();
    private ProductParams currentParams;
    private boolean hasMore = false;
    private boolean inFlight = false;

    public CatalogListViewModel(@NonNull CatalogRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.currentParams = new ProductParams();
        this.paramsLiveData.setValue(currentParams);
    }

    @NonNull
    public LiveData<UiState<List<ProductSummary>>> getProductsState() {
        return productsState;
    }

    @NonNull
    public LiveData<ProductParams> getParams() {
        return paramsLiveData;
    }

    @NonNull
    public LiveData<Integer> getTotalCount() {
        return totalCountLiveData;
    }

    @NonNull
    public LiveData<String> getErrorCode() {
        return errorCodeLiveData;
    }

    public boolean hasMore() {
        return hasMore;
    }

    public boolean isInFlight() {
        return inFlight;
    }

    @NonNull
    public ProductParams getCurrentParams() {
        return currentParams;
    }

    /**
     * Initializes or reloads products with the given parameters.
     */
    public void setParams(@NonNull ProductParams params) {
        this.currentParams = params;
        this.paramsLiveData.setValue(params);
        loadFirstPage();
    }

    private int activeRequestId = 0;

    /**
     * Updates the search query and resets pagination to page 1.
     */
    public void setQuery(@Nullable String query) {
        ProductParams updated = currentParams.withQuery(query);
        if (!Objects.equals(currentParams.getQ(), updated.getQ())) {
            this.currentParams = updated;
            this.paramsLiveData.setValue(currentParams);
            this.inFlight = false;
            loadFirstPage();
        }
    }

    /**
     * Updates the category filter and resets pagination to page 1.
     */
    public void setCategoryId(@Nullable Long categoryId) {
        ProductParams updated = currentParams.withCategoryId(categoryId);
        if (!Objects.equals(currentParams.getCategoryId(), updated.getCategoryId())) {
            this.currentParams = updated;
            this.paramsLiveData.setValue(currentParams);
            this.inFlight = false;
            loadFirstPage();
        }
    }

    /**
     * Updates the sort order and resets pagination to page 1.
     */
    public void setSort(@NonNull String sort) {
        ProductParams updated = currentParams.withSort(sort);
        if (!Objects.equals(currentParams.getSort(), updated.getSort())) {
            this.currentParams = updated;
            this.paramsLiveData.setValue(currentParams);
            this.inFlight = false;
            loadFirstPage();
        }
    }

    /**
     * Loads the first page of products, clearing any existing pagination state.
     */
    public void loadFirstPage() {
        if (inFlight) {
            return;
        }
        currentParams = currentParams.withPage(1);
        paramsLiveData.setValue(currentParams);
        fetchProducts(true);
    }

    /**
     * Reloads the product catalog from scratch (alias for loadFirstPage).
     */
    public void reload() {
        loadFirstPage();
    }

    /**
     * Requests the next page of products if more items exist and no request is in-flight.
     */
    public void loadNextPage() {
        if (inFlight || !hasMore) {
            return;
        }
        currentParams = currentParams.nextPage();
        paramsLiveData.setValue(currentParams);
        fetchProducts(false);
    }

    /**
     * Retries the current request.
     */
    public void retry() {
        if (inFlight) {
            return;
        }
        fetchProducts(currentParams.getPage() == 1);
    }

    private void fetchProducts(boolean isFirstPage) {
        final int requestId = ++activeRequestId;
        inFlight = true;
        errorCodeLiveData.setValue(null);

        if (isFirstPage) {
            productsState.setValue(UiState.loading());
        }

        repository.listProducts(currentParams, new CatalogRepository.CatalogCallback<PagedResponse<ProductSummary>>() {
            @Override
            public void onSuccess(PagedResponse<ProductSummary> result) {
                if (requestId != activeRequestId) {
                    return;
                }
                inFlight = false;
                List<ProductSummary> newItems = result.getData();
                Pagination pagination = result.getPagination();

                if (isFirstPage) {
                    loadedProducts.clear();
                }
                loadedProducts.addAll(newItems);

                if (pagination != null) {
                    hasMore = pagination.getCurrentPage() < pagination.getTotalPages();
                    totalCountLiveData.setValue(pagination.getTotalItems());
                } else {
                    hasMore = false;
                    totalCountLiveData.setValue(loadedProducts.size());
                }

                if (loadedProducts.isEmpty()) {
                    productsState.setValue(UiState.empty());
                } else {
                    productsState.setValue(UiState.content(Collections.unmodifiableList(new ArrayList<>(loadedProducts))));
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (requestId != activeRequestId) {
                    return;
                }
                inFlight = false;
                int messageRes = R.string.catalog_error_load_products;
                if (throwable instanceof ApiException) {
                    ApiException apiException = (ApiException) throwable;
                    errorCodeLiveData.setValue(apiException.getCode());
                    messageRes = ErrorMessages.getMessageRes(apiException.getCode());
                }

                if (isFirstPage || loadedProducts.isEmpty()) {
                    productsState.setValue(UiState.error(messageRes, true));
                } else {
                    // Retain current items if append failed
                    productsState.setValue(UiState.content(Collections.unmodifiableList(new ArrayList<>(loadedProducts))));
                }
            }
        });
    }
}
