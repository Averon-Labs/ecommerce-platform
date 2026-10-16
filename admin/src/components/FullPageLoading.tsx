/** Zero-CLS skeleton shown while the session is being checked. */
export function FullPageLoading() {
  return (
    <div
      className="flex min-h-screen items-center justify-center bg-background"
      role="status"
      aria-busy="true"
      aria-label="Checking admin session"
    >
      <div className="w-full max-w-sm space-y-3 p-6" aria-hidden="true">
        <div className="h-6 w-2/3 animate-pulse rounded-full bg-muted" />
        <div className="h-4 w-full animate-pulse rounded-full bg-muted" />
        <div className="h-4 w-5/6 animate-pulse rounded-full bg-muted" />
      </div>
    </div>
  );
}
