import { act, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from '../App';
import { ApiError, apiRequest } from '../api/http';
import { jsonResponse, mockApi } from '../test/apiMock';
import type { MockRoute } from '../test/apiMock';

/**
 * Independent verification checks for AUTH-003 (admin SPA portion).
 *
 * These complement `authFlow.test.tsx` by driving the real `apiRequest` 401
 * path end to end (not the exported event directly), by covering the
 * URL-encoded CSRF cookie value, and by pinning the local-session recovery
 * and 403/429 fallback behavior.
 */

const ADMIN_USER = {
  id: 2,
  email: 'demo-admin@eshopverse.test',
  firstName: 'Demo',
  lastName: 'Administrator',
  role: 'admin',
  status: 'active',
} as const;

function sessionSignedIn(): MockRoute {
  return {
    method: 'GET',
    path: '/admin/auth/session',
    handler: () => jsonResponse(200, { data: { user: ADMIN_USER } }),
  };
}

function sessionGuest(): MockRoute {
  return {
    method: 'GET',
    path: '/admin/auth/session',
    handler: () =>
      jsonResponse(401, {
        error: { code: 'UNAUTHENTICATED', message: 'Authentication is required.' },
      }),
  };
}

function csrfRoute(): MockRoute {
  return { method: 'GET', path: '/admin/auth/csrf', handler: () => jsonResponse(204) };
}

function loginRoute(handler: MockRoute['handler']): MockRoute {
  return { method: 'POST', path: '/admin/auth/login', handler };
}

function loginSuccess(): MockRoute {
  return loginRoute(() => jsonResponse(200, { data: { user: ADMIN_USER } }));
}

function productsRoute(handler: MockRoute['handler']): MockRoute {
  return { method: 'GET', path: '/admin/products', handler };
}

function typeCredentials(email: string, password = 'Password123') {
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: email } });
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: password } });
}

async function waitForSignInHeading() {
  return screen.findByRole('heading', { level: 1, name: 'Admin sign in' }, { timeout: 3000 });
}

async function waitForProductsHeading() {
  return screen.findByRole('heading', { level: 1, name: 'Products' }, { timeout: 3000 });
}

beforeEach(() => {
  window.history.pushState({}, '', '/');
});

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT';
});

