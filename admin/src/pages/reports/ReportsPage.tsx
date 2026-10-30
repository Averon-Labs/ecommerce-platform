import { useEffect, useState } from 'react';
import { getAdminSalesReport } from '../../api/reports';
import { ApiError } from '../../api/http';
import type { SalesReport } from '../../api/types';
import { EmptyPanel, ErrorPanel } from '../../components/StatePanels';
import { formatUsd } from '../../lib/format';
import { OrderStatusBadge } from '../orders/OrderBadges';
import {
  cardClass,
  fieldErrorClass,
  inputClass,
  labelClass,
  primaryButtonClass,
  secondaryButtonClass,
} from '../../lib/ui';

const PRESETS = [
  { days: 7, label: 'Last 7 days' },
  { days: 30, label: 'Last 30 days' },
  { days: 90, label: 'Last 90 days' },
];

const MONEY_PATTERN = /^(\d+)\.(\d{2})$/;

/** Parse the contract's fixed-precision money string into integer cents. */
function toCents(value: string): number {
  const match = MONEY_PATTERN.exec(value);
  return match ? Number(match[1]) * 100 + Number(match[2]) : 0;
}

/** UTC calendar date `offsetDays` before today, as `YYYY-MM-DD`. */
function utcDay(offsetDays: number): string {
  const now = new Date();
  const todayUtc = Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate());
  return new Date(todayUtc - offsetDays * 86_400_000).toISOString().slice(0, 10);
}

function lastDays(days: number): { from: string; to: string } {
  return { from: utcDay(days - 1), to: utcDay(0) };
}

/** `2026-10-29` → `Oct 29`, parsed and rendered in UTC. */
function formatShortDate(value: string): string {
  const date = new Date(`${value}T00:00:00Z`);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    timeZone: 'UTC',
  }).format(date);
}

/** Fixed-height KPI/report skeleton so the first paint matches the loaded layout. */
function ReportSkeleton() {
  return (
    <div role="status" aria-busy="true" aria-label="Loading sales report" className="space-y-4">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }, (_, index) => (
          <div key={index} aria-hidden="true" className={`${cardClass} h-28 animate-pulse bg-muted`} />
        ))}
      </div>
      <div aria-hidden="true" className={`${cardClass} h-[22rem] animate-pulse bg-muted`} />
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div aria-hidden="true" className={`${cardClass} h-72 animate-pulse bg-muted`} />
        <div aria-hidden="true" className={`${cardClass} h-72 animate-pulse bg-muted`} />
      </div>
    </div>
  );
}

function SummaryCards({ report }: { report: SalesReport }) {
  return (
    <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <div className={`${cardClass} p-4`}>
        <dt className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          Revenue
        </dt>
        <dd className="mt-1 text-2xl font-bold tabular-nums">
          {formatUsd(report.summary.revenue)}
        </dd>
        <p className="mt-1 text-xs text-muted-foreground">Paid orders incl. shipping</p>
      </div>
      <div className={`${cardClass} p-4`}>
        <dt className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          Paid orders
        </dt>
        <dd className="mt-1 text-2xl font-bold tabular-nums">{report.summary.paidOrders}</dd>
        <p className="mt-1 text-xs text-muted-foreground">
          of {report.summary.totalOrders} orders in range
        </p>
      </div>
      <div className={`${cardClass} p-4`}>
        <dt className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          Items sold
        </dt>
        <dd className="mt-1 text-2xl font-bold tabular-nums">{report.summary.itemsSold}</dd>
        <p className="mt-1 text-xs text-muted-foreground">Units in paid orders</p>
      </div>
      <div className={`${cardClass} p-4`}>
        <dt className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          Average order value
        </dt>
        <dd className="mt-1 text-2xl font-bold tabular-nums">
          {formatUsd(report.summary.averageOrderValue)}
        </dd>
        <p className="mt-1 text-xs text-muted-foreground">Per paid order</p>
      </div>
    </dl>
  );
}

