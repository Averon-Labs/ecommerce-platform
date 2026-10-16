import { API_BASE_URL } from '../config/api';

/** One field-level problem from the documented `VALIDATION_ERROR` envelope. */
export interface ApiErrorDetail {
  field?: string;
  message: string;
}

/** Error envelope from docs/openapi.yaml: `{ error: { code, message, details? } }`. */
interface ErrorEnvelope {
  error?: {
    code?: string;
    message?: string;
    details?: ApiErrorDetail[];
  };
}

/**
 * Failure returned by the API, carrying the documented error code, optional
 * field-level details, and the `Retry-After` hint for rate-limited requests.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly details: ApiErrorDetail[];
  readonly retryAfterSeconds: number | null;

  constructor(
    status: number,
    code: string,
    message: string,
    details: ApiErrorDetail[] = [],
    retryAfterSeconds: number | null = null,
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.details = details;
    this.retryAfterSeconds = retryAfterSeconds;
  }

  /** First validation message for a field, when the API reported one. */
  fieldError(field: string): string | null {
    return this.details.find((detail) => detail.field === field)?.message ?? null;
  }
}

/**
 * Broadcast when an authenticated request is rejected with 401, so the auth
 * provider can end the local session and route the admin back to sign-in.
 */
export const ADMIN_UNAUTHORIZED_EVENT = 'eshopverse:admin-unauthorized';

const CSRF_COOKIE = 'XSRF-TOKEN';
const CSRF_HEADER = 'X-XSRF-TOKEN';
const CSRF_MISMATCH_CODE = 'CSRF_TOKEN_MISMATCH';

/** Read a cookie value, mirroring how the SPA reads `XSRF-TOKEN`. */
export function readCookie(name: string): string | null {
  const prefix = `${name}=`;
  const entry = document.cookie.split('; ').find((part) => part.startsWith(prefix));
  return entry ? decodeURIComponent(entry.slice(prefix.length)) : null;
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  signal?: AbortSignal;
}

function parseBody(text: string): unknown {
  if (!text) {
    return undefined;
  }

  try {
    return JSON.parse(text);
  } catch {
    return undefined;
  }
}

/**
 * Call the admin API with the first-party session cookie.
 *
 * Every request sends `credentials: 'include'` and `Accept: application/json`
 * so failure states come back as the documented JSON envelope. Mutations
 * echo the readable `XSRF-TOKEN` cookie in `X-XSRF-TOKEN`; no token is ever
 * kept in storage. A single automatic recovery is attempted when the API
 * reports a rotated CSRF cookie.
 */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  return send<T>(path, options, true);
}

async function send<T>(
  path: string,
  options: RequestOptions,
  allowCsrfRetry: boolean,
): Promise<T> {
  const method = options.method ?? 'GET';
  const headers: Record<string, string> = { Accept: 'application/json' };

  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  if (method !== 'GET') {
    const token = readCookie(CSRF_COOKIE);
    if (token) {
      headers[CSRF_HEADER] = token;
    }
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    credentials: 'include',
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    signal: options.signal,
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const payload = parseBody(await response.text());

  if (!response.ok) {
    const envelope = (payload ?? {}) as ErrorEnvelope;
    const code = envelope.error?.code ?? 'INTERNAL_ERROR';
    const message = envelope.error?.message ?? 'Something went wrong. Try again.';

    // A rotated CSRF cookie (for example, the session restarted in another
    // tab) is recoverable: refresh the cookie once and replay the mutation.
    if (allowCsrfRetry && response.status === 403 && code === CSRF_MISMATCH_CODE && method !== 'GET') {
      await ensureCsrfCookie();
      return send<T>(path, options, false);
    }

    const retryAfter = Number.parseInt(response.headers.get('Retry-After') ?? '', 10);
    const error = new ApiError(
      response.status,
      code,
      message,
      envelope.error?.details ?? [],
      Number.isFinite(retryAfter) ? retryAfter : null,
    );

    if (error.status === 401) {
      window.dispatchEvent(new CustomEvent(ADMIN_UNAUTHORIZED_EVENT));
    }

    throw error;
  }

  return payload as T;
}

/** `GET /admin/auth/csrf` — establishes the `XSRF-TOKEN` cookie to echo. */
export async function ensureCsrfCookie(): Promise<void> {
  await send<void>('/admin/auth/csrf', { method: 'GET' }, false);
}
