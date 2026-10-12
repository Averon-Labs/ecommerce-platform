# Admin Dashboard — Agent Instructions

> Read the root `AGENTS.md` first. This file contains admin dashboard-specific rules.

The canonical Admin task steps and acceptance criteria are in [`docs/ADMIN_ROADMAP.md`](../docs/ADMIN_ROADMAP.md). Project gates and shared tasks are in [`docs/ROADMAP.md`](../docs/ROADMAP.md).

---

## Git Policy and Phase-Based Branches

- **Phase-Based Branches:** Admin work uses feature branches created per phase (`feature/admin-phase-<N>`), housing all steps of that phase together in a single branch created from `develop`.
- **Sequential Steps as Commits:** Each step within a phase is developed, verified, and committed sequentially as a focused Conventional Commit: `<type>(admin-pNN-sNN-<task-id>): <summary>`.
- **PR Lifecycle:** When all steps of an Admin phase are completed and verified, a Pull Request targeting `develop` is submitted for review.

---

## Architecture

| Aspect | Decision | Status |
|--------|----------|--------|
| Type | Web application | DECIDED |
| Technology | React and TypeScript | DECIDED |
| API access | REST API only | DECIDED |
| Authentication | Laravel Sanctum first-party SPA session | DECIDED |

Use a supported stable toolchain selected at project initialization. The SPA and API must use a compatible first-party domain arrangement for secure cookie/CSRF behavior.

---

## Boundaries

- Admin code lives exclusively in `admin/`.
- Communication with the backend is through the REST API only.
- Admin functionality uses admin-specific API endpoints.
- Do not embed business logic that belongs in the backend.

## Visual Design

- Follow [`docs/DESIGN_SYSTEM.md`](../docs/DESIGN_SYSTEM.md) and [`docs/SCREEN_SPECIFICATIONS.md`](../docs/SCREEN_SPECIFICATIONS.md) for every admin page.
- Reuse the shared coral/neutral palette, Lato typography, spacing, shapes, status semantics, and responsive shell. Do not add a dashboard-only palette or copy Android's bottom navigation into the web layout.
- Keep the first release within product scope: products, categories, inventory, orders, and admin sign-in. An analytics/overview page, customer administration, notifications, transaction history, CMS, and wishlist are deferred.
- Tables, forms, dialogs, loading/empty/error states, focus behavior, and narrow-screen layouts follow the shared patterns in the design and screen specifications.

---

## First-Release Capabilities

- Product management (CRUD)
- Category management
- Order management
- Inventory management

Customer administration and analytics are deferred; see `docs/PRODUCT.md`.

---

## Testing

- Use Vitest and React Testing Library for component/behavior tests unless the initialized project has a documented equivalent.
- See `docs/TESTING.md` for shared test levels and conventions.
