/**
 * Admin user record returned by the session endpoints.
 *
 * Mirrors the `User` schema in docs/openapi.yaml (the same user shape the
 * customer API documents); `role` is `admin` for dashboard accounts.
 */
export interface AdminUser {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  status: string;
  createdAt?: string;
}
