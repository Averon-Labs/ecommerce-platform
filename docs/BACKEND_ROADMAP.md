# Backend Roadmap

## Purpose and authority

Plan PHP/Laravel REST API and MySQL work, including contract-first implementation and isolated database checks.

This file is the canonical task list for its component. Each task appears once here; stable Task IDs identify work across branches and PRs. Project-wide phase gates and cross-component tasks are canonical in [ROADMAP.md](ROADMAP.md). Follow [repository-wide agent instructions](../AGENTS.md), component guidance, authoritative documents linked by the task, and the actual implementation.

A roadmap assignment uses this roadmap name, phase, and step, for example: Backend Roadmap, Phase 1, Step 1; a stable Task ID may also identify the work. Step numbers restart at 1 in every phase. Lowercase letter labels restart at `a` in each acceptance-criteria block and identify individual criteria for discussion or verification, not separate assignments. Check dependencies before starting; a later step is not ready until its listed dependencies are complete. See [the project roadmap](ROADMAP.md) and [repository Git naming conventions](../AGENTS.md).

## Phase steps

## Phase 0 — Foundation

**Project phase gate:** see [Phase 0 in the Project Roadmap](ROADMAP.md).

### Step 1 — FND-002 — Initialize Laravel API and local database workflow

**Status:** `[x]`

**Depends on:** none

**Prerequisites to start:**
- PHP 8.1+ available locally
- Composer installed
- MySQL 8.0+ available
- Development environment ready

**Scope:** create the backend application foundation using the accepted Laravel/PHP/MySQL stack, without implementing product endpoints.

**Acceptance criteria:**

- [x] **a.** Select supported stable framework/runtime versions and commit the dependency lockfile; document the selected versions.
- [x] **b.** Provide safe local configuration instructions and `.env.example` placeholders; no real secrets are checked in.
- [x] **c.** Configure migrations and a disposable isolated test database workflow; document how a developer creates and resets local data safely.
- [x] **d.** Configure the backend unit/feature test runner and a minimal passing baseline test.
- [x] **e.** Record and verify exact install, setup, migration, style/static-check, and test commands in `docs/DEVELOPMENT.md`.
- [x] **f.** Do not add business endpoints before FND-004 is reviewed.

**Verification (2026-10-11, re-verified 2026-10-12):** clean `composer install` (79 packages from the committed `composer.lock`), `composer validate --strict` and `composer audit` clean; `php artisan migrate` + `php artisan migrate:status` green for the `eshopverse_dev` / disposable `eshopverse_test` workflow; `php artisan test` = 5 tests, 8 assertions, 0 failures; `vendor\bin\pint --test` passed; `php artisan serve` answered `GET /up` 200 and an unknown route 404. The 2026-10-11 run used hosted MySQL 8.4.11 over verified TLS 1.3; after the provider deleted that free service, every database-dependent command was re-verified on 2026-10-12 against the documented local MySQL 8.4 workflow. Commands and safety rules are recorded in `docs/DEVELOPMENT.md`.

### Step 2 — FND-004 — Draft and review the OpenAPI contract

**Status:** `[x]`

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

- [x] **a.** Describe registration, login/logout, profile, and password-reset flows; public category/product listing, search, sorting, pagination, and product details; customer cart; order creation, history/detail, and simulated payment result; admin authentication, catalog/category/inventory management, and order-status management.
- [x] **b.** Define request/response schemas, required/optional fields, validation constraints, status codes, shared error envelope, pagination, USD money representation, and representative examples.
- [x] **c.** Define Android bearer-token and admin Sanctum session/CSRF security schemes, ownership/role expectations, and rate-limit classes/limits.
- [x] **d.** Define order idempotency behavior, price/stock authority, order/address snapshots, and simulated payment success/failure state semantics consistently with `PRODUCT.md` and `DATABASE.md`.
- [x] **e.** Resolve category lifecycle and product reassignment behavior: the roadmap mentions category archive, while the current logical category schema has no status field.
- [x] **f.** Validate the document with a documented OpenAPI validator and record the exact validation command; resolve all errors.
- [x] **g.** Obtain API-owner review/acceptance and update `API_CONTRACT.md` to link to the approved machine-readable contract without duplicating endpoint schemas.

