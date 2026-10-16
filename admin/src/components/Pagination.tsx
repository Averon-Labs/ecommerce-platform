import type { Pagination as PaginationMeta } from '../api/types';
import { secondaryButtonClass } from '../lib/ui';

interface PaginationProps {
  pagination: PaginationMeta;
  onPageChange: (page: number) => void;
  busy?: boolean;
  itemNoun: string;
}

/** API-driven pagination: page controls follow `pagination` metadata only. */
export function Pagination({
  pagination,
  onPageChange,
  busy = false,
  itemNoun,
}: PaginationProps) {
  const { currentPage, totalItems, totalPages } = pagination;
  const lastPage = Math.max(totalPages, 1);

  return (
    <nav
      aria-label="Pagination"
      className="flex flex-wrap items-center justify-between gap-3 border-t border-border px-4 py-3"
    >
      <p className="text-sm text-muted-foreground">
        {totalItems} {totalItems === 1 ? itemNoun : `${itemNoun}s`}
      </p>
      <div className="flex items-center gap-2">
        <button
          type="button"
          className={secondaryButtonClass}
          onClick={() => onPageChange(currentPage - 1)}
          disabled={busy || currentPage <= 1}
        >
          Previous
        </button>
        <span className="px-2 text-sm font-semibold" aria-live="polite">
          Page {currentPage} of {lastPage}
        </span>
        <button
          type="button"
          className={secondaryButtonClass}
          onClick={() => onPageChange(currentPage + 1)}
          disabled={busy || currentPage >= lastPage}
        >
          Next
        </button>
      </div>
    </nav>
  );
}
