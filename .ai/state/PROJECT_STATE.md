# Project State

> Last updated: 2026-10-11

## Current Phase

**Phase 0 — Foundation**

## Status

| Component | Status | Notes |
|-----------|--------|-------|
| Repository structure | ✅ Established | Android scaffold and component guidance exist |
| Documentation | ⚠️ Foundation documented | Project gates/shared tasks are in `docs/ROADMAP.md`; numbered canonical task steps are in the Android, Backend, and Admin roadmaps; `docs/android/` now holds the task-brief template, the mock strategy, and the Phase 0–2 implementation specs; verified component commands remain open |
| Android app | 🟡 Phase 2 In Progress | Java/XML Material 3 app. Phase 0 Foundation complete (FND-001, FND-006, UI-002, APP-002, DOM-002, DATA-002, NET-002). Phase 1 complete (AUTH-002, CAT-002). Phase 2 Step 1 complete (CART-002). Phase 2 Step 2 complete (CHECK-001 — checkout and simulated payment flow, Screens M-11..M-14, zero credential capture, idempotent order placement, safe retry). 136 unit tests passing, instrumentation test suite verified, 0 lint errors. |
| Backend API | 🟡 Initialized — FND-002 implemented, awaiting independent verification | Laravel 13 (`laravel/framework` 13.35.0) on PHP 8.4.25, Composer 2.10.3, PHPUnit 12.5.38, Pint 1.32.1 (PSR-12). Database is a **hosted** MySQL 8.4.11 (Aiven Free tier) with `eshopverse_dev` + `eshopverse_test`, reached over TLS 1.3 with certificate verification (`MYSQL_ATTR_SSL_CA`). Verified 2026-10-11: `composer install`, `composer validate --strict`, `composer audit`, `pint --test`, `migrate`, `test` (5 tests / 8 assertions), `serve` (`/up` 200). `routes/api.php` is empty and mounted at `/api/v1`; no business endpoint exists before FND-004 acceptance. |
| Admin dashboard | ⬜ Not initialized | `admin/` has guidance only; React/TypeScript setup is FND-003 |
| API contract | ⚠️ Draft awaiting review | `docs/openapi.yaml` exists as an OpenAPI 3.1 draft resolving the recorded open contract decisions, including category lifecycle; it passes a structural check (no duplicate keys, no unresolved `$ref`) but API-owner acceptance is still required before endpoint or client work (FND-004) |
| CI/CD | ⚠️ Placeholder | `.github/workflows/ci.yml` only validates repository structure; Android FND-006 adds the Android build/lint/test job and FND-005 owns component-check consolidation |
| Database | ⬜ Not implemented | Logical model documented; migrations and isolated test database are part of backend/component Tasks |

## Active Branches

| Branch | Purpose | Owner |
|--------|---------|-------|
| main | Pre-policy baseline; future release target | — |
| develop | Shared integration branch; created from current `main` | — |
| docs/project-p00-s09-plan-005-android-execution-structure | Task branch for PLAN-005 | — |

## Recent Decisions

- **Backend database (2026-10-11, owner decision):** no local MySQL — a hosted MySQL service is used instead. The chosen provider is Aiven Free tier (MySQL 8.4.11, remote host/port, project `ca.pem`); two databases `eshopverse_dev` and `eshopverse_test` were created on it. Connections run over TLS 1.3 and are verified against the CA file, as `docs/SECURITY.md` requires. Credentials and `MYSQL_ATTR_SSL_CA` live only in the git-ignored `backend/.env`.
- **Backend test naming (2026-10-11):** PHP test methods use `snake_case` because Laravel Pint enforces `php_unit_method_casing`; the `docs/TESTING.md` PHP example was aligned with the enforced style.
- **Backend API mounting (2026-10-11):** `routes/api.php` exists and is registered with the `api/v1` prefix from `ARCHITECTURE.md`, but stays empty until the OpenAPI contract is accepted (FND-004).

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

1. Independent verification of Backend **FND-002** (roadmap status is `[~]`): tester repeats the commands in `docs/DEVELOPMENT.md`, then the owner commits `backend/` (`composer.lock`, `.env.example`, config, tests) — backend agents run no Git commands.
2. Owner review of `docs/openapi.yaml` and of the open decisions OPEN-1..OPEN-11 (contract, token storage, contrast token, Lato weight, backup policy, Room scope, mock runner). This unblocks Backend FND-004 acceptance and therefore every Phase 1 endpoint task.
3. Complete Phase 0 tasks in their canonical roadmaps, respecting the waves in `docs/ANDROID_ROADMAP.md` and the shared phase gate.
4. Android FND-006 first: it unblocks the parallel wave (UI-002, DOM-002, DATA-002) and produces the build/test evidence that is currently missing.
5. Begin Phase 1 only after Phase 0 exit criteria are verified.
