# Backend Roadmap

## Purpose and authority

Plan PHP/Laravel REST API and MySQL work, including contract-first implementation and isolated database checks.

This file is the canonical task list for its component. Each task appears once here; stable Task IDs identify work across branches and PRs. Project-wide phase gates and cross-component tasks are canonical in [ROADMAP.md](ROADMAP.md). Follow [repository-wide agent instructions](../AGENTS.md), component guidance, authoritative documents linked by the task, and the actual implementation.

A roadmap assignment uses this roadmap name, phase, and step, for example: Backend Roadmap, Phase 1, Step 1; a stable Task ID may also identify the work. Step numbers restart at 1 in every phase. Lowercase letter labels restart at `a` in each acceptance-criteria block and identify individual criteria for discussion or verification, not separate assignments. Check dependencies before starting; a later step is not ready until its listed dependencies are complete. See [the project roadmap](ROADMAP.md) and [repository Git naming conventions](../AGENTS.md).

## Component status

**✅ All backend phases are complete (2026-10-15; Phase 4 added and completed 2026-10-25).** Phases 0–2 are `[x]` with dated verification records below; Phase 3 records the backend contributions to the cross-component QUAL tasks, whose canonical status and verification record live in [ROADMAP.md](ROADMAP.md). Phase 4 (AUTH-B01..AUTH-B03) hardens Google sign-in configuration, distinguishes deactivated accounts, and supports multiple trusted client audiences. Final clean-checkout verification (2026-10-15, re-run on this fresh copy of the merged tree): `composer install` (79 packages from `composer.lock`), `composer validate --strict` valid, `composer audit` no advisories, `php artisan migrate:fresh --seed` (15 migrations, seeded demo store), `php artisan test` = 153 tests / 2062 assertions / 0 failures, `vendor\bin\pint --test` passed. No backend task remains open; the owner-approved 2026-10-19 addition **AUTH-004 — Google Sign-In** (Phase 1 Step 4) is implemented and verified (see its entry below); the canonical QUAL-001..QUAL-005 entries are marked `[~]` (backend contributions delivered; Android and admin contributions pending).

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

### Step 4 — AUTH-004 — Google Sign-In API

**Status:** `[x]`

**Depends on:** FND-002, FND-004, AUTH-001, owner-approved contract change

**Prerequisites to start:**
- Owner approval of the Google Sign-In contract change recorded 2026-10-19
- FND-002, FND-004, AUTH-001 complete (Sanctum bearer sessions exist)
- `GOOGLE_CLIENT_ID` configured from `.env` (placeholder in `.env.example`; not a secret)

**Scope:** owner-approved Google Sign-In / Sign-Up for the customer app: server-side Google ID-token verification without an SDK dependency, account creation/linking, and the shared Sanctum `AuthSession` response.

**Acceptance criteria:**

- [x] **a.** `POST /auth/google` matches the accepted contract: `AuthSession` envelope on `200` (existing account) and `201` (new account); missing, too-short, or oversized `idToken` answers `422 VALIDATION_ERROR`; invalid tokens answer `401 INVALID_CREDENTIALS`; a conflicting Google identity answers `409 GOOGLE_ACCOUNT_CONFLICT`; the endpoint carries its own `google-login` 10/min/ip limit.
- [x] **b.** The ID token is verified locally against Google's published JWKS (RS256 signature, issuer, audience `GOOGLE_CLIENT_ID`, expiry, verified email); the raw token is never stored or logged, and no new dependency is added.
- [x] **c.** A verified Google email signs in the linked account, links an account that has no `google_id`, or creates a password-less customer account; `role` is always `customer`, and suspended accounts are rejected without disclosure.
- [x] **d.** The schema change is a reviewed migration: unique nullable `google_id` and nullable `password`, reflected in `docs/DATABASE.md`.
- [x] **e.** Feature tests cover creation, existing sign-in, linking, conflict, suspended, malformed/expired/wrong-audience/wrong-issuer/unverified tokens, unconfigured client ID, unavailable keys, and rate limiting; the full suite and Pint pass.

