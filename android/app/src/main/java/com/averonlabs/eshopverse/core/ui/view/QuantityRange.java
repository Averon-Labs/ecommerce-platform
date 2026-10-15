package com.averonlabs.eshopverse.core.ui.view;

public final class QuantityRange {
    private int min = 1;
    private int max = Integer.MAX_VALUE;
    private int quantity = 1;

    public QuantityRange() {}

    public int getQuantity() {
        return quantity;
    }

    public int setQuantity(int value) {
        quantity = clamp(value);
        return quantity;
    }

    public int setMin(int value) {
        if (value < 0 || value > max) {
            throw new IllegalArgumentException("Minimum must be non-negative and no greater than maximum");
        }
        min = value;
        return setQuantity(quantity);
    }

    public int setMax(int value) {
        if (value < min) {
            throw new IllegalArgumentException("Maximum must be no less than minimum");
        }
        max = value;
        return setQuantity(quantity);
    }

    public int increment() {
        if (quantity < max) {
            quantity++;
        }
        return quantity;
    }

    public int decrement() {
        if (quantity > min) {
            quantity--;
        }
        return quantity;
    }

    private int clamp(int value) {
        return Math.max(min, Math.min(value, max));
    }
}
