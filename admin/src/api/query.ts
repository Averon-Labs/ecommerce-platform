type QueryValue = string | number | boolean | undefined | null;

/**
 * Build a query string from API filter values, omitting unset/empty entries
 * so the server applies its documented defaults.
 */
export function buildQuery(params: Record<string, QueryValue>): string {
  const search = new URLSearchParams();

  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === '') {
      continue;
    }
    search.set(key, String(value));
  }

  const query = search.toString();
  return query ? `?${query}` : '';
}
