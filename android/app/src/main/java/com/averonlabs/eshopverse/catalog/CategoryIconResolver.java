package com.averonlabs.eshopverse.catalog;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.averonlabs.eshopverse.R;

/**
 * Resolves bespoke vector icons for catalog categories across Home and Explore screens.
 */
public final class CategoryIconResolver {

    private CategoryIconResolver() {}

    @DrawableRes
    public static int getCategoryIcon(@Nullable String categoryName) {
        if (categoryName == null) {
            return R.drawable.ic_category_all;
        }

        String lower = categoryName.toLowerCase().trim();

        if (lower.contains("all")) {
            return R.drawable.ic_category_all;
        } else if (lower.contains("t-shirt") || lower.contains("tshirt") || lower.contains("tee")) {
            return R.drawable.ic_category_tshirts;
        } else if (lower.contains("shirt") || lower.contains("top") || lower.contains("blouse")) {
            return R.drawable.ic_category_shirts;
        } else if (lower.contains("pant") || lower.contains("jean") || lower.contains("trouser") || lower.contains("chino")) {
            return R.drawable.ic_category_pants;
        } else if (lower.contains("dress")) {
            return R.drawable.ic_category_dresses;
        } else if (lower.contains("jacket") || lower.contains("coat") || lower.contains("puffer") || lower.contains("biker")) {
            return R.drawable.ic_category_jackets;
        } else if (lower.contains("active") || lower.contains("sport") || lower.contains("gym")) {
            return R.drawable.ic_category_activewear;
        } else if (lower.contains("shoe") || lower.contains("footwear") || lower.contains("sneaker") || lower.contains("boot")) {
            return R.drawable.ic_category_shoes;
        } else if (lower.contains("access")) {
            return R.drawable.ic_category_accessories;
        } else if (lower.contains("leather") || lower.contains("suede")) {
            return R.drawable.ic_category_leather;
        } else if (lower.contains("bag") || lower.contains("backpack") || lower.contains("tote")) {
            return R.drawable.ic_category_bags;
        } else {
            return R.drawable.ic_category_all;
        }
    }
}
