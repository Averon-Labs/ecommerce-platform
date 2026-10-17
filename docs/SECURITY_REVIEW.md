# Security and Privacy Review — QUAL-003

**Component:** Backend (Laravel API) — review 2026-10-14; Admin SPA (React dashboard) — review 2026-10-17
**Scope:** Implemented first-release controls reviewed against [`SECURITY.md`](SECURITY.md) and the [OWASP API Security Top 10](https://owasp.org/API-Security/), covering the backend and admin-SPA portions of the cross-component QUAL-003 task. Android controls are that component's contribution (pending).

Each finding records severity, evidence, and resolution. "Fixed" findings carry a regression test. This is a portfolio-demo review, not a production audit.

---

## Backend (Laravel API) — review 2026-10-14

### F-1 — Global rate limiter was not applied (Medium) — FIXED

- **Risk:** OWASP API4 — Unrestricted resource consumption.
- **Evidence:** `docs/openapi.yaml` `x-rate-limit-classes` defines `global: { limit: 300, window: 1m, key: ip }`, which "applies to every operation … in addition to" the narrower per-operation classes. Before this phase the backend registered only the narrower limiters (`public-read`, `auth-*`, `customer-write`, `admin-*`). The operations the contract lists as "governed by `global` only" — `getCustomerProfile`, `getCart`, `listOrders`, `getOrder`, `getAdminCsrfCookie`, `getAdminSession`, `adminListProducts`, `adminGetProduct`, `adminListCategories`, `adminListInventory`, `adminListOrders`, `adminGetOrder` — plus `password-reset/confirm`, carried no throttle at all, leaving them open to unrestricted requests.
- **Resolution:** Registered the `global` limiter (300/min/ip) in `AppServiceProvider` and appended `throttle:global` to the whole `api` middleware group in `bootstrap/app.php`, so it runs on every `/api/v1` operation and stacks on top of the narrower per-route limiters exactly as the contract describes.
- **Regression test:** `tests/Feature/Quality/RateLimitRegressionTest.php` — a documented global-only endpoint (`GET /cart`) now answers `429 RATE_LIMITED` in the shared envelope once the (test-lowered) global budget is spent.

### F-2 — Dependency advisories (Informational) — none found

- **Evidence:** `composer audit` → "No security vulnerability advisories found." `composer.validate --strict` → "`composer.json` is valid." Dependencies are version-pinned via the committed `composer.lock`.

### F-3 — Secrets and demo data (Informational) — clean

- **Evidence:** `backend/.env` is git-ignored (`.gitignore` lines 3–5) and no `.env` is committed; `.env.example` contains placeholders only (empty `DB_PASSWORD`, `DEMO_ADMIN_PASSWORD`, `APP_KEY`, AWS keys). The demo administrator password comes from `DEMO_ADMIN_PASSWORD` (git-ignored) or is generated randomly per install; no reusable public credential or real personal/payment data exists in seeders, tests, or repository history.

### F-4 — Authentication, session, and CSRF (Informational) — verified in earlier phases

- Sanctum bearer tokens (Android) and stateful cookie sessions + CSRF (admin SPA) with server-enforced admin role (`EnsureUserIsAdmin`). Object-level ownership returns `404` (never `403`) so resources are never confirmed to non-owners. Covered by the AUTH-001/AUTH-003/CAT-003/ORD-001 feature tests; the object-level boundary is re-exercised in `PurchaseFlowRegressionTest`.

### F-5 — Input validation, data exposure, and logging (Informational) — verified

- All input is validated server-side through `ApiFormRequest` (unknown fields rejected). Data access uses Eloquent/parameterized queries (no concatenated SQL). The shared exception handler renders a user-safe envelope (`{error:{code,message[,details]}}`) with no stack traces, SQL, or file paths; passwords are bcrypt-hashed; no password, token, or PII is logged. The simulated payment stores no card data.

---

## Admin SPA (React dashboard) — review 2026-10-17

Admin contribution to QUAL-003 per [`ADMIN_ROADMAP.md`](ADMIN_ROADMAP.md) Phase 3 Step 3: session/CSRF behavior, role boundaries, browser storage, error states, and dependency risks. No admin-side defect was found; findings are informational verifications, plus the accepted limitations below. Live probes ran against the local API with disposable data only (a fresh guest CSRF handshake, the demo administrator from `DEMO_ADMIN_PASSWORD`, and one freshly registered non-admin account).

### A-1 — Dependency advisories (Informational) — none found

- **Evidence:** `npm audit` and `npm audit --omit=dev` both report "found 0 vulnerabilities". Runtime dependencies are exact-pinned (`react` 19.3.1, `react-dom` 19.3.1, `react-router-dom` 7.18.4) with a committed `package-lock.json`; the QUAL-002 self-hosted Lato files are static OFL assets, not a dependency.

### A-2 — Session and CSRF behavior (Informational) — verified

- **Evidence (live):** the guest CSRF handshake (`GET /admin/auth/csrf`) sets a JS-readable `XSRF-TOKEN` cookie (Sanctum first-party SPA design; that cookie alone is not HttpOnly) and a guest `eshop_admin_session` cookie; admin login returns `200` and the authenticated session cookie is `HttpOnly` and `SameSite=Lax`; the session identifier rotates on login (fixation protection); a mutation without the `X-XSRF-TOKEN` header answers `403 CSRF_TOKEN_MISMATCH`; logout ends the session (`401` afterwards). The SPA (`src/api/http.ts`) reads the cookie only to echo it, never keeps a token in storage, retries a rotated-cookie mutation at most once, and ends the local session on any `401`.
- **Resolution:** none required.

### A-3 — Role boundaries (Informational) — verified

- **Evidence:** a signed-in non-admin account's stateful session receives `401 UNAUTHENTICATED` on `GET /admin/auth/session`; the `403 FORBIDDEN` demoted-session path (backend `AdminAuthTest`) surfaces the "This account is not an administrator." notice and routes to sign-in (`src/auth/AuthContext.tsx`); admin screens load exclusively through `/admin/*` endpoints that enforce the role server-side, so no client-side-only authorization exists.
- **Resolution:** none required.

### A-4 — Browser storage, client surface, and logging (Informational) — clean

- **Evidence:** source review — no `localStorage`/`sessionStorage` and no bearer tokens anywhere in `admin/src` (the only cookie read is the `XSRF-TOKEN` echo in `src/api/http.ts`); no `console` logging; no `dangerouslySetInnerHTML`, `innerHTML`, or `eval`; the API origin comes solely from the public `VITE_API_BASE_URL` (`.env.example` documents it contains no credentials).

### A-5 — CORS (Informational) — verified

- **Evidence (live):** simple and preflight requests carrying `Origin: https://evil.example` receive no `Access-Control-Allow-Origin`; the documented SPA origin (`http://localhost:5173`) is allowed with credentials; no wildcard origin is configured.

### A-6 — Security-relevant error states (Informational) — verified

- **Evidence:** `401`/`403` end the local session and route to sign-in with a notice; `409`/`422` keep the admin on the screen with field-level or conflict messages; `429` surfaces the `Retry-After` hint. All messages come from the user-safe envelope (no stack traces, SQL, or file paths) and map through the tested HTTP layer (`src/api/http.test.ts`, `src/auth/authFlow.test.tsx`, `src/auth/authVerification.test.tsx`).

---

## Accepted non-production limitations

Per [`SECURITY.md`](SECURITY.md) § Portfolio Demo Boundaries, and stated explicitly for the reviewer:

- **Payment is simulated.** No provider is contacted; no card number, security code, or live token is requested, accepted, logged, or stored. `DemoPayment` rows record an explicit outcome only.
- **No production readiness is claimed.** A real launch requires a separate threat model, privacy/legal review, deployment controls, monitoring, backup/recovery, and incident response.
- **Local development runs over loopback HTTP** (no TLS); a hosted deployment must terminate HTTPS and use a TLS-enabled MySQL connection (`MYSQL_ATTR_SSL_CA`), as documented in `DEVELOPMENT.md`. The admin session cookie is then expected to carry `Secure` (set `SESSION_SECURE_COOKIE=true` in the deployment environment; the checked-in `.env.example` keeps `false` for loopback).
- **Rate limits are tuned for demo traffic** (global 300/min/ip and the narrower classes) and would be re-derived for production scale.

The demo/non-production notice required by QUAL-003 criterion (d) lives in `SECURITY.md` § Portfolio Demo Boundaries and is mirrored in `HANDOFF.md`; the admin order detail additionally states that the demo payment method collects no card data.
