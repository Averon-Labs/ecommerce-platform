# Backend Roadmap

## Purpose and authority

Plan PHP/Laravel REST API and MySQL work, including contract-first implementation and isolated database checks.

This file is the canonical task list for its component. Each task appears once here; stable Task IDs identify work across branches and PRs. Project-wide phase gates and cross-component tasks are canonical in [ROADMAP.md](ROADMAP.md). Follow [repository-wide agent instructions](../AGENTS.md), component guidance, authoritative documents linked by the task, and the actual implementation.

A roadmap assignment uses this roadmap name, phase, and step, for example: Backend Roadmap, Phase 1, Step 1; a stable Task ID may also identify the work. Step numbers restart at 1 in every phase. Lowercase letter labels restart at `a` in each acceptance-criteria block and identify individual criteria for discussion or verification, not separate assignments. Check dependencies before starting; a later step is not ready until its listed dependencies are complete. See [the project roadmap](ROADMAP.md) and [repository Git naming conventions](../AGENTS.md).

## Phase steps

## Phase 0 — Foundation

**Project phase gate:** see [Phase 0 in the Project Roadmap](ROADMAP.md).

### Step 1 — FND-002 — Initialize Laravel API and local database workflow

**Status:** `[~]`

**Depends on:** none

**Prerequisites to start:**
- PHP 8.1+ available locally
- Composer installed
- MySQL 8.0+ available
- Development environment ready

**Scope:** create the backend application foundation using the accepted Laravel/PHP/MySQL stack, without implementing product endpoints.

**Acceptance criteria:**

- [ ] **a.** Select supported stable framework/runtime versions and commit the dependency lockfile; document the selected versions.
- [ ] **b.** Provide safe local configuration instructions and `.env.example` placeholders; no real secrets are checked in.
- [ ] **c.** Configure migrations and a disposable isolated test database workflow; document how a developer creates and resets local data safely.
- [ ] **d.** Configure the backend unit/feature test runner and a minimal passing baseline test.
- [ ] **e.** Record and verify exact install, setup, migration, style/static-check, and test commands in `docs/DEVELOPMENT.md`.
- [ ] **f.** Do not add business endpoints before FND-004 is reviewed.

**Verification:** clean dependency install, disposable-database migration, and backend baseline test/checks.

### Step 2 — FND-004 — Draft and review the OpenAPI contract

**Status:** `[ ]`

**Depends on:** none; must finish before any endpoint or client integration Task

**Prerequisites to start:**
- `docs/PRODUCT.md` finalized
- `docs/API_CONTRACT.md` base version exists
- `docs/DATABASE.md` logical schema defined
- `docs/SECURITY.md` authentication/authorization rules defined
- OpenAPI 3.x validator tool available
- Close decisions OPEN-1 through OPEN-4 (category lifecycle, etc.)

**CRITICAL:** This task **blocks** all Android Phase 1-2 client integration and all Backend Phase 1-2 endpoint implementation. Complete and get acceptance before any team proceeds with API-consuming work.

**Scope:** create `docs/openapi.yaml` from the accepted product/API/database/security decisions and close the remaining contract decisions.

**Acceptance criteria:**

- [ ] **a.** Describe registration, login/logout, profile, and password-reset flows; public category/product listing, search, sorting, pagination, and product details; customer cart; order creation, history/detail, and simulated payment result; admin authentication, catalog/category/inventory management, and order-status management.
- [ ] **b.** Define request/response schemas, required/optional fields, validation constraints, status codes, shared error envelope, pagination, USD money representation, and representative examples.
- [ ] **c.** Define Android bearer-token and admin Sanctum session/CSRF security schemes, ownership/role expectations, and rate-limit classes/limits.
- [ ] **d.** Define order idempotency behavior, price/stock authority, order/address snapshots, and simulated payment success/failure state semantics consistently with `PRODUCT.md` and `DATABASE.md`.
- [ ] **e.** Resolve category lifecycle and product reassignment behavior: the roadmap mentions category archive, while the current logical category schema has no status field.
- [ ] **f.** Validate the document with a documented OpenAPI validator and record the exact validation command; resolve all errors.
- [ ] **g.** Obtain API-owner review/acceptance and update `API_CONTRACT.md` to link to the approved machine-readable contract without duplicating endpoint schemas.

