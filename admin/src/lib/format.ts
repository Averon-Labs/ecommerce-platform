const USD_PATTERN = /^(\d+)\.(\d{2})$/;

/** Render the contract's fixed-precision money string as `$1,234.56`. */
export function formatUsd(price: string): string {
  const match = USD_PATTERN.exec(price);

  if (!match) {
    return `$${price}`;
  }

  const [, whole, cents] = match;
  const grouped = whole.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return `$${grouped}.${cents}`;
}

/** Render an API timestamp for the admin's locale, or an em dash. */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '—';
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return '—';
  }

  return new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}
