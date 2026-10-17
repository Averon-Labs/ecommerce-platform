import { apiRequest } from './http';
import { buildQuery } from './query';
import type { AdminOrder, AdminOrderQuery, AdminOrderSummary, OrderStatus, Paged } from './types';

/**
 * Admin order endpoints (docs/openapi.yaml — Admin orders).
 *
 * The list is server-paged/filterable; the detail carries immutable
 * snapshots and `allowedTransitions`, and the only mutation is the
 * permitted status transition — `confirmed` is never set from here.
 */

export function listAdminOrders(
  query: AdminOrderQuery = {},
  signal?: AbortSignal,
): Promise<Paged<AdminOrderSummary>> {
  const search = buildQuery({
    page: query.page,
    perPage: query.perPage,
    q: query.q,
    status: query.status,
    from: query.from,
    to: query.to,
  });

  return apiRequest<Paged<AdminOrderSummary>>(`/admin/orders${search}`, { signal });
}

export async function getAdminOrder(
  orderId: number,
  signal?: AbortSignal,
): Promise<AdminOrder> {
  const payload = await apiRequest<{ data: AdminOrder }>(`/admin/orders/${orderId}`, { signal });
  return payload.data;
}

/** Applies one permitted transition; the response is the refreshed order. */
export async function updateAdminOrderStatus(
  orderId: number,
  status: OrderStatus,
): Promise<AdminOrder> {
  const payload = await apiRequest<{ data: AdminOrder }>(`/admin/orders/${orderId}/status`, {
    method: 'PATCH',
    body: { status },
  });
  return payload.data;
}
