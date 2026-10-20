package com.averonlabs.eshopverse.catalog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Immutable query parameter object for product catalog requests per docs/android/PHASE-1-SPEC.md §E.3.
 * Enforces pagination rules (perPage=20) and automatic page resets on filter modifications.
 */
public final class ProductParams {

    public static final String SORT_NEWEST = "newest";
    public static final String SORT_PRICE_ASC = "price_asc";
    public static final String SORT_PRICE_DESC = "price_desc";
    public static final String SORT_NAME_ASC = "name_asc";

    public static final int DEFAULT_PER_PAGE = 20;

    private final int page;
    private final int perPage;
    @Nullable private final String q;
    @Nullable private final Long categoryId;
    @Nullable private final Boolean inStock;
    @Nullable private final Double minPrice;
    @Nullable private final Double maxPrice;
    @NonNull private final String sort;

    public ProductParams() {
        this(1, DEFAULT_PER_PAGE, null, null, null, null, null, SORT_NEWEST);
    }

    public ProductParams(
            int page,
            int perPage,
            @Nullable String q,
            @Nullable Long categoryId,
            @Nullable Boolean inStock,
            @Nullable String sort
    ) {
        this(page, perPage, q, categoryId, inStock, null, null, sort);
    }

    public ProductParams(
            int page,
            int perPage,
            @Nullable String q,
            @Nullable Long categoryId,
            @Nullable Boolean inStock,
            @Nullable Double minPrice,
            @Nullable Double maxPrice,
            @Nullable String sort
    ) {
        this.page = Math.max(1, page);
        this.perPage = perPage > 0 ? perPage : DEFAULT_PER_PAGE;
        this.q = sanitizeQuery(q);
        this.categoryId = categoryId != null && categoryId > 0 ? categoryId : null;
        this.inStock = inStock;
        this.minPrice = minPrice != null && minPrice >= 0.0 ? minPrice : null;
        this.maxPrice = maxPrice != null && maxPrice >= 0.0 ? maxPrice : null;
        this.sort = sanitizeSort(sort);
    }

    public int getPage() {
        return page;
    }

    public int getPerPage() {
        return perPage;
    }

    @Nullable
    public String getQ() {
        return q;
    }

    @Nullable
    public Long getCategoryId() {
        return categoryId;
    }

    @Nullable
    public Boolean getInStock() {
        return inStock;
    }

    @Nullable
    public Double getMinPrice() {
        return minPrice;
    }

    @Nullable
    public Double getMaxPrice() {
        return maxPrice;
    }

    @NonNull
    public String getSort() {
        return sort;
    }

    /**
     * Calculates the number of user filters currently active that differ from default.
     */
    public int getActiveFilterCount() {
        int count = 0;
        if (!SORT_NEWEST.equals(sort)) {
            count++;
        }
        if (Boolean.TRUE.equals(inStock)) {
            count++;
        }
        if (minPrice != null && minPrice > 0.0) {
            count++;
        }
        if (maxPrice != null && maxPrice < 200.0) {
            count++;
        }
        return count;
    }

    /**
     * Updates the search query, trimming and collapsing whitespace, and resets page to 1.
     */
    @NonNull
    public ProductParams withQuery(@Nullable String newQuery) {
        return new ProductParams(1, perPage, newQuery, categoryId, inStock, minPrice, maxPrice, sort);
    }

    /**
     * Updates the category filter and resets page to 1.
     */
    @NonNull
    public ProductParams withCategoryId(@Nullable Long newCategoryId) {
        return new ProductParams(1, perPage, q, newCategoryId, inStock, minPrice, maxPrice, sort);
    }

    /**
     * Updates the sort order and resets page to 1.
     */
    @NonNull
    public ProductParams withSort(@Nullable String newSort) {
        return new ProductParams(1, perPage, q, categoryId, inStock, minPrice, maxPrice, newSort);
    }

    /**
     * Updates the in-stock filter and resets page to 1.
     */
    @NonNull
    public ProductParams withInStock(@Nullable Boolean newInStock) {
        return new ProductParams(1, perPage, q, categoryId, newInStock, minPrice, maxPrice, sort);
    }

    /**
     * Updates the price range filter and resets page to 1.
     */
    @NonNull
    public ProductParams withPriceRange(@Nullable Double newMinPrice, @Nullable Double newMaxPrice) {
        return new ProductParams(1, perPage, q, categoryId, inStock, newMinPrice, newMaxPrice, sort);
    }

    /**
     * Advances to the next page, preserving all filter criteria.
     */
    @NonNull
    public ProductParams nextPage() {
        return new ProductParams(page + 1, perPage, q, categoryId, inStock, minPrice, maxPrice, sort);
    }

    /**
     * Sets a specific page number, preserving all filter criteria.
     */
    @NonNull
    public ProductParams withPage(int newPage) {
        return new ProductParams(newPage, perPage, q, categoryId, inStock, minPrice, maxPrice, sort);
    }

    @Nullable
    private static String sanitizeQuery(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim().replaceAll("\\s+", " ");
        return trimmed.isEmpty() ? null : trimmed;
    }

    @NonNull
    private static String sanitizeSort(@Nullable String raw) {
        if (raw == null) {
            return SORT_NEWEST;
        }
        switch (raw.trim()) {
            case SORT_PRICE_ASC:
                return SORT_PRICE_ASC;
            case SORT_PRICE_DESC:
                return SORT_PRICE_DESC;
            case SORT_NAME_ASC:
                return SORT_NAME_ASC;
            case SORT_NEWEST:
            default:
                return SORT_NEWEST;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductParams that = (ProductParams) o;
        return page == that.page &&
                perPage == that.perPage &&
                Objects.equals(q, that.q) &&
                Objects.equals(categoryId, that.categoryId) &&
                Objects.equals(inStock, that.inStock) &&
                Objects.equals(minPrice, that.minPrice) &&
                Objects.equals(maxPrice, that.maxPrice) &&
                Objects.equals(sort, that.sort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, perPage, q, categoryId, inStock, minPrice, maxPrice, sort);
    }

    @NonNull
    @Override
    public String toString() {
        return "ProductParams{" +
                "page=" + page +
                ", perPage=" + perPage +
                ", q='" + q + '\'' +
                ", categoryId=" + categoryId +
                ", inStock=" + inStock +
                ", minPrice=" + minPrice +
                ", maxPrice=" + maxPrice +
                ", sort='" + sort + '\'' +
                '}';
    }
}
