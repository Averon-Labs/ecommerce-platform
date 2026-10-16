/**
 * Shared class strings for the admin controls so buttons, inputs, and cards
 * stay consistent across pages (docs/DESIGN_SYSTEM.md — Web Dashboard
 * Components). Minimum web target is 44 CSS px, satisfied with min-h-11.
 */

export const primaryButtonClass =
  'inline-flex min-h-11 items-center justify-center gap-2 rounded-full bg-primary px-5 text-sm font-bold text-primary-foreground transition-opacity hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-60';

export const secondaryButtonClass =
  'inline-flex min-h-11 items-center justify-center gap-2 rounded-full border border-border bg-card px-5 text-sm font-semibold text-foreground hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-60';

export const dangerButtonClass =
  'inline-flex min-h-11 items-center justify-center gap-2 rounded-full bg-destructive px-5 text-sm font-bold text-destructive-foreground transition-opacity hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-60';

export const actionButtonClass =
  'inline-flex min-h-11 items-center rounded-lg px-3 text-sm font-semibold text-primary underline-offset-4 hover:bg-primary/10 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-60';

export const inputClass =
  'min-h-11 w-full rounded-xl border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring';

export const labelClass = 'block text-sm font-semibold';

export const helperTextClass = 'mt-1 text-xs text-muted-foreground';

export const fieldErrorClass = 'mt-1 text-xs font-semibold text-destructive';

export const cardClass = 'rounded-3xl border border-border bg-card shadow-glass';

export const statusBadgeBase = 'inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold';