function RevenueChart({ report }: { report: SalesReport }) {
  const maxCents = Math.max(...report.daily.map((day) => toCents(day.revenue)), 0);

  return (
    <figure className={`${cardClass} p-4 sm:p-6`} aria-labelledby="reports-revenue-title">
      <figcaption>
        <h2 id="reports-revenue-title" className="text-base font-bold">
          Daily revenue
        </h2>
        <p className="mt-1 text-sm text-muted-foreground">
          {formatUsd(report.summary.revenue)} across {report.summary.paidOrders} paid orders ·{' '}
          {formatShortDate(report.range.from)}–{formatShortDate(report.range.to)} (UTC)
        </p>
      </figcaption>

      <div aria-hidden="true" className="mt-5 h-56 border-b border-border">
        <div className="flex h-full items-end gap-px">
          {report.daily.map((day) => {
            const height = maxCents === 0 ? 0 : (toCents(day.revenue) / maxCents) * 100;
            return (
              <div key={day.date} className="flex h-full flex-1 items-end">
                <div
                  className="w-full rounded-t bg-primary-strong/80"
                  style={{ height: `${height}%` }}
                  title={`${day.date}: ${formatUsd(day.revenue)}`}
                />
              </div>
            );
          })}
        </div>
      </div>
      <div className="mt-2 flex justify-between text-xs text-muted-foreground">
        <span>{formatShortDate(report.range.from)}</span>
        <span>{formatShortDate(report.range.to)}</span>
      </div>

      <table className="sr-only">
        <caption>Daily revenue values</caption>
        <thead>
          <tr>
            <th scope="col">Date</th>
            <th scope="col">Revenue</th>
            <th scope="col">Paid orders</th>
            <th scope="col">Items sold</th>
          </tr>
        </thead>
        <tbody>
          {report.daily.map((day) => (
            <tr key={day.date}>
              <th scope="row">{day.date}</th>
              <td>{formatUsd(day.revenue)}</td>
              <td>{day.orders}</td>
              <td>{day.itemsSold}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </figure>
  );
}

function TopProducts({ report }: { report: SalesReport }) {
  return (
    <section aria-labelledby="top-products-title" className={`${cardClass} p-4 sm:p-6`}>
      <h2 id="top-products-title" className="text-base font-bold">
        Top products
      </h2>
      <p className="mt-1 text-sm text-muted-foreground">Ranked by revenue in the selected range.</p>

      {report.topProducts.length === 0 ? (
        <p className="mt-4 text-sm text-muted-foreground">No paid products in this range.</p>
      ) : (
        <div className="mt-4 overflow-x-auto">
          <table className="w-full text-left text-sm">
            <caption className="sr-only">Top products by revenue</caption>
            <thead className="border-b border-border text-xs uppercase tracking-wide text-muted-foreground">
              <tr>
                <th scope="col" className="py-2 pr-3 font-semibold">
                  Product
                </th>
                <th scope="col" className="py-2 pr-3 text-right font-semibold">
                  Units
                </th>
                <th scope="col" className="py-2 text-right font-semibold">
                  Revenue
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {report.topProducts.map((product) => (
                <tr key={product.productId}>
                  <th scope="row" className="py-3 pr-3 font-semibold">
                    {product.name}
                  </th>
                  <td className="py-3 pr-3 text-right tabular-nums">{product.quantity}</td>
                  <td className="py-3 text-right tabular-nums">{formatUsd(product.revenue)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function StatusMix({ report }: { report: SalesReport }) {
  const maxOrders = Math.max(...report.ordersByStatus.map((row) => row.orders), 0);

  return (
    <section aria-labelledby="status-mix-title" className={`${cardClass} p-4 sm:p-6`}>
      <h2 id="status-mix-title" className="text-base font-bold">
        Orders by status
      </h2>
      <p className="mt-1 text-sm text-muted-foreground">
        Every order created in the range, including unpaid and cancelled orders.
      </p>

      <ul aria-label="Orders by status" className="mt-4 space-y-3">
        {report.ordersByStatus.map((row) => (
          <li key={row.status}>
            <div className="flex items-center justify-between gap-3">
              <OrderStatusBadge status={row.status} />
              <span className="text-sm font-semibold tabular-nums">
                {row.orders} <span className="font-normal text-muted-foreground">orders</span>
              </span>
            </div>
            <div aria-hidden="true" className="mt-2 h-2 rounded-full bg-muted">
              <div
                className="h-full rounded-full bg-primary-strong/70"
                style={{ width: maxOrders === 0 ? '0%' : `${(row.orders / maxOrders) * 100}%` }}
              />
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}

/**
 * RPT-002 — Sales reports. Every figure comes from the read-only
 * `GET /admin/reports/sales` aggregate; the page only chooses the range and
 * renders it. The chart is rendered with CSS bars, so no chart dependency or
 * runtime third-party request is added.
 */
export default function ReportsPage() {
  const [range, setRange] = useState(() => lastDays(30));
  const [fromDraft, setFromDraft] = useState(() => range.from);
  const [toDraft, setToDraft] = useState(() => range.to);
  const [rangeError, setRangeError] = useState<string | null>(null);
  const [report, setReport] = useState<SalesReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setLoadError(null);

    getAdminSalesReport({ from: range.from, to: range.to }, controller.signal)
      .then((payload) => {
        if (!controller.signal.aborted) {
          setReport(payload);
        }
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
            ? (error.fieldError('from') ?? error.message)
            : 'The sales report could not be loaded. Check your connection and try again.',
        );
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [range, reloadToken]);

  function isPreset(days: number): boolean {
    const preset = lastDays(days);
    return range.from === preset.from && range.to === preset.to;
  }

  function selectPreset(days: number) {
    const preset = lastDays(days);
    setRange(preset);
    setFromDraft(preset.from);
    setToDraft(preset.to);
    setRangeError(null);
  }

  function applyRange() {
    if (fromDraft !== '' && toDraft !== '' && fromDraft > toDraft) {
      setRangeError('The from date must not be after the to date.');
      return;
    }

    setRangeError(null);
    setRange({ from: fromDraft, to: toDraft });
  }

  const hasReport = report !== null && !loadError;

  return (
    <section aria-labelledby="reports-title" className="space-y-4">
      <div>
        <h1 id="reports-title" className="text-2xl font-bold tracking-tight">
          Sales reports
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Paid revenue and order mix from completed payments. Dates and daily buckets use UTC.
        </p>
      </div>

      <form
        className={`${cardClass} p-4 sm:p-6`}
        aria-label="Report range"
        onSubmit={(event) => {
          event.preventDefault();
          applyRange();
        }}
      >
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-sm font-semibold">Presets</span>
          {PRESETS.map((preset) => (
            <button
              key={preset.days}
              type="button"
              className={isPreset(preset.days) ? primaryButtonClass : secondaryButtonClass}
              aria-pressed={isPreset(preset.days)}
              onClick={() => selectPreset(preset.days)}
            >
              {preset.label}
            </button>
          ))}
        </div>

        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-[1fr_1fr_auto] sm:items-end">
          <div>
            <label htmlFor="reports-from" className={labelClass}>
              From date
            </label>
            <input
              id="reports-from"
              type="date"
              className={`${inputClass} mt-1`}
              value={fromDraft}
              onChange={(event) => setFromDraft(event.target.value)}
            />
          </div>
          <div>
            <label htmlFor="reports-to" className={labelClass}>
              To date
            </label>
            <input
              id="reports-to"
              type="date"
              className={`${inputClass} mt-1`}
              value={toDraft}
              onChange={(event) => setToDraft(event.target.value)}
              aria-describedby={rangeError ? 'reports-range-error' : undefined}
            />
          </div>
          <button type="submit" className={primaryButtonClass}>
            Apply range
          </button>
        </div>
        <p className="mt-2 text-xs text-muted-foreground">
          Inclusive UTC dates. With no dates, the server returns the last 30 days; the maximum span
          is 366 days.
        </p>
        {rangeError && (
          <p id="reports-range-error" role="alert" className={fieldErrorClass}>
            {rangeError}
          </p>
        )}
      </form>

      {loading && report === null ? (
        <ReportSkeleton />
      ) : !hasReport ? (
        loadError ? (
          <ErrorPanel
            title="Sales report could not be loaded"
            message={loadError}
            onRetry={() => setReloadToken((token) => token + 1)}
          />
        ) : null
      ) : report.summary.totalOrders === 0 ? (
        <EmptyPanel
          title="No orders in this range"
          description="Orders appear in the report once customers check out in the selected UTC dates."
        />
      ) : (
        <div className="space-y-4" aria-busy={loading}>
          <SummaryCards report={report} />
          <RevenueChart report={report} />
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <TopProducts report={report} />
            <StatusMix report={report} />
          </div>
        </div>
      )}
    </section>
  );
}
