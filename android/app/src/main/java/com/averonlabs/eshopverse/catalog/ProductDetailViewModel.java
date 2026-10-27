package com.averonlabs.eshopverse.catalog;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.error.ErrorMessages;
import com.averonlabs.eshopverse.core.model.ProductDetail;
import com.averonlabs.eshopverse.core.net.ApiException;
import com.averonlabs.eshopverse.core.state.UiState;

import java.util.Objects;

import androidx.annotation.Nullable;
import com.averonlabs.eshopverse.core.model.ProductSummary;

/**
 * ViewModel managing product details, image carousel index, and quantity stepper state
 * per docs/android/PHASE-1-SPEC.md §E.4.
 */
public class ProductDetailViewModel extends ViewModel {

    private final CatalogRepository repository;

    private final MutableLiveData<UiState<ProductDetail>> productState = new MutableLiveData<>();
    private final MutableLiveData<Integer> quantityLiveData = new MutableLiveData<>(1);
    private final MutableLiveData<Integer> selectedImageIndex = new MutableLiveData<>(0);
    private final MutableLiveData<ProductSummary> optimisticSummary = new MutableLiveData<>(null);

    private long currentProductId = -1;
    private boolean inFlight = false;

    public ProductDetailViewModel(@NonNull CatalogRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
    }

    @NonNull
    public LiveData<UiState<ProductDetail>> getProductState() {
        return productState;
    }

    @NonNull
    public LiveData<Integer> getQuantity() {
        return quantityLiveData;
    }

    @NonNull
    public LiveData<Integer> getSelectedImageIndex() {
        return selectedImageIndex;
    }

    @NonNull
    public LiveData<ProductSummary> getOptimisticSummary() {
        return optimisticSummary;
    }

    public void setOptimisticSummary(@Nullable ProductSummary summary) {
        if (summary != null && optimisticSummary.getValue() == null) {
            optimisticSummary.setValue(summary);
        }
    }

    public void setQuantity(int quantity) {
        quantityLiveData.setValue(Math.max(1, Math.min(99, quantity)));
    }

    public void setSelectedImageIndex(int index) {
        selectedImageIndex.setValue(index);
    }

    public void loadProduct(long productId) {
        this.currentProductId = productId;
        if (inFlight) {
            return;
        }
        inFlight = true;
        productState.setValue(UiState.loading());
        quantityLiveData.setValue(1);
        selectedImageIndex.setValue(0);

        repository.getProduct(productId, new CatalogRepository.CatalogCallback<ProductDetail>() {
            @Override
            public void onSuccess(ProductDetail result) {
                inFlight = false;
                if (result == null) {
                    productState.setValue(UiState.empty());
                } else {
                    productState.setValue(UiState.content(result));
                }
            }

            @Override
            public void onError(Throwable throwable) {
                inFlight = false;
                int messageRes = R.string.catalog_error_load_details;
                if (throwable instanceof ApiException) {
                    messageRes = ErrorMessages.getMessageRes(((ApiException) throwable).getCode());
                }
                productState.setValue(UiState.error(messageRes, true));
            }
        });
    }

    public void retry() {
        if (currentProductId > 0) {
            loadProduct(currentProductId);
        }
    }
}
