import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listAdminInventory, updateAdminInventory } from '../../api/catalog';
import { ApiError } from '../../api/http';
import type { InventoryRow, Pagination as PaginationMeta } from '../../api/types';
import { Dialog } from '../../components/Dialog';
import { Pagination } from '../../components/Pagination';
import { EmptyPanel, ErrorPanel, FeedbackBanner, TableSkeleton } from '../../components/StatePanels';
import { StatusBadge } from '../../components/StatusBadge';
import { formatDateTime } from '../../lib/format';
import { useDebouncedValue } from '../../lib/useDebouncedValue';
import {
  actionButtonClass,
  cardClass,
  fieldErrorClass,
  helperTextClass,
  inputClass,
  labelClass,
  primaryButtonClass,
  secondaryButtonClass,
} from '../../lib/ui';

const PER_PAGE = 20;
const MAX_QUANTITY = 1_000_000;

function AvailabilityBadge({ inStock }: { inStock: boolean }) {
  if (inStock) {
    return <StatusBadge tone="success">In stock</StatusBadge>;
  }

  return <StatusBadge tone="error">Out of stock</StatusBadge>;
}

function validateQuantity(value: string): string | null {
  const trimmed = value.trim();

  if (trimmed === '') {
    return 'Enter the new quantity.';
  }

  if (!/^\d+$/.test(trimmed)) {
    return 'Enter a whole number of 0 or more.';
  }

  if (Number(trimmed) > MAX_QUANTITY) {
    return `Keep the quantity to ${MAX_QUANTITY} or less.`;
  }

  return null;
}

/**
 * A-05 — Inventory. Shows absolute stock per product; adjustment submits the
 * resulting quantity (never a delta), and zero is explicitly out of stock.
 * There is no low-stock threshold and no adjustment history in this release.
 */
