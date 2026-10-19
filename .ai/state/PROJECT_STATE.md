# Project State

> Last updated: 2026-10-19

## Current Phase

**Phase 3 — Quality and Portfolio Delivery (component roadmaps delivered; final cross-component validation pending)**

## Status

| Component | Status | Notes |
|-----------|--------|-------|
| Repository structure | ✅ Established | Android scaffold and component guidance exist |
| Documentation | ⚠️ Foundation documented | Project gates/shared tasks are in `docs/ROADMAP.md`; numbered canonical task steps are in the Android, Backend, and Admin roadmaps; `docs/android/` now holds the task-brief template, the mock strategy, and the Phase 0–2 implementation specs; verified component commands remain open |
| Android app | ✅ Phases 0–3 complete | Java/XML Material 3 app covering the full customer journey. Phase 3 evidence in `docs/ANDROID_ROADMAP.md`: live-backend purchase-flow regression, accessibility/contrast and responsive audit, security/privacy hardening, demo/handoff documentation, and a clean-checkout release rehearsal with 0 test failures and 0 lint errors. |
| Backend API | ✅ Complete — Backend Roadmap Phases 0–3 all delivered and verified | Laravel 13 (`laravel/framework` 13.35.0) on PHP 8.4.25, Composer 2.10.3, PHPUnit 12, Pint (PSR-12), Sanctum ^4.3. Database: local zip-based MySQL 8.4 (`127.0.0.1:3306`) with `eshopverse_dev` + disposable `eshopverse_test` (`phpunit.xml` hard-wired; a hosted MySQL 8+ TLS service is a documented drop-in replacement). Full REST surface per the accepted contract: customer auth, catalog reads, cart, checkout quote, idempotent order creation, simulated payment, admin session/catalog/inventory/order operations, and every rate-limit class from `x-rate-limit-classes` (including the `global` 300/min limiter added in QUAL-003). Final clean-checkout verification 2026-10-15: `composer install` (79 packages from `composer.lock`), `composer validate --strict` valid, `composer audit` no advisories, `migrate:fresh --seed` (15 migrations, seeded demo store), `php artisan test` = 153 tests / 2062 assertions / 0 failures, `pint --test` passed. Per-phase records in `docs/BACKEND_ROADMAP.md`; findings in `docs/SECURITY_REVIEW.md`; reviewer guide in `docs/HANDOFF.md`. |
| Admin dashboard | 🟢 Phases 0–3 delivered; quality contributions independently verified 2026-10-17 at `6d3d7f3` and merged via [PR #47](https://github.com/Averon-Labs/eshop-verse/pull/47); UI-003 light/dark theme implemented 2026-10-19 | React 19 + TypeScript 6 + Vite 8 SPA in `admin/`. Phases 0–2 delivered and merged ([PR #42](https://github.com/Averon-Labs/eshop-verse/pull/42), [PR #43](https://github.com/Averon-Labs/eshop-verse/pull/43), [PR #44](https://github.com/Averon-Labs/eshop-verse/pull/44) merged 2026-10-17): FND-003 foundation; AUTH-003 Sanctum stateful session authentication; CAT-004 product/category/inventory screens; ORD-003 order list/detail with server-limited status transitions. Phase 3 admin contributions to QUAL-001–QUAL-005 committed and independently verified: purchase-flow regression evidence (36/36 live probes); accessibility pass fixing coral text/focus contrast to WCAG AA via the ADR-002 darker coral, self-hosted OFL Lato replacing the Google Fonts runtime request, and a repeatable `npm run check:contrast` (now 34/34 for light + dark); security review findings A-1–A-6 in `docs/SECURITY_REVIEW.md`; admin walkthrough in `docs/HANDOFF.md`; clean-checkout rehearsal of `a15def7` (82 tests, build, preview smoke) in `docs/DEVELOPMENT.md`. UI-003 adds a persisted light/dark toggle (OS default, saved choice, `aria-pressed`, 44px target) with 3 new behavior tests (85 total) and a production build packaged at `deploy/eshopverse-admin-dist.zip`. Verification records in `docs/ADMIN_ROADMAP.md` Phase 3; UI-003 follow-up PR pending. |
| Public demo deployment | ✅ Live (optional, owner-approved 2026-10-19) | ByetHost free hosting at `https://eshopverse.byethost8.com`: Laravel app in `htdocs/eshopverse_app`, admin SPA and API on the same HTTPS origin, database imported and seeded. Verified live: `/up` 200, `/api/v1/products` JSON, admin login 200. The UI-003 theme build is ready for upload; see `docs/DEPLOYMENT.md`. This working copy is prepared for the owner to push to GitHub; no Git operations were performed in this session. |
| API contract | ✅ Accepted (FND-004) | `docs/openapi.yaml` (OpenAPI 3.1) was reviewed and accepted by the API owner on 2026-10-12 as the binding machine-readable contract; `docs/API_CONTRACT.md` records shared conventions and links to it without duplicating endpoint schemas. Any contract change is made in `openapi.yaml` first and re-accepted. |
| CI/CD | ⚠️ Partial | `.github/workflows/ci.yml` runs the repository-structure, Android (build/lint/tests), and backend (install/tests) jobs; an admin job is not present, and the documented-check consolidation plus a verified workflow run remain with **FND-005** (CI/CD pipeline changes require human approval per `AGENTS.md`) |
| Database | ✅ Implemented (backend) | 15 migrations shipped with the backend (users/account fields, categories, products, product images, inventories, personal access tokens, carts, cart items, orders, order items, demo payments, idempotency keys); deterministic fictional seed data via `php artisan migrate:fresh --seed` (5 categories, 12 products / 10 active, demo customer + env/random-password demo admin, no real personal data); disposable `eshopverse_test` database enforced by `phpunit.xml`. Logical model and decisions remain documented in `docs/DATABASE.md`. |

## Active Branches

| Branch | Purpose | Owner |
|--------|---------|-------|
| main | Pre-policy baseline; future release target | — |
| develop | Shared integration branch; created from current `main` | — |
| feature/admin-phase-3 | Admin Phase 3 QUAL-001–QUAL-005 merged via [PR #47](https://github.com/Averon-Labs/eshop-verse/pull/47); branch recreated for the UI-003 light/dark theme follow-up (PR pending) | — |

## Recent Decisions

- **Backend database (2026-10-11, owner decision):** no local MySQL — a hosted MySQL service is used instead. The chosen provider is Aiven Free tier (MySQL 8.4.11, remote host/port, project `ca.pem`); two databases `eshopverse_dev` and `eshopverse_test` were created on it. Connections run over TLS 1.3 and are verified against the CA file, as `docs/SECURITY.md` requires. Credentials and `MYSQL_ATTR_SSL_CA` live only in the git-ignored `backend/.env`. **Superseded 2026-10-12:** the provider deleted that free service after inactivity; the documented local zip-based MySQL 8.4 workflow in `docs/DEVELOPMENT.md` is now the verified default, with a hosted MySQL 8+ TLS service remaining a documented drop-in replacement.
- **Backend test naming (2026-10-11):** PHP test methods use `snake_case` because Laravel Pint enforces `php_unit_method_casing`; the `docs/TESTING.md` PHP example was aligned with the enforced style.
- **Backend API mounting (2026-10-11):** `routes/api.php` exists and is registered with the `api/v1` prefix from `ARCHITECTURE.md`, but stays empty until the OpenAPI contract is accepted (FND-004). **Superseded:** FND-004 was accepted 2026-10-12 and the full endpoint set (auth, catalog, cart, checkout/orders, admin operations) now lives behind that prefix; `routes/api.php` is no longer empty.
- **Admin visual direction resolved (2026-10-17, owner decision):** the admin dashboard keeps the documented coral/neutral palette per ADR-002 and `admin/AGENTS.md`. The `docs/DESIGN_SYSTEM.md` MUSE palette introduced with the Android redesign conflicts with ADR-002 for the web dashboard and is logged as a finding in the admin Phase 3 QUAL-002 record for a future design task — no admin re-theme was performed.
- **Admin light/dark theme (2026-10-19, owner request):** UI-003 adds a persisted light/dark toggle to the existing admin coral/neutral palette. The OS preference is the default; an explicit choice is stored as the non-sensitive `eshop-admin-theme` value. The toggle is in the shell header and sign-in/standalone states, uses 44px targets and `aria-pressed`, and the pre-paint boot script prevents a light flash. `npm run check:contrast` now covers 34 pairs (17 light + 17 dark), all passing. ADR-002 was amended; Android dark mode remains deferred.

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

**Immediate deployment step (2026-10-19):** upload `deploy/eshopverse-admin-dist.zip` to `htdocs` on
ByetHost, extract it over `index.html`/`assets`/`fonts`, hard-refresh, and verify the theme toggle and failed-
sign-in session behavior. The backend and database are unchanged and already working. Full checklist:
[`docs/DEPLOYMENT.md`](../../docs/DEPLOYMENT.md).

1. **Backend component: nothing outstanding.** Backend Roadmap Phases 0–3 are complete and verified (final clean-checkout run 2026-10-15: 153 tests / 2062 assertions / 0 failures, pint clean, composer audit clean). Per AGENTS.md, backend agents run no Git commands — the owner manages commits/pushes/PRs. The canonical cross-component QUAL-001..QUAL-005 entries in `docs/ROADMAP.md` are marked `[~]` (backend and admin contributions delivered; the Android contributions — screenshots, accessibility pass, full multi-component rehearsal — are still pending); the backend portions are recorded in `docs/BACKEND_ROADMAP.md` Phase 3.
2. **Admin component: Phase 3 merged; UI-003 follow-up pending.** The Phase 3 admin contributions (QUAL-001–QUAL-005) were independently verified on 2026-10-17 at `6d3d7f3` by a different agent (all checks reproduced; one probe non-hermeticity note recorded) and merged into `develop` via [PR #47](https://github.com/Averon-Labs/eshop-verse/pull/47). The UI-003 light/dark theme is prepared on `feature/admin-phase-3` for a follow-up PR.
3. **CI consolidation, needs owner approval:** the workflow runs Android and backend jobs; the admin job, documented-check consolidation, and a verified workflow run belong to cross-component **FND-005** and, as CI/CD pipeline changes, require explicit human approval per AGENTS.md before any edit.
4. **Android component: roadmap complete.** Phases 0–3 are delivered in `docs/ANDROID_ROADMAP.md`; the project-level QUAL consolidation and final cross-component validation remain.
5. **Release-side:** once the admin and Android QUAL contributions are verified, close the Phase 3 gate and run the release checks per `docs/ROADMAP.md`.
