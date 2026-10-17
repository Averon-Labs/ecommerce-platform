import type { OrderStatus, PaymentStatus } from '../../api/types';
import { StatusBadge } from '../../components/StatusBadge';

/** Human-readable labels for the reviewed `OrderStatus` enum. */
export function orderStatusLabel(status: OrderStatus): string {
  switch (status) {
    case 'pending_payment':
      return 'Pending payment';
    case 'confirmed':
      return 'Confirmed';
    case 'processing':
      return 'Processing';
    case 'shipped':
      return 'Shipped';
    case 'delivered':
      return 'Delivered';
    case 'cancelled':
      return 'Cancelled';
  }
}

/** Human-readable labels for the reviewed `PaymentStatus` enum. */
export function paymentStatusLabel(status: PaymentStatus): string {
  switch (status) {
    case 'pending':
      return 'Pending';
    case 'completed':
      return 'Completed';
    case 'failed':
      return 'Failed';
  }
}

/**
 * Status text plus semantic color from the design-system status table
 * (docs/DESIGN_SYSTEM.md): delivered/completed success, pending payment
 * warning, cancelled/failed error, everything else neutral.
 */
export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  if (status === 'delivered') {
    return <StatusBadge tone="success">{orderStatusLabel(status)}</StatusBadge>;
  }

  if (status === 'pending_payment') {
    return <StatusBadge tone="warning">{orderStatusLabel(status)}</StatusBadge>;
  }

  if (status === 'cancelled') {
    return <StatusBadge tone="error">{orderStatusLabel(status)}</StatusBadge>;
  }

  return <StatusBadge tone="neutral">{orderStatusLabel(status)}</StatusBadge>;
}

export function PaymentStatusBadge({ status }: { status: PaymentStatus }) {
  if (status === 'completed') {
    return <StatusBadge tone="success">{paymentStatusLabel(status)}</StatusBadge>;
  }

  if (status === 'failed') {
    return <StatusBadge tone="error">{paymentStatusLabel(status)}</StatusBadge>;
  }

  return <StatusBadge tone="warning">{paymentStatusLabel(status)}</StatusBadge>;
}