**Verification:** OpenAPI validation plus a human review against the referenced product, architecture, database, and security documents. Endpoint implementation remains blocked until acceptance.

## Phase 1 — Accounts and Catalog

**Project phase gate:** see [Phase 1 in the Project Roadmap](ROADMAP.md).

### Step 1 — AUTH-001 — Customer account API

**Status:** `[x]`

**Depends on:** FND-002, FND-004, FND-005

**Prerequisites to start:**
- FND-002 complete (Laravel app initialized, migrations working)
- **FND-004 complete** (OpenAPI contract accepted with auth endpoints)
- **FND-005 complete** (Project: CI and documentation structure ready)
- Database schema for users table defined
- Password hashing strategy decided
- Sanctum token configuration decided
- Rate limiting strategy defined
- Local mail sink configured for testing password reset

**Scope:** customer registration, login/logout, profile, password reset, secure password storage, rate limiting/lockout controls, and revocable Android Sanctum tokens.

**Acceptance criteria:**

- [x] **a.** Valid registration/login/profile/logout/reset flows match the reviewed OpenAPI schemas and security rules.
- [x] **b.** Invalid input, duplicate email, invalid credentials, reset-token failure/expiry, rate limits, and logout revocation return documented outcomes without leaking secrets.
- [x] **c.** Tests prove passwords are hashed, token lifetime/revocation rules and account lockout after repeated failures are enforced, and reset email can be safely exercised through a local mail sink.
- [x] **d.** Tests cover relevant validation, authentication, and abuse boundaries; no real email or external provider is required for local tests.

**Verification (2026-10-12):** implemented per accepted `docs/openapi.yaml` operations (register, login, logout, profile GET/PATCH, password-reset request/confirm). Sanctum `laravel/sanctum` ^4.3 added (pinned in `composer.lock`); bearer tokens expire after 30 days (`SANCTUM_TOKEN_EXPIRATION=43200`), logout revokes only the presented token, and a successful reset revokes every token. Passwords hashed with bcrypt; login answers a deliberately vague `401 INVALID_CREDENTIALS`, locks the account for 15 minutes after 5 consecutive failures (`429 ACCOUNT_LOCKED` + `Retry-After`), and resets the counter on success. Password-reset request always answers `202` with no account disclosure; the token is delivered only through the configured mail flow (local sink in tests, `ResetPassword::toMailUsing` without any web route). Rate limits per contract: login 10/min/IP, register 5/h/IP, reset 3/h/email + 10/h/IP. `php artisan test` = 38 tests, 322 assertions, 0 failures (5 baseline + 33 new AUTH-001 feature tests); `vendor\bin\pint --test` passed; migrations green on `eshopverse_dev` and disposable `eshopverse_test`.

### Step 2 — CAT-001 — Catalog read API and fictional seed data

**Status:** `[x]`

**Depends on:** FND-002, FND-004, FND-005

**Prerequisites to start:**
- FND-002 complete (Laravel app, migrations ready)
- **FND-004 complete** (OpenAPI contract accepted with catalog endpoints)
- **FND-005 complete** (Project: CI and documentation ready)
- Database schema for categories, products, product_images defined
- Fixed-precision USD money representation decided
- Product status enum values defined
- Fictional seed data prepared (no real personal data)

**Scope:** category/product schema, fictional seed catalog, and public read/search/sort/pagination endpoints.

**Acceptance criteria:**

- [x] **a.** Migrations enforce documented relationships, valid prices, product status, and image ordering; use fixed-precision USD money.
- [x] **b.** Seed/reset creates useful fictional categories/products/images reproducibly and never contains real personal data.
- [x] **c.** Listing, detail, category, search, sort, pagination, and empty-result behavior match OpenAPI.
- [x] **d.** Invalid filters and pagination boundaries return documented errors; inactive/unavailable products do not leak into customer results.

