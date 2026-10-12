# Android Task Brief — Standard Format

## Purpose

This document defines the **one brief** an agent receives for an Android task. It exists for two
reasons:

1. **Determinism** — every agent gets the same inputs, the same boundaries, and the same definition
   of done, so two agents on two branches produce compatible code.
2. **Token economy** — a brief names the *minimum* reading set for the task. Agents read the brief and
   the files it names, not the whole `docs/` tree. Reading less is not optional tidiness here; it is
   part of the requirement.

The canonical acceptance criteria always live in [`../ANDROID_ROADMAP.md`](../ANDROID_ROADMAP.md);
this template never restates them. The brief adds the implementation-level detail the criteria
deliberately leave out (files, endpoints, resources, tests).

Phase specifications: [`PHASE-0-FOUNDATION-SPEC.md`](PHASE-0-FOUNDATION-SPEC.md),
[`PHASE-1-SPEC.md`](PHASE-1-SPEC.md), [`PHASE-2-SPEC.md`](PHASE-2-SPEC.md).

---

## 1. Reading sets

Do not read beyond the set for your task type. If a brief's reading set is wrong for the work, report
it as a blocker instead of reading more.

| Task type | Required reading | Explicitly not required |
|-----------|------------------|-------------------------|
| Foundation (FND-006, UI-002, APP-002, NET-002, DOM-002, DATA-002) | `AGENTS.md`, `android/AGENTS.md`, this task's entry in `ANDROID_ROADMAP.md`, the matching section of `PHASE-0-FOUNDATION-SPEC.md`, `docs/DEVELOPMENT.md` | `docs/SCREEN_SPECIFICATIONS.md`, `docs/DESIGN_SYSTEM.md` (UI-002 is the one exception), `docs/DATABASE.md` |
| Screen/feature (AUTH-002, CAT-002, CART-002, CHECK-001, ORD-002) | `AGENTS.md`, `android/AGENTS.md`, this task's entry in `ANDROID_ROADMAP.md`, the matching section of `PHASE-N-SPEC.md`, `docs/openapi.yaml` **operations listed in the brief only**, the `M-xx` rows named in the brief | `docs/DESIGN_SYSTEM.md`, `docs/DATABASE.md`, `docs/ARCHITECTURE.md`, `docs/PRODUCT.md`, other phase specs |
| Test-only stage | `AGENTS.md`, `.agents/workflows/test.md`, the task entry, the phase spec section, `docs/TESTING.md` | everything else |

`AGENTS.md` (root) is always first and is never optional.

---

## 2. Brief block

Paste this block into the assignment. Keep every field; write `none` rather than deleting a field.

```text
TASK: <TASK-ID> — <title>
BRANCH: <type>/android-phase-<N>
BASE: develop @ <sha>
READ: <reading set per section 1>
DEPENDS ON: <task ids, each with current status>
SCOPE: <one sentence>

CONTRACT (operations this task consumes)
- <METHOD /path> — <why>
- ...

SCREENS
- M-xx <name> — <what the screen must do>
- ...

FILES I OWN (create or edit; this is the whitelist)
- <path> — <intent>
- ...

FILES I MUST NOT TOUCH
- <path> — owned by <TASK-ID>
- ...

RESOURCES TO ADD
- strings: strings_<feature>.xml keys — <list>
- layouts: <names>
- drawable: <names>
- no new colors, dimens, or shared styles (report a real gap instead)

TESTS
- unit: <class> — <scenarios>
- androidTest: <class> — <flow>
- fixtures: <fixture names from the mock strategy>

DONE WHEN
- <exact commands> all pass
- every acceptance criterion <a..n> in the roadmap entry has evidence
- commit created on BRANCH with message `<type>(android-pNN-sNN-<task-id>): <summary>`
- not pushed

QUESTIONS BEFORE STARTING: <or "none — proceed">
```

---

## 3. File ownership

Parallel work only survives if two agents never need the same file. The following files are
**shared** and each has exactly one owning task. A feature task that believes it needs one must stop
and ask the coordinator instead of editing it.

