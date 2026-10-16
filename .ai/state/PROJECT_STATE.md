# Project State

> Last updated: 2026-10-16

## Current Phase

**Phase 0 — Foundation**

## Status

| Component | Status | Notes |
|-----------|--------|-------|
| Repository structure | ✅ Established | Android scaffold and component guidance exist |
| Documentation | ⚠️ Foundation documented | Project gates/shared tasks are in `docs/ROADMAP.md`; numbered canonical task steps are in the Android, Backend, and Admin roadmaps; `docs/android/` now holds the task-brief template, the mock strategy, and the Phase 0–2 implementation specs; verified component commands remain open |
| Android app | 🟡 Phase 2 In Progress | Java/XML Material 3 app. Phase 0 Foundation complete (FND-001, FND-006, UI-002, APP-002, DOM-002, DATA-002, NET-002). Phase 1 complete (AUTH-002, CAT-002). Phase 2 Step 1 complete (CART-002). Phase 2 Step 2 complete (CHECK-001 — checkout and simulated payment flow, Screens M-11..M-14, zero credential capture, idempotent order placement, safe retry). 136 unit tests passing, instrumentation test suite verified, 0 lint errors. |
| Backend API | ✅ Complete — Backend Roadmap Phases 0–3 all delivered and verified | Laravel 13 (`laravel/framework` 13.35.0) on PHP 8.4.25, Composer 2.10.3, PHPUnit 12, Pint (PSR-12), Sanctum ^4.3. Database: local zip-based MySQL 8.4 (`127.0.0.1:3306`) with `eshopverse_dev` + disposable `eshopverse_test` (`phpunit.xml` hard-wired; a hosted MySQL 8+ TLS service is a documented drop-in replacement). Full REST surface per the accepted contract: customer auth, catalog reads, cart, checkout quote, idempotent order creation, simulated payment, admin session/catalog/inventory/order operations, and every rate-limit class from `x-rate-limit-classes` (including the `global` 300/min limiter added in QUAL-003). Final clean-checkout verification 2026-10-15: `composer install` (79 packages from `composer.lock`), `composer validate --strict` valid, `composer audit` no advisories, `migrate:fresh --seed` (15 migrations, seeded demo store), `php artisan test` = 153 tests / 2062 assertions / 0 failures, `pint --test` passed. Per-phase records in `docs/BACKEND_ROADMAP.md`; findings in `docs/SECURITY_REVIEW.md`; reviewer guide in `docs/HANDOFF.md`. |
| Admin dashboard | 🟡 Phase 1 complete — branch prepared for review (not merged) | React 19 + TypeScript 6 + Vite 8 SPA in `admin/`. FND-003 foundation; AUTH-003 Sanctum stateful session authentication (CSRF handshake at `GET /admin/auth/csrf`, `GET /admin/auth/session` boot, expiry/logout handling, no browser-stored tokens) with the reviewable backend integration support (credentialed CORS for the documented SPA origins; contract-conform 401 for plain requests); CAT-004 product, category, and inventory screens (search/filter/paginate lists, create/edit forms with client validation and server `422`/`409` field mapping, archive/restore with conflict handling, absolute stock adjustment). Tailwind CSS 4 and react-router-dom 7 added with an audit-clean lockfile. 58 tests passing in 8 files; `npm run typecheck` / `npm run lint` / `npm run build` clean; live API probes passed. Verification records in `docs/ADMIN_ROADMAP.md`; commands in `docs/DEVELOPMENT.md`. |
| API contract | ✅ Accepted (FND-004) | `docs/openapi.yaml` (OpenAPI 3.1) was reviewed and accepted by the API owner on 2026-10-12 as the binding machine-readable contract; `docs/API_CONTRACT.md` records shared conventions and links to it without duplicating endpoint schemas. Any contract change is made in `openapi.yaml` first and re-accepted. |
| CI/CD | ⚠️ Placeholder | `.github/workflows/ci.yml` only validates repository structure; Android FND-006 adds the Android build/lint/test job and FND-005 owns component-check consolidation |
| Database | ✅ Implemented (backend) | 15 migrations shipped with the backend (users/account fields, categories, products, product images, inventories, personal access tokens, carts, cart items, orders, order items, demo payments, idempotency keys); deterministic fictional seed data via `php artisan migrate:fresh --seed` (5 categories, 12 products / 10 active, demo customer + env/random-password demo admin, no real personal data); disposable `eshopverse_test` database enforced by `phpunit.xml`. Logical model and decisions remain documented in `docs/DATABASE.md`. |

## Active Branches

| Branch | Purpose | Owner |
|--------|---------|-------|
| main | Pre-policy baseline; future release target | — |
| develop | Shared integration branch; created from current `main` | — |
| docs/project-p00-s09-plan-005-android-execution-structure | Task branch for PLAN-005 | — |

