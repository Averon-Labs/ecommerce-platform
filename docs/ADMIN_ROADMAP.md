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

**Phase contribution PR:** steps 1–5 of this phase shipped together on `feature/admin-phase-3` — [PR #47](https://github.com/Averon-Labs/eshop-verse/pull/47) targeted `develop` and was merged 2026-10-17. Step 6 (UI-003) is a post-phase enhancement requested by the owner on 2026-10-19; it is delivered in [PR #55](https://github.com/Averon-Labs/eshop-verse/pull/55) on `feature/admin-phase-3` without changing the historical PR #47 scope.

### Step 1 — QUAL-001 — Component contribution

**Canonical task:** [QUAL-001 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Supply admin-side regression evidence and resolve admin-specific failures.

**Contribution record (2026-10-17):** admin-side regression evidence supplied on `feature/admin-phase-3`. Admin checks from `admin/`: `npm run typecheck`, `npm run lint`, and `npm test` (82 tests in 10 files) pass; `npm run build` passes. Backend regression (read-only run) from `backend/`: `php artisan test --filter="AdminOrder|PurchaseFlow"` = 12 tests, 281 assertions, 0 failures. Live probes against the local API with disposable data (one registered `qual001-<epoch>@example.com` customer, two disposable orders; no admin code changes were needed because no admin-specific failures were found): 36/36 checks passed — admin CSRF handshake and login (`204`, `200`), mutation without `X-XSRF-TOKEN` → `403 CSRF_TOKEN_MISMATCH`, guest `GET /admin/orders` → `401 UNAUTHENTICATED`, customer bearer credential on admin routes → `401 UNAUTHENTICATED`, non-admin stateful credential on `GET /admin/auth/session` → `401 UNAUTHENTICATED`, order list `q` filter and pagination (`perPage=101` → `422 VALIDATION_ERROR`), detail `timeline`/`allowedTransitions`, the documented `confirmed → processing → shipped → delivered` chain, `pending_payment → cancelled` on a failed-payment order, `409 INVALID_TRANSITION` for `delivered → processing`, guest mutation blocked by CSRF from the SPA origin and by auth from a foreign client, and post-logout rejection. The demoted-session `403 FORBIDDEN` path is covered by `backend/tests/Feature/Admin/AdminAuthTest.php::test_session_endpoint_answers_guest_demoted_and_valid_admin_states`. The canonical QUAL-001 task stays `[~]` pending its Android contribution (ORD-002); full end-to-end purchase→admin-management evidence therefore remains open at the project level. **Independent verification (2026-10-17, `6d3d7f3`):** a different agent reproduced the admin checks, the live probes, and (separately re-run by the coordinator) the backend filter (12 tests / 281 assertions). The probe script is not hermetic — it consumes stock from the first catalog product — so it first reported 30/36 against an exhausted database and reproduced 36/36 after restoring the seeder-intended stock; the six failures were probe-state exhaustion (`409 INSUFFICIENT_STOCK`, correct product behavior), not an admin defect. Rerun it against a freshly seeded database or restock that product first.

### Step 2 — QUAL-002 — Component contribution

**Canonical task:** [QUAL-002 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Review admin accessibility, keyboard behavior, responsive layouts, visual consistency, and screen states.

**Contribution record (2026-10-17):** reviewed all shipped screens (A-01 sign-in, A-02 products, A-03 product form, A-04 categories, A-05 inventory, A-06 orders, A-07 order detail) and the shared shell/components against criteria **a–c**, with measured WCAG 2.2 contrast for every light-theme token pair. Three findings were fixed in `42d6f47` and `d41771c`; nothing else needed changes. **(1) Coral text contrast:** brand coral `#F7767E` rendered as text (row action links, active navigation, sign-in eyebrow) measured 2.64:1 on cards and 2.51:1 on the canvas, below the 4.5:1 text minimum; the ADR-002 darker coral `#B92F3D` is now applied via `--primary-strong`, measuring 5.08:1–5.94:1. **(2) Focus indicator contrast:** the same coral as the focus ring measured 2.64:1/2.51:1 against the required 3:1 graphic minimum; the ring now uses `#B92F3D`, measuring 5.64:1–5.94:1. **(3) Third-party font request:** Lato loaded from Google Fonts at runtime, contradicting the ADR-002 consequence that web font loading must not require a third-party request; the OFL TTFs are now self-hosted from `admin/public/fonts/` (license alongside) with no `googleapis`/`gstatic` reference left in the production build. Verified present without changes: dialogs move focus in, trap Tab, close on Escape, and restore focus; the shell provides a skip link and labelled navigation; every control has a visible or programmatic label (including wrapped radio groups and `sr-only` labels on repeated image-URL fields); tables use `scope` headers and switch to labelled card lists below the `md` breakpoint; loading, empty, filtered-empty, validation, conflict, unauthorized, and network-error states are labelled and recoverable with retry; pagination announces changes; no clickable non-interactive elements, no `autoFocus` misuse. Evidence: `npm run typecheck`, `npm run lint`, `npm run check:contrast` (17/17 pairs), `npm test` (82 tests in 10 files), and `npm run build` all pass; `npm run check:contrast` makes the contrast findings reproducible. No unrelated redesign was introduced; deferred/noted: the unused scaffold components (`BentoGrid`, `GlassCard`, `FloatingNav`, `CompoundModal`) are not part of any shipped screen and were left untouched, the `.dark` token block is not reachable from the first-release UI, WOFF2 optimization of the self-hosted fonts is deferred (no converter tooling available; OFL TTFs shipped), and the `docs/DESIGN_SYSTEM.md` MUSE palette drift from ADR-002's coral direction is recorded for the design owner (owner confirmed 2026-10-17: keep coral, log the conflict, no re-theme here). The canonical QUAL-002 task stays `[~]` pending its Android contribution. **Independent verification (2026-10-17, `6d3d7f3`):** a different agent reran `typecheck`/`lint`/`check:contrast` (17/17)/`test` (82)/`build`, recomputed all contrast ratios with an independent implementation (values match; the rendered `--primary` token is #F77881, so the recorded 2.64/2.51 ratios are correct even though the brand hex is annotated #F7767E), and rebuilt in a fresh clean worktree with no `googleapis`/`gstatic` reference in the output.

### Step 3 — QUAL-003 — Component contribution

**Canonical task:** [QUAL-003 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Review admin session/CSRF behavior, role boundaries, browser storage, error states, and dependency risks.

**Contribution record (2026-10-17):** no admin-side security defect was found; the findings are recorded as informational verifications A-1–A-6 in [`SECURITY_REVIEW.md`](SECURITY_REVIEW.md) § Admin SPA. Evidence: `npm audit` and `npm audit --omit=dev` both report 0 vulnerabilities and runtime dependencies are exact-pinned with a committed lockfile; source review confirms no `localStorage`/`sessionStorage`/bearer tokens (the only cookie read is the `XSRF-TOKEN` echo), no console logging, no `dangerouslySetInnerHTML`/`innerHTML`/`eval`, and no credentials in the public `VITE_API_BASE_URL` config; live probes against the local API with disposable data passed 14/14 — guest CSRF handshake sets the JS-readable `XSRF-TOKEN` and a guest session, admin login sets an `HttpOnly` `SameSite=Lax` session cookie whose identifier rotates on login, mutations without `X-XSRF-TOKEN` answer `403 CSRF_TOKEN_MISMATCH`, a foreign origin (`https://evil.example`) receives no `Access-Control-Allow-Origin` on simple or preflight requests while the documented SPA origin is allowed with credentials, a non-admin stateful session receives `401 UNAUTHENTICATED` on `GET /admin/auth/session`, and logout ends the session. Criterion (d): the simulated-payment and non-production notice in [`SECURITY.md`](SECURITY.md) § Portfolio Demo Boundaries and [`HANDOFF.md`](HANDOFF.md) matches the admin UI copy ("Demo method only — no card data is ever collected."). The canonical QUAL-003 task stays `[~]` pending its Android contribution. **Independent verification (2026-10-17, `6d3d7f3`):** a different agent reran `npm audit`/`npm audit --omit=dev` (0 vulnerabilities), the source greps, and the 14-check security probe script (14/14). **Update (2026-10-19):** UI-003 later added the non-sensitive `eshop-admin-theme` preference to `localStorage`; no auth/session value is stored there, so the original security finding still holds for credentials and session state.

### Step 4 — QUAL-004 — Component contribution

**Canonical task:** [QUAL-004 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Verify admin setup, demo roles, workflows, and limitations in the handoff guide.

**Contribution record (2026-10-17):** added the **Admin Dashboard** chapter to [`HANDOFF.md`](HANDOFF.md) — what the component is, clean setup (`npm ci`, optional `.env.local`, `npm run dev` on the pinned `http://localhost:5173`, the local origin arrangement), the admin checks including the new `npm run check:contrast`, the administrator demo account (local-only, from `DEMO_ADMIN_PASSWORD`; no self-registration), a screen-by-screen walkthrough for A-01–A-07 with routes and key behaviors, three guided workflows (catalog archive/conflict, inventory adjust/out-of-stock, order `Processing → Shipped → Delivered` plus `pending_payment → cancelled` and `409 INVALID_TRANSITION`), and the admin known limitations (demo image URLs only, stock in A-05, card fallback at narrow widths, no theme toggle, deferred analytics/customer-admin/CMS/wishlist scope, loopback HTTP). The intro and component-status notes now state that the guide covers all three components. Commands mirror the verified [`DEVELOPMENT.md`](DEVELOPMENT.md) § Admin Dashboard; no credential or secret is written into the guide (criterion **b**). The canonical QUAL-004 task stays `[~]` pending its Android contribution. **Independent verification (2026-10-17, `6d3d7f3`):** a different agent cross-checked the walkthrough routes against `admin/src/App.tsx`, the screen IDs and titles against `docs/SCREEN_SPECIFICATIONS.md`, and the setup/check commands against `DEVELOPMENT.md` and `package.json` — all match; no credential appears in the guide. **Update (2026-10-19):** the “no theme toggle” limitation is superseded by UI-003; [`HANDOFF.md`](HANDOFF.md) now documents the toggle, persistence, and the non-sensitive-storage boundary.

### Step 5 — QUAL-005 — Component contribution

**Canonical task:** [QUAL-005 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Run documented admin checks from a clean checkout and report exact commands and outcomes.

**Contribution record (2026-10-17):** rehearsed from a clean checkout of commit `a15def7` (`git worktree add --detach`, no `node_modules`, no `dist`, no `.env.local`) repeating the exact [`DEVELOPMENT.md`](DEVELOPMENT.md) commands: `npm ci` (committed lockfile, 0 vulnerabilities) → `npm run typecheck`, `npm run lint`, `npm run check:contrast` (17/17), `npm test` (10 files, 82 tests), `npm run build` all pass; `npm run preview` served `/` (200), `/fonts/lato-regular.ttf` (200), and `/fonts/Lato-OFL.txt` (200) with no third-party font reference in the served HTML. Commands, results, and the tested commit are recorded in [`DEVELOPMENT.md`](DEVELOPMENT.md) § Admin Dashboard (criterion **c**). Also corrected the stale admin routing row in that section (missing `/orders`, `/orders/:orderId`). Limitation reported honestly: the checked-in CI workflow does not yet run an admin job — that, its verified run, and any branch-protection claim belong to the pending project task **FND-005** (CI changes require human approval); no false CI claim is made. The canonical QUAL-005 task stays `[~]` pending its Android contribution. **Independent verification (2026-10-17, `6d3d7f3`):** a different agent repeated the full clean-checkout rehearsal in a fresh detached worktree (`npm ci`, typecheck, lint, 17/17 contrast, 82 tests, identical build bundles) plus the preview smoke (`/`, `/fonts/lato-regular.ttf`, `/fonts/Lato-OFL.txt` all 200, no third-party font reference) and confirmed `git diff a15def7..HEAD` is docs-only; the CI limitation (no admin job; FND-005) is stated without any false claim.

### Step 6 — UI-003 — Admin light/dark theme

**Status:** `[x]` implemented and locally verified 2026-10-19 (owner-requested post-phase enhancement).

**Scope:** add a persisted light/dark theme to the admin SPA without changing API, database, authentication, or
product scope. Light remains the baseline; the operating-system preference is the default until the user makes
an explicit choice.

**Acceptance criteria:**

- [x] **a.** The admin shell, sign-in screen, not-found screen, and API-unreachable state expose an accessible
  light/dark toggle with a minimum 44px target, a visible focus ring, a fixed accessible name, and
  `aria-pressed` state; it is operable by keyboard and does not rely on color alone.
- [x] **b.** An explicit choice persists across reloads and sessions; otherwise the app follows
  `prefers-color-scheme`. The root element receives the `dark` class and `color-scheme` before first paint so
  dark-mode users do not see a light flash.
- [x] **c.** Existing light and dark semantic tokens, status badges, dialogs, tables, forms, and shell surfaces
  adapt with no hardcoded light-only text or surface; the stored preference is the non-sensitive
  `eshop-admin-theme` value only, never an auth token.
- [x] **d.** Automated tests cover a stored preference, the system-preference fallback, the toggle, the root
  class, `color-scheme`, and persistence; the contrast script verifies both palettes.
- [x] **e.** [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md), [`SCREEN_SPECIFICATIONS.md`](SCREEN_SPECIFICATIONS.md),
  [`HANDOFF.md`](HANDOFF.md), and [`DEPLOYMENT.md`](DEPLOYMENT.md) describe the behavior and the upload path;
  the production build remains same-origin with `VITE_API_BASE_URL=/api/v1`.

**Verification record (2026-10-19, local build):** added `src/theme/ThemeContext.tsx`,
`src/components/ThemeToggle.tsx`, a boot script in `index.html`, `color-scheme` tokens, and 3 theme tests.
`npm run typecheck`, `npm run lint`, `npm run check:contrast` (34/34 pairs — 17 light + 17 dark), `npm test`
(11 files, 85 tests), and `npm run build` pass. Production output: `index.html`, one CSS bundle
(`index-tL7PdeFP.css`, ~37 kB), one JS bundle (`index-X4fCBd4x.js`, ~348 kB), and self-hosted Lato fonts.
A local preview smoke test at 1200px confirmed: the OS-dark default rendered dark, the toggle switched to
light, `localStorage["eshop-admin-theme"]` became `light`, and a reload kept the light theme. The prepared
upload archive is `deploy/eshopverse-admin-dist.zip`; live upload is the owner's remaining deployment step
(see [`DEPLOYMENT.md`](DEPLOYMENT.md)). No independent tester was assigned to this post-phase enhancement.

## Phase 4 — Product Images and Deletion

**Project phase gate:** this phase has no entry in the project roadmap; it is admin-owned work requested directly by the owner on 2026-10-27, delivered together with the backend CAT-B01/CAT-B02 steps.

### Step 1 — CAT-005 — Image upload/remove controls and product deletion

**Status:** `[~]`

**Depends on:** CAT-B01, CAT-B02 (backend)

**Scope:** Replace the URL fields of A-03 with upload and remove controls backed by the new backend operations, and give A-02 a confirmed Delete action.

**Acceptance criteria:**
- [x] **a.** The product form sends image files as multipart to `POST /admin/products/{id}/images`, shows each stored image with its preview and a **Remove** control that calls `DELETE /admin/products/{id}/images/{imageId}`, and keeps up/down ordering in the `imageUrls` payload on save; URL entry is gone.
- [x] **b.** A new product disables its file picker until it has been saved once, the eight-image limit and the server's `422`/`409` messages are surfaced as an accessible alert, and image operations are immediate so they never count as unsaved edits.
- [x] **c.** The product list offers a confirmed **Delete** that calls `DELETE /admin/products/{id}`, reports the server's message (including `409 PRODUCT_HAS_ORDERS`) and the success feedback, and refreshes the list.

**Verification (2026-10-27, builder):** `admin/src/api/http.ts` gained a `formData` path that deliberately omits `Content-Type` so the browser sets the multipart boundary (the CSRF header and retry behavior are unchanged); `catalog.ts` adds `uploadAdminProductImage`, `deleteAdminProductImage`, and `deleteAdminProduct`; `ProductImage` in `types.ts` now carries `id`. `ProductFormPage` tracks the image list separately from the saved baseline, so an upload or removal updates both and the unsaved-change guard still catches edits to the other fields (asserted by a test that navigates away without a dialog after an upload and a removal). `ProductsPage` adds the confirmed delete row/card action. Evidence: `npm run typecheck`, `npm run lint`, `npm run check:contrast` (34/34), `npm test` (11 files, 89 tests — 4 new: multipart upload with no hand-written content type, immediate delete, upload-failure alert, delete confirm/conflict), and `npm run build` all pass. Pending: independent tester stage and the owner's Git/PR step.
