package com.averonlabs.eshopverse.core.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.averonlabs.eshopverse.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

public final class QuantityStepperView extends LinearLayout {
    public interface OnQuantityChangedListener {
        void onQuantityChanged(int quantity);
    }

    private final QuantityRange quantityRange = new QuantityRange();
    private final MaterialTextView quantityText;
    private final MaterialButton decrementButton;
    private final MaterialButton incrementButton;
    @Nullable private OnQuantityChangedListener listener;

    public QuantityStepperView(Context context) {
        this(context, null);
    }

    public QuantityStepperView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuantityStepperView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        decrementButton = createButton(context, "−");
        decrementButton.setContentDescription(context.getString(R.string.catalog_cd_stepper_decrement));
        quantityText = new MaterialTextView(context);
        quantityText.setTextAppearance(R.style.TextAppearance_EShopVerse_ComponentTitle);
        quantityText.setGravity(Gravity.CENTER);
        incrementButton = createButton(context, "+");
        incrementButton.setContentDescription(context.getString(R.string.catalog_cd_stepper_increment));

        addView(decrementButton, buttonLayoutParams());
        addView(quantityText, new LayoutParams(
                getResources().getDimensionPixelSize(R.dimen.touch_target_min),
                getResources().getDimensionPixelSize(R.dimen.touch_target_min)));
        addView(incrementButton, buttonLayoutParams());
        renderQuantity();

        decrementButton.setOnClickListener(view -> notifyUserAction(quantityRange.decrement()));
        incrementButton.setOnClickListener(view -> notifyUserAction(quantityRange.increment()));
    }

    public void setQuantity(int quantity) {
        quantityRange.setQuantity(quantity);
        renderQuantity();
    }

    public int getQuantity() {
        return quantityRange.getQuantity();
    }

    public void setMin(int min) {
        quantityRange.setMin(min);
        renderQuantity();
    }

    public void setMax(int max) {
        quantityRange.setMax(max);
        renderQuantity();
    }

    public void setOnQuantityChangedListener(@Nullable OnQuantityChangedListener listener) {
        this.listener = listener;
    }

    public void setDecrementContentDescription(CharSequence description) {
        decrementButton.setContentDescription(description);
    }

    public void setIncrementContentDescription(CharSequence description) {
        incrementButton.setContentDescription(description);
    }

    private MaterialButton createButton(Context context, String label) {
        MaterialButton button = new MaterialButton(
                context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(label);
        button.setMinWidth(getResources().getDimensionPixelSize(R.dimen.touch_target_min));
        button.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch_target_min));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        return button;
    }

    private LayoutParams buttonLayoutParams() {
        int target = getResources().getDimensionPixelSize(R.dimen.touch_target_min);
        return new LayoutParams(target, target);
    }

    private void notifyUserAction(int quantity) {
        renderQuantity();
        if (listener != null) {
            listener.onQuantityChanged(quantity);
        }
    }

    public MaterialButton getDecrementButton() {
        return decrementButton;
    }

    public MaterialButton getIncrementButton() {
        return incrementButton;
    }

    public MaterialTextView getQuantityTextView() {
        return quantityText;
    }

    private void renderQuantity() {
        int qty = quantityRange.getQuantity();
        quantityText.setText(String.valueOf(qty));
        quantityText.setContentDescription(getContext().getString(R.string.stepper_quantity_desc, qty));
    }
}
