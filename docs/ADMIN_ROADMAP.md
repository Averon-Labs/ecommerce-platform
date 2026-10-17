# Admin Roadmap

## Purpose and authority

Plan React/TypeScript dashboard work through the REST API, following shared dashboard design and session rules.

This file is the canonical task list for its component. Each task appears once here; stable Task IDs identify work across branches and PRs. Project-wide phase gates and cross-component tasks are canonical in [ROADMAP.md](ROADMAP.md). Follow [repository-wide agent instructions](../AGENTS.md), component guidance, authoritative documents linked by the task, and the actual implementation.

A roadmap assignment uses this roadmap name, phase, and step, for example: Admin Roadmap, Phase 1, Step 1; a stable Task ID may also identify the work. Step numbers restart at 1 in every phase. Lowercase letter labels restart at `a` in each acceptance-criteria block and identify individual criteria for discussion or verification, not separate assignments. Check dependencies before starting; a later step is not ready until its listed dependencies are complete. See [the project roadmap](ROADMAP.md) and [repository Git naming conventions](../AGENTS.md).

## Phase steps

## Phase 0 — Foundation

**Project phase gate:** see [Phase 0 in the Project Roadmap](ROADMAP.md).

### Step 1 — FND-003 — Initialize the admin application and local workflow

**Status:** `[x]`

**Depends on:** none

**Prerequisites to start:**
- Node.js 18+ available locally
- Package manager (npm/yarn/pnpm) available
- React 18+, TypeScript, Vite tooling decided
- Vitest and React Testing Library selected
- Local development CORS/origin strategy for Sanctum planned

**Scope:** create the React/TypeScript admin foundation; do not implement product workflows yet.

> ⚠️ **Developer Alignment & Backend Integration Notes for FND-003:**
> - **Port & Origins:** Backend has pre-configured `SANCTUM_STATEFUL_DOMAINS` with `localhost:5173` and `localhost:5174`. The React Vite dev server should run on port 5173 (`http://localhost:5173`).
> - **API Base URL:** Default local Laravel backend runs at `http://localhost:8000/api/v1`.
> - **Credentials & Cookies:** All HTTP client requests from Admin SPA must configure `credentials: 'include'` (withCredentials in Axios/Fetch) so Sanctum session cookies (`eshop_admin_session`) and `XSRF-TOKEN` are sent.

**Acceptance criteria:**

- [x] **a.** Select supported stable tooling and commit the package lockfile; use the agreed Vitest and React Testing Library strategy or document a reviewed equivalent.
- [x] **b.** Configure the API origin without committing credentials and document the local SPA/API origin arrangement needed for Sanctum cookies and CSRF.
- [x] **c.** Provide a minimal app and a passing behavior-oriented test.
- [x] **d.** Record and verify exact install, development, type-check/lint, test, and production-build commands in `docs/DEVELOPMENT.md`.
- [x] **e.** Do not call or implement product endpoints before FND-004 is reviewed.

**Verification:** clean dependency install, baseline UI test, static checks, and production build.