## Recent Decisions

- **Backend database (2026-10-11, owner decision):** no local MySQL — a hosted MySQL service is used instead. The chosen provider is Aiven Free tier (MySQL 8.4.11, remote host/port, project `ca.pem`); two databases `eshopverse_dev` and `eshopverse_test` were created on it. Connections run over TLS 1.3 and are verified against the CA file, as `docs/SECURITY.md` requires. Credentials and `MYSQL_ATTR_SSL_CA` live only in the git-ignored `backend/.env`. **Superseded 2026-10-12:** the provider deleted that free service after inactivity; the documented local zip-based MySQL 8.4 workflow in `docs/DEVELOPMENT.md` is now the verified default, with a hosted MySQL 8+ TLS service remaining a documented drop-in replacement.
- **Backend test naming (2026-10-11):** PHP test methods use `snake_case` because Laravel Pint enforces `php_unit_method_casing`; the `docs/TESTING.md` PHP example was aligned with the enforced style.
- **Backend API mounting (2026-10-11):** `routes/api.php` exists and is registered with the `api/v1` prefix from `ARCHITECTURE.md`, but stays empty until the OpenAPI contract is accepted (FND-004). **Superseded:** FND-004 was accepted 2026-10-12 and the full endpoint set (auth, catalog, cart, checkout/orders, admin operations) now lives behind that prefix; `routes/api.php` is no longer empty.

- First-release portfolio scope and stack recorded in ADR-001
- Sequential build/test/debug workflow documented
- Canonical project gates and Task-ID index in `docs/ROADMAP.md`; component task details and numbered steps in `docs/ANDROID_ROADMAP.md`, `docs/BACKEND_ROADMAP.md`, and `docs/ADMIN_ROADMAP.md`
- Gitflow-lite policy chosen: task PRs target `develop`; release PRs target `main`; GitHub settings still need verification/configuration (GIT-001)
- Previous Android scaffold configuration: Java 8 source compatibility, minSdk 25, JUnit 4; superseded by the regenerated Android Studio scaffold recorded above.
- Android scaffold regenerated from Android Studio; package ID and toolchain values recorded from its Gradle files. All screen implementations must follow the shared Material 3 design system.
- Owner selected the supplied coral/neutral, Lato-based visual reference for Android and admin; DES-001 captures its scope, tokens, page specs, and excluded concepts.
- PLAN-005 restructured the Android backlog: six foundation steps precede the screen steps, Phase 1/2 steps now depend only on Android and shared steps, the integration boundary is the reviewed contract plus a local mock instead of a running backend, and task execution is defined by a token-efficient brief with explicit file ownership.
- The repository was bootstrapped as a Git repository with a `main` baseline and a `develop` integration branch; `android/gradlew` was corrected to mode `100755` and line endings are pinned by `.gitattributes`, both of which would otherwise break Linux CI.
- Known environment gap: the current checkout cannot build the Android app (JDK 8, no Android SDK). It is recorded in `docs/ROADMAP.md`, `docs/ANDROID_ROADMAP.md`, `docs/DEVELOPMENT.md`, and `docs/android/PHASE-0-FOUNDATION-SPEC.md`.

## Next Steps

1. **Backend component: nothing outstanding.** Backend Roadmap Phases 0–3 are complete and verified (final clean-checkout run 2026-10-15: 153 tests / 2062 assertions / 0 failures, pint clean, composer audit clean). Per AGENTS.md, backend agents run no Git commands — the owner manages commits/pushes/PRs. The canonical cross-component QUAL-001..QUAL-005 entries in `docs/ROADMAP.md` are marked `[~]` (backend contributions delivered; the Android and admin contributions — screenshots, accessibility pass, full multi-component rehearsal — are still pending); the backend portions are recorded in `docs/BACKEND_ROADMAP.md` Phase 3.
2. **Backend-adjacent, needs owner approval:** enabling the backend job in `.github/workflows/ci.yml` (currently a commented template that still says PHP `8.2` and `vendor/bin/phpunit`, both outdated versus the project's PHP 8.4 / `php artisan test`). This belongs to cross-component FND-005 (criterion b) and, as a CI/CD pipeline change, requires explicit human approval per AGENTS.md before any edit.
3. Complete Phase 0 tasks in their canonical roadmaps, respecting the waves in `docs/ANDROID_ROADMAP.md` and the shared phase gate.
4. Android FND-006 first: it unblocks the parallel wave (UI-002, DOM-002, DATA-002) and produces the build/test evidence that is currently missing.
5. Begin Phase 1 only after Phase 0 exit criteria are verified.
