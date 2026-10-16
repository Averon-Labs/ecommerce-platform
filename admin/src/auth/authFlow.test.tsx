import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from '../App';
import { ADMIN_UNAUTHORIZED_EVENT } from '../api/http';
import { jsonResponse, mockApi } from '../test/apiMock';
import type { MockRoute } from '../test/apiMock';

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

async function waitForSignInHeading() {
  return screen.findByRole('heading', { level: 1, name: 'Admin sign in' });
}

async function waitForProductsHeading() {
  return screen.findByRole('heading', { level: 1, name: 'Products' });
}

function typeCredentials(email: string, password = 'Password123') {
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: email } });
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: password } });
}

beforeEach(() => {
  window.history.pushState({}, '', '/');
  window.localStorage.clear();
});

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT';
});

describe('admin session UI', () => {
  it('guest_boot_shows_the_admin_sign_in_screen', async () => {
    mockApi([sessionGuest()]);
    render(<App />);

    await waitForSignInHeading();
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
    expect(screen.queryByText(ADMIN_USER.email)).not.toBeInTheDocument();
  });

  it('authenticated_boot_renders_the_shell_and_default_section', async () => {
    mockApi([sessionSignedIn()]);
    render(<App />);

    await waitForProductsHeading();
    expect(screen.getByRole('navigation', { name: 'Admin sections' })).toBeInTheDocument();
    expect(screen.getAllByText('Demo Administrator').length).toBeGreaterThan(0);
  });

  it('sign_in_flow_lands_on_products_without_browser_storage', async () => {
    const fetchMock = mockApi([sessionGuest(), csrfRoute(), loginSuccess()]);
    render(<App />);

    await waitForSignInHeading();
    typeCredentials(ADMIN_USER.email);
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitForProductsHeading();
    expect(window.localStorage.length).toBe(0);
    expect(
      fetchMock.mock.calls.some(
        ([, init]) => (init as RequestInit | undefined)?.method === 'POST',
      ),
    ).toBe(true);
  });

  it('invalid_credentials_show_an_error_and_preserve_the_email', async () => {
    mockApi([
      sessionGuest(),
      csrfRoute(),
      loginRoute(() =>
        jsonResponse(401, {
          error: { code: 'INVALID_CREDENTIALS', message: 'The email or password is incorrect.' },
        }),
      ),
    ]);
    render(<App />);

    await waitForSignInHeading();
    typeCredentials(ADMIN_USER.email);
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('The email or password is incorrect.');
    expect(screen.getByLabelText('Email')).toHaveValue(ADMIN_USER.email);
  });

  it('rate_limited_login_reports_the_retry_after_seconds', async () => {
    mockApi([
      sessionGuest(),
      csrfRoute(),
      loginRoute(() =>
        jsonResponse(
          429,
          { error: { code: 'RATE_LIMITED', message: 'Too many requests.' } },
          { 'Retry-After': '30' },
        ),
      ),
    ]);
    render(<App />);

    await waitForSignInHeading();
    typeCredentials(ADMIN_USER.email);
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Too many sign-in attempts. Try again in 30 seconds.',
    );
  });

  it('client_validation_blocks_the_request_and_focuses_the_first_invalid_field', async () => {
    const fetchMock = mockApi([sessionGuest()]);
    render(<App />);

    await waitForSignInHeading();
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByText('Email is required.')).toBeInTheDocument();
    expect(screen.getByText('Password is required.')).toBeInTheDocument();
    expect(document.activeElement).toBe(screen.getByLabelText('Email'));
    expect(
      fetchMock.mock.calls.filter(
        ([, init]) => (init as RequestInit | undefined)?.method === 'POST',
      ),
    ).toHaveLength(0);
  });

  it('server_validation_errors_map_onto_the_form_fields', async () => {
    mockApi([
      sessionGuest(),
      csrfRoute(),
      loginRoute(() =>
        jsonResponse(422, {
          error: {
            code: 'VALIDATION_ERROR',
            message: 'One or more request fields are invalid.',
            details: [
              { field: 'email', message: 'The email field must be a valid email address.' },
              { field: 'password', message: 'The password field is required.' },
            ],
          },
        }),
      ),
    ]);
    render(<App />);

    await waitForSignInHeading();
    typeCredentials(ADMIN_USER.email);
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(
      await screen.findByText('The email field must be a valid email address.'),
    ).toBeInTheDocument();
    expect(screen.getByText('The password field is required.')).toBeInTheDocument();
  });

  it('sign_out_posts_with_the_xsrf_header_and_returns_to_sign_in', async () => {
    document.cookie = 'XSRF-TOKEN=test-token';
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
    const headers = (logoutCall?.[1] as RequestInit).headers as Record<string, string>;
    expect(headers['X-XSRF-TOKEN']).toBe('test-token');
  });

  it('session_loss_returns_the_admin_to_sign_in_with_a_notice', async () => {
    mockApi([sessionSignedIn()]);
    render(<App />);

    await waitForProductsHeading();
    window.dispatchEvent(new CustomEvent(ADMIN_UNAUTHORIZED_EVENT));

    await waitForSignInHeading();
    expect(await screen.findByRole('status')).toHaveTextContent('Your session expired. Sign in again.');
  });

  it('non_admin_session_shows_the_access_notice_on_sign_in', async () => {
    mockApi([
      {
        method: 'GET',
        path: '/admin/auth/session',
        handler: () =>
          jsonResponse(403, {
            error: { code: 'FORBIDDEN', message: 'Administrator access is required.' },
          }),
      },
    ]);
    render(<App />);

    await waitForSignInHeading();
    expect(await screen.findByRole('status')).toHaveTextContent(
      'This account is not an administrator.',
    );
  });

  it('unreachable_api_is_recoverable_with_retry', async () => {
    let sessionCalls = 0;
    mockApi([
      {
        method: 'GET',
        path: '/admin/auth/session',
        handler: () => {
          sessionCalls += 1;
          if (sessionCalls === 1) {
            throw new TypeError('Network request failed');
          }
          return jsonResponse(200, { data: { user: ADMIN_USER } });
        },
      },
    ]);
    render(<App />);

    expect(await screen.findByRole('heading', { name: /reach the API/i })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

    await waitForProductsHeading();
  });
});