| Shared file | Owner | Everyone else |
|-------------|-------|---------------|
| `AndroidManifest.xml` | APP-002 | never edit; report the needed permission/component |
| `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties` | FND-006 | never add a dependency; report the need |
| `MainActivity.java`, `activity_main.xml`, `content_main.xml` | APP-002 | never edit |
| `navigation/nav_main.xml` | APP-002 | never edit; add destinations inside your own nested graph |
| `values/colors.xml`, `values/themes.xml`, `values/type.xml`, `values/styles.xml`, `values/dimens.xml` | UI-002 | never edit; report a missing style/token |
| `values/strings.xml` (app-level) | APP-002 | use your own `values/strings_<feature>.xml` |
| `values-night/**`, `values-v23/**` | UI-002 (deletes or owns) | never create new qualified theme files |
| `xml/network_security_config.xml`, `xml/backup_rules.xml`, `xml/data_extraction_rules.xml` | APP-002 for the manifest links, DATA-002 for the backup content | never edit |
| `res/drawable/ic_launcher_*`, `res/mipmap-*` | FND-006 | never edit |

Rules:

- **Feature-owned resource files.** Every feature owns `values/strings_<feature>.xml` and its own
  `fragment_*` / `item_*` / `dialog_*` layouts, its own `navigation/nav_<feature>.xml`, its own
  repository, ViewModel, adapter, and test classes under `com.averonlabs.eshopverse.<feature>`.
- **No new colors or dimensions outside UI-002.** Design tokens are centralized; a screen task that
  needs a token does not have one, it has a bug report for UI-002.
- **Use the shared tokens for everything every screen already has.** Screen gutters use
  `@dimen/screen_horizontal_margin`; corner radii use the `corner_*` tokens; card elevation uses the
  shared elevation token; text uses a `TextAppearance.EShopVerse.*` style. A literal `dp` value, a
  literal `"#RRGGBB"`, or an unqualified `app:cardElevation` in a screen layout is a defect, not a
  shortcut. (The scaffold's sample card in `fragment_home.xml` violates this and is replaced by
  CAT-002; do not copy its pattern.)
- **One writer per file at a time.** If a brief's whitelist overlaps a file already under
  construction on another branch, the coordinator serializes the two tasks.

---

## 4. Package layout

Feature-first, layered inside the feature. This is the target shape for all feature tasks.

```text
com.averonlabs.eshopverse
├── core/                     # FND-006, NET-002, DOM-002, DATA-002 own these
│   ├── money/                # Money, MoneyFormat
│   ├── net/                  # ApiClient, ApiRoutes, AuthInterceptor, mapping
│   ├── model/                # shared API models (Product, Category, Order, ...)
│   ├── state/                # UiState, Event, Paged
│   └── storage/              # SecureTokenStore
├── shell/                    # APP-002: MainActivity, shell fragments, bottom nav
├── auth/                     # AUTH-002: screens, AuthViewModel, AuthRepository
├── catalog/                  # CAT-002
├── cart/                     # CART-002
├── checkout/                 # CHECK-001
└── orders/                   # ORD-002
```

Shared API models live in `core/model` and are created by the task that first needs them. A later task
that needs an extra field on an existing model adds the field in its own commit and lists the change
in its brief; it does not fork a second model class for the same payload.

---

## 5. Definition of done for an Android task

1. Every acceptance criterion `a..n` of the roadmap entry is met and has evidence (command output,
   test name, or screenshot path).
2. The documented Gradle commands pass: `clean assembleDebug`, `lint`, `testDebugUnitTest`, and
   `connectedDebugAndroidTest` where the brief requires a device run.
3. New behavior has tests; existing tests still pass; nothing is skipped, disabled, or weakened.
4. No new colour, dimension, style, or string literal outside the design system and string resources.
5. No secret, token, base URL credential, or `Authorization` header value appears in a log, a test
   fixture, or a committed file.
6. One focused commit on the task branch using the Conventional Commit scope
   `(<component>-pNN-sNN-<task-id>)`. Not pushed.
7. The brief's "FILES I MUST NOT TOUCH" list is still untouched (`git status --short` proves it).

---

## 6. Escalation

Stop and ask the coordinator — do not improvise — when:

- the brief and `openapi.yaml` disagree, or the contract lacks an operation the screen needs;
- a criterion requires a shared file or a new dependency;
- a design token, style, or component is missing;
- the mock fixture and the contract disagree;
- the task cannot be verified because the toolchain is unavailable (see the environment section of
  `PHASE-0-FOUNDATION-SPEC.md`); report the exact missing prerequisite rather than claiming a pass.
