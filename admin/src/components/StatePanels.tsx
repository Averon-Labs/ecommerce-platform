import type { ReactNode } from 'react';
import { cardClass, secondaryButtonClass } from '../lib/ui';

/** Static skeleton rows while a list loads (zero layout shift). */
export function TableSkeleton({ label, rows = 5 }: { label: string; rows?: number }) {
  return (
    <div role="status" aria-busy="true" aria-label={label} className="space-y-2 p-4">
      {Array.from({ length: rows }, (_, index) => (
        <div key={index} aria-hidden="true" className="h-12 animate-pulse rounded-xl bg-muted" />
      ))}
    </div>
  );
}

/** Recoverable loading failure with an explicit retry. */
export function ErrorPanel({
  title,
  message,
  onRetry,
}: {
  title: string;
  message?: string | null;
  onRetry: () => void;
}) {
  return (
    <div role="alert" className={`${cardClass} p-6 text-center`}>
      <p className="text-base font-semibold">{title}</p>
      {message ? <p className="mt-1 text-sm text-muted-foreground">{message}</p> : null}
      <button type="button" className={`${secondaryButtonClass} mt-4`} onClick={onRetry}>
        Retry
      </button>
    </div>
  );
}

/** Empty state for both “nothing exists yet” and “no results for filters”. */
export function EmptyPanel({
  title,
  description,
  children,
}: {
  title: string;
  description: string;
  children?: ReactNode;
}) {
  return (
    <div className={`${cardClass} p-8 text-center`}>
      <p className="text-base font-semibold">{title}</p>
      <p className="mt-1 text-sm text-muted-foreground">{description}</p>
      {children ? <div className="mt-4 flex flex-wrap justify-center gap-3">{children}</div> : null}
    </div>
  );
}

/** Non-blocking success feedback announced to assistive technology. */
export function FeedbackBanner({ message }: { message: string }) {
  return (
    <p role="status" className="rounded-xl border border-border bg-muted px-3 py-2 text-sm font-semibold">
      {message}
    </p>
  );
}
