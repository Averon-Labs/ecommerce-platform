package com.averonlabs.eshopverse.orders;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import com.averonlabs.eshopverse.core.model.OrderStatus;
import com.averonlabs.eshopverse.core.model.PaymentStatus;
import com.averonlabs.eshopverse.core.ui.view.StatusBadgeView;

import org.junit.Test;

/**
 * Tests that every OrderStatus and PaymentStatus maps to a non-zero string resource
 * and a defined semantic StatusBadgeView status (no status renders blank).
 */
public class OrderStatusTest {

    @Test
    public void testAllOrderStatusesMapToStringAndBadge() {
        for (OrderStatus status : OrderStatus.values()) {
            int resId = OrderStatusFormatter.getStatusTextRes(status);
            assertNotEquals("OrderStatus " + status + " must map to non-zero resource", 0, resId);

            StatusBadgeView.Status badgeStatus = OrderStatusFormatter.getBadgeStatus(status);
            assertNotNull("OrderStatus " + status + " must map to non-null badge status", badgeStatus);
        }
    }

    @Test
    public void testAllPaymentStatusesMapToStringAndBadge() {
        for (PaymentStatus paymentStatus : PaymentStatus.values()) {
            int resId = OrderStatusFormatter.getPaymentStatusTextRes(paymentStatus);
            assertNotEquals("PaymentStatus " + paymentStatus + " must map to non-zero resource", 0, resId);

            StatusBadgeView.Status badgeStatus = OrderStatusFormatter.getPaymentBadgeStatus(paymentStatus);
            assertNotNull("PaymentStatus " + paymentStatus + " must map to non-null badge status", badgeStatus);
        }
    }
}