**Verification (2026-10-19, owner-requested full recheck on the working tree):** Every criterion confirmed against the implementation, the accepted contract, and the executed suite. Evidence: `php artisan test tests/Feature/Auth/GoogleAuthTest.php` = 16 tests / 122 assertions / 0 failures (creation, linked sign-in, email linking, conflict, suspended, malformed, missing/short/oversized/unknown-field validation, expired, wrong audience, wrong issuer, unverified email, unknown signing key, unconfigured client ID, unavailable Google keys, rate limit with `Retry-After`, plus DB-level nullable/unique `google_id`); `php artisan test` = 173 tests / 2202 assertions / 0 failures; `vendor/bin/pint --test` passed; `php artisan migrate:rollback --step=1` + `php artisan migrate` round-trip clean on the development database; `docs/openapi.yaml` Redocly lint = 0 errors / 2 accepted warnings; `composer.json` unchanged (no dependency added). Implemented in `GoogleAuthService` (local JWKS verification, cached 3600s, refetch on key rotation), `GoogleAuthController` (plus the extracted `AuthSessionIssuer` now shared with AUTH-001), `GoogleAuthRequest`, the route `POST /auth/google` with the `auth-google` limiter, and migration `2026_10_19_000100_add_google_id_to_users_table.php`; docs synced in `docs/openapi.yaml`, `docs/API_CONTRACT.md`, `docs/DATABASE.md`, `docs/PRODUCT.md`, `docs/DEVELOPMENT.md`, and `backend/.env.example`.

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

**Backend contribution (2026-10-14):** `tests/Feature/Quality/PurchaseFlowRegressionTest.php` walks the documented customer journey end to end against the real seeded store — sign in as the seeded demo customer, browse the catalog, add/update a cart line, quote, create an order (immutable snapshots, cart emptied), complete the simulated success, and read the resulting order — verifying the database side effects (inventory decremented, payment `completed`, timeline `pending_payment → confirmed`). It also covers the boundary cases: duplicate submission (same key+body replays, conflicting reuse 409 `IDEMPOTENCY_KEY_REUSED`), simulated failure (stays `pending_payment`, stock unchanged, retryable), and an object-level authorization boundary (another customer answers 404). Isolated disposable data, no real provider. Backend-side regression evidence delivered; the canonical task stays open pending the Android and admin contributions.

### Step 2 — QUAL-002 — Component contribution

