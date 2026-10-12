# Android Application — Agent Instructions

> Read the root `AGENTS.md` first. This file contains Android-specific rules.

## Strict Git Permissions for Android Tasks

Agents working on Android tasks are authorized **ONLY** to perform staging and commits (`git add`, `git commit`) on the active task branch provided by the owner.
- **NEVER** create branches (`git branch`, `git checkout -b`, `git switch -c`).
- **NEVER** switch or change branches (`git checkout`, `git switch`).
- **NEVER** push, pull, fetch, merge, reset, delete branches, or create PRs (`git push`, `git pull`, `git branch -d`, `gh pr`).
- Branch creation, branch switching, pushing, merging, PR creation, and branch deletion are **STRICTLY AND EXCLUSIVELY** owner-managed. Agents must never touch them without explicit permission.
- **Phase-based branches:** Android feature branches are maintained per phase (`feature/android-phase-<N>`), and all steps within that phase are developed and committed sequentially on that phase branch.

The canonical Android task steps, status, scope, dependencies, acceptance criteria, and verification are in [`docs/ANDROID_ROADMAP.md`](../docs/ANDROID_ROADMAP.md). Android assignments are executed and reported from that roadmap only; do not take on or list backend/admin work. Use [`docs/ROADMAP.md`](../docs/ROADMAP.md) only for explicitly cross-component work and final project/release validation. If an external task is a dependency, report it only as a blocker for the affected Android task.

---

## Architecture

| Aspect | Decision | Status |
|--------|----------|--------|
| Language | Java | DECIDED |
| UI | XML layouts with Material 3 | DECIDED |
| Architecture Pattern | MVVM (Model-View-ViewModel) | DECIDED |
| Data Layer | Repository Pattern | DECIDED |
| Local Database | Deferred for initial release (ADR-004) | DECIDED |
| Networking | REST API | DECIDED |
| HTTP Client | Retrofit | DECIDED |
| Image Loading | Glide | DECIDED |
| Dependency Injection | Constructor injection/manual wiring for the first release | DECIDED |
| Navigation | AndroidX Navigation | DECIDED |
| Namespace / application ID | `com.averonlabs.eshopverse` | DECIDED |
| Compile SDK | Android API 36, minor API level 1 | DECIDED |
| Target SDK | 36 | DECIDED |
| Minimum SDK | 25 | DECIDED |
| Java source/target | Java 11 | DECIDED |
| Android Gradle Plugin | 9.2.1 | DECIDED |
| Gradle wrapper | 9.4.1 | DECIDED |
| Material Components | see `gradle/libs.versions.toml` | OWNED BY FND-006; the merged dependency set for Phases 1–2 is declared there |
| Token storage | platform-protected storage only (Android Keystore AES-256-GCM, ADR-003) | DECIDED |
| Local persistence | none (Room deferred, ADR-004) | DECIDED |

---

## Project Structure

Follow the checked-in Android Gradle scaffold and namespace `com.averonlabs.eshopverse`. Organize new code by feature and layer without replacing the Kotlin DSL Gradle files.

Target package layout (created by the Android Phase 0 steps):

```
com.averonlabs.eshopverse
├── core/          managed by FND-006, DOM-002, DATA-002, NET-002
│   ├── money/     fixed-precision money and en-US formatting
│   ├── model/     contract-derived API models
│   ├── net/       API client, routes, interceptors, error mapping
│   ├── state/     UiState, Event, Paged
│   ├── storage/   token storage
│   └── ui/view/   the shared custom views owned by UI-002
├── shell/         managed by APP-002
├── auth/          AUTH-002
├── catalog/       CAT-002
├── cart/          CART-002
├── checkout/      CHECK-001
└── orders/        ORD-002
```

Resource ownership matters as much as code ownership. Every feature owns
`res/values/strings_<feature>.xml` and its own `fragment_*`, `item_*`, `dialog_*` layouts and its own
`navigation/nav_<feature>.xml`. Shell, manifest, Gradle, theme, colour, dimension, style, typography,
and app-level string files each have exactly one owning step. The full ownership table, the per-task
reading set, and the task-brief format are in
[`docs/android/TASK-BRIEF-TEMPLATE.md`](../docs/android/TASK-BRIEF-TEMPLATE.md) — read it before
starting any Android task.

