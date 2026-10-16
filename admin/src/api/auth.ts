import { apiRequest, ensureCsrfCookie } from './http';
import type { AdminUser } from './types';

interface UserEnvelope {
  data: { user: AdminUser };
}

/**
 * Admin session endpoints (docs/openapi.yaml).
 *
 * The SPA authenticates with the first-party Sanctum session cookie only:
 * it reads the CSRF cookie before mutations and never stores bearer tokens
 * or passwords in browser storage.
 */

/** `GET /admin/auth/session` — the signed-in administrator, or 401. */
export async function fetchAdminSession(): Promise<AdminUser> {
  const payload = await apiRequest<UserEnvelope>('/admin/auth/session');
  return payload.data.user;
}

/** `POST /admin/auth/login` — starts an admin session. */
export async function loginAdmin(email: string, password: string): Promise<AdminUser> {
  await ensureCsrfCookie();
  const payload = await apiRequest<UserEnvelope>('/admin/auth/login', {
    method: 'POST',
    body: { email, password },
  });
  return payload.data.user;
}

/** `POST /admin/auth/logout` — ends the admin session. */
export async function logoutAdmin(): Promise<void> {
  await ensureCsrfCookie();
  await apiRequest<void>('/admin/auth/logout', { method: 'POST' });
}
