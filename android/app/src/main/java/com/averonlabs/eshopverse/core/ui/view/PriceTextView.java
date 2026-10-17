package com.averonlabs.eshopverse.core.ui.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.averonlabs.eshopverse.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textview.MaterialTextView;

/** Displays a value already formatted by the domain money formatter. */
public final class PriceTextView extends MaterialTextView {
    public PriceTextView(Context context) {
        this(context, null);
    }

    public PriceTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public PriceTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setTextAppearance(R.style.TextAppearance_EShopVerse_ComponentTitle);
        if (attrs == null || attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "textColor") == null) {
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface));
        }
    }

    /** The caller must pass the final localized/currency-formatted value from MoneyFormat. */
    public void setFormattedPrice(CharSequence formattedPrice) {
        setText(formattedPrice);
    }
}