**Verification:** OpenAPI validation (0 errors — exact command and output in `docs/DEVELOPMENT.md`) plus API-owner review against the referenced product, architecture, database, and security documents; acceptance recorded 2026-10-12. Endpoint implementation is unblocked.

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

**Status:** `[x]`

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

- [x] **a.** Server validates product availability, positive bounded quantity, and current product/stock state on every mutation.
- [x] **b.** Cart totals use server-side fixed-precision money; clients cannot set authoritative prices.
- [x] **c.** Customer ownership is enforced on every read and mutation; tests try another customer's cart identifiers.
- [x] **d.** Empty cart, unavailable product, stock conflict, invalid quantity, and normal edits return the documented outcomes.

**Verification (2026-10-14):** Implemented against `docs/openapi.yaml` Cart operations `getCart`, `addCartItem`, `updateCartItem`, `removeCartItem`, `clearCart`. Schema: `carts` (unique `user_id`, lazily created on first mutation) and `cart_items` (unique `cart_id`+`product_id`, unsigned quantity, `added_at`); the `unit_price` decimal(11,2) add-time snapshot was added — and `docs/DATABASE.md` amended — because the binding contract's `CartItem.priceChanged` requires a stored line price to compare against the current product price. Ownership: the cart is implicit per bearer token — no client-facing cart identifier exists to leak — and a two-customer test proves each token only ever sees and mutates its own lines (a foreign line answers 404 `CART_ITEM_NOT_FOUND`). Mutations (`throttle:customer-write`, the new 60/min/token limiter from `x-rate-limit-classes`, registered in `AppServiceProvider`) validate on every call: missing product 404 `NOT_FOUND`, non-`active` product 409 `PRODUCT_UNAVAILABLE`, quantity outside 1..99 (and increments pushing a line past 99) 422 `VALIDATION_ERROR`, quantity above current stock 409 `INSUFFICIENT_STOCK` with the available count in `error.details`; adding increments an existing line. Every read reprices all lines from the current product record (`App\Support\Money` integer-cent arithmetic, never floats), so price drift and newly-unavailable/out-of-shopped lines surface as `priceChanged`/`unavailable`/`hasIssues` without client action; `subtotal` includes all lines, the flat USD 5.00 sample shipping (`x-money-rules` OPEN-3) is previewed for any non-empty cart while an empty cart answers `0.00` shipping/total (recorded decision: an empty cart previews no order), and clients can never set a price (request bodies accept only the contract fields; `ApiFormRequest` rejects unknown fields). `php artisan test` = 110 tests, 1343 assertions, 0 failures (23 new in `tests/Feature/Cart/CartTest.php`); `vendor\bin\pint --test` passes.

### Step 2 — ORD-001 — Order creation and simulated payment API

**Status:** `[x]`

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

**Scope:** server-authoritative checkout quote/order creation, address and line-item snapshots, idempotency, and explicitly simulated success/failure payment outcomes, plus the admin order list/detail/status operations that the Admin Roadmap's ORD-003 expects this task to provide (recorded scope decision; no other backend task owns them).

**Acceptance criteria:**

- [x] **a.** Server recalculates prices, subtotal, configured sample shipping, and total using fixed precision; it ignores client totals.
- [x] **b.** Order stores immutable product/address/price snapshots and follows documented state transitions.
- [x] **c.** Order creation and payment/stock transitions are transaction-safe under concurrent or stale-stock requests; match `DATABASE.md` by changing inventory transactionally only when simulated payment succeeds.
- [x] **d.** Repeating the same idempotency key/request does not create duplicate orders; conflicting reuse returns a documented result.
- [x] **e.** Simulation accepts no card data, calls no real provider, and clearly distinguishes success from failure; failed payment is never shown as paid.
- [x] **f.** Tests cover duplicate submission, stale stock, insufficient stock, transaction rollback, totals, ownership, and both payment outcomes.