---

## Conventions

### Naming

- **Activities:** `<Feature>Activity.java` (e.g., `LoginActivity.java`)
- **Fragments:** `<Feature>Fragment.java` (e.g., `ProductListFragment.java`)
- **ViewModels:** `<Feature>ViewModel.java` (e.g., `CartViewModel.java`)
- **Repositories:** `<Feature>Repository.java` (e.g., `ProductRepository.java`)
- **Layouts:** `activity_<feature>.xml`, `fragment_<feature>.xml`, `item_<feature>.xml`
- **Drawables:** `ic_<name>.xml` for icons, `bg_<name>.xml` for backgrounds

### Code Style

- Follow standard Java conventions.
- Use meaningful variable and method names.
- Keep methods short and focused.
- Document public APIs with Javadoc.

### Resources

- Use string resources for all user-visible text.
- Use dimension resources for spacing.
- Use color resources defined in the design system.
- Follow [`docs/DESIGN_SYSTEM.md`](../docs/DESIGN_SYSTEM.md) for every screen. Use the shared Material 3 theme, XML/View components, styles, and resource tokens; keep screen patterns visually consistent across the app.
- Follow the page-level flows and states in [`docs/SCREEN_SPECIFICATIONS.md`](../docs/SCREEN_SPECIFICATIONS.md); use the same specified palette, Lato type hierarchy, and reusable patterns throughout.
- Do not introduce one-off colors, spacing, typography, shapes, elevations, or custom component styles in individual screens. Propose changes centrally and update both the design-system guide and shared resources.
- Support content descriptions for accessibility.

### Non-negotiable rules

- **Java and XML only.** Do not add Kotlin sources or Kotlin-only artifacts (Kotlin extensions
  libraries) to the app module. AGP 9 provides built-in Kotlin support, but this project is Java/XML by
  decision.
- **No local design decisions.** A screen never introduces a colour, dimension, style, text appearance,
  elevation, or custom component. Missing tokens or styles are reported back to the owning step
  (UI-002); they are not worked around locally.
- **Strings.** New user-visible text goes in `res/values/strings_<feature>.xml`. Only the shell step
  edits `res/values/strings.xml`.
- **Money.** Use the shared fixed-precision money type and formatter. Never parse money into a `double`
  or `float`, and never perform money arithmetic in a screen, adapter, or layout binding.
- **Never log sensitive data.** No token, password, email address, or full request body may reach
  logcat, a crash report, or a `toString()`. Debug HTTP logging is basic-level with the authorization
  header redacted; body-level logging is never enabled.
- **Transport.** HTTPS in every non-development configuration. Cleartext is permitted only in the
  debug build and only for the documented local development hosts.
- **Shared files.** Do not edit a file owned by another step (shell, Gradle, manifest, shared tokens,
  or another feature's package). Report the need instead.
- **Git operations.** Agents are authorized ONLY to stage and create local commits (`git add`, `git commit`) on the active task branch. All other Git operations (`status`, `diff`, `branch`, `switch`, `push`, `pull`, `fetch`, `merge`, `reset`, or PR creation) remain strictly owner-managed.

---

## Boundaries

- Android code lives exclusively in `android/`.
- Communication with the backend is through the REST API only.
- API models should mirror the API contract in `docs/API_CONTRACT.md`.
- Local Room entities may differ from API models.
- Do not embed backend logic in the Android app.

---

## Testing

- Unit tests in `app/src/test/`
- Instrumented tests in `app/src/androidTest/`
- See `docs/TESTING.md` for conventions.
- Add tests for new behavior with the implementation.
- Keep all user-visible UI and errors in English string resources.
- Prefer pure-JVM tests. Do not add a framework that runs Android code in the JVM without an
  explicitly recorded toolchain decision.
- API-client tests replay contract-conformant fixtures through a mock web server; fixtures live in one
  shared directory described by [`docs/android/API-MOCK-STRATEGY.md`](../docs/android/API-MOCK-STRATEGY.md).
  A fixture that contradicts `docs/openapi.yaml` is a defect, not a shortcut.
- A test that only proves a screen renders is not coverage of the behavior the criteria describe.
