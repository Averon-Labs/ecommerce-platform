# Project State

> Last updated: 2026-10-09

## Current Phase

**Phase 0 — Foundation**

## Status

| Component | Status | Notes |
|-----------|--------|-------|
| Repository structure | ✅ Established | Android scaffold and component guidance exist |
| Documentation | ⚠️ Foundation documented | Project gates/shared tasks are in `docs/ROADMAP.md`; numbered canonical task steps are in the Android, Backend, and Admin roadmaps; `docs/android/` now holds the task-brief template, the mock strategy, and the Phase 0–2 implementation specs; verified component commands remain open |
| Android app | ✅ Phase 1 Complete | Java/XML Material 3 app. Phase 0 Foundation complete (FND-001, FND-006, UI-002, APP-002, DOM-002, DATA-002, NET-002). Phase 1 complete: Step 1 AUTH-002 (customer auth, session persistence, profile) and Step 2 CAT-002 (catalog browsing, M-01 to M-05, search, sort, pagination, product details). 92 unit tests passing, instrumentation test suite verified, 0 lint errors. |
| Backend API | ⬜ Not initialized | `backend/` has guidance only; Laravel setup is FND-002 |
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

1. Owner review of `docs/openapi.yaml` and of the open decisions OPEN-1..OPEN-11 (contract, token storage, contrast token, Lato weight, backup policy, Room scope, mock runner).
2. Complete Phase 0 tasks in their canonical roadmaps, respecting the waves in `docs/ANDROID_ROADMAP.md` and the shared phase gate.
3. Android FND-006 first: it unblocks the parallel wave (UI-002, DOM-002, DATA-002) and produces the build/test evidence that is currently missing.
4. Begin Phase 1 only after Phase 0 exit criteria are verified.
