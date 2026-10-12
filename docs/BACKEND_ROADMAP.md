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

**Status:** `[ ]`

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

- [ ] **a.** Valid registration/login/profile/logout/reset flows match the reviewed OpenAPI schemas and security rules.
- [ ] **b.** Invalid input, duplicate email, invalid credentials, reset-token failure/expiry, rate limits, and logout revocation return documented outcomes without leaking secrets.
- [ ] **c.** Tests prove passwords are hashed, token lifetime/revocation rules and account lockout after repeated failures are enforced, and reset email can be safely exercised through a local mail sink.
- [ ] **d.** Tests cover relevant validation, authentication, and abuse boundaries; no real email or external provider is required for local tests.

**Verification:** backend unit and API feature/security tests for every listed success and failure path.

### Step 2 — CAT-001 — Catalog read API and fictional seed data

**Status:** `[ ]`

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

- [ ] **a.** Migrations enforce documented relationships, valid prices, product status, and image ordering; use fixed-precision USD money.
- [ ] **b.** Seed/reset creates useful fictional categories/products/images reproducibly and never contains real personal data.
- [ ] **c.** Listing, detail, category, search, sort, pagination, and empty-result behavior match OpenAPI.
- [ ] **d.** Invalid filters and pagination boundaries return documented errors; inactive/unavailable products do not leak into customer results.

**Verification:** migration/seed checks and API feature tests for results, filters, pagination, validation, and public access.

### Step 3 — CAT-003 — Admin catalog and inventory API

**Status:** `[ ]`

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

- [ ] **a.** CRUD and inventory operations follow OpenAPI, validation, and database constraints.
- [ ] **b.** Guest/customer access is rejected; tests cover role enforcement, invalid values, missing resources, and inventory boundaries.
- [ ] **c.** Public catalog changes reflect persisted admin updates; deleted/archived data follows the documented status behavior.
- [ ] **d.** No client-supplied total or stock value bypasses server-side validation.

**Verification:** backend feature tests with database state assertions and authorization tests for each protected operation.

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
