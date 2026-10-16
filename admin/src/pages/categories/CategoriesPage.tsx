import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import {
  archiveAdminCategory,
  createAdminCategory,
  listAdminCategories,
  restoreAdminCategory,
  updateAdminCategory,
} from '../../api/catalog';
import { ApiError } from '../../api/http';
import type { AdminCategory, CategoryInput, CategoryStatus } from '../../api/types';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { Dialog } from '../../components/Dialog';
import { EmptyPanel, ErrorPanel, FeedbackBanner, TableSkeleton } from '../../components/StatePanels';
import { StatusBadge } from '../../components/StatusBadge';
import {
  actionButtonClass,
  cardClass,
  fieldErrorClass,
  inputClass,
  labelClass,
  primaryButtonClass,
  secondaryButtonClass,
} from '../../lib/ui';

function CategoryStatusBadge({ status }: { status: CategoryStatus }) {
  if (status === 'active') {
    return <StatusBadge tone="success">Active</StatusBadge>;
  }

  return <StatusBadge tone="neutral">Archived</StatusBadge>;
}

/** Merge the documented `details[]` messages with the top-level message. */
function describeActionError(error: unknown, fallback: string): string {
  if (!(error instanceof ApiError)) {
    return fallback;
  }

  const extra = error.details
    .map((detail) => detail.message)
    .filter((message) => message.length > 0)
    .join(' ');

  return extra ? `${error.message} ${extra}` : error.message;
}

/**
 * A-04 — Categories. The API returns the full list (no pagination), so search
 * and status filtering run client-side; lifecycle actions use the reviewed
 * archive/restore endpoints and never delete.
 */
