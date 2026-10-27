package com.averonlabs.eshopverse.core.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.averonlabs.eshopverse.core.money.Money;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

/**
 * ProductSummary model derived from docs/openapi.yaml ProductSummary schema.
 */
public class ProductSummary implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @SerializedName("id")
    private final int id;

    @SerializedName("name")
    private final String name;

    @SerializedName("price")
    private final String price;

    @SerializedName("currency")
    private final Currency currency;

    @SerializedName("categoryId")
    private final Integer categoryId;

    @SerializedName("categoryName")
    private final String categoryName;

    @SerializedName("imageUrl")
    private final String imageUrl;

    @SerializedName("inStock")
    private final boolean inStock;

    @SerializedName("status")
    private final ProductStatus status;

    public ProductSummary(int id, @NonNull String name, @NonNull String price, @NonNull Currency currency,
                          @Nullable Integer categoryId, @Nullable String categoryName,
                          @Nullable String imageUrl, boolean inStock, @Nullable ProductStatus status) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.price = Objects.requireNonNull(price, "price cannot be null");
        this.currency = Objects.requireNonNull(currency, "currency cannot be null");
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.imageUrl = imageUrl;
        this.inStock = inStock;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getPrice() {
        return price;
    }

    @NonNull
    public Money getPriceMoney() {
        return Money.parse(price);
    }

    @NonNull
    public Currency getCurrency() {
        return currency;
    }

    @Nullable
    public Integer getCategoryId() {
        return categoryId;
    }

    @Nullable
    public String getCategoryName() {
        return categoryName;
    }

    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isInStock() {
        return inStock;
    }

    @Nullable
    public ProductStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductSummary that = (ProductSummary) o;
        return id == that.id &&
                inStock == that.inStock &&
                Objects.equals(name, that.name) &&
                Objects.equals(price, that.price) &&
                currency == that.currency &&
                Objects.equals(categoryId, that.categoryId) &&
                Objects.equals(categoryName, that.categoryName) &&
                Objects.equals(imageUrl, that.imageUrl) &&
                status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, price, currency, categoryId, categoryName, imageUrl, inStock, status);
    }
}