**Verification (2026-10-14):** Implemented against `docs/openapi.yaml` Checkout/Orders operations `createCheckoutQuote`, `createOrder`, `listOrders`, `getOrder`, `submitSimulatedPayment` plus the three Admin orders operations (`adminListOrders`, `adminGetOrder`, `adminUpdateOrderStatus` — the Admin Roadmap's ORD-003 dependency folded into this task's scope as recorded above). Schema: four migrations — `orders` (unique `reference` = `ESV-` + zero-padded primary key set inside the creating transaction, `shipping_address_snapshot` JSON, append-only `timeline` JSON; `payment_status` deliberately derived from the latest `demo_payments` row so it can never drift), `order_items` (immutable product-name/quantity/unit-price snapshots; products are archived, never deleted, so snapshots stay intact), `demo_payments`, and `idempotency_keys` (unique user+endpoint+key — added because the contract requires `Idempotency-Key` and the logical model had no store; documented in `DATABASE.md` together with the Orders decisions). Pricing: `App\Support\CartPricing` was extracted from the CART-001 controller so `GET /cart`, the quote, and order creation share one integer-cent repricing path; the server recomputes subtotal + flat USD 5.00 (`x-money-rules` OPEN-3) + total and ignores every client money field (unknown fields answer 422 via `ApiFormRequest`). Quote: side-effect free, validates the `ShippingAddress` schema (422), answers 409 `CART_EMPTY`/`PRODUCT_UNAVAILABLE`/`INSUFFICIENT_STOCK`, and echoes the address with a deterministic `addr_` fingerprint. Create: `201` with immutable snapshots and the cart emptied; 409 `CART_EMPTY`/`INSUFFICIENT_STOCK` only (recorded decision: availability is the quote's documented gate — the `POST /orders` 409 list documents no `PRODUCT_UNAVAILABLE`); idempotency replays the stored `201` for the same key+body, answers 409 `IDEMPOTENCY_KEY_REUSED` for a different body and 400 `MISSING_IDEMPOTENCY_KEY` for absent/non-UUID keys, and its record is scoped per user+endpoint and written inside the creating transaction so a concurrent duplicate insert rolls the whole order back. Reads: paged newest-first owned summaries; a foreign order answers 404, never 403. Payment: `success` locks the inventory rows, re-checks availability, decrements transactionally, moves the order to `confirmed`, and records a `completed` demo payment (`DEMO-` + 12 `[A-Z0-9]`); stale stock answers 409 `INSUFFICIENT_STOCK` and rolls everything back (recorded decision: this operation's own 409 list omits the stale-stock case, but integrity forbids overselling and the code exists in the global `x-error-codes`); `failure` records a `failed` attempt and keeps `pending_payment` so the customer retries with a new key (OPEN-4), and a failed payment is never rendered as paid; duplicate submissions replay without a second decrement; non-pending orders answer 409 `INVALID_ORDER_STATE`; any body field beyond `outcome` is 422, so card data cannot be submitted. Admin: list filters LIKE-escaped `q` (reference/customer email), `status`, inclusive UTC `from`/`to`, and page/perPage; detail exposes `allowedTransitions` from `App\Support\OrderStateMachine` (`confirmed` reachable only through payment); `PATCH /admin/orders/{orderId}/status` applies a permitted transition or answers 409 `INVALID_TRANSITION`, appends to `timeline`, and is throttled `admin-write`; quote/create/payment carry `throttle:customer-write`. Test-environment note for later admin suites: the admin session identity persists in-process, so every customer call must run before `actingAsAdmin()` (Sanctum resolves the session guard before bearer tokens). `php artisan test` = 143 tests, 1947 assertions, 0 failures (33 new: 25 in `tests/Feature/Order/OrderTest.php`, 8 in `tests/Feature/Admin/AdminOrderTest.php`); `vendor\bin\pint --test` passes. Phase-2 completion sweep (2026-10-14): a fresh-eyes audit of CART-001 + ORD-001 against the binding contract confirmed full schema/error-code/rate-limit conformance and fixed two concurrency defects under criterion (c) — `POST /orders/{orderId}/payment` now locks every stock row in one `product_id`-ordered statement (the previous per-line locking reads took rows in cart-line order, so two concurrent payments on differently ordered carts could interleave locks and deadlock), and the admin status update now performs its read-modify-write inside a transaction on a locked order row (a stale read could overwrite a concurrently appended payment timeline event). Behavior is unchanged; the full suite still reports 143 tests, 1947 assertions, 0 failures.

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