export default function InventoryPage() {
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebouncedValue(search);
  const [availability, setAvailability] = useState('');
  const [page, setPage] = useState(1);
  const [rows, setRows] = useState<InventoryRow[]>([]);
  const [pagination, setPagination] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState(0);
  const [feedback, setFeedback] = useState<string | null>(null);

  const [adjustTarget, setAdjustTarget] = useState<InventoryRow | null>(null);
  const [adjustValue, setAdjustValue] = useState('');
  const [adjustError, setAdjustError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setLoadError(null);

    listAdminInventory(
      {
        page,
        perPage: PER_PAGE,
        q: debouncedSearch.trim() || undefined,
        inStock: availability === '' ? undefined : availability === 'in-stock',
      },
      controller.signal,
    )
      .then((payload) => {
        if (controller.signal.aborted) {
          return;
        }
        setRows(payload.data);
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
            : 'The inventory could not be loaded. Check your connection and try again.',
        );
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [page, debouncedSearch, availability, reloadToken]);

  const hasFilters = search.trim() !== '' || availability !== '';

  function clearFilters() {
    setSearch('');
    setAvailability('');
    setPage(1);
  }

  function openAdjust(row: InventoryRow) {
    setAdjustTarget(row);
    setAdjustValue(String(row.quantity));
    setAdjustError(null);
  }

  async function saveQuantity() {
    if (!adjustTarget) {
      return;
    }

    const validationError = validateQuantity(adjustValue);
    setAdjustError(validationError);

    if (validationError) {
      return;
    }

    setSaving(true);

    try {
      const updated = await updateAdminInventory(adjustTarget.productId, Number(adjustValue.trim()));
      setAdjustTarget(null);
      setFeedback(`Stock for ${updated.name} is now ${updated.quantity}.`);
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        setAdjustTarget(null); // The session handler routes back to sign-in.
        return;
      }
      setAdjustError(
        error instanceof ApiError ? error.message : 'The quantity could not be saved. Try again.',
      );
    } finally {
      setSaving(false);
      setReloadToken((token) => token + 1);
    }
  }

  return (
    <section aria-labelledby="inventory-title" className="space-y-4">
      <div>
        <h1 id="inventory-title" className="text-2xl font-bold tracking-tight">
          Inventory
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Set the absolute stock quantity per product. Zero means out of stock.
        </p>
      </div>

      {feedback && <FeedbackBanner message={feedback} />}

      <form
        className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3"
        aria-label="Inventory filters"
        onSubmit={(event) => event.preventDefault()}
      >
        <div>
          <label htmlFor="inventory-search" className={labelClass}>
            Search inventory
          </label>
          <input
            id="inventory-search"
            type="search"
            className={`${inputClass} mt-1`}
            value={search}
            placeholder="Product name or SKU"
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(1);
            }}
          />
        </div>
        <div>
          <label htmlFor="inventory-availability" className={labelClass}>
            Availability
          </label>
          <select
            id="inventory-availability"
            className={`${inputClass} mt-1`}
            value={availability}
            onChange={(event) => {
              setAvailability(event.target.value);
              setPage(1);
            }}
          >
            <option value="">All rows</option>
            <option value="in-stock">In stock</option>
            <option value="out-of-stock">Out of stock</option>
          </select>
        </div>
        {hasFilters && (
          <div className="flex items-end">
            <button type="button" className={secondaryButtonClass} onClick={clearFilters}>
              Clear filters
            </button>
          </div>
        )}
      </form>

      {loading && rows.length === 0 ? (
        <div className={cardClass}>
          <TableSkeleton label="Loading inventory" rows={4} />
        </div>
      ) : loadError ? (
        <ErrorPanel
          title="Inventory could not be loaded"
          message={loadError}
          onRetry={() => setReloadToken((token) => token + 1)}
        />
      ) : rows.length === 0 ? (
        hasFilters ? (
          <EmptyPanel
            title="No inventory rows match your filters"
            description="Try a different search or clear the filters."
          >
            <button type="button" className={secondaryButtonClass} onClick={clearFilters}>
              Clear filters
            </button>
          </EmptyPanel>
        ) : (
          <EmptyPanel
            title="No inventory yet"
            description="Inventory rows appear with products. Add a product to get started."
          >
            <Link to="/products/new" className={primaryButtonClass}>
              Add a product
            </Link>
          </EmptyPanel>
        )
      ) : (
        <div className={cardClass}>
          <div className="hidden overflow-x-auto md:block" aria-busy={loading}>
            <table className="w-full min-w-[720px] text-left text-sm">
              <caption className="sr-only">Inventory</caption>
              <thead className="border-b border-border bg-muted/50 text-xs uppercase tracking-wide text-muted-foreground">
                <tr>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    Product
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    SKU
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    Quantity
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    Availability
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    Updated
                  </th>
                  <th scope="col" className="px-4 py-3 text-right font-semibold">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {rows.map((row) => (
                  <tr key={row.productId}>
                    <td className="px-4 py-3 font-semibold">{row.name}</td>
                    <td className="px-4 py-3 text-muted-foreground">{row.sku ?? '—'}</td>
                    <td className="px-4 py-3 font-semibold tabular-nums">{row.quantity}</td>
                    <td className="px-4 py-3">
                      <AvailabilityBadge inStock={row.inStock} />
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">
                      {formatDateTime(row.updatedAt)}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end">
                        <button
                          type="button"
                          aria-label={`Adjust stock for ${row.name}`}
                          className={actionButtonClass}
                          onClick={() => openAdjust(row)}
                        >
                          Adjust stock
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <ul className="divide-y divide-border md:hidden" aria-label="Inventory list">
            {rows.map((row) => (
              <li key={row.productId} className="p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold">{row.name}</p>
                    <p className="text-xs text-muted-foreground">SKU {row.sku ?? '—'}</p>
                  </div>
                  <AvailabilityBadge inStock={row.inStock} />
                </div>
                <dl className="mt-3 grid grid-cols-2 gap-2 text-sm">
                  <div>
                    <dt className="text-xs text-muted-foreground">Quantity</dt>
                    <dd className="font-semibold tabular-nums">{row.quantity}</dd>
                  </div>
                  <div>
                    <dt className="text-xs text-muted-foreground">Updated</dt>
                    <dd className="text-muted-foreground">{formatDateTime(row.updatedAt)}</dd>
                  </div>
                </dl>
                <div className="mt-3">
                  <button
                    type="button"
                    aria-label={`Adjust stock for ${row.name}`}
                    className={actionButtonClass}
                    onClick={() => openAdjust(row)}
                  >
                    Adjust stock
                  </button>
                </div>
              </li>
            ))}
          </ul>

          {pagination && (
            <Pagination
              pagination={pagination}
              onPageChange={setPage}
              busy={loading}
              itemNoun="inventory row"
            />
          )}
        </div>
      )}

      <Dialog
        open={adjustTarget !== null}
        title={adjustTarget ? `Adjust stock — ${adjustTarget.name}` : 'Adjust stock'}
        onClose={saving ? () => undefined : () => setAdjustTarget(null)}
        footer={
          <>
            <button
              type="button"
              className={secondaryButtonClass}
              onClick={() => setAdjustTarget(null)}
              disabled={saving}
            >
              Cancel
            </button>
            <button
              type="button"
              className={primaryButtonClass}
              onClick={() => void saveQuantity()}
              disabled={saving}
              aria-busy={saving}
            >
              {saving ? 'Saving…' : 'Save quantity'}
            </button>
          </>
        }
      >
        {adjustTarget && (
          <div className="space-y-4">
            <p className="text-sm">
              Current quantity:{' '}
              <span className="font-semibold tabular-nums">{adjustTarget.quantity}</span>
            </p>

            <div>
              <label htmlFor="inventory-quantity" className={labelClass}>
                New quantity
              </label>
              <input
                id="inventory-quantity"
                type="number"
                min={0}
                step={1}
                inputMode="numeric"
                className={`${inputClass} mt-1`}
                value={adjustValue}
                onChange={(event) => setAdjustValue(event.target.value)}
                aria-describedby={
                  adjustError
                    ? 'inventory-quantity-help inventory-quantity-error'
                    : 'inventory-quantity-help'
                }
              />
              <p id="inventory-quantity-help" className={helperTextClass}>
                The resulting quantity, not a change. Zero means out of stock.
              </p>
              {adjustError && (
                <p id="inventory-quantity-error" role="alert" className={fieldErrorClass}>
                  {adjustError}
                </p>
              )}
            </div>
          </div>
        )}
      </Dialog>
    </section>
  );
}
