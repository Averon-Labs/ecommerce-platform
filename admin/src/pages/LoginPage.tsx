import { useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { ApiError } from '../api/http';
import { useAuth } from '../auth/AuthContext';
import { FullPageLoading } from '../components/FullPageLoading';

interface FieldErrors {
  email?: string;
  password?: string;
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function describeFailure(error: unknown): string {
  if (error instanceof ApiError) {
    switch (error.status) {
      case 401:
        return 'The email or password is incorrect.';
      case 403:
        return 'The sign-in request was rejected. Reload the page and try again.';
      case 429:
        return error.retryAfterSeconds !== null
          ? `Too many sign-in attempts. Try again in ${error.retryAfterSeconds} seconds.`
          : 'Too many sign-in attempts. Try again shortly.';
      default:
        return error.status >= 500
          ? 'The server could not complete the sign-in. Try again.'
          : error.message;
    }
  }
  return 'The API could not be reached. Check your connection and try again.';
}

/**
 * A-01 — Admin sign in (docs/SCREEN_SPECIFICATIONS.md).
 *
 * Email + password only; no self-registration. Validation, throttling, and
 * credential failures surface inline and are announced; the exported form
 * works with the keyboard alone.
 */
export default function LoginPage() {
  const { status, notice, signIn } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const emailRef = useRef<HTMLInputElement>(null);
  const passwordRef = useRef<HTMLInputElement>(null);

  if (status === 'checking') {
    return <FullPageLoading />;
  }

  if (status === 'authenticated') {
    return <Navigate to="/products" replace />;
  }

  function validate(): boolean {
    const errors: FieldErrors = {};
    const trimmedEmail = email.trim();

    if (!trimmedEmail) {
      errors.email = 'Email is required.';
    } else if (!EMAIL_PATTERN.test(trimmedEmail)) {
      errors.email = 'Enter a valid email address.';
    }

    if (!password) {
      errors.password = 'Password is required.';
    }

    setFieldErrors(errors);

    if (errors.email) {
      emailRef.current?.focus();
      return false;
    }

    if (errors.password) {
      passwordRef.current?.focus();
      return false;
    }

    return true;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);

    if (!validate()) {
      return;
    }

    setSubmitting(true);

    try {
      await signIn(email.trim(), password);
      navigate('/products', { replace: true });
    } catch (error) {
      if (error instanceof ApiError && error.status === 422) {
        const mapped: FieldErrors = {};
        for (const detail of error.details) {
          if (detail.field === 'email' || detail.field === 'password') {
            mapped[detail.field] = detail.message;
          }
        }

        if (Object.keys(mapped).length > 0) {
          setFieldErrors(mapped);
          setFormError('Check the highlighted fields and try again.');
        } else {
          setFormError(error.message);
        }
      } else {
        setFormError(describeFailure(error));
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-10">
      <div className="w-full max-w-md">
        <div className="mb-6 text-center">
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-primary-strong">EShop Verse</p>
          <h1 className="mt-2 text-2xl font-bold tracking-tight">Admin sign in</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Use your administrator account. Customers sign in from the mobile app.
          </p>
        </div>

        <form
          noValidate
          aria-label="Admin sign in"
          onSubmit={(event) => void handleSubmit(event)}
          className="rounded-3xl border border-border bg-card p-6 shadow-glass sm:p-8"
        >
          {notice && (
            <p role="status" className="mb-4 rounded-xl bg-muted px-3 py-2 text-sm">
              {notice}
            </p>
          )}

          {formError && (
            <p
              role="alert"
              className="mb-4 rounded-xl border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
            >
              {formError}
            </p>
          )}

          <label htmlFor="admin-email" className="block text-sm font-semibold">
            Email
          </label>
          <input
            ref={emailRef}
            id="admin-email"
            name="email"
            type="email"
            autoComplete="email"
            inputMode="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            aria-invalid={fieldErrors.email !== undefined}
            aria-describedby={fieldErrors.email ? 'admin-email-error' : undefined}
            className="mt-1 min-h-11 w-full rounded-xl border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          />
          {fieldErrors.email && (
            <p id="admin-email-error" className="mt-1 text-xs text-destructive">
              {fieldErrors.email}
            </p>
          )}

          <label htmlFor="admin-password" className="mt-4 block text-sm font-semibold">
            Password
          </label>
          <input
            ref={passwordRef}
            id="admin-password"
            name="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            aria-invalid={fieldErrors.password !== undefined}
            aria-describedby={fieldErrors.password ? 'admin-password-error' : undefined}
            className="mt-1 min-h-11 w-full rounded-xl border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          />
          {fieldErrors.password && (
            <p id="admin-password-error" className="mt-1 text-xs text-destructive">
              {fieldErrors.password}
            </p>
          )}

          <button
            type="submit"
            disabled={submitting}
            aria-busy={submitting}
            className="mt-6 flex min-h-11 w-full items-center justify-center rounded-full bg-primary px-6 text-sm font-bold text-primary-foreground transition-opacity hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-60"
          >
            {submitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <p className="mt-4 text-center text-xs text-muted-foreground">
          No admin self-registration — accounts are provisioned by the backend.
        </p>
      </div>
    </main>
  );
}
