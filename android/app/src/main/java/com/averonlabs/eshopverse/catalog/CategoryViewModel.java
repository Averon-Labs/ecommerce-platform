package com.averonlabs.eshopverse.catalog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.error.ErrorMessages;
import com.averonlabs.eshopverse.core.model.Category;
import com.averonlabs.eshopverse.core.net.ApiException;
import com.averonlabs.eshopverse.core.state.UiState;

import java.util.List;
import java.util.Objects;

/**
 * ViewModel managing the category navigation list per docs/android/PHASE-1-SPEC.md §E.
 */
public class CategoryViewModel extends ViewModel {

    private final CatalogRepository repository;

    private final MutableLiveData<UiState<List<Category>>> categoriesState = new MutableLiveData<>();
    private final MutableLiveData<Long> selectedCategoryId = new MutableLiveData<>(null);
    private boolean inFlight = false;

    public CategoryViewModel(@NonNull CatalogRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
    }

    @NonNull
    public LiveData<UiState<List<Category>>> getCategoriesState() {
        return categoriesState;
    }

    @NonNull
    public LiveData<Long> getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void selectCategory(@Nullable Long categoryId) {
        selectedCategoryId.setValue(categoryId);
    }

    public void loadCategories() {
        if (inFlight) {
            return;
        }
        inFlight = true;
        categoriesState.setValue(UiState.loading());

        repository.listCategories(new CatalogRepository.CatalogCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> result) {
                inFlight = false;
                if (result == null || result.isEmpty()) {
                    categoriesState.setValue(UiState.empty());
                } else {
                    categoriesState.setValue(UiState.content(result));
                }
            }

            @Override
            public void onError(Throwable throwable) {
                inFlight = false;
                int messageRes = R.string.catalog_error_load_categories;
                if (throwable instanceof ApiException) {
                    messageRes = ErrorMessages.getMessageRes(((ApiException) throwable).getCode());
                }
                categoriesState.setValue(UiState.error(messageRes, true));
            }
        });
    }
}
