package com.averonlabs.eshopverse.catalog.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.money.MoneyFormat;
import com.averonlabs.eshopverse.core.ui.ImageLoaderHelper;
import com.averonlabs.eshopverse.core.ui.view.PriceTextView;
import com.averonlabs.eshopverse.core.ui.view.StatusBadgeView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

/**
 * Reusable product card component per docs/android/PHASE-1-SPEC.md §E.4.
 * Strictly presents product image, name, and USD price with availability.
 * Contains no ratings, favourite hearts, discount stickers, or add actions.
 */
public class ProductCardView extends MaterialCardView {

    private ImageView imageView;
    private MaterialTextView nameView;
    private PriceTextView priceView;
    private StatusBadgeView outOfStockBadge;

    public ProductCardView(@NonNull Context context) {
        this(context, null);
    }

    public ProductCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, com.google.android.material.R.attr.materialCardViewElevatedStyle);
    }

    public ProductCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {
        LayoutInflater.from(context).inflate(R.layout.item_product_card, this, true);
        imageView = findViewById(R.id.iv_product_image);
        nameView = findViewById(R.id.tv_product_name);
        priceView = findViewById(R.id.tv_product_price);
        outOfStockBadge = findViewById(R.id.badge_out_of_stock);
    }

    public void bind(@NonNull ProductSummary product) {
        nameView.setText(product.getName());
        String formattedPrice = MoneyFormat.format(product.getPriceMoney());
        priceView.setFormattedPrice(formattedPrice);
        priceView.setTextColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.color_botanical_green));

        String stockLabel;
        if (!product.isInStock()) {
            outOfStockBadge.setVisibility(View.VISIBLE);
            outOfStockBadge.setStatus(StatusBadgeView.Status.NEUTRAL);
            outOfStockBadge.setText(R.string.catalog_out_of_stock);
            stockLabel = getContext().getString(R.string.catalog_out_of_stock);
        } else {
            outOfStockBadge.setVisibility(View.GONE);
            stockLabel = getContext().getString(R.string.catalog_in_stock);
        }

        setContentDescription(getContext().getString(
                R.string.catalog_cd_product_card,
                product.getName(),
                formattedPrice,
                stockLabel
        ));

        String imageUrl = product.getImageUrl();
        ImageLoaderHelper.loadImage(imageView, imageUrl);
    }
}