**Verification (2026-10-12):** four migrations (`categories`, `products`, `product_images`, `inventories`) enforce FK relationships, unsigned fixed-precision `price` decimal(11,2) (widened from decimal(10,2) during Step 3 — CAT-003 — so the column accepts the full contract Money pattern `^\d{1,9}\.\d{2}$`)/`quantity`, product/category status enums, unique SKU, one inventory row per product (unique `product_id`), and image ordering (`sort_order` + `id`). `DatabaseSeeder` seeds a deterministic fictional catalog (5 categories — 1 archived; 12 products — 1 inactive, 1 archived, 1 out-of-stock; 25 images; placeholder URLs via `picsum.photos` seed strings; no real personal data) idempotently via `updateOrCreate`. Endpoints: `GET /categories` (active only, `sortOrder` then `name`), `GET /products` (`q` case-insensitive name/description search with escaped LIKE wildcards, `categoryId` → 422 `CATEGORY_NOT_FOUND` for unknown/archived, `inStock`, four sort orders, `page`/`perPage` 1–100, pagination meta with 0-page empty results), `GET /products/{productId}` (ordered images, exact `stockQuantity`, 404 for missing/inactive/archived/non-numeric). Money serialized as a two-fraction-digit decimal string plus `USD`; listings expose only the boolean `inStock`. Rate limited `public-read` 120/min/IP. `php artisan test` = 60 tests, 502 assertions, 0 failures (38 AUTH-001 + 22 new CAT-001); `vendor\bin\pint --test` passed; live smoke of all five read paths against seeded `eshopverse_dev` verified.

### Step 3 — CAT-003 — Admin catalog and inventory API

**Status:** `[x]`

**Depends on:** FND-002, FND-004, AUTH-003, CAT-001

**Prerequisites to start:**
- FND-002, FND-004 complete
- **AUTH-003 complete** (Admin: admin authentication working - backend portion)
- CAT-001 complete (catalog read API and schema exist)
- Admin role authorization middleware ready
- Category archive/status behavior decided (resolve OPEN-2, the category lifecycle decision recorded in FND-004)
- Inventory update validation rules defined

**Scope:** admin-only create/read/update/archive operations for products and categories, plus validated inventory updates.

**Acceptance criteria:**

- [x] **a.** CRUD and inventory operations follow OpenAPI, validation, and database constraints.
- [x] **b.** Guest/customer access is rejected; tests cover role enforcement, invalid values, missing resources, and inventory boundaries.
- [x] **c.** Public catalog changes reflect persisted admin updates; deleted/archived data follows the documented status behavior.
- [x] **d.** No client-supplied total or stock value bypasses server-side validation.

**Verification (2026-10-13):** Implemented with the AUTH-003 backend portion against `docs/openapi.yaml`. Framework: `$middleware->statefulApi()` gives stateful first-party requests cookie sessions + CSRF verification (bearer flows untouched); the `admin` middleware alias (`EnsureUserIsAdmin`) guards all catalog routes — guests and every non-admin credential (customer bearer, demoted, suspended) answer 401 `UNAUTHENTICATED` per the contract's "not a valid administrator session", while `GET /admin/auth/session` answers a demoted session 403 `FORBIDDEN`; the framework's 419 TokenMismatch is rendered as a 403 `CSRF_TOKEN_MISMATCH` envelope. Admin session lifecycle: `GET /admin/auth/csrf` (204 + readable XSRF cookie), `POST /admin/auth/login` (stateful origin required, identical 401 `INVALID_CREDENTIALS` for wrong password/customer/suspended, session-id regeneration, `admin-login` 10/min/IP), `GET /admin/auth/session`, `POST /admin/auth/logout` (204, session destroyed and CSRF token rotated). Catalog: 16 admin routes under `auth:sanctum` + `admin` with `admin-write` 120/min/session on mutations — products list/store/show/full-replace update/archive (409 `SKU_TAKEN`, `INVALID_PRODUCT_STATE`; creation never fabricates an inventory row), categories list/create/update/archive/restore (409 `CATEGORY_NAME_TAKEN`, `CATEGORY_NOT_EMPTY` with the blocking count in `error.details`, `INVALID_CATEGORY_STATE`; OPEN-2 resolved as implemented), inventory list/absolute-quantity PATCH (0–1,000,000 integers only, `updateOrCreate`, missing row reported as 0/out of stock). `products.price` widened to decimal(11,2) so the column accepts the full contract Money pattern (the CAT-001 record below is corrected accordingly); dev database rebuilt via `migrate:fresh --seed`. `AdminUserSeeder` creates the local/demo-only admin (`demo-admin@eshopverse.test`, password from the git-ignored `DEMO_ADMIN_PASSWORD` env var or random per install — never committed or logged). `php artisan test` = 87 tests, 1080 assertions, 0 failures (60 prior + 27 new admin tests); `vendor\bin\pint --test` passed; `migrate:fresh --seed` verified (5 categories, 12 products, 25 images, 12 inventories, 1 demo admin). Re-audit (2026-10-13): boolean query spellings `inStock=1|0` are normalized on both the public and admin lists (regression-tested), and a concurrent SKU collision on PUT now answers 409 `SKU_TAKEN` like POST.

