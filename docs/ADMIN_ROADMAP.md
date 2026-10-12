# Admin Roadmap

## Purpose and authority

Plan React/TypeScript dashboard work through the REST API, following shared dashboard design and session rules.

This file is the canonical task list for its component. Each task appears once here; stable Task IDs identify work across branches and PRs. Project-wide phase gates and cross-component tasks are canonical in [ROADMAP.md](ROADMAP.md). Follow [repository-wide agent instructions](../AGENTS.md), component guidance, authoritative documents linked by the task, and the actual implementation.

A roadmap assignment uses this roadmap name, phase, and step, for example: Admin Roadmap, Phase 1, Step 1; a stable Task ID may also identify the work. Step numbers restart at 1 in every phase. Lowercase letter labels restart at `a` in each acceptance-criteria block and identify individual criteria for discussion or verification, not separate assignments. Check dependencies before starting; a later step is not ready until its listed dependencies are complete. See [the project roadmap](ROADMAP.md) and [repository Git naming conventions](../AGENTS.md).

## Phase 0 — Foundation

**Project phase gate:** see [Phase 0 in the Project Roadmap](ROADMAP.md).

### Step 1 — FND-003 — Initialize the admin application and local workflow

**Status:** `[ ]`

**Depends on:** none

**Prerequisites to start:**
- Node.js 18+ available locally
- Package manager (npm/yarn/pnpm) available
- React 18+, TypeScript, Vite tooling decided
- Vitest and React Testing Library selected
- Local development CORS/origin strategy for Sanctum planned

**Scope:** create the React/TypeScript admin foundation; do not implement product workflows yet.

**Acceptance criteria:**

- [ ] **a.** Select supported stable tooling and commit the package lockfile; use the agreed Vitest and React Testing Library strategy or document a reviewed equivalent.
- [ ] **b.** Configure the API origin without committing credentials and document the local SPA/API origin arrangement needed for Sanctum cookies and CSRF.
- [ ] **c.** Provide a minimal app and a passing behavior-oriented test.
- [ ] **d.** Record and verify exact install, development, type-check/lint, test, and production-build commands in `docs/DEVELOPMENT.md`.
- [ ] **e.** Do not call or implement product endpoints before FND-004 is reviewed.

**Verification:** clean dependency install, baseline UI test, static checks, and production build.

## Phase 1 — Accounts and Catalog

**Project phase gate:** see [Phase 1 in the Project Roadmap](ROADMAP.md).

### Step 1 — AUTH-003 — Admin session authentication and API authorization

**Status:** `[ ]`

**Depends on:** FND-002, FND-003, FND-004, FND-005, DES-001

**Prerequisites to start:**
- **FND-002 complete** (Backend: Laravel app initialized; the Sanctum admin session/CSRF configuration is delivered by the AUTH-003 backend portion)
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

**Scope:** secure first-party admin sign-in/session lifecycle, Sanctum CSRF flow, and server-enforced admin role checks.

**Acceptance criteria:**

- [ ] **a.** Admin login/logout and session/CSRF behavior work with the documented local SPA/API origins and reviewed contract.
- [ ] **b.** Admin routes reject guests and customer accounts; tests verify role checks server-side, not only hidden UI controls.
- [ ] **c.** Session cookies use secure settings appropriate to local vs. HTTPS environments; no bearer token is stored in browser local storage for the admin SPA.
- [ ] **d.** Failure and expired-session UI states are visible and recoverable.

**Verification:** backend authentication/authorization feature tests and admin UI tests for login, logout, CSRF/session expiry, and rejected access.

### Step 2 — CAT-004 — Admin catalog and inventory screens

**Status:** `[ ]`

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

**Scope:** React admin screens for product, category, and stock management through the REST API.

**Acceptance criteria:**

- [ ] **a.** Admin can list, create, edit, archive, and validate product/category data and update stock through documented APIs.
- [ ] **b.** Loading, empty, validation, conflict, unauthorized/session-expired, and network-error states are understandable and recoverable.
- [ ] **c.** Forms are keyboard accessible, have labels, and behave at supported narrow and desktop widths.
- [ ] **d.** Tests cover critical form behavior and API error mapping without depending on backend internals.

**Verification:** admin component/behavior tests and production build/type checks.

## Phase 2 — Cart, Checkout, and Orders

**Project phase gate:** see [Phase 2 in the Project Roadmap](ROADMAP.md).

### Step 1 — ORD-003 — Admin order management

**Status:** `[ ]`

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

**Scope:** admin order list/detail and permitted status updates.

**Acceptance criteria:**

- [ ] **a.** Admin can inspect order snapshots and apply only documented valid state transitions.
- [ ] **b.** Server rejects unauthorized users and invalid transitions; tests cover both.
- [ ] **c.** Dashboard shows loading, empty, conflict, error, and updated status states.
- [ ] **d.** Admin tests verify behavior through API responses, not implementation details.

**Verification:** backend transition/authorization feature tests and admin behavior tests.

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
