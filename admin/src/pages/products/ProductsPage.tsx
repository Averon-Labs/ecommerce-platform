import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import {
  archiveAdminProduct,
  deleteAdminProduct,
  listAdminCategories,
  listAdminProducts,
} from '../../api/catalog';
import { ApiError } from '../../api/http';
import type { AdminCategory, AdminProduct, Pagination as PaginationMeta, ProductStatus } from '../../api/types';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { Pagination } from '../../components/Pagination';
import { StatusBadge } from '../../components/StatusBadge';
import { EmptyPanel, ErrorPanel, FeedbackBanner, TableSkeleton } from '../../components/StatePanels';
import { formatUsd } from '../../lib/format';
import { useDebouncedValue } from '../../lib/useDebouncedValue';
import {
  actionButtonClass,
  cardClass,
  inputClass,
  labelClass,
  primaryButtonClass,
  secondaryButtonClass,
} from '../../lib/ui';

const PER_PAGE = 20;

function ProductStatusBadge({ status }: { status: ProductStatus }) {
  if (status === 'active') {
    return <StatusBadge tone="success">Active</StatusBadge>;
  }

  if (status === 'inactive') {
    return <StatusBadge tone="neutral">Inactive</StatusBadge>;
  }

  return <StatusBadge tone="neutral">Archived</StatusBadge>;
}

function StockValue({ product }: { product: AdminProduct }) {
  if (!product.inStock) {
    return <span className="font-semibold text-destructive">Out of stock</span>;
  }

  return <span className="tabular-nums">{product.stockQuantity} in stock</span>;
}

function ProductThumb({ product }: { product: AdminProduct }) {
  if (product.imageUrl) {
    return (
      <img
        src={product.imageUrl}
        alt=""
        className="h-10 w-10 shrink-0 rounded-lg border border-border object-cover"
      />
    );
  }

  return (
    <span
      aria-hidden="true"
      className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border border-dashed border-border text-[10px] text-muted-foreground"
    >
      No image
    </span>
  );
}

/**
 * A-02 — Products. Filters and pagination follow the admin API query
 * parameters; archiving keeps row data authoritative by refetching the page
 * after every confirmed action.
 */
