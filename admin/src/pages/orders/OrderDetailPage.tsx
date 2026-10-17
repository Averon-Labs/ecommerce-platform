import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError } from '../../api/http';
import { getAdminOrder, updateAdminOrderStatus } from '../../api/orders';
import type { AdminOrder, OrderStatus } from '../../api/types';
import { Dialog } from '../../components/Dialog';
import { EmptyPanel, ErrorPanel, FeedbackBanner, TableSkeleton } from '../../components/StatePanels';
import { formatDateTime, formatUsd } from '../../lib/format';
import {
  dangerButtonClass,
  labelClass,
  primaryButtonClass,
  secondaryButtonClass,
  inputClass,
  cardClass,
} from '../../lib/ui';
import { OrderStatusBadge, PaymentStatusBadge, orderStatusLabel, paymentStatusLabel } from './OrderBadges';

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-wrap items-baseline justify-between gap-2 border-b border-border py-2 last:border-b-0">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="text-sm font-semibold">{value}</dd>
    </div>
  );
}

function ItemsTable({ order }: { order: AdminOrder }) {
  return (
    <>
      <div className="hidden overflow-x-auto md:block">
        <table className="w-full min-w-[560px] text-left text-sm">
          <caption className="sr-only">Order items</caption>
          <thead className="border-b border-border bg-muted/50 text-xs uppercase tracking-wide text-muted-foreground">
            <tr>
              <th scope="col" className="px-4 py-3 font-semibold">
                Product
              </th>
              <th scope="col" className="px-4 py-3 font-semibold">
                Quantity
              </th>
              <th scope="col" className="px-4 py-3 font-semibold">
                Unit price (USD)
              </th>
              <th scope="col" className="px-4 py-3 font-semibold">
                Line total (USD)
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {order.items.map((item) => (
              <tr key={item.productId}>
                <td className="px-4 py-3 font-semibold">{item.productName}</td>
                <td className="px-4 py-3 tabular-nums">{item.quantity}</td>
                <td className="px-4 py-3 tabular-nums">{formatUsd(item.unitPrice)}</td>
                <td className="px-4 py-3 tabular-nums">{formatUsd(item.lineTotal)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <ul className="divide-y divide-border md:hidden" aria-label="Order items list">
        {order.items.map((item) => (
          <li key={item.productId} className="p-4">
            <p className="font-semibold">{item.productName}</p>
            <dl className="mt-2 grid grid-cols-2 gap-2 text-sm">
              <div>
                <dt className="text-xs text-muted-foreground">Quantity</dt>
                <dd className="tabular-nums">{item.quantity}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Unit price (USD)</dt>
                <dd className="tabular-nums">{formatUsd(item.unitPrice)}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Line total (USD)</dt>
                <dd className="tabular-nums">{formatUsd(item.lineTotal)}</dd>
              </div>
            </dl>
          </li>
        ))}
      </ul>
    </>
  );
}

/**
 * A-07 — Order details. Read-only snapshots, payment outcome, and timeline
 * come straight from the server; the status action offers only the
 * transitions in `allowedTransitions`, confirms the target, and adopts the
 * server's refreshed order after saving — never an optimistic guess.
 */
export default function OrderDetailPage() {
  const { orderId } = useParams();
  const parsedOrderId = Number(orderId);
  const validOrderId = Number.isInteger(parsedOrderId) && parsedOrderId > 0;

  const [order, setOrder] = useState<AdminOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [reloadToken, setReloadToken] = useState(0);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [targetStatus, setTargetStatus] = useState('');
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!validOrderId) {
      setLoading(false);
      setNotFound(true);
      return;
    }

    const controller = new AbortController();
    setLoading(true);
    setLoadError(null);
    setNotFound(false);

    getAdminOrder(parsedOrderId, controller.signal)
      .then((payload) => {
        if (controller.signal.aborted) {
          return;
        }
        setOrder(payload);
        setTargetStatus(payload.allowedTransitions[0] ?? '');
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) {
          return;
        }
        if (error instanceof ApiError && error.status === 401) {
          return; // The session handler routes back to sign-in.
        }
        if (error instanceof ApiError && error.status === 404) {
          setNotFound(true);
          return;
        }
        setLoadError(
          error instanceof ApiError
            ? error.message
            : 'The order could not be loaded. Check your connection and try again.',
        );
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [parsedOrderId, validOrderId, reloadToken]);

  async function applyTransition() {
    if (!order || targetStatus === '') {
      return;
    }

    setSaving(true);
    setActionError(null);

    try {
      const updated = await updateAdminOrderStatus(order.id, targetStatus as OrderStatus);
      setOrder(updated);
      setTargetStatus(updated.allowedTransitions[0] ?? '');
      setConfirmOpen(false);
      setFeedback(
        `${updated.reference} is now ${orderStatusLabel(updated.status).toLowerCase()}.`,
      );
    } catch (error) {
      setConfirmOpen(false);
      if (error instanceof ApiError && error.status === 401) {
        return; // The session handler routes back to sign-in.
      }
      setActionError(
        error instanceof ApiError
          ? error.message
          : 'The status could not be updated. Refresh the order and try again.',
      );
    } finally {
      setSaving(false);
    }
  }

  if (!validOrderId || notFound) {
    return (
      <EmptyPanel
        title="Order not found"
        description="The requested order does not exist or is no longer available."
      >
        <Link to="/orders" className={secondaryButtonClass}>
          Back to orders
        </Link>
      </EmptyPanel>
    );
  }

  if (loading) {
    return (
      <div className={cardClass}>
        <TableSkeleton label="Loading order" rows={6} />
      </div>
    );
  }

  if (loadError || !order) {
    return (
      <ErrorPanel
        title="Order could not be loaded"
        message={loadError}
        onRetry={() => setReloadToken((token) => token + 1)}
      />
    );
  }

  const confirmTarget = targetStatus === '' ? null : (targetStatus as OrderStatus);

  return (
    <section aria-labelledby="order-title" className="space-y-4">
      <div>
        <Link to="/orders" className={secondaryButtonClass}>
          Back to orders
        </Link>
      </div>

      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 id="order-title" className="text-2xl font-bold tracking-tight tabular-nums">
            {order.reference}
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Placed {formatDateTime(order.createdAt)} · Last updated {formatDateTime(order.updatedAt)}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <OrderStatusBadge status={order.status} />
          <PaymentStatusBadge status={order.paymentStatus} />
        </div>
      </div>

      {feedback && <FeedbackBanner message={feedback} />}
      {actionError && (
        <div
          role="alert"
          className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
        >
          <span>{actionError}</span>
          <button
            type="button"
            className={secondaryButtonClass}
            onClick={() => {
              setActionError(null);
              setReloadToken((token) => token + 1);
            }}
          >
            Refresh order
          </button>
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className={`${cardClass} p-5 lg:col-span-1`}>
          <h2 className="text-base font-bold">Customer</h2>
          <dl className="mt-2">
            <DetailRow
              label="Name"
              value={`${order.customer.firstName} ${order.customer.lastName}`}
            />
            <DetailRow label="Email" value={order.customer.email} />
          </dl>

          <h2 className="mt-6 text-base font-bold">Shipping address</h2>
          <address className="mt-2 text-sm not-italic">
            <p className="font-semibold">{order.shippingAddress.recipientName}</p>
            <p>{order.shippingAddress.line1}</p>
            {order.shippingAddress.line2 ? <p>{order.shippingAddress.line2}</p> : null}
            <p>
              {order.shippingAddress.city}, {order.shippingAddress.region}{' '}
              {order.shippingAddress.postalCode}
            </p>
            <p>{order.shippingAddress.country}</p>
          </address>
          <p className="mt-3 text-xs text-muted-foreground">
            Snapshotted for this order; later address edits do not change it.
          </p>

          {order.notes ? (
            <>
              <h2 className="mt-6 text-base font-bold">Customer notes</h2>
              <p className="mt-2 text-sm">{order.notes}</p>
            </>
          ) : null}
        </div>

        <div className={`${cardClass} lg:col-span-2`}>
          <h2 className="px-4 pt-5 text-base font-bold">Items</h2>
          <div className="mt-2" aria-busy={loading}>
            <ItemsTable order={order} />
          </div>
          <dl className="border-t border-border px-4 py-3">
            <DetailRow label="Subtotal" value={formatUsd(order.subtotal)} />
            <DetailRow label="Sample shipping" value={formatUsd(order.shipping)} />
            <div className="flex flex-wrap items-baseline justify-between gap-2 py-2 text-base font-bold">
              <dt>Total</dt>
              <dd className="tabular-nums">
                {formatUsd(order.total)} {order.currency}
              </dd>
            </div>
          </dl>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div className={`${cardClass} p-5`}>
          <h2 className="text-base font-bold">Payment</h2>
          {order.payment ? (
            <>
              <p className="mt-1 text-xs text-muted-foreground">
                Demo method only — no card data is ever collected.
              </p>
              <dl className="mt-2">
                <DetailRow label="Status" value={paymentStatusLabel(order.payment.status)} />
                <DetailRow label="Amount" value={formatUsd(order.payment.amount)} />
                <DetailRow label="Transaction" value={order.payment.transactionId ?? '—'} />
                <DetailRow label="Recorded" value={formatDateTime(order.payment.createdAt)} />
              </dl>
            </>
          ) : (
            <p className="mt-2 text-sm text-muted-foreground">
              No payment attempt recorded yet. The order awaits the customer&apos;s simulated
              payment outcome.
            </p>
          )}
        </div>

        <div className={`${cardClass} p-5`}>
          <h2 className="text-base font-bold">Status</h2>
          <div className="mt-2">
            <OrderStatusBadge status={order.status} />
          </div>

          {order.allowedTransitions.length > 0 ? (
            <div className="mt-4">
              <label htmlFor="order-next-status" className={labelClass}>
                Next status
              </label>
              <select
                id="order-next-status"
                className={`${inputClass} mt-1`}
                value={targetStatus}
                onChange={(event) => {
                  setTargetStatus(event.target.value);
                  setActionError(null);
                }}
              >
                {order.allowedTransitions.map((status) => (
                  <option key={status} value={status}>
                    {orderStatusLabel(status)}
                  </option>
                ))}
              </select>
              <p className="mt-1 text-xs text-muted-foreground">
                Only the transitions the server currently permits are offered here.
              </p>
              <button
                type="button"
                className={`${primaryButtonClass} mt-3`}
                onClick={() => setConfirmOpen(true)}
                disabled={saving || targetStatus === ''}
              >
                Update status
              </button>
            </div>
          ) : (
            <p className="mt-3 text-sm text-muted-foreground">
              This order is in a terminal state; no further status changes are available.
            </p>
          )}

          <h3 className="mt-6 text-sm font-bold">Timeline</h3>
          <ol className="mt-2 space-y-2">
            {order.timeline.map((event, index) => (
              <li key={`${event.status}-${event.at}-${index}`} className="flex items-center gap-3">
                <OrderStatusBadge status={event.status} />
                <span className="text-xs text-muted-foreground tabular-nums">
                  {formatDateTime(event.at)}
                </span>
              </li>
            ))}
          </ol>
        </div>
      </div>

      <Dialog
        open={confirmOpen}
        title={
          confirmTarget
            ? `Move ${order.reference} to ${orderStatusLabel(confirmTarget)}?`
            : 'Update order status?'
        }
        onClose={saving ? () => undefined : () => setConfirmOpen(false)}
        footer={
          <>
            <button
              type="button"
              className={secondaryButtonClass}
              onClick={() => setConfirmOpen(false)}
              disabled={saving}
            >
              Cancel
            </button>
            <button
              type="button"
              className={confirmTarget === 'cancelled' ? dangerButtonClass : primaryButtonClass}
              onClick={() => void applyTransition()}
              disabled={saving}
              aria-busy={saving}
            >
              {saving ? 'Saving…' : confirmTarget ? `Move to ${orderStatusLabel(confirmTarget)}` : 'Save'}
            </button>
          </>
        }
      >
        <p className="text-sm text-muted-foreground">
          The server records this transition in the order timeline and returns the refreshed
          order. Status changes are never applied optimistically.
        </p>
      </Dialog>
    </section>
  );
}
