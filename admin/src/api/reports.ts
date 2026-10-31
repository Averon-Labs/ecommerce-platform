import { apiRequest } from './http';
import { buildQuery } from './query';
import type { SalesReport, SalesReportQuery } from './types';

/**
 * Admin sales report (docs/openapi.yaml — Admin reports).
 *
 * Read-only and server-aggregated: the page never recomputes money from the
 * catalog. Omitting the dates lets the server apply its 30-day default.
 */
export async function getAdminSalesReport(
  query: SalesReportQuery = {},
  signal?: AbortSignal,
): Promise<SalesReport> {
  const search = buildQuery({
    from: query.from,
    to: query.to,
    topLimit: query.topLimit,
  });

  const payload = await apiRequest<{ data: SalesReport }>(`/admin/reports/sales${search}`, {
    signal,
  });
  return payload.data;
}
