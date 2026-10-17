import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiError } from '../../api/http';
import { listAdminOrders } from '../../api/orders';
import type { AdminOrderSummary, OrderStatus, Pagination as PaginationMeta } from '../../api/types';
import { Pagination } from '../../components/Pagination';
import { EmptyPanel, ErrorPanel, TableSkeleton } from '../../components/StatePanels';
import { formatDateTime, formatUsd } from '../../lib/format';
import { useDebouncedValue } from '../../lib/useDebouncedValue';
import {
  actionButtonClass,
  cardClass,
  inputClass,
  labelClass,
  primaryButtonClass,
  secondaryButtonClass,
} from '../../lib/ui';
import { OrderStatusBadge, PaymentStatusBadge } from './OrderBadges';

const PER_PAGE = 20;

const ORDER_STATUS_OPTIONS: Array<{ value: OrderStatus; label: string }> = [
  { value: 'pending_payment', label: 'Pending payment' },
  { value: 'confirmed', label: 'Confirmed' },
  { value: 'processing', label: 'Processing' },
  { value: 'shipped', label: 'Shipped' },
  { value: 'delivered', label: 'Delivered' },
  { value: 'cancelled', label: 'Cancelled' },
];

function CustomerCell({ order }: { order: AdminOrderSummary }) {
  return (
    <div>
      <p className="font-semibold">
        {order.customer.firstName} {order.customer.lastName}
      </p>
      <p className="text-xs text-muted-foreground">{order.customer.email}</p>
    </div>
  );
}

/**
 * A-06 — Orders. Filters, pagination, and row data come from the admin order
 * API only; every reference opens A-07, where status changes are restricted
 * to the transitions the server currently permits.
 */