export default function CategoriesPage() {
  const [categories, setCategories] = useState<AdminCategory[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState(0);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<AdminCategory | null>(null);
  const [formName, setFormName] = useState('');
  const [formSortOrder, setFormSortOrder] = useState('0');
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [formAlert, setFormAlert] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const [archiveTarget, setArchiveTarget] = useState<AdminCategory | null>(null);
  const [archiving, setArchiving] = useState(false);
  const [restoringId, setRestoringId] = useState<number | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setLoadError(null);

    listAdminCategories(controller.signal)
      .then((items) => {
        if (!controller.signal.aborted) {
          setCategories(items);
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
            ? error.message
            : 'The categories could not be loaded. Check your connection and try again.',
        );
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [reloadToken]);

  const hasFilters = search.trim() !== '' || statusFilter !== '';
  const visibleCategories = categories.filter((category) => {
    const matchesSearch =
      search.trim() === '' ||
      category.name.toLowerCase().includes(search.trim().toLowerCase());
    const matchesStatus = statusFilter === '' || category.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  function clearFilters() {
    setSearch('');
    setStatusFilter('');
  }

  function nextSortOrder(): number {
    return categories.reduce((next, category) => Math.max(next, category.sortOrder + 1), 0);
  }

  function openCreate() {
    setEditing(null);
    setFormName('');
    setFormSortOrder(String(nextSortOrder()));
    setFormErrors({});
    setFormAlert(null);
    setFormOpen(true);
  }

  function openEdit(category: AdminCategory) {
    setEditing(category);
    setFormName(category.name);
    setFormSortOrder(String(category.sortOrder));
    setFormErrors({});
    setFormAlert(null);
    setFormOpen(true);
  }

  function validateCategoryForm(): boolean {
    const errors: Record<string, string> = {};
    const name = formName.trim();
    const order = formSortOrder.trim();

    if (!name) {
      errors.name = 'Name is required.';
    } else if (name.length > 80) {
      errors.name = 'Keep the name to 80 characters or fewer.';
    }

    if (order === '') {
      errors.sortOrder = 'Display order is required.';
    } else if (!/^\d+$/.test(order) || Number(order) > 9999) {
      errors.sortOrder = 'Enter a whole number between 0 and 9999.';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  }

  async function handleCategorySubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormAlert(null);

    if (!validateCategoryForm()) {
      return;
    }

    setSaving(true);
    const input: CategoryInput = { name: formName.trim(), sortOrder: Number(formSortOrder.trim()) };

    try {
      if (editing) {
        const saved = await updateAdminCategory(editing.id, input);
        setFormOpen(false);
        setFeedback(`${saved.name} was saved.`);
      } else {
        const created = await createAdminCategory(input);
        setFormOpen(false);
        setFeedback(`${created.name} was created.`);
      }
      setReloadToken((token) => token + 1);
    } catch (error) {
      if (error instanceof ApiError && error.code === 'CATEGORY_NAME_TAKEN') {
        setFormErrors({ name: error.message });
      } else if (error instanceof ApiError && error.status === 422) {
        const mapped: Record<string, string> = {};
        for (const detail of error.details) {
          if (detail.field) {
            mapped[detail.field] = detail.message;
          }
        }
        setFormErrors(mapped);
        setFormAlert(
          Object.keys(mapped).length === 0 ? error.message : 'Check the highlighted fields.',
        );
      } else {
        setFormAlert(
          error instanceof ApiError ? error.message : 'The category could not be saved. Try again.',
        );
      }
    } finally {
      setSaving(false);
    }
  }

  async function confirmArchive() {
    if (!archiveTarget) {
      return;
    }

    setArchiving(true);
    setActionError(null);

    try {
      const archived = await archiveAdminCategory(archiveTarget.id);
      setArchiveTarget(null);
      setFeedback(`${archived.name} was archived.`);
    } catch (error) {
      setArchiveTarget(null);
      setActionError(describeActionError(error, 'The category could not be archived. Try again.'));
    } finally {
      setArchiving(false);
      setReloadToken((token) => token + 1);
    }
  }

  async function restore(category: AdminCategory) {
    setRestoringId(category.id);
    setActionError(null);

    try {
      const restored = await restoreAdminCategory(category.id);
      setFeedback(`${restored.name} was restored.`);
    } catch (error) {
      setActionError(describeActionError(error, 'The category could not be restored. Try again.'));
    } finally {
      setRestoringId(null);
      setReloadToken((token) => token + 1);
    }
  }

  function renderActions(category: AdminCategory) {
    return (
      <>
        <button
          type="button"
          aria-label={`Edit ${category.name}`}
          className={actionButtonClass}
          onClick={() => openEdit(category)}
        >
          Edit
        </button>
        {category.status === 'active' ? (
          <button
            type="button"
            aria-label={`Archive ${category.name}`}
            className={actionButtonClass}
            onClick={() => {
              setActionError(null);
              setArchiveTarget(category);
            }}
          >
            Archive
          </button>
        ) : (
          <button
            type="button"
            aria-label={`Restore ${category.name}`}
            className={actionButtonClass}
            onClick={() => void restore(category)}
            disabled={restoringId === category.id}
            aria-busy={restoringId === category.id}
          >
            {restoringId === category.id ? 'Restoring…' : 'Restore'}
          </button>
        )}
      </>
    );
  }

  return (
    <section aria-labelledby="categories-title" className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 id="categories-title" className="text-2xl font-bold tracking-tight">
            Categories
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Organize the catalog. Categories are archived, never deleted.
          </p>
        </div>
        <button type="button" className={primaryButtonClass} onClick={openCreate}>
          Add category
        </button>
      </div>

      {feedback && <FeedbackBanner message={feedback} />}
      {actionError && (
        <p
          role="alert"
          className="rounded-xl border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
        >
          {actionError}
        </p>
      )}

      <form
        className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3"
        aria-label="Category filters"
        onSubmit={(event) => event.preventDefault()}
      >
        <div>
          <label htmlFor="categories-search" className={labelClass}>
            Search categories
          </label>
          <input
            id="categories-search"
            type="search"
            className={`${inputClass} mt-1`}
            value={search}
            placeholder="Category name"
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>
        <div>
          <label htmlFor="categories-status" className={labelClass}>
            Status
          </label>
          <select
            id="categories-status"
            className={`${inputClass} mt-1`}
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
          >
            <option value="">All statuses</option>
            <option value="active">Active</option>
            <option value="archived">Archived</option>
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

      {loading && categories.length === 0 ? (
        <div className={cardClass}>
          <TableSkeleton label="Loading categories" rows={4} />
        </div>
      ) : loadError ? (
        <ErrorPanel
          title="Categories could not be loaded"
          message={loadError}
          onRetry={() => setReloadToken((token) => token + 1)}
        />
      ) : categories.length === 0 ? (
        <EmptyPanel
          title="No categories yet"
          description="Add the first category to organize the catalog."
        >
          <button type="button" className={primaryButtonClass} onClick={openCreate}>
            Add category
          </button>
        </EmptyPanel>
      ) : visibleCategories.length === 0 ? (
        <EmptyPanel
          title="No categories match your filters"
          description="Try a different search or clear the filters."
        >
          <button type="button" className={secondaryButtonClass} onClick={clearFilters}>
            Clear filters
          </button>
        </EmptyPanel>
      ) : (
        <div className={cardClass}>
          <div className="hidden overflow-x-auto md:block" aria-busy={loading}>
            <table className="w-full min-w-[640px] text-left text-sm">
              <caption className="sr-only">Categories</caption>
              <thead className="border-b border-border bg-muted/50 text-xs uppercase tracking-wide text-muted-foreground">
                <tr>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    Name
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    Display order
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    Products
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    Status
                  </th>
                  <th scope="col" className="px-4 py-3 text-right font-semibold">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {visibleCategories.map((category) => (
                  <tr key={category.id}>
                    <td className="px-4 py-3 font-semibold">{category.name}</td>
                    <td className="px-4 py-3 tabular-nums text-muted-foreground">
                      {category.sortOrder}
                    </td>
                    <td className="px-4 py-3 tabular-nums">
                      {category.productCount}{' '}
                      {category.productCount === 1 ? 'product' : 'products'}
                    </td>
                    <td className="px-4 py-3">
                      <CategoryStatusBadge status={category.status} />
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        {renderActions(category)}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <ul className="divide-y divide-border md:hidden" aria-label="Categories list">
            {visibleCategories.map((category) => (
              <li key={category.id} className="p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold">{category.name}</p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      Display order {category.sortOrder} · {category.productCount}{' '}
                      {category.productCount === 1 ? 'product' : 'products'}
                    </p>
                  </div>
                  <CategoryStatusBadge status={category.status} />
                </div>
                <div className="mt-3 flex gap-1">{renderActions(category)}</div>
              </li>
            ))}
          </ul>
        </div>
      )}

      <Dialog
        open={formOpen}
        title={editing ? `Edit ${editing.name}` : 'Add category'}
        onClose={saving ? () => undefined : () => setFormOpen(false)}
      >
        <form
          noValidate
          aria-label={editing ? 'Edit category' : 'Add category'}
          onSubmit={(event) => void handleCategorySubmit(event)}
          className="space-y-4"
        >
          {formAlert && (
            <p
              role="alert"
              className="rounded-xl border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
            >
              {formAlert}
            </p>
          )}

          <div>
            <label htmlFor="category-name" className={labelClass}>
              Name
            </label>
            <input
              id="category-name"
              className={`${inputClass} mt-1`}
              value={formName}
              maxLength={120}
              onChange={(event) => setFormName(event.target.value)}
              aria-invalid={formErrors.name !== undefined}
              aria-describedby={formErrors.name ? 'category-name-error' : undefined}
            />
            {formErrors.name && (
              <p id="category-name-error" className={fieldErrorClass}>
                {formErrors.name}
              </p>
            )}
          </div>

          <div>
            <label htmlFor="category-order" className={labelClass}>
              Display order
            </label>
            <input
              id="category-order"
              type="number"
              min={0}
              max={9999}
              inputMode="numeric"
              className={`${inputClass} mt-1`}
              value={formSortOrder}
              onChange={(event) => setFormSortOrder(event.target.value)}
              aria-invalid={formErrors.sortOrder !== undefined}
              aria-describedby={formErrors.sortOrder ? 'category-order-error' : undefined}
            />
            {formErrors.sortOrder && (
              <p id="category-order-error" className={fieldErrorClass}>
                {formErrors.sortOrder}
              </p>
            )}
          </div>

          <div className="flex flex-wrap justify-end gap-3">
            <button
              type="button"
              className={secondaryButtonClass}
              onClick={() => setFormOpen(false)}
              disabled={saving}
            >
              Cancel
            </button>
            <button
              type="submit"
              className={primaryButtonClass}
              disabled={saving}
              aria-busy={saving}
            >
              {saving ? 'Saving…' : 'Save'}
            </button>
          </div>
        </form>
      </Dialog>

      <ConfirmDialog
        open={archiveTarget !== null}
        title={archiveTarget ? `Archive ${archiveTarget.name}?` : 'Archive category?'}
        description="The category is hidden from the storefront. It can only be archived after its active products are moved to another category or archived."
        confirmLabel="Archive category"
        busy={archiving}
        onCancel={() => {
          if (!archiving) {
            setArchiveTarget(null);
          }
        }}
        onConfirm={() => void confirmArchive()}
      />
    </section>
  );
}
