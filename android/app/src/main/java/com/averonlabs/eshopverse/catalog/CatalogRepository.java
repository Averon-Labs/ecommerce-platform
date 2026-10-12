package com.averonlabs.eshopverse.catalog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.averonlabs.eshopverse.core.model.Category;
import com.averonlabs.eshopverse.core.model.ProductDetail;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.net.ApiErrorMapper;
import com.averonlabs.eshopverse.core.net.ApiException;
import com.averonlabs.eshopverse.core.net.ApiResponse;
import com.averonlabs.eshopverse.core.net.ApiRoutes;
import com.averonlabs.eshopverse.core.net.PagedResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository coordinating product and category catalog operations per docs/android/PHASE-1-SPEC.md §E.
 */
public class CatalogRepository {

    public interface CatalogCallback<T> {
        void onSuccess(T result);
        void onError(Throwable throwable);
    }

    private final ApiRoutes apiRoutes;

    public CatalogRepository(@NonNull ApiRoutes apiRoutes) {
        this.apiRoutes = Objects.requireNonNull(apiRoutes, "apiRoutes cannot be null");
    }

    /**
     * Retrieves the list of active product categories (GET /categories).
     */
    public void listCategories(@NonNull CatalogCallback<List<Category>> callback) {
        apiRoutes.listCategories().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<List<Category>>> call,
                    @NonNull Response<ApiResponse<List<Category>>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Category> data = response.body().getData();
                    callback.onSuccess(data != null ? data : Collections.emptyList());
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<List<Category>>> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t);
            }
        });
    }

    /**
     * Lists, searches, and paginates products matching the provided query parameters (GET /products).
     */
    public void listProducts(
            @NonNull ProductParams params,
            @NonNull CatalogCallback<PagedResponse<ProductSummary>> callback
    ) {
        apiRoutes.listProducts(
                params.getPage(),
                params.getPerPage(),
                params.getCategoryId(),
                params.getQ(),
                params.getSort()
        ).enqueue(new Callback<PagedResponse<ProductSummary>>() {
            @Override
            public void onResponse(
                    @NonNull Call<PagedResponse<ProductSummary>> call,
                    @NonNull Response<PagedResponse<ProductSummary>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<PagedResponse<ProductSummary>> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t);
            }
        });
    }

    /**
     * Fetches detailed information for a single product by ID (GET /products/{productId}).
     */
    public void getProduct(long productId, @NonNull CatalogCallback<ProductDetail> callback) {
        apiRoutes.getProduct(productId).enqueue(new Callback<ApiResponse<ProductDetail>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<ProductDetail>> call,
                    @NonNull Response<ApiResponse<ProductDetail>> response
            ) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    callback.onSuccess(response.body().getData());
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<ApiResponse<ProductDetail>> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t);
            }
        });
    }

    @NonNull
    private ApiException parseError(@NonNull Response<?> response) {
        String body = null;
        if (response.errorBody() != null) {
            try {
                body = response.errorBody().string();
            } catch (IOException ignored) {
            }
        }
        return ApiErrorMapper.map(response.code(), body);
    }
}