export default function OrdersPage() {
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebouncedValue(search);
  const [statusFilter, setStatusFilter] = useState('');
  const [fromFilter, setFromFilter] = useState('');
  const [toFilter, setToFilter] = useState('');
  const [page, setPage] = useState(1);
  const [orders, setOrders] = useState<AdminOrderSummary[]>([]);
  const [pagination, setPagination] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setLoadError(null);

    listAdminOrders(
      {
        page,
        perPage: PER_PAGE,
        q: debouncedSearch.trim() || undefined,
        status: statusFilter === '' ? undefined : (statusFilter as OrderStatus),
        from: fromFilter || undefined,
        to: toFilter || undefined,
      },
      controller.signal,
    )
      .then((payload) => {
        if (controller.signal.aborted) {
          return;
        }
        setOrders(payload.data);
        setPagination(payload.pagination);
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) {
          return;
        }
        if (error instanceof ApiError && error.status === 401) {
          return; // The session handler routes back to sign-in.
        }
        setLoadError(
          error instanceof ApiError
            ? error.message
            : 'The orders could not be loaded. Check your connection and try again.',
        );
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [page, debouncedSearch, statusFilter, fromFilter, toFilter, reloadToken]);

  const hasFilters =
    search.trim() !== '' || statusFilter !== '' || fromFilter !== '' || toFilter !== '';

  function clearFilters() {
    setSearch('');
    setStatusFilter('');
    setFromFilter('');
    setToFilter('');
    setPage(1);
  }

  return (
    <section aria-labelledby="orders-title" className="space-y-4">
      <div>
        <h1 id="orders-title" className="text-2xl font-bold tracking-tight">
          Orders
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Inspect customer orders and manage status transitions through the server.
        </p>
      </div>

      <form
        className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-5"
        aria-label="Order filters"
        onSubmit={(event) => event.preventDefault()}
      >
        <div>
          <label htmlFor="orders-search" className={labelClass}>
            Search orders
          </label>
          <input
            id="orders-search"
            type="search"
            className={`${inputClass} mt-1`}
            value={search}
            placeholder="Reference or customer email"
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(1);
            }}
          />
        </div>

        <div>
          <label htmlFor="orders-status" className={labelClass}>
            Status
          </label>
          <select
            id="orders-status"
            className={`${inputClass} mt-1`}
            value={statusFilter}
            onChange={(event) => {
              setStatusFilter(event.target.value);
              setPage(1);
            }}
          >
            <option value="">All statuses</option>
            {ORDER_STATUS_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label htmlFor="orders-from" className={labelClass}>
            From date
          </label>
          <input
            id="orders-from"
            type="date"
            className={`${inputClass} mt-1`}
            value={fromFilter}
            onChange={(event) => {
              setFromFilter(event.target.value);
              setPage(1);
            }}
          />
        </div>

        <div>
          <label htmlFor="orders-to" className={labelClass}>
            To date
          </label>
          <input
            id="orders-to"
            type="date"
            className={`${inputClass} mt-1`}
            value={toFilter}
            onChange={(event) => {
              setToFilter(event.target.value);
              setPage(1);
            }}
          />
        </div>

        {hasFilters && (
          <div className="flex items-end">
            <button type="button" className={secondaryButtonClass} onClick={clearFilters}>
              Clear filters
            </button>
          </div>
        )}
      </form>

      {loading && orders.length === 0 ? (
        <div className={cardClass}>
          <TableSkeleton label="Loading orders" />
        </div>
      ) : loadError ? (
        <ErrorPanel
          title="Orders could not be loaded"
          message={loadError}
          onRetry={() => setReloadToken((token) => token + 1)}
        />
      ) : orders.length === 0 ? (
        hasFilters ? (
          <EmptyPanel
            title="No orders match your filters"
            description="Try a different search or clear the filters."
          >
            <button type="button" className={secondaryButtonClass} onClick={clearFilters}>
              Clear filters
            </button>
          </EmptyPanel>
        ) : (
          <EmptyPanel
            title="No orders yet"
            description="Orders appear here after customers complete checkout."
          >
            <Link to="/products" className={primaryButtonClass}>
              View products
            </Link>
          </EmptyPanel>
        )
      ) : (
        <div className={cardClass}>
          <div className="hidden overflow-x-auto md:block" aria-busy={loading}>
            <table className="w-full min-w-[860px] text-left text-sm">
              <caption className="sr-only">Orders</caption>
              <thead className="border-b border-border bg-muted/50 text-xs uppercase tracking-wide text-muted-foreground">
                <tr>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    Order
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    Customer
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    Date
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    Total (USD)
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    Order status
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    Payment
                  </th>
                  <th scope="col" className="px-4 py-3 text-right font-semibold">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {orders.map((order) => (
                  <tr key={order.id}>
                    <td className="px-4 py-3 font-semibold tabular-nums">{order.reference}</td>
                    <td className="px-4 py-3">
                      <CustomerCell order={order} />
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">
                      {formatDateTime(order.createdAt)}
                    </td>
                    <td className="px-4 py-3 tabular-nums">{formatUsd(order.total)}</td>
                    <td className="px-4 py-3">
                      <OrderStatusBadge status={order.status} />
                    </td>
                    <td className="px-4 py-3">
                      <PaymentStatusBadge status={order.paymentStatus} />
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end">
                        <Link
                          to={`/orders/${order.id}`}
                          aria-label={`View order ${order.reference}`}
                          className={actionButtonClass}
                        >
                          View
                        </Link>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <ul className="divide-y divide-border md:hidden" aria-label="Orders list">
            {orders.map((order) => (
              <li key={order.id} className="p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold tabular-nums">{order.reference}</p>
                    <p className="text-xs text-muted-foreground">
                      {formatDateTime(order.createdAt)}
                    </p>
                  </div>
                  <OrderStatusBadge status={order.status} />
                </div>
                <dl className="mt-3 grid grid-cols-2 gap-2 text-sm">
                  <div>
                    <dt className="text-xs text-muted-foreground">Customer</dt>
                    <dd>
                      <CustomerCell order={order} />
                    </dd>
                  </div>
                  <div>
                    <dt className="text-xs text-muted-foreground">Total (USD)</dt>
                    <dd className="tabular-nums">{formatUsd(order.total)}</dd>
                  </div>
                  <div>
                    <dt className="text-xs text-muted-foreground">Payment</dt>
                    <dd>
                      <PaymentStatusBadge status={order.paymentStatus} />
                    </dd>
                  </div>
                </dl>
                <div className="mt-3">
                  <Link
                    to={`/orders/${order.id}`}
                    aria-label={`View order ${order.reference}`}
                    className={actionButtonClass}
                  >
                    View
                  </Link>
                </div>
              </li>
            ))}
          </ul>

          {pagination && (
            <Pagination
              pagination={pagination}
              onPageChange={setPage}
              busy={loading}
              itemNoun="order"
            />
          )}
        </div>
      )}
    </section>
  );
}