## Phase 2 — Cart, Checkout, and Orders

**Project phase gate:** see [Phase 2 in the Project Roadmap](ROADMAP.md).

### Step 1 — CART-001 — Customer cart API

**Status:** `[ ]`

**Depends on:** FND-002, FND-004, AUTH-001, CAT-001

**Prerequisites to start:**
- FND-002, FND-004 complete
- AUTH-001 complete (customer authentication working, token verification ready)
- CAT-001 complete (products available, stock tracking exists)
- Database schema for carts, cart_items defined
- Cart ownership authorization rules defined
- Product availability validation rules defined
- Fixed-precision money calculations for totals

**Scope:** one persistent cart per signed-in customer with add/update/remove/read operations.

**Acceptance criteria:**

- [ ] **a.** Server validates product availability, positive bounded quantity, and current product/stock state on every mutation.
- [ ] **b.** Cart totals use server-side fixed-precision money; clients cannot set authoritative prices.
- [ ] **c.** Customer ownership is enforced on every read and mutation; tests try another customer's cart identifiers.
- [ ] **d.** Empty cart, unavailable product, stock conflict, invalid quantity, and normal edits return the documented outcomes.

**Verification:** database-backed API tests for all mutations, totals, errors, and cross-customer authorization.

### Step 2 — ORD-001 — Order creation and simulated payment API

**Status:** `[ ]`

**Depends on:** FND-002, FND-004, AUTH-001, CAT-001, CART-001

**Prerequisites to start:**
- FND-002, FND-004, AUTH-001, CAT-001 complete
- CART-001 complete (cart API working, cart totals calculated)
- Database schema for orders, order_items, addresses defined
- Order state machine and status transitions defined in contract
- Idempotency key handling strategy defined
- Simulated payment success/failure logic defined
- Transaction-safe inventory decrement strategy defined
- Shipping calculation (sample) strategy defined
- Immutable snapshot strategy for products/prices/addresses

**Scope:** server-authoritative checkout quote/order creation, address and line-item snapshots, idempotency, and explicitly simulated success/failure payment outcomes.

**Acceptance criteria:**

- [ ] **a.** Server recalculates prices, subtotal, configured sample shipping, and total using fixed precision; it ignores client totals.
- [ ] **b.** Order stores immutable product/address/price snapshots and follows documented state transitions.
- [ ] **c.** Order creation and payment/stock transitions are transaction-safe under concurrent or stale-stock requests; match `DATABASE.md` by changing inventory transactionally only when simulated payment succeeds.
- [ ] **d.** Repeating the same idempotency key/request does not create duplicate orders; conflicting reuse returns a documented result.
- [ ] **e.** Simulation accepts no card data, calls no real provider, and clearly distinguishes success from failure; failed payment is never shown as paid.
- [ ] **f.** Tests cover duplicate submission, stale stock, insufficient stock, transaction rollback, totals, ownership, and both payment outcomes.

**Verification:** database-backed API/transaction tests, idempotency tests, and security/ownership tests.

## Phase 3 — Quality and Portfolio Delivery

**Project phase gate:** see [Phase 3 in the Project Roadmap](ROADMAP.md).

### Step 1 — QUAL-001 — Component contribution

**Canonical task:** [QUAL-001 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Supply API/database-side purchase-flow regression evidence and resolve backend-specific failures.

### Step 2 — QUAL-002 — Component contribution

**Canonical task:** [QUAL-002 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Verify API error behavior and backend-supported UI states against shared quality requirements.

### Step 3 — QUAL-003 — Component contribution

**Canonical task:** [QUAL-003 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Review authentication, authorization, validation, rate limits, data protection, logging, and dependency risks.

### Step 4 — QUAL-004 — Component contribution

**Canonical task:** [QUAL-004 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Verify backend setup, migrations, seed data, demo roles, reset, and limitations in the handoff guide.

### Step 5 — QUAL-005 — Component contribution

**Canonical task:** [QUAL-005 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Run documented backend and OpenAPI checks from a clean checkout and report exact commands and outcomes.
