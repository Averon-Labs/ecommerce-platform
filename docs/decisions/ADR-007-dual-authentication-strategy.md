# ADR-007: Dual Authentication Strategy: Stateful SPA Sessions and Revocable Mobile Bearer Tokens

## Status

PROPOSED

## Date

2026-10-15

## Context

EShop Verse serves two distinct client environments with different security boundaries:
1. **Admin Dashboard:** A web single-page application (React/Vite) running inside browser sandboxes subject to Cross-Site Scripting (XSS) and Cross-Site Request Forgery (CSRF) attack vectors.
2. **Android Application:** A native mobile application running within the Android OS app sandbox.

A single authentication pattern (e.g. Bearer tokens in localStorage or basic session cookies on mobile) does not adhere to modern security best practices across both platforms.

## Options Considered

### Option A: Universal Bearer Tokens across Web and Mobile
- **Pros:** Unified backend authentication middleware and endpoints.
- **Cons:** Web SPAs storing bearer tokens in `localStorage` or `sessionStorage` are vulnerable to token exfiltration via XSS.

### Option B: Universal Cookie Sessions
- **Pros:** Standard web cookie handling.
- **Cons:** Cookie management and CSRF handshakes in native mobile apps introduce unnecessary friction and state management issues.

### Option C: Dual Authentication Architecture via Laravel Sanctum (Selected)
- **Admin Dashboard (SPA):** Uses stateful cookie authentication (`SESSION_COOKIE=eshop_admin_session`) with `HttpOnly`, `SameSite=Lax`, and `X-XSRF-TOKEN` CSRF verification.
- **Android Customer App:** Uses revocable personal access tokens (Bearer tokens) stored securely in `EncryptedSharedPreferences` backed by the Android Keystore (ADR-003).

## Decision

Implement the dual authentication architecture:
1. Admin routes (`/api/v1/admin/*`) require stateful session cookie authentication and enforce CSRF verification through the `statefulApi()` middleware pipeline.
2. Customer mobile routes (`/api/v1/*`) accept stateless `Authorization: Bearer <token>` headers issued via Laravel Sanctum's personal access tokens table.
3. Customer accounts are strictly prohibited from accessing admin endpoints, enforced server-side by `EnsureUserIsAdmin` middleware.

## Consequences

- Prevents XSS token exfiltration in the admin web dashboard.
- Maintains standard native Android mobile token lifecycle backed by hardware encryption (ADR-003).
- Requires configuring `SANCTUM_STATEFUL_DOMAINS` in backend configuration for development origins (e.g. `localhost:5173`).
