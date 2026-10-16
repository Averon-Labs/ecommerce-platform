import { API_BASE_URL } from '../config/api';

interface ServiceUnreachableProps {
  message: string | null;
  onRetry: () => void;
}

/**
 * Recoverable boot failure: the API could not be reached while checking the
 * session. Shows the API origin and an explicit retry (screen specifications —
 * error/retry state).
 */
export function ServiceUnreachable({ message, onRetry }: ServiceUnreachableProps) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-10">
      <div className="w-full max-w-md rounded-3xl border border-border bg-card p-6 text-center shadow-glass sm:p-8">
        <h1 className="text-xl font-bold">Can’t reach the API</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          {message ?? 'The admin API is unreachable. Check your connection and try again.'}
        </p>
        <p className="mt-1 text-xs text-muted-foreground">API origin: {API_BASE_URL}</p>
        <button
          type="button"
          onClick={onRetry}
          className="mt-6 inline-flex min-h-11 items-center rounded-full bg-primary px-6 text-sm font-semibold text-primary-foreground hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          Retry
        </button>
      </div>
    </main>
  );
}
