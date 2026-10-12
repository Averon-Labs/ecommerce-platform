package com.averonlabs.eshopverse.orders;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.model.OrderStatus;
import com.averonlabs.eshopverse.core.model.PaymentStatus;
import com.averonlabs.eshopverse.core.ui.view.StatusBadgeView;

/**
 * Maps OrderStatus and PaymentStatus enums to localized strings and semantic badge colors.
 * Guarantees no status renders blank or without semantic styling.
 */
public final class OrderStatusFormatter {

    private OrderStatusFormatter() {}

    @StringRes
    public static int getStatusTextRes(@NonNull OrderStatus status) {
        switch (status) {
            case PENDING_PAYMENT:
                return R.string.order_status_pending_payment;
            case CONFIRMED:
                return R.string.order_status_confirmed;
            case PROCESSING:
                return R.string.order_status_processing;
            case SHIPPED:
                return R.string.order_status_shipped;
            case DELIVERED:
                return R.string.order_status_delivered;
            case CANCELLED:
                return R.string.order_status_cancelled;
            default:
                return R.string.order_status_confirmed;
        }
    }

    @NonNull
    public static StatusBadgeView.Status getBadgeStatus(@NonNull OrderStatus status) {
        switch (status) {
            case PENDING_PAYMENT:
                return StatusBadgeView.Status.WARNING;
            case CONFIRMED:
            case PROCESSING:
            case SHIPPED:
            case DELIVERED:
                return StatusBadgeView.Status.SUCCESS;
            case CANCELLED:
                return StatusBadgeView.Status.ERROR;
            default:
                return StatusBadgeView.Status.NEUTRAL;
        }
    }

    @StringRes
    public static int getPaymentStatusTextRes(@NonNull PaymentStatus paymentStatus) {
        switch (paymentStatus) {
            case PENDING:
                return R.string.payment_status_pending;
            case COMPLETED:
                return R.string.payment_status_completed;
            case FAILED:
                return R.string.payment_status_failed;
            default:
                return R.string.payment_status_pending;
        }
    }

    @NonNull
    public static StatusBadgeView.Status getPaymentBadgeStatus(@NonNull PaymentStatus paymentStatus) {
        switch (paymentStatus) {
            case PENDING:
                return StatusBadgeView.Status.WARNING;
            case COMPLETED:
                return StatusBadgeView.Status.SUCCESS;
            case FAILED:
                return StatusBadgeView.Status.ERROR;
            default:
                return StatusBadgeView.Status.NEUTRAL;
        }
    }
}