export default function ProductsPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebouncedValue(search);
  const [categoryFilter, setCategoryFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(1);
  const [products, setProducts] = useState<AdminProduct[]>([]);
  const [pagination, setPagination] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [categories, setCategories] = useState<AdminCategory[]>([]);
  const [reloadToken, setReloadToken] = useState(0);
  const [archiveTarget, setArchiveTarget] = useState<AdminProduct | null>(null);
  const [archiving, setArchiving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<AdminProduct | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // Feedback handed over by the product form after a create/update.
  useEffect(() => {
    const state = location.state as { feedback?: string } | null;

    if (state?.feedback) {
      setFeedback(state.feedback);
      navigate(`${location.pathname}${location.search}`, { replace: true, state: null });
    }
  }, [location, navigate]);

  // Category options for the filter; a failure here is non-blocking.
  useEffect(() => {
    const controller = new AbortController();

    listAdminCategories(controller.signal)
      .then((items) => {
        if (!controller.signal.aborted) {
          setCategories(items);
        }
      })
      .catch(() => undefined);

    return () => controller.abort();
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setLoadError(null);

    listAdminProducts(
      {
        page,
        perPage: PER_PAGE,
        q: debouncedSearch.trim() || undefined,
        categoryId: categoryFilter === '' ? undefined : Number(categoryFilter),
        status: statusFilter === '' ? undefined : (statusFilter as ProductStatus),
      },
      controller.signal,
    )
      .then((payload) => {
        if (controller.signal.aborted) {
          return;
        }
        setProducts(payload.data);
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
            : 'The products could not be loaded. Check your connection and try again.',
        );
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [page, debouncedSearch, categoryFilter, statusFilter, reloadToken]);

  const hasFilters = search.trim() !== '' || categoryFilter !== '' || statusFilter !== '';

  function clearFilters() {
    setSearch('');
    setCategoryFilter('');
    setStatusFilter('');
    setPage(1);
  }

  async function confirmArchive() {
    if (!archiveTarget) {
      return;
    }

    setArchiving(true);
    setActionError(null);

    try {
      const archived = await archiveAdminProduct(archiveTarget.id);
      setArchiveTarget(null);
      setFeedback(`${archived.name} was archived.`);
    } catch (error) {
      setArchiveTarget(null);
      setActionError(
        error instanceof ApiError ? error.message : 'The product could not be archived. Try again.',
      );
    } finally {
      setArchiving(false);
      setReloadToken((token) => token + 1);
    }
  }

  async function confirmDelete() {
    if (!deleteTarget) {
      return;
    }

    setDeleting(true);
    setActionError(null);

    try {
      const removed = deleteTarget.name;
      await deleteAdminProduct(deleteTarget.id);
      setDeleteTarget(null);
      setFeedback(`${removed} was deleted, including its images.`);
    } catch (error) {
      // 409 PRODUCT_HAS_ORDERS arrives with the server's explanation.
      setDeleteTarget(null);
      setActionError(
        error instanceof ApiError ? error.message : 'The product could not be deleted. Try again.',
      );
    } finally {
      setDeleting(false);
      setReloadToken((token) => token + 1);
    }
  }

  return (
    <section aria-labelledby="products-title" className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 id="products-title" className="text-2xl font-bold tracking-tight">
            Products
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Create, edit, archive, or delete catalog products. Stock is managed in Inventory.
          </p>
        </div>
        <Link to="/products/new" className={primaryButtonClass}>
          Add product
        </Link>
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
        className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4"
        aria-label="Product filters"
        onSubmit={(event) => event.preventDefault()}
      >
        <div>
          <label htmlFor="products-search" className={labelClass}>
            Search products
          </label>
          <input
            id="products-search"
            type="search"
            className={`${inputClass} mt-1`}
            value={search}
            placeholder="Name or SKU"
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(1);
            }}
          />
        </div>

        <div>
          <label htmlFor="products-category" className={labelClass}>
            Category
          </label>
          <select
            id="products-category"
            className={`${inputClass} mt-1`}
            value={categoryFilter}
            onChange={(event) => {
              setCategoryFilter(event.target.value);
              setPage(1);
            }}
          >
            <option value="">All categories</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
                {category.status === 'archived' ? ' (archived)' : ''}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label htmlFor="products-status" className={labelClass}>
            Status
          </label>
          <select
            id="products-status"
            className={`${inputClass} mt-1`}
            value={statusFilter}
            onChange={(event) => {
              setStatusFilter(event.target.value);
              setPage(1);
            }}
          >
            <option value="">All statuses</option>
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
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

      {loading && products.length === 0 ? (
        <div className={cardClass}>
          <TableSkeleton label="Loading products" />
        </div>
      ) : loadError ? (
        <ErrorPanel
          title="Products could not be loaded"
          message={loadError}
          onRetry={() => setReloadToken((token) => token + 1)}
        />
      ) : products.length === 0 ? (
        hasFilters ? (
          <EmptyPanel
            title="No products match your filters"
            description="Try a different search or clear the filters."
          >
            <button type="button" className={secondaryButtonClass} onClick={clearFilters}>
              Clear filters
            </button>
          </EmptyPanel>
        ) : (
          <EmptyPanel
            title="No products yet"
            description="Add the first product to start building the catalog."
          >
            <Link to="/products/new" className={primaryButtonClass}>
              Add product
            </Link>
          </EmptyPanel>
        )
      ) : (
        <div className={cardClass}>
          <div className="hidden overflow-x-auto md:block" aria-busy={loading}>
            <table className="w-full min-w-[760px] text-left text-sm">
              <caption className="sr-only">Products</caption>
              <thead className="border-b border-border bg-muted/50 text-xs uppercase tracking-wide text-muted-foreground">
                <tr>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    Product
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    SKU
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    Category
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    Price (USD)
                  </th>
                  <th scope="col" className="px-4 py-3 font-semibold">
                    Stock
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
                {products.map((product) => (
                  <tr key={product.id}>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <ProductThumb product={product} />
                        <span className="font-semibold">{product.name}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">{product.sku ?? '—'}</td>
                    <td className="px-4 py-3 text-muted-foreground">
                      {product.categoryName ?? '—'}
                    </td>
                    <td className="px-4 py-3 tabular-nums">{formatUsd(product.price)}</td>
                    <td className="px-4 py-3">
                      <StockValue product={product} />
                    </td>
                    <td className="px-4 py-3">
                      <ProductStatusBadge status={product.status} />
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <Link
                          to={`/products/${product.id}/edit`}
                          aria-label={`Edit ${product.name}`}
                          className={actionButtonClass}
                        >
                          Edit
                        </Link>
                        {product.status === 'archived' ? (
                          <span className="px-3 text-xs font-semibold text-muted-foreground">
                            Archived
                          </span>
                        ) : (
                          <button
                            type="button"
                            aria-label={`Archive ${product.name}`}
                            className={actionButtonClass}
                            onClick={() => {
                              setActionError(null);
                              setArchiveTarget(product);
                            }}
                          >
                            Archive
                          </button>
                        )}
                        <button
                          type="button"
                          aria-label={`Delete ${product.name}`}
                          className={actionButtonClass}
                          onClick={() => {
                            setActionError(null);
                            setDeleteTarget(product);
                          }}
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <ul className="divide-y divide-border md:hidden" aria-label="Products list">
            {products.map((product) => (
              <li key={product.id} className="p-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <ProductThumb product={product} />
                    <div>
                      <p className="font-semibold">{product.name}</p>
                      <p className="text-xs text-muted-foreground">SKU {product.sku ?? '—'}</p>
                    </div>
                  </div>
                  <ProductStatusBadge status={product.status} />
                </div>
                <dl className="mt-3 grid grid-cols-2 gap-2 text-sm">
                  <div>
                    <dt className="text-xs text-muted-foreground">Category</dt>
                    <dd>{product.categoryName ?? '—'}</dd>
                  </div>
                  <div>
                    <dt className="text-xs text-muted-foreground">Price (USD)</dt>
                    <dd className="tabular-nums">{formatUsd(product.price)}</dd>
                  </div>
                  <div>
                    <dt className="text-xs text-muted-foreground">Stock</dt>
                    <dd>
                      <StockValue product={product} />
                    </dd>
                  </div>
                </dl>
                <div className="mt-3 flex gap-1">
                  <Link
                    to={`/products/${product.id}/edit`}
                    aria-label={`Edit ${product.name}`}
                    className={actionButtonClass}
                  >
                    Edit
                  </Link>
                  {product.status !== 'archived' && (
                    <button
                      type="button"
                      aria-label={`Archive ${product.name}`}
                      className={actionButtonClass}
                      onClick={() => {
                        setActionError(null);
                        setArchiveTarget(product);
                      }}
                    >
                      Archive
                    </button>
                  )}
                  <button
                    type="button"
                    aria-label={`Delete ${product.name}`}
                    className={actionButtonClass}
                    onClick={() => {
                      setActionError(null);
                      setDeleteTarget(product);
                    }}
                  >
                    Delete
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
              itemNoun="product"
            />
          )}
        </div>
      )}

      <ConfirmDialog
        open={archiveTarget !== null}
        title={archiveTarget ? `Archive ${archiveTarget.name}?` : 'Archive product?'}
        description="The product is hidden from the public catalog. Existing orders keep their snapshots and stock is not changed."
        confirmLabel="Archive product"
        busy={archiving}
        onCancel={() => {
          if (!archiving) {
            setArchiveTarget(null);
          }
        }}
        onConfirm={() => void confirmArchive()}
      />

      <ConfirmDialog
        open={deleteTarget !== null}
        title={deleteTarget ? `Delete ${deleteTarget.name}?` : 'Delete product?'}
        description="The product and its uploaded images are removed from the server for good. Archive it instead if an order already references it."
        confirmLabel="Delete product"
        busy={deleting}
        onCancel={() => {
          if (!deleting) {
            setDeleteTarget(null);
          }
        }}
        onConfirm={() => void confirmDelete()}
      />
    </section>
  );
}
