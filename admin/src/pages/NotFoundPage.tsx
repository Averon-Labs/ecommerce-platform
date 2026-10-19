import { Link } from 'react-router-dom';
import { ThemeToggle } from '../components/ThemeToggle';

export function NotFoundPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-10">
      <ThemeToggle className="fixed right-4 top-4 z-10" />
      <div className="w-full max-w-md rounded-3xl border border-border bg-card p-6 text-center shadow-glass sm:p-8">
        <h1 className="text-xl font-bold">Page not found</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          The page you requested does not exist.
        </p>
        <Link
          to="/products"
          className="mt-6 inline-flex min-h-11 items-center rounded-full bg-primary px-6 text-sm font-semibold text-primary-foreground hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          Go to Products
        </Link>
      </div>
    </main>
  );
}
