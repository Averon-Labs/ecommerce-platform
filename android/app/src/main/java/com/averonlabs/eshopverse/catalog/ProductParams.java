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
    @NonNull private final String sort;

    public ProductParams() {
        this(1, DEFAULT_PER_PAGE, null, null, null, SORT_NEWEST);
    }

    public ProductParams(
            int page,
            int perPage,
            @Nullable String q,
            @Nullable Long categoryId,
            @Nullable Boolean inStock,
            @Nullable String sort
    ) {
        this.page = Math.max(1, page);
        this.perPage = perPage > 0 ? perPage : DEFAULT_PER_PAGE;
        this.q = sanitizeQuery(q);
        this.categoryId = categoryId != null && categoryId > 0 ? categoryId : null;
        this.inStock = inStock;
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

    @NonNull
    public String getSort() {
        return sort;
    }

    /**
     * Updates the search query, trimming and collapsing whitespace, and resets page to 1.
     */
    @NonNull
    public ProductParams withQuery(@Nullable String newQuery) {
        return new ProductParams(1, perPage, newQuery, categoryId, inStock, sort);
    }

    /**
     * Updates the category filter and resets page to 1.
     */
    @NonNull
    public ProductParams withCategoryId(@Nullable Long newCategoryId) {
        return new ProductParams(1, perPage, q, newCategoryId, inStock, sort);
    }

    /**
     * Updates the sort order and resets page to 1.
     */
    @NonNull
    public ProductParams withSort(@Nullable String newSort) {
        return new ProductParams(1, perPage, q, categoryId, inStock, newSort);
    }

    /**
     * Updates the in-stock filter and resets page to 1.
     */
    @NonNull
    public ProductParams withInStock(@Nullable Boolean newInStock) {
        return new ProductParams(1, perPage, q, categoryId, newInStock, sort);
    }

    /**
     * Advances to the next page, preserving all filter criteria.
     */
    @NonNull
    public ProductParams nextPage() {
        return new ProductParams(page + 1, perPage, q, categoryId, inStock, sort);
    }

    /**
     * Sets a specific page number, preserving all filter criteria.
     */
    @NonNull
    public ProductParams withPage(int newPage) {
        return new ProductParams(newPage, perPage, q, categoryId, inStock, sort);
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
                Objects.equals(sort, that.sort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, perPage, q, categoryId, inStock, sort);
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
                ", sort='" + sort + '\'' +
                '}';
    }
}
