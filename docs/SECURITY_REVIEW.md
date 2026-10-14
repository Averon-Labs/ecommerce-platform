# Security and Privacy Review — Backend (QUAL-003)

**Component:** Backend (Laravel API)
**Date:** 2026-10-14
**Scope:** Implemented first-release controls reviewed against [`SECURITY.md`](SECURITY.md) and the [OWASP API Security Top 10](https://owasp.org/API-Security/), limited to the backend portion of the cross-component QUAL-003 task. Android and admin-SPA controls are their components' contributions.

Each finding records severity, evidence, and resolution. "Fixed" findings carry a regression test. This is a portfolio-demo review, not a production audit.

---

## Findings

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

## Accepted non-production limitations

Per [`SECURITY.md`](SECURITY.md) § Portfolio Demo Boundaries, and stated explicitly for the reviewer:

- **Payment is simulated.** No provider is contacted; no card number, security code, or live token is requested, accepted, logged, or stored. `DemoPayment` rows record an explicit outcome only.
- **No production readiness is claimed.** A real launch requires a separate threat model, privacy/legal review, deployment controls, monitoring, backup/recovery, and incident response.
- **Local development runs over loopback HTTP** (no TLS); a hosted deployment must terminate HTTPS and use a TLS-enabled MySQL connection (`MYSQL_ATTR_SSL_CA`), as documented in `DEVELOPMENT.md`.
- **Rate limits are tuned for demo traffic** (global 300/min/ip and the narrower classes) and would be re-derived for production scale.

The demo/non-production notice required by QUAL-003 criterion (d) lives in `SECURITY.md` § Portfolio Demo Boundaries and is mirrored in `HANDOFF.md`.
