import { afterEach, describe, expect, it, vi } from 'vitest';
import { ADMIN_UNAUTHORIZED_EVENT, ApiError, apiRequest, ensureCsrfCookie } from './http';
import { jsonResponse, mockApi } from '../test/apiMock';

const CSRF_COOKIE = 'XSRF-TOKEN';

function clearCookies() {
  document.cookie = `${CSRF_COOKIE}=; expires=Thu, 01 Jan 1970 00:00:00 GMT`;
}

afterEach(() => {
  clearCookies();
  vi.unstubAllGlobals();
});

describe('apiRequest', () => {
  it('mutation_sends_session_cookie_and_decoded_xsrf_header', async () => {
    document.cookie = `${CSRF_COOKIE}=abc%3D%3D`;
    const fetchMock = mockApi([
      { method: 'POST', path: '/admin/auth/logout', handler: () => jsonResponse(204) },
    ]);

    await apiRequest('/admin/auth/logout', { method: 'POST' });

    const init = fetchMock.mock.calls[0][1] as RequestInit;
    const headers = init.headers as Record<string, string>;
    expect(init.credentials).toBe('include');
    expect(headers.Accept).toBe('application/json');
    expect(headers['X-XSRF-TOKEN']).toBe('abc==');
  });

  it('get_requests_send_neither_content_type_nor_xsrf_header', async () => {
    document.cookie = `${CSRF_COOKIE}=abc`;
    const fetchMock = mockApi([
      {
        method: 'GET',
        path: '/admin/auth/session',
        handler: () => jsonResponse(200, { data: { user: { id: 1 } } }),
      },
    ]);

    await apiRequest('/admin/auth/session');

    const headers = (fetchMock.mock.calls[0][1] as RequestInit).headers as Record<string, string>;
    expect(headers['X-XSRF-TOKEN']).toBeUndefined();
    expect(headers['Content-Type']).toBeUndefined();
  });

  it('error_envelope_becomes_api_error_with_code_details_and_field_errors', async () => {
    mockApi([
      {
        method: 'GET',
        path: '/admin/products',
        handler: () =>
          jsonResponse(422, {
            error: {
              code: 'VALIDATION_ERROR',
              message: 'One or more request fields are invalid.',
              details: [{ field: 'name', message: 'The name field is required.' }],
            },
          }),
      },
    ]);

    const error = await apiRequest('/admin/products').catch((reason: unknown) => reason);

    expect(error).toBeInstanceOf(ApiError);
    const apiError = error as ApiError;
    expect(apiError.status).toBe(422);
    expect(apiError.code).toBe('VALIDATION_ERROR');
    expect(apiError.fieldError('name')).toBe('The name field is required.');
    expect(apiError.fieldError('sku')).toBeNull();
  });

  it('rate_limited_response_exposes_retry_after_seconds', async () => {
    mockApi([
      {
        method: 'GET',
        path: '/admin/products',
        handler: () =>
          jsonResponse(
            429,
            { error: { code: 'RATE_LIMITED', message: 'Too many requests.' } },
            { 'Retry-After': '30' },
          ),
      },
    ]);

    const error = (await apiRequest('/admin/products').catch(
      (reason: unknown) => reason,
    )) as ApiError;

    expect(error.retryAfterSeconds).toBe(30);
  });

  it('401_response_dispatches_the_admin_unauthorized_event', async () => {
    const listener = vi.fn();
    window.addEventListener(ADMIN_UNAUTHORIZED_EVENT, listener);

    mockApi([
      {
        method: 'GET',
        path: '/admin/auth/session',
        handler: () =>
          jsonResponse(401, {
            error: { code: 'UNAUTHENTICATED', message: 'Authentication is required.' },
          }),
      },
    ]);

    await apiRequest('/admin/auth/session').catch(() => undefined);

    expect(listener).toHaveBeenCalledTimes(1);
    window.removeEventListener(ADMIN_UNAUTHORIZED_EVENT, listener);
  });

  it('204_response_resolves_with_undefined', async () => {
    mockApi([{ method: 'GET', path: '/admin/auth/csrf', handler: () => jsonResponse(204) }]);

    await expect(ensureCsrfCookie()).resolves.toBeUndefined();
  });

  it('csrf_mismatch_refreshes_the_cookie_and_replays_the_mutation_once', async () => {
    let loginAttempts = 0;
    const fetchMock = mockApi([
      {
        method: 'POST',
        path: '/admin/auth/login',
        handler: () => {
          loginAttempts += 1;
          return loginAttempts === 1
            ? jsonResponse(403, {
                error: {
                  code: 'CSRF_TOKEN_MISMATCH',
                  message: 'The X-XSRF-TOKEN header is missing or does not match the cookie.',
                },
              })
            : jsonResponse(200, { data: { user: { id: 1, email: 'demo-admin@eshopverse.test' } } });
        },
      },
      { method: 'GET', path: '/admin/auth/csrf', handler: () => jsonResponse(204) },
    ]);

    const payload = await apiRequest<{ data: { user: { id: number } } }>('/admin/auth/login', {
      method: 'POST',
      body: { email: 'demo-admin@eshopverse.test', password: 'secret' },
    });

    expect(payload.data.user.id).toBe(1);
    expect(loginAttempts).toBe(2);
    expect(fetchMock).toHaveBeenCalledTimes(3);
  });

  it('csrf_mismatch_is_not_retried_more_than_once', async () => {
    mockApi([
      {
        method: 'POST',
        path: '/admin/auth/login',
        handler: () =>
          jsonResponse(403, {
            error: { code: 'CSRF_TOKEN_MISMATCH', message: 'Mismatch.' },
          }),
      },
      { method: 'GET', path: '/admin/auth/csrf', handler: () => jsonResponse(204) },
    ]);

    const error = (await apiRequest('/admin/auth/login', { method: 'POST', body: {} }).catch(
      (reason: unknown) => reason,
    )) as ApiError;

    expect(error.code).toBe('CSRF_TOKEN_MISMATCH');
    expect(error.status).toBe(403);
  });
});