describe('AUTH-003 independent verification', () => {
  it('unauthorized_authenticated_request_ends_the_session_and_sign_in_recovers', async () => {
    mockApi([
      sessionSignedIn(),
      productsRoute(() =>
        jsonResponse(401, {
          error: {
            code: 'UNAUTHENTICATED',
            message: 'Authentication is required for this endpoint.',
          },
        }),
      ),
      csrfRoute(),
      loginSuccess(),
    ]);
    render(<App />);

    await waitForProductsHeading();

    await act(async () => {
      await apiRequest('/admin/products').catch(() => undefined);
    });

    await waitForSignInHeading();
    expect(screen.getByRole('status')).toHaveTextContent('Your session expired. Sign in again.');

    // The expired-session state is recoverable: signing in again reaches the shell.
    typeCredentials(ADMIN_USER.email);
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));
    await waitForProductsHeading();
  }, 20000);

  it('sign_out_echoes_the_url_decoded_csrf_cookie_with_credentials', async () => {
    document.cookie = 'XSRF-TOKEN=abc%3D%3D';
    const fetchMock = mockApi([
      sessionSignedIn(),
      csrfRoute(),
      { method: 'POST', path: '/admin/auth/logout', handler: () => jsonResponse(204) },
    ]);
    render(<App />);

    await waitForProductsHeading();
    fireEvent.click(screen.getByRole('button', { name: /account menu/i }));
    fireEvent.click(await screen.findByRole('menuitem', { name: 'Sign out' }));
    await waitForSignInHeading();

    const logoutCall = fetchMock.mock.calls.find(
      ([url, init]) =>
        String(url).endsWith('/admin/auth/logout') &&
        (init as RequestInit | undefined)?.method === 'POST',
    );
    expect(logoutCall).toBeDefined();
    const init = logoutCall?.[1] as RequestInit;
    expect((init.headers as Record<string, string>)['X-XSRF-TOKEN']).toBe('abc==');
    expect(init.credentials).toBe('include');
  }, 15000);

  it('sign_out_still_ends_the_local_session_when_the_api_is_unreachable', async () => {
    document.cookie = 'XSRF-TOKEN=test-token';
    mockApi([
      sessionSignedIn(),
      {
        method: 'GET',
        path: '/admin/auth/csrf',
        handler: () => {
          throw new TypeError('Network request failed');
        },
      },
    ]);
    render(<App />);

    await waitForProductsHeading();
    fireEvent.click(screen.getByRole('button', { name: /account menu/i }));
    fireEvent.click(await screen.findByRole('menuitem', { name: 'Sign out' }));

    await waitForSignInHeading();
    expect(screen.queryByText('Your session expired. Sign in again.')).not.toBeInTheDocument();
  }, 15000);

  it('failed_sign_in_with_a_server_error_preserves_email_and_password', async () => {
    mockApi([
      sessionGuest(),
      csrfRoute(),
      loginRoute(() =>
        jsonResponse(500, {
          error: { code: 'INTERNAL_ERROR', message: 'An unexpected error occurred.' },
        }),
      ),
    ]);
    render(<App />);

    await waitForSignInHeading();
    typeCredentials(ADMIN_USER.email);
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'The server could not complete the sign-in. Try again.',
    );
    expect(screen.getByLabelText('Email')).toHaveValue(ADMIN_USER.email);
    expect(screen.getByLabelText('Password')).toHaveValue('Password123');
  });

  it('rate_limited_login_without_retry_after_shows_the_generic_hint', async () => {
    mockApi([
      sessionGuest(),
      csrfRoute(),
      loginRoute(() =>
        jsonResponse(429, {
          error: { code: 'RATE_LIMITED', message: 'Too many requests.' },
        }),
      ),
    ]);
    render(<App />);

    await waitForSignInHeading();
    typeCredentials(ADMIN_USER.email);
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Too many sign-in attempts. Try again shortly.',
    );
  });

  it('sign_in_form_is_label_associated_and_submits_without_a_pointer', async () => {
    const fetchMock = mockApi([sessionGuest(), csrfRoute(), loginSuccess()]);
    render(<App />);

    await waitForSignInHeading();
    expect(screen.getByLabelText('Email')).toHaveAccessibleName('Email');
    expect(screen.getByLabelText('Password')).toHaveAccessibleName('Password');
    expect(screen.getByRole('button', { name: 'Sign in' })).toHaveAttribute('type', 'submit');

    typeCredentials(ADMIN_USER.email);
    fireEvent.submit(screen.getByRole('form', { name: 'Admin sign in' }));

    await waitForProductsHeading();
    expect(
      fetchMock.mock.calls.some(([, init]) => (init as RequestInit | undefined)?.method === 'POST'),
    ).toBe(true);
  }, 15000);

  it('forbidden_authenticated_request_is_surfaced_without_ending_the_session', async () => {
    mockApi([
      sessionSignedIn(),
      productsRoute(() =>
        jsonResponse(403, {
          error: { code: 'FORBIDDEN', message: 'Administrator access is required.' },
        }),
      ),
    ]);
    render(<App />);

    await waitForProductsHeading();

    let error: unknown;
    await act(async () => {
      error = await apiRequest('/admin/products').catch((reason: unknown) => reason);
    });

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(403);
    expect((error as ApiError).code).toBe('FORBIDDEN');
    // Server-side authorization is not masked by the client: the shell stays,
    // no local session is discarded, and no expiry notice is invented.
    expect(screen.getByRole('heading', { level: 1, name: 'Products' })).toBeInTheDocument();
    expect(screen.queryByText('Your session expired. Sign in again.')).not.toBeInTheDocument();
  });

  it('non_admin_session_boot_shows_the_access_notice_and_stays_recoverable', async () => {
    mockApi([
      {
        method: 'GET',
        path: '/admin/auth/session',
        handler: () =>
          jsonResponse(403, {
            error: { code: 'FORBIDDEN', message: 'This account is no longer an administrator.' },
          }),
      },
      csrfRoute(),
      loginSuccess(),
    ]);
    render(<App />);

    await waitForSignInHeading();
    expect(await screen.findByRole('status')).toHaveTextContent(
      'This account is not an administrator.',
    );

    typeCredentials(ADMIN_USER.email);
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));
    await waitForProductsHeading();
  }, 15000);
});