**Canonical task:** [QUAL-002 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Verify API error behavior and backend-supported UI states against shared quality requirements.

**Backend contribution (2026-10-14):** `tests/Feature/Quality/ApiQualityStateTest.php` verifies the two contract behaviors every screen depends on: (1) every failure path — 401 `UNAUTHENTICATED`, 404 `NOT_FOUND`, 409 `CART_EMPTY`, 422 `VALIDATION_ERROR` — returns the single documented `{error:{code,message[,details]}}` envelope so a client can render one generic error state, and (2) every empty result — the cart-less cart read, an empty order list, a no-match catalog search, and a page requested past the end — returns a well-formed payload (correct `pagination` with `totalPages` 0) so screens show an empty state rather than a dead end. This proves the server supports consistent loading/empty/error/success/retry states at the contract layer; the Android/admin rendering of those states is their components' contribution.

**Backend follow-up (2026-10-24):** Wrong HTTP methods on `/api/*` now return the shared error envelope with `405 METHOD_NOT_ALLOWED`; the code is added to `docs/openapi.yaml` and `docs/API_CONTRACT.md`, and `ApiRoutingTest` pins it.

### Step 3 — QUAL-003 — Component contribution

**Canonical task:** [QUAL-003 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Review authentication, authorization, validation, rate limits, data protection, logging, and dependency risks.

**Backend contribution (2026-10-14):** Findings record in [`SECURITY_REVIEW.md`](SECURITY_REVIEW.md). One in-scope, high-risk finding was **fixed**: the contract's `global` 300/min/ip rate limiter (`x-rate-limit-classes`, which applies to every operation in addition to narrower classes) was not implemented — the "global-only" operations (`getCart`, `listOrders`, `getOrder`, `getCustomerProfile`, all admin reads, `password-reset/confirm`) carried no throttle at all. It is now registered in `AppServiceProvider` and appended to the whole `api` middleware group in `bootstrap/app.php`, stacking with the narrower per-route limiters; regression test `RateLimitRegressionTest` pins it. Remaining checks passed with no defects: `composer audit` clean, `composer validate --strict` valid, no committed secrets (`.env` git-ignored, `.env.example` placeholders only), Eloquent/parameterized queries only, user-safe error envelope with no stack traces/SQL/paths, bcrypt hashing, no PII/token logging, object-level ownership returns 404 not 403. Accepted non-production limitations (simulated payment, loopback HTTP, demo-tuned limits) are recorded per `SECURITY.md`.

**Backend follow-up (2026-10-24):** Email identity handling is now consistently case-insensitive across registration, sign-in, and password reset. `User` stores trimmed lowercase addresses, request objects normalize input, legacy rows are migrated by `2026_10_23_000100_normalize_user_emails.php`, and the migration aborts instead of merging case-only duplicate accounts. Regression tests cover each flow.

### Step 4 — QUAL-004 — Component contribution

**Canonical task:** [QUAL-004 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Verify backend setup, migrations, seed data, demo roles, reset, and limitations in the handoff guide.

**Backend contribution (2026-10-14):** Created [`HANDOFF.md`](HANDOFF.md), a reviewer-facing guide covering the backend clean setup (`composer install` → `.env` → `key:generate` → `migrate:fresh --seed` → `test` → `pint` → `serve`), the seeded demo roles (local-only `demo@example.com` customer and the env/random-password `demo-admin@eshopverse.test` administrator — no committed public credential), the `php artisan migrate:fresh --seed` reset procedure, a walk-through of the purchase flow, and the known limitations (simulated payment, flat sample shipping, no production-readiness claim). The documented reset was executed against the development database: all 15 migrations ran and seeding produced 2 users (1 admin), 5 categories, 12 products (10 active), and 12 inventories. The visual walkthrough/screenshots (criterion c) remain the Android and admin contributions.

### Step 5 — QUAL-005 — Component contribution

**Canonical task:** [QUAL-005 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Run documented backend and OpenAPI checks from a clean checkout and report exact commands and outcomes.

**Backend contribution (2026-10-14):** Ran the documented backend checks on the committed tree and recorded exact commands and results in the `DEVELOPMENT.md` verification table: `composer install` (exit 0), `.env` from `.env.example` + `key:generate` (no secret checked in), `composer validate --strict` (valid), `composer audit` (no advisories), `php artisan migrate:fresh --seed` (15 migrations, seeded demo store), `php artisan test` (153 tests, 2062 assertions, 0 failures), and `vendor\bin\pint --test` (passed). `docs/openapi.yaml` was not modified this phase, so the accepted Redocly lint baseline (0 errors, 2 intentional warnings from FND-004) still holds unchanged. The `phpunit.xml`-enforced test database (`eshopverse_test`) keeps runs isolated from the development database.

**Backend re-verification (2026-10-24):** With `config.platform.php` pinned to 8.3.36, `composer install`, `composer validate --strict`, and `composer audit` pass; MySQL 8.4.0 reports 180 tests / 2,229 assertions / 179 passed / 1 skipped (the MySQL-impossible case-collision fixture); Pint passes; and Redocly validates `docs/openapi.yaml` with 0 errors and the 2 accepted server warnings. Exact commands and outputs are in `DEVELOPMENT.md`.

## Phase 4 — Authentication Hardening & Identity Services

**Project phase gate:** see [Phase 4 in the Project Roadmap](ROADMAP.md).

### Step 1 — AUTH-B01 — Google Auth Configuration Hardening & Diagnostics

**Status:** `[x]`

**Depends on:** AUTH-004

**Scope:** Enhance `GoogleAuthService` error diagnostics and configuration validation so that unconfigured `GOOGLE_CLIENT_ID` produces clear server logs rather than generic internal errors.

**Acceptance criteria:**
- [x] **a.** Log an explicit warning via `Log::error` when `GOOGLE_CLIENT_ID` is missing from environment configuration before terminating verification.
- [x] **b.** Provide documented setup guidance in `backend/.env.example` and `docs/DEPLOYMENT.md` for Web Client ID provisioning.
- [x] **c.** Verify clean test execution and assert that misconfigured client ID produces appropriate log telemetry.

**Verification (2026-10-25):** `GoogleAuthService::verify()` now resolves all trusted audiences first and, when none are configured, writes `Google Auth failed: GOOGLE_CLIENT_ID is not configured in .env (set GOOGLE_CLIENT_ID or GOOGLE_CLIENT_IDS).` via `Log::error` before failing closed with the unchanged `500 INTERNAL_ERROR` (no token is ever accepted without a known audience). The message names both supported variables and contains no token material. `backend/.env.example` documents the Web application client ID from Google Cloud Console, the exact-match requirement, and the fail-closed/logging behavior; `docs/DEVELOPMENT.md` and `docs/DEPLOYMENT.md` gained local and hosted provisioning guidance (Console path, Web application type, authorized origin, comma-separated `GOOGLE_CLIENT_IDS`, where to find the log line). `GoogleAuthTest::test_google_sign_in_fails_closed_when_client_id_is_not_configured` now also spies on the log and asserts the explicit message, the `500` envelope, and that no account was created. Evidence: `tests/Feature/Auth` = 60 tests / 479 assertions / 0 failures; `vendor/bin/pint --test` passed; full suite = 185 tests / 2,244 assertions with 2 engine-only failures under the SQLite fallback (see the Phase 4 note below).

### Step 2 — AUTH-B02 — User Status Differentiation & Inactive Account Rejection

**Status:** `[x]`

**Depends on:** AUTH-001, AUTH-004

**Scope:** Distinguish inactive/suspended accounts (`status !== 'active'`) from invalid credentials, returning contract-friendly 403 `ACCOUNT_DISABLED` instead of 401 `INVALID_CREDENTIALS`.

**Acceptance criteria:**
- [x] **a.** In `AuthController::login` and `GoogleAuthController::login`, check if user exists and is inactive, throwing `ApiException(403, 'ACCOUNT_DISABLED', ...)` instead of `INVALID_CREDENTIALS`.
- [x] **b.** Update `docs/openapi.yaml` error codes and add feature tests in `LoginTest` and `GoogleAuthTest` asserting 403 response for deactivated accounts.
- [x] **c.** Confirm error envelope matches standard contract schema `{error: {code: "ACCOUNT_DISABLED", message: "..."}}`.

**Verification (2026-10-25):** Password sign-in now checks the account state only after the password has been verified, so a deactivated account with correct credentials answers `403 ACCOUNT_DISABLED` with the user-facing message "This account has been deactivated. Please contact support.", while a wrong password on the same account keeps the identical vague `401 INVALID_CREDENTIALS` (and still counts toward lockout) so account existence is never disclosed without credentials. Google sign-in answers the same `403` after Google has verified the identity. `docs/openapi.yaml` documents the `403` response on both operations through the new shared `AccountDisabled` response component, the new `ACCOUNT_DISABLED` entry in `x-error-codes`, and updated operation descriptions; `docs/API_CONTRACT.md` records the ownership-before-disclosure rule. Tests: `LoginTest::test_login_rejects_suspended_account_with_account_disabled` and `GoogleAuthTest::test_google_sign_in_rejects_suspended_account_with_account_disabled` assert the `403` envelope, and `LoginTest::test_login_does_not_disclose_suspended_account_with_wrong_password` pins the non-disclosure path. Recorded decision: admin sign-in keeps its existing non-specific `401` (AUTH-003 behavior) because admin sessions intentionally never reveal account state. Evidence: `tests/Feature/Auth` = 60 tests / 479 assertions / 0 failures; Redocly lint = 0 errors / 2 accepted server warnings; Pint passed.

### Step 3 — AUTH-B03 — Multi-Audience Support for Google Identity

**Status:** `[x]`

**Depends on:** AUTH-004

**Scope:** Allow `GoogleAuthService::verifyClaims` to validate against multiple trusted client IDs (comma-separated or array in config) to support Android, Admin, and potential Web clients concurrently.

**Acceptance criteria:**
- [x] **a.** Support comma-separated `GOOGLE_CLIENT_IDS` in `config/services.php` alongside single client ID.
- [x] **b.** Assert token audience against the whitelist in `verifyClaims`.
- [x] **c.** Add regression unit/feature tests for multi-audience validation.

**Verification (2026-10-25):** `config/services.php` adds `google.client_ids` from `GOOGLE_CLIENT_IDS` while keeping `google.client_id`; `GoogleAuthService::configuredAudiences()` accepts either setting as a string or an array, splits comma-separated values, trims blanks, and de-duplicates, so no extra configuration format is required. `verifyClaims()` accepts a token whose `aud` claim (string or array) matches any trusted ID using `hash_equals`, and rejects every audience outside the whitelist with `401 INVALID_CREDENTIALS`. Four new GoogleAuthTest cases cover a comma-separated additional audience, an array-valued `client_id`, an array `aud` claim containing a trusted ID, and an untrusted audience that creates no account. `docs/openapi.yaml` and `docs/API_CONTRACT.md` document the multi-audience behavior, and `backend/.env.example`, `docs/DEVELOPMENT.md`, and `docs/DEPLOYMENT.md` document `GOOGLE_CLIENT_IDS`. Evidence: `tests/Feature/Auth` = 60 tests / 479 assertions / 0 failures; Pint passed; Redocly lint 0 errors / 2 accepted warnings.

**Phase 4 test-environment note:** the repository's test configuration targets MySQL (`eshopverse_test`). No MySQL server is installed in the 2026-10-25 verification environment, so the suite was run against SQLite (`DB_CONNECTION=sqlite DB_DATABASE=:memory:` with a temporary `.env` for `APP_KEY`/session cookie names). Result: 185 tests / 2,244 assertions, with only two engine-specific failures — `AdminCategoryTest::test_admin_category_create_validates_shapes_and_rejects_duplicate_names` (MySQL's case-insensitive collation vs SQLite) and `DatabaseWorkflowTest::test_test_database_is_the_isolated_test_database` (deliberately asserts the `mysql` driver). Every auth/customer/admin test relevant to these tasks passed.

**Phase 4 CI verification (2026-10-25):** the repository's own `.github/workflows/ci.yml` **Backend Build & Test** job ran on the pushed Phase 4 head with its MySQL 8.4 service (`eshopverse_test`) and completed **successfully**: `composer install`, `composer validate --strict`, `composer audit`, `vendor/bin/pint --test`, and the full `php artisan test` (run [40216437407](https://github.com/Averon-Labs/eshop-verse/actions/runs/40216437407)). That closes the MySQL item above — the two engine-specific SQLite failures do not occur on the real test database. Delivered through [PR #62](https://github.com/Averon-Labs/eshop-verse/pull/62) (`feature/backend-phase-4` → `develop`).

**Phase 4 live smoke (2026-10-25):** the API was also exercised over real HTTP (`php -S 127.0.0.1:8765` with a throwaway SQLite database, removed afterwards). Registration returned `201` with a working Sanctum token and sign-in returned `200`. Suspending that customer and signing in with the correct password returned `403 ACCOUNT_DISABLED` with the documented message, while a wrong password still returned the identical vague `401 INVALID_CREDENTIALS`; no token was issued on the disabled path. `POST /auth/google` with no client ID returned `500 INTERNAL_ERROR`, wrote `Google Auth failed: GOOGLE_CLIENT_ID is not configured in .env...` to `storage/logs/laravel.log`, and created no account. With client IDs configured, malformed, wrong-audience, and forged-signature tokens all returned `401 INVALID_CREDENTIALS` and created no account; forged tokens carrying either the primary or the additional trusted audience reached the real Google JWKS endpoint and were rejected there (the local PHP build needed an explicit CA bundle for outbound TLS). A success-path Google token cannot be minted locally, so the successful signup/sign-in journey is proven by the hermetic `GoogleAuthTest` cases, which fake only Google's JWKS response and run the real route, controller, service, database, and Sanctum token issuance.

**Real client-ID check (2026-10-25):** the owner supplied a Google OAuth client from the `eshop-verse` project. It was configured in the git-ignored `backend/.env`; `config('services.google.client_id')` reported it and the live endpoint then answered `401 INVALID_CREDENTIALS` (not the unconfigured `500`) for malformed, wrong-audience, and forged-signature tokens, still creating no account. A genuine Google-issued token could not be obtained non-interactively: the first download was an installed/Android client (no client secret or redirect URIs), while ID-token verification requires the **Web application** client ID used by the app as its server client ID, and Google blocks the OAuth loopback flow for local testing. The owner then supplied a **Web application** client for the same project. It was configured locally; `config('services.google.client_id')` reports it, a forged token carrying that audience reached Google's real JWKS endpoint and the key response was cached, an Android-client audience was rejected, and no account was created by any failed attempt. The client secret was not stored or used: ID-token verification needs only the public client ID. Remaining real-account test: the Android app still carries the placeholder Web client ID, so an end-to-end Google sign-in needs the app pointed at the supplied Web client and run on a device/emulator (Android component); no backend code defect blocks it.