**Verification record (2026-10-16):** re-verified end to end at commit `0df6e2a`, then re-run after the test-isolation fix in `6091651`. From `admin/`: `rm -rf node_modules dist && npm ci` installed 192 packages from the committed `package-lock.json` with `npm audit` reporting 0 vulnerabilities; `npm run typecheck`, `npm run lint`, `npm test` (1 file, 2 tests), and `npm run build` (`dist/index.html` + one JS bundle) passed; `npm run dev` answered `GET http://localhost:5173/` with HTTP 200. Exact commands and outcomes are recorded in `docs/DEVELOPMENT.md`. During the recheck, React Testing Library cleanup was made explicit in `admin/src/setupTests.ts` because Vitest runs without globals — renders otherwise leaked between tests in the same file — and a second `App` test now guards that isolation. Criterion **b** is confirmed against `backend/.env.example` (`SANCTUM_STATEFUL_DOMAINS` lists `localhost:5173` and `127.0.0.1:5173`); criterion **e** holds — the app calls no product or auth endpoint. Tracked by [PR #42](https://github.com/Averon-Labs/eshop-verse/pull/42), which targets `develop`.

## Phase 1 — Accounts and Catalog

**Project phase gate:** see [Phase 1 in the Project Roadmap](ROADMAP.md).

### Step 1 — AUTH-003 — Admin session authentication and API authorization

**Status:** `[x]`

**Depends on:** FND-002, FND-003, FND-004, FND-005, DES-001

**Prerequisites to start:**
- **FND-002 complete** (Backend: Laravel app with Sanctum configured)
- FND-003 complete (Admin: React app initialized)
- **FND-004 complete** (Backend: OpenAPI contract with admin auth endpoints)
- **FND-005 complete** (Project: CI and documentation ready)
- **DES-001 complete** (Project: Admin login screen design finalized)
- Sanctum session/CSRF flow understood
- Local SPA/API origin arrangement documented
- Admin role enforcement rules defined in backend
- Session cookie secure settings defined

**BACKEND PORTION:** This task has a backend implementation component (admin auth endpoints, role middleware). Backend colleague must implement that part.

**Demo admin account:** created by a backend database seeder owned by the AUTH-003 backend portion; it is local/demo-only, is documented in the handoff guide (QUAL-004), and must never be a reusable public credential.

> ⚠️ **Developer Alignment & Backend Contract Notes for AUTH-003:**
> - **Backend Status:** The backend portion of AUTH-003 is ALREADY fully implemented in `AdminAuthController.php` and verified by tests.
> - **CSRF Handshake:** Call `GET /api/v1/admin/auth/csrf` to initialize the `XSRF-TOKEN` cookie before login. Send `X-XSRF-TOKEN` header on mutation requests (`POST /api/v1/admin/auth/login`, `POST /api/v1/admin/auth/logout`).
> - **Demo Admin Credentials:** `demo-admin@eshopverse.test` (password configured in local `.env` as `DEMO_ADMIN_PASSWORD`).
> - **Session Storage:** Do NOT store tokens or passwords in `localStorage`. Rely exclusively on HttpOnly session cookie and `GET /api/v1/admin/auth/session` on initial app boot.

**Scope:** secure first-party admin sign-in/session lifecycle, Sanctum CSRF flow, and server-enforced admin role checks.

**Acceptance criteria:**

- [x] **a.** Admin login/logout and session/CSRF behavior work with the documented local SPA/API origins and reviewed contract.
- [x] **b.** Admin routes reject guests and customer accounts; tests verify role checks server-side, not only hidden UI controls.
- [x] **c.** Session cookies use secure settings appropriate to local vs. HTTPS environments; no bearer token is stored in browser local storage for the admin SPA.
- [x] **d.** Failure and expired-session UI states are visible and recoverable.

**Verification:** backend authentication/authorization feature tests and admin UI tests for login, logout, CSRF/session expiry, and rejected access.

**Backend portion (2026-10-13):** implemented and independently verified on the backend — admin auth endpoints (`/admin/auth/csrf|login|session|logout`), Sanctum stateful cookie sessions + CSRF enforcement, `EnsureUserIsAdmin` role middleware, `admin-login`/`admin-write` rate limiters, and the demo-admin seeder (see Backend Roadmap — Phase 1 — Step 3 — CAT-003 verification record). Server-side behavior for criteria **a**/**b**/**c** is covered; the admin SPA work later completed criteria **c**/**d** and the admin UI tests (see the verification record below).

**Verification record (2026-10-16):** implemented in `c8224e2` (backend integration support), `d2a0cc0` (admin SPA), and independently verified at `58dc0ff` (test-only tester commit) — all criteria **a–d** PASS. Backend: `php artisan test` = 157 tests, 0 failures, including the new credentialed-CORS feature tests (`backend/tests/Feature/ApiCorsTest.php`) and the contract-conform 401 test for plain (non-JSON-`Accept`) requests; the reviewable integration changes are `redirectGuestsTo(null)` in `backend/bootstrap/app.php` and `backend/config/cors.php` (explicit `localhost:5173`/`localhost:5174` origins, `supports_credentials: true`, `CORS_ALLOWED_ORIGINS` override). Admin: `npm test` = 29 tests at this step (8 added by the tester), `npm run typecheck`, `npm run lint`, and `npm run build` all pass. The SPA performs its CSRF handshake at `GET /admin/auth/csrf`; live probes against the local API confirmed the full session handshake (CSRF cookie 204 with `XSRF-TOKEN`, login 200 with the admin JSON, `GET /admin/auth/session` 200, logout 204, re-session 401) plus wrong-password 401 `INVALID_CREDENTIALS`, missing-CSRF-header 403 `CSRF_TOKEN_MISMATCH`, customer-account 401 identical to unknown-account 401, and 401 rejection of every admin endpoint for guests; a 403 response for a non-admin session shows the visible "not an administrator" notice. Criterion **c** holds: the SPA derives auth state from `GET /admin/auth/session` and stores no token in `localStorage`; the session cookie is HttpOnly with `SameSite=Lax` in local development (Secure/None settings documented for HTTPS deployments in `backend/.env.example`). Criterion **d** holds: invalid credentials, rate limiting, unreachable service (boot retry), and expired sessions (global 401 → sign-in with an explanatory notice) are visible and recoverable. Known limitation: real-browser cross-origin behavior was verified with jsdom tests plus live `curl` probes, not a browser; browser E2E remains with QUAL-005. Tracked by [PR #43](https://github.com/Averon-Labs/eshop-verse/pull/43), which targets `develop`.

### Step 2 — CAT-004 — Admin catalog and inventory screens

**Status:** `[x]`

**Depends on:** FND-003, FND-004, AUTH-003, CAT-003, DES-001

**Prerequisites to start:**
- FND-003 complete (Admin: React app ready)
- **FND-004 complete** (Backend: OpenAPI contract with admin catalog endpoints)
- AUTH-003 complete (Admin: authentication/session working)
- **CAT-003 complete** (Backend: admin catalog/inventory API implemented)
- **DES-001 complete** (Project: Admin catalog/inventory screen designs finalized)
- Admin dashboard layout/navigation structure defined
- Form validation strategy decided
- Error state UI patterns defined
- Keyboard accessibility requirements understood

> ⚠️ **Developer Alignment & Contract Notes for CAT-004:**
> - **Category Lifecycle:** Categories use `status: 'active' | 'archived'`. Destructive deletion is not permitted; the SPA follows the authoritative contract in `docs/openapi.yaml`: edit with `PUT /api/v1/admin/categories/{id}`, archive with `POST /api/v1/admin/categories/{id}/archive`, and restore with `POST /api/v1/admin/categories/{id}/restore`. Backend rejects archiving while active products remain assigned to the category.

**Scope:** React admin screens for product, category, and stock management through the REST API.

**Acceptance criteria:**

- [x] **a.** Admin can list, create, edit, archive, and validate product/category data and update stock through documented APIs.
- [x] **b.** Loading, empty, validation, conflict, unauthorized/session-expired, and network-error states are understandable and recoverable.
- [x] **c.** Forms are keyboard accessible, have labels, and behave at supported narrow and desktop widths.
- [x] **d.** Tests cover critical form behavior and API error mapping without depending on backend internals.

**Verification:** admin component/behavior tests and production build/type checks.

**Verification record (2026-10-16):** implemented in `ecd64ee` (products A-02, product form A-03, categories A-04, inventory A-05, shared dialog/badge/pagination/state-panel/navigation-guard components, the catalog API module, and 20 new behavior tests) and independently verified at `8c4344f` (test-only commit on top of `ecd64ee`, +299 lines) — all criteria **a–d** PASS. The tester added 9 further tests plus one strengthened assertion (final total 58 tests in 8 files; three consecutive clean runs on the final content; typecheck, lint, and production build all clean) covering category status filtering, the categories empty state, product/category form validation boundaries and `422` field mapping, inventory error retry and in-dialog server-rejection handling, and that product filters and pagination follow API metadata. Live probes against the local admin API confirmed product create (201) → update (200) → archive (200) → re-archive `409 INVALID_PRODUCT_STATE`; category create (201) → duplicate-name `409 CATEGORY_NAME_TAKEN` → update (200) → archive (200) → restore (200) → archive non-empty `409 CATEGORY_NOT_EMPTY` with blocking details; inventory `PATCH` absolute quantity (200; zero ⇒ `inStock: false` in list responses); list filters `q`/`status`/`categoryId`; and `422 VALIDATION_ERROR` for `perPage=0`/`101`. Criterion **a** holds: listing/creating/editing/archiving and stock updates use only the documented `docs/openapi.yaml` endpoints. Criterion **b** holds: loading skeletons, empty catalog and filtered-empty panels, client validation plus server `422`/`409` field mapping, archive conflict messages, guest `401` routing, and recoverable network-error panels with retry. Criterion **c** holds: every field has a visible or programmatic label, dialogs move focus in, trap Tab, close on Escape, and restore focus, tables switch to labeled narrow-screen cards at the documented breakpoint, and actions meet minimum target sizes (source-verified; no browser E2E on this machine — the same accepted limitation as AUTH-003). Criterion **d** holds: tests assert behavior through the mocked HTTP layer only (no backend internals), covering list states, filters, pagination, archive confirm/refresh/conflict, form validation and `422`/`409` mapping, the unsaved-changes guard, category search and lifecycle, and inventory validation. Documented deviations: the sidebar lists only implemented sections (Orders arrives with ORD-003 in Phase 2); the category lifecycle follows the authoritative contract rather than the older `PATCH` note (corrected above); category search/status filtering runs client-side because the API returns the full list. Images are demo URLs only (no upload) and stock is managed exclusively in Inventory, both stated in the UI copy. Tracked by [PR #43](https://github.com/Averon-Labs/eshop-verse/pull/43), which targets `develop`.

## Phase 2 — Cart, Checkout, and Orders

**Project phase gate:** see [Phase 2 in the Project Roadmap](ROADMAP.md).

### Step 1 — ORD-003 — Admin order management

**Status:** `[x]`

**Depends on:** FND-003, FND-004, AUTH-003, ORD-001, DES-001

**Prerequisites to start:**
- FND-003 complete (Admin: React app ready)
- **FND-004 complete** (Backend: OpenAPI contract with admin order endpoints)
- AUTH-003 complete (Admin: authentication/session working)
- **ORD-001 complete** (Backend: order API with admin endpoints implemented)
- **DES-001 complete** (Project: Admin order management screen designs finalized)
- Order status transition rules defined in backend
- Admin order authorization rules defined
- Order snapshot display patterns defined

> ⚠️ **Developer Alignment & Contract Notes for ORD-003:**
> - **Permitted Order Status Transitions:** Valid transitions enforced by backend state machine:
>   - `pending_payment -> cancelled`
>   - `confirmed -> processing | cancelled`
>   - `processing -> shipped | cancelled`
>   - `shipped -> delivered`
>   - (`confirmed` can only be set via customer payment success, never manually by admin).
> - **Order Timeline:** Render the server-provided `timeline` array of `{status, at}` records.

**Scope:** admin order list/detail and permitted status updates.

**Acceptance criteria:**

- [x] **a.** Admin can inspect order snapshots and apply only documented valid state transitions.
- [x] **b.** Server rejects unauthorized users and invalid transitions; tests cover both.
- [x] **c.** Dashboard shows loading, empty, conflict, error, and updated status states.
- [x] **d.** Admin tests verify behavior through API responses, not implementation details.

**Verification:** backend transition/authorization feature tests and admin behavior tests.

**Verification record (2026-10-17):** implemented in `e468ec5` (A-06 order list with reference/customer search, status and date filters, API pagination, and state panels; A-07 order detail with immutable snapshots, payment outcome, timeline, and an allowed-only status action behind confirmation; orders API module, shared order/payment badges, Orders destination, and 24 order tests), independently verified at `3b36810` (test-only commit, 9 added tests — final total 82 tests in 10 files), and stabilized at `4357fd7` (test-only: product-form category-option race fixed, 5/8 → 0/8 isolated failures; auth verification waits raised to 8 s, 2/3 → 0/3 full-suite failures; retested independently at 6/6 isolated and 3/3 full-suite clean runs). All criteria **a–d** PASS. Admin checks: `npm run typecheck`, `npm run lint`, `npm test` (82 tests in 10 files, repeated clean runs), and `npm run build` all pass. Backend: `php artisan test --filter="AdminOrder|Order"` = 41 tests, 719 assertions, 0 failures. Live probes against the local API confirmed: guest `GET /admin/orders` and guest `PATCH …/status` → `401 UNAUTHENTICATED`; a disposable customer completed register → cart → order → pay-success (order `confirmed`, payment `completed`); list pagination and filters (`q` by customer email, `status=pending_payment`) returned the matching rows; `perPage=101` → `422 VALIDATION_ERROR`; detail exposed the server `timeline` and `allowedTransitions`. The state machine enforced live: `confirmed → processing → shipped → delivered` each `200` with updated `allowedTransitions` (`delivered` → `[]`), `pending_payment → cancelled` `200`, while terminal → `shipped`, `pending_payment → shipped`, and `delivered → processing` all `409 INVALID_TRANSITION`. Criterion **a** holds: both screens use only the documented `docs/openapi.yaml` admin order endpoints, and the status action offers exactly the server's `allowedTransitions` (never `confirmed`). Criterion **b** holds: client `401`s end the session and route to sign-in; `409` conflicts show the server message with a "Refresh order" path and no optimistic status claim. Criterion **c** holds: loading skeletons, empty and filtered-empty panels, error/retry, conflict, and post-response updated status are covered by behavior tests. Criterion **d** holds: all admin tests drive the mocked HTTP layer and assert user-visible behavior; backend internals are never asserted. Known limitation: no browser E2E (the same accepted limitation as Phase 1; browser E2E remains with QUAL-005). Tracked by [PR #44](https://github.com/Averon-Labs/eshop-verse/pull/44), which targets `develop`.

## Phase 3 — Quality and Portfolio Delivery

**Project phase gate:** see [Phase 3 in the Project Roadmap](ROADMAP.md).

### Step 1 — QUAL-001 — Component contribution

**Canonical task:** [QUAL-001 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Supply admin-side regression evidence and resolve admin-specific failures.

### Step 2 — QUAL-002 — Component contribution

**Canonical task:** [QUAL-002 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Review admin accessibility, keyboard behavior, responsive layouts, visual consistency, and screen states.

### Step 3 — QUAL-003 — Component contribution

**Canonical task:** [QUAL-003 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Review admin session/CSRF behavior, role boundaries, browser storage, error states, and dependency risks.

### Step 4 — QUAL-004 — Component contribution

**Canonical task:** [QUAL-004 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Verify admin setup, demo roles, workflows, and limitations in the handoff guide.

### Step 5 — QUAL-005 — Component contribution

**Canonical task:** [QUAL-005 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Run documented admin checks from a clean checkout and report exact commands and outcomes.
