# Android Roadmap

## Purpose and authority

Plan Android customer-app work in Java/XML using the documented MVVM, Repository, Material 3, and API boundaries.

This file is the sole execution roadmap for Android work. Every Android task, its status, scope, dependencies, acceptance criteria, and verification belongs here. When assigned an Android phase, step, or Task ID, work from this file and Android-specific guidance; do not take on or report backend/admin tasks. [ROADMAP.md](ROADMAP.md) is reserved for cross-component tasks and final project/release validation, not as an additional Android task list.

A roadmap assignment uses this roadmap name, phase, and step, for example: Android Roadmap, Phase 1, Step 1; a stable Task ID may also identify the work. Step numbers restart at 1 in every phase. Lowercase letter labels restart at `a` in each acceptance-criteria block and identify individual criteria for discussion or verification, not separate assignments. Check only the assigned Android task's dependencies before starting. If an external backend/admin/shared dependency is incomplete, report that dependency as a blocker for the Android task and do not adopt or report the external task as Android work. The project roadmap is used for final cross-component/release validation. Follow [repository-wide agent instructions](../AGENTS.md) for implementation workflow and Git conventions.

### How an Android task is executed

1. Read the assignment brief, which follows [`android/TASK-BRIEF-TEMPLATE.md`](android/TASK-BRIEF-TEMPLATE.md).
2. Read **only** the reading set that brief names. The template defines it per task type; reading the whole `docs/` tree is not required and is not wanted.
3. The acceptance criteria below are the definition of truth. The linked phase specification supplies the implementation detail (files, operations, validation, tests) and never restates these criteria.
4. Do not edit a file owned by another task. The ownership table is in the brief template.

| Phase | Implementation spec |
|-------|---------------------|
| Phase 0 | [`android/PHASE-0-FOUNDATION-SPEC.md`](android/PHASE-0-FOUNDATION-SPEC.md) |
| Phase 1 | [`android/PHASE-1-SPEC.md`](android/PHASE-1-SPEC.md) |
| Phase 2 | [`android/PHASE-2-SPEC.md`](android/PHASE-2-SPEC.md) |
| All phases | [`android/API-MOCK-STRATEGY.md`](android/API-MOCK-STRATEGY.md) |

### Contract dependency

Android tasks consume operations defined in [`openapi.yaml`](openapi.yaml). That contract is drafted under Backend FND-004 and must be **reviewed and accepted before any Android Phase 1 or Phase 2 client integration starts**. Because the Android client is verified against contract-conformant fixtures and a local mock server, a task does not wait for the corresponding backend implementation; the real API is exercised by the cross-component regression task QUAL-001.

## Phase steps

## Phase 0 — Foundation

**Cross-component release check:** see [Phase 0 in the Project Roadmap](ROADMAP.md) during final project validation. This is not an additional Android task assignment.

Phase 0 supplies every cross-cutting layer the screen tasks depend on, so Phase 1 and Phase 2 tasks are additive and parallel-safe. Waves are sequential; tasks inside a wave may run concurrently.

| Wave | Steps | Parallel-safe because |
|------|-------|-----------------------|
| 1 | 2 | Owns the only Gradle and CI files |
| 2 | 3, 5, 6 | Disjoint resource and `core/` packages |
| 3 | 4, 7 | Disjoint shell and network packages |

### Step 1 — FND-001 — Verify Android scaffold and toolchain

**Status:** `[x]`

**Depends on:** none

**Scope:** inspect the regenerated Android Gradle project, reconcile its actual configuration with Android documentation, and keep its project layout and repository guidance aligned.

**Acceptance criteria:**

- [x] **a.** Record the supported JDK, Android SDK/build-tools requirements, Gradle/Android Gradle Plugin versions, Java language level, namespace, and min/target SDK from the actual project; resolve the documented minSdk disagreement.
- [x] **b.** Track the regenerated Android Studio project under `android/` without IDE state, local SDK paths, or generated build/cache files; include the Gradle Wrapper JAR needed by a clean checkout and preserve the rest of the repository.
- [x] **c.** Ensure every Android XML screen is required to use the shared Material 3 theme, tokens, components, and reusable visual/state patterns documented in `docs/DESIGN_SYSTEM.md`.
- [x] **d.** Remove Android Studio template-only sample content in the first implementation task before treating the scaffold as an application foundation; keep verification status open until build, lint, and test checks are run.
- [x] **e.** Verify a clean setup, build, lint, JVM test, and any configured instrumentation-test commands; record exact commands and prerequisites in `docs/DEVELOPMENT.md`.
- [x] **f.** Confirm existing scaffold tests pass; if no test sources exist, record their absence and the Gradle `NO-SOURCE` result. Do not treat placeholder tests as feature coverage. No JVM or instrumentation test source files currently exist; Gradle `test` completed with `NO-SOURCE`.
- [x] **g.** Keep the app Java/XML and avoid changing product behavior in this foundation task.

**Verification:** fresh-environment Android build/lint/tests; compare the documented commands with the actual Gradle tasks and CI environment.

**Historical verification record:** `clean assembleDebug`, `lint`, and `test` were reported as passing when this step was completed (`test` with `NO-SOURCE`). Template placeholder content was removed and replaced with a clean `HomeFragment` host and Material 3 design tokens. `android/gradle/wrapper/gradle-wrapper.jar` is tracked.

**Correction (2026-10-09, recorded during PLAN-005):** that evidence is **not reproducible in the current checkout**, which has only JDK 8, no `ANDROID_HOME`, and no Android SDK; AGP 9.2 requires JDK 17. Criterion `e` and `f` therefore rest on an unverifiable run, and no test has ever actually executed in this repository. This step stays `[x]` for the configuration reconciliation it performed; independent re-verification is carried by FND-006, which is designed to produce a real transcript.

### Step 2 — FND-006 — Toolchain, dependency, and test-harness modernization

**Status:** `[x]`

**Depends on:** FND-001

**Prerequisites to start:**
- FND-001 verification complete (configuration reconciliation done)
- JDK 17+ available locally
- Android SDK Platform 36.1 installed
- Build Tools 36.0.0 installed

**Spec:** [`android/PHASE-0-FOUNDATION-SPEC.md`](android/PHASE-0-FOUNDATION-SPEC.md) §B and §D

**Scope:** bring the Android build, dependency set, test harness, and CI job up to the toolchain the project actually uses, and produce the missing build/test evidence.

**Acceptance criteria:**

- [x] **a.** Local prerequisites are available (JDK 25, Android SDK Platform 36.1, Build Tools 36.0.0); the Gradle wrapper runs and the verified command transcript is in `docs/DEVELOPMENT.md`.
- [x] **b.** Dependencies are declared in `gradle/libs.versions.toml` at stable versions compatible with the chosen Compile SDK 36.1; the selected versions are recorded in `docs/DEVELOPMENT.md`. No Kotlin-only artifact replaces a Java API.
- [x] **c.** The declared dependency set covers planned Phase 1 and 2 work; no dependency additions are currently specified for those phases.
- [x] **d.** JVM tests execute successfully and exercise JUnit, Mockito, and the fixture loader.
- [x] **e.** The obsolete `values-v23/themes.xml` is absent and the §D.2 build configuration is applied.
- [x] **f.** `.github/workflows/ci.yml` has an Android job running the clean build, lint, and JVM unit tests on JDK 17; the repository-validation job remains enabled. A hosted CI run has not yet been observed.
- [x] **g.** `android/gradlew` is executable in the Git index (`100755`); `.gitattributes` defines LF line endings and the clean wrapper task succeeded locally.
- [x] **h.** `docs/DEVELOPMENT.md` records prerequisites, commands, current verification, and limitations; the obsolete “unconfirmed” statement has been replaced with current evidence.

**Verification:** `clean assembleDebug lintDebug testDebugUnitTest` completed locally (see `docs/DEVELOPMENT.md`). Git index check returned mode `100755` for `android/gradlew`. `connectedDebugAndroidTest` was not run because it requires a connected device. CI configuration is present but no hosted run was observed.

### Step 3 — UI-002 — Android design token and component style layer

**Status:** `[x]`

**Depends on:** FND-006

**Prerequisites to start:**
- FND-006 complete (dependency set and test harness ready)
- `docs/DESIGN_SYSTEM.md` finalized and reviewed
- Lato font files and licence are bundled locally
- Supporting-text and status-container contrast values are accepted and measured
- Lato weight for "600" is Semibold (600)

**Spec:** [`android/PHASE-0-FOUNDATION-SPEC.md`](android/PHASE-0-FOUNDATION-SPEC.md) §E

**Scope:** materialize `docs/DESIGN_SYSTEM.md` into Android resources — the complete Material 3 colour role set, Lato typography at the required weights, spacing/shape/status tokens, component styles, and the four shared custom views — and close the theme and contrast defects.

**Acceptance criteria:**

- [x] **a.** Every Material 3 colour role, status semantic, spacing, shape, and typography value the first-release screens use exists as a resource token; no screen task needs to invent a colour, dimension, style, or text appearance.
- [x] **b.** The supporting-text contrast failure is fixed and the value is updated in `docs/DESIGN_SYSTEM.md` and `values/colors.xml` together, with a measured contrast table covering every foreground/background pair the app uses.
- [x] **c.** Lato is bundled locally with its licence notice at the weights the typography table names; the weight used for "600" is decided and recorded, and each weight is selected without relying on `res/font` family parsing on API 25.
- [x] **d.** Component styles and the shared `QuantityStepperView`, `PriceTextView`, `StateContainerView`, and `StatusBadgeView` exist and are the only custom views the first release adds.
- [x] **e.** The night-mode/status-bar defect is closed and the light baseline is explicit rather than incidental; the dark-mode deferral is preserved.
- [x] **f.** Tests cover the shared views' logic and rendering, and `lint` is clean.

**Verification:** unit tests (`QuantityRangeTest`, `SharedViewEnumsTest`) and instrumentation test sources (`SharedViewsTest` verified via `assembleDebugAndroidTest`) executed/compiled cleanly; `lintDebug` and `testDebugUnitTest` passed with zero errors; measured contrast table added to `docs/DESIGN_SYSTEM.md`; unused template colors and `values-night` folder cleaned up.

### Step 4 — APP-002 — App shell and navigation

**Status:** `[x]`

**Depends on:** FND-006, UI-002

**Prerequisites to start:**
- FND-006 complete (dependencies ready)
- UI-002 complete (Material 3 theme, tokens, and shared views available)
- Design system tokens and component styles exist

**Spec:** [`android/PHASE-0-FOUNDATION-SPEC.md`](android/PHASE-0-FOUNDATION-SPEC.md) §F

**Scope:** own the shared shell — manifest and permissions, single activity, top app bar, four-destination bottom navigation, the navigation interface (graph ids and arguments), shell host fragments, and the debug/release cleartext policy — and close the insets defect.

**Acceptance criteria:**

- [x] **a.** The app boots into a working shell with Home, Explore, Cart, and Account reachable, and the cart badge API exists and stays hidden until a cart count is supplied.
- [x] **b.** The manifest declares the networking permissions the app needs, and the release configuration permits no cleartext while the debug configuration permits it only for the local development hosts.
- [x] **c.** System-bar and keyboard insets are applied once, to the correct views, and nothing is clipped on gesture navigation at 360dp.
- [x] **d.** The shell defines fixed nested-graph ids and navigation arguments so no feature task edits `nav_main.xml` or `MainActivity`, and the bottom navigation hides on focused steps through one shared rule.
- [x] **e.** The shell contains no business logic and adds no product behavior beyond navigation.
- [x] **f.** A focused UI test proves the app boots, all four destinations are reachable, and back navigation behaves.

**Verification:** unit tests (`ShellContractVerificationTest`) and instrumentation test sources (`ShellInstrumentationTest` verified via `assembleDebugAndroidTest`) executed/compiled cleanly; `lintDebug` and `testDebugUnitTest` passed with zero errors; edge-to-edge insets applied cleanly (top to `AppBarLayout`, bottom to `BottomNavigationView`, left/right to root `CoordinatorLayout`); debug cleartext config scoped to localhost and `10.0.2.2`; release cleartext prohibited.

### Step 5 — DOM-002 — Money, API models, and shared UI state

**Status:** `[x]`

**Depends on:** FND-006, FND-004

**Prerequisites to start:**
- FND-006 complete (dependency set ready)
- **FND-004 complete** (Backend: `docs/openapi.yaml` reviewed and accepted)
- All contract decisions closed (OPEN-1 through OPEN-4)
- API schemas defined for all Phase 1-2 models

**Spec:** [`android/PHASE-0-FOUNDATION-SPEC.md`](android/PHASE-0-FOUNDATION-SPEC.md) §G

**Scope:** one implementation of fixed-precision money, en-US formatting, the complete Phase 1–2 API model set derived from the contract, the shared UI state/event primitives, and error-code messaging.

**Acceptance criteria:**

- [x] **a.** Money is a fixed-precision decimal type parsed from strings and formatted for en-US USD; no code path converts money to or from a binary floating-point value, and a test proves it.
- [x] **b.** All Phase 1 and Phase 2 API models and enums exist, match the contract schema names, and are documented as contract-derived; no screen task needs to fork a model.
- [x] **c.** Every documented error code maps to a user-visible English message with a generic fallback, and a test proves the mapping is total.
- [x] **d.** Shared loading/content/empty/error state and one-shot event primitives exist for the ViewModels, and no screen re-implements them.
- [x] **e.** Tests avoid Android framework dependencies where practical.

**Verification:** unit tests for money boundaries, formatting, reflection check ensuring no float/double exposure (`MoneyTest`), model JSON parsing (`ApiModelParsingTest`), total error-code coverage (`ErrorMessagesTest`), and state/event primitives (`UiStateAndEventTest`); `lintDebug` and `testDebugUnitTest` passed with zero errors.

### Step 6 — DATA-002 — Secure token storage and local persistence decision

**Status:** `[x]`

**Depends on:** FND-006

**Prerequisites to start:**
- FND-006 complete (dependency set ready)
- Token storage mechanism decided (decision OPEN-6)
- Backup policy decided (decision OPEN-9)
- Room persistence strategy decided (decision OPEN-10)

**Spec:** [`android/PHASE-0-FOUNDATION-SPEC.md`](android/PHASE-0-FOUNDATION-SPEC.md) §H

**Scope:** platform-protected token storage with backup exclusion, plus an explicit, documented decision on local persistence instead of the current contradiction in `docs/ARCHITECTURE.md`.

**Acceptance criteria:**

- [x] **a.** A token store exists that uses platform-protected storage only, exposes save/read/clear/expiry, and never writes the token to a log, crash report, or `toString()`.
- [x] **b.** The store is excluded from cloud backup and device-to-device transfer, and the backup attributes or rules used are recorded as evidence.
- [x] **c.** The storage mechanism decision is recorded as an ADR, including why the alternative was rejected.
- [x] **d.** The Room/local-persistence contradiction is resolved: either Room is deferred for the first release and `docs/ARCHITECTURE.md` and `README.md` are corrected, or Room is scoped into a named task with a stated offline requirement.
- [x] **e.** Tests cover expiry, clear-on-logout, and redaction; a device test proves the store works on both the minimum and current API levels.

**Verification:** unit tests (`TokenStoreTest`), instrumentation device test (`KeystoreTokenStoreTest`), backup rules exclusions in `data_extraction_rules.xml` and `backup_rules.xml` with `allowBackup="false"`, ADR-003 and ADR-004 accepted, `testDebugUnitTest`, `assembleDebugAndroidTest`, and `lintDebug` passed with zero errors.

### Step 7 — NET-002 — Network and API client core

**Status:** `[x]`

**Depends on:** FND-006, FND-004, DOM-002, DATA-002

**Prerequisites to start:**
- FND-006 complete (Retrofit, OkHttp, Gson dependencies ready)
- **FND-004 complete** (Backend: OpenAPI contract accepted)
- DOM-002 complete (API models, error codes, Money class exist)
- DATA-002 complete (token storage ready)
- Mock server runner decided (decision OPEN-5)
- Fixture directory structure planned

**Spec:** [`android/PHASE-0-FOUNDATION-SPEC.md`](android/PHASE-0-FOUNDATION-SPEC.md) §I and [`android/API-MOCK-STRATEGY.md`](android/API-MOCK-STRATEGY.md)

**Scope:** one API client for the whole app — base URL per build type, retrofit routes for the Phase 1–2 operations, bearer-token interceptor, idempotency-key interceptor, error-envelope mapping, the single 401 hook, and fixture-driven tests.

**Acceptance criteria:**

- [x] **a.** The client is built from a build-time base URL; debug and release origins differ, and switching to the real backend is a base-URL change with no production-code edit.
- [x] **b.** The bearer token is attached from protected storage, is never logged, and debug logging redacts it; the client never logs request bodies.
- [x] **c.** Mutations carry a stable idempotency key per logical attempt, and no mutation is retried automatically.
- [x] **d.** Every documented error envelope is mapped to a typed error carrying the stable code and field errors; a `401` invokes one shared session hook.
- [x] **e.** Pagination and collection envelopes are modelled once and reused.
- [x] **f.** MockWebServer tests using the shared fixtures prove success and failure parsing, the auth header, the no-retry rule, the 401 hook, and idempotency-key behaviour.
- [x] **g.** The local mock server described in the mock strategy runs from the shared fixture directory and honours idempotency, so Phases 1 and 2 can be verified before the backend exists.

**Verification:** 8 unit tests in `ApiClientTest` passing with MockWebServer, 10 shared fixture files in `fixtures/api/`, Python 3 local mock server in `tools/mock-api/server.py` (ADR-005), build-time base URL (`BuildConfig.API_BASE_URL` defaulting to `http://10.0.2.2:8081/api/v1` for debug and `https://api.eshopverse.example.com/api/v1` for release), `testDebugUnitTest` (50 passed), `assembleDebugAndroidTest`, and `lintDebug` passed with zero errors. Phase 0 complete!

## Phase 1 — Accounts and Catalog

**Cross-component release check:** see [Phase 1 in the Project Roadmap](ROADMAP.md) during final project validation. This is not an additional Android task assignment.

**Entry criteria:** Phase 0 complete; `openapi.yaml` reviewed and accepted; the mock server or a real API reachable at the build's base URL.

Both steps in this phase may run concurrently: they own disjoint packages, graphs, and string files.

### Step 1 — AUTH-002 — Android customer authentication and profile

**Status:** `[x]`

**Depends on:** FND-001, FND-006, UI-002, APP-002, DOM-002, DATA-002, NET-002, DES-001

**Contract dependency:** FND-004 (accepted `openapi.yaml`). **Runtime integration:** AUTH-001 — exercised by QUAL-001, not blocking this task.

**Prerequisites to start:**
- All Phase 0 steps complete (FND-006, UI-002, APP-002, DOM-002, DATA-002, NET-002)
- **FND-004 complete** (Backend: OpenAPI contract accepted)
- **DES-001 complete** (Project: Screen specifications with M-06, M-07, M-08, M-09, M-17 finalized)
- Shell navigation and auth graph defined in APP-002
- Design tokens, Material 3 components ready from UI-002
- Token storage API ready from DATA-002
- API client with auth endpoints ready from NET-002
- Mock server or real API available for testing

**Screens:** M-06, M-07, M-08, M-09, M-17

**Spec:** [`android/PHASE-1-SPEC.md`](android/PHASE-1-SPEC.md) §D

**Scope:** English registration, sign-in, sign-out, profile, and password-reset screens with API integration and protected token persistence.

**Acceptance criteria:**

- [x] **a.** Forms show field validation, loading, server errors, network errors, and success states in English, and map every documented error code to the specified behaviour rather than a generic message.
- [x] **b.** Tokens use platform-protected storage, are never logged, and are cleared on logout or on a rejected/expired session; a failed sign-out still ends the local session.
- [x] **c.** Repository/ViewModel behavior is testable without Android framework dependencies where practical; critical UI flow has a focused UI test.
- [x] **d.** UI uses XML/Material 3, existing design-system styles, accessibility labels, and correct keyboard/input types; the task adds no new colour, dimension, or style.
- [x] **e.** Auth-required navigation returns the customer to the originating destination after a successful sign-in, and no guest cart or guest checkout path is created.

**Verification:** Android unit tests for state, storage, and error mapping; fixture-driven API-client tests; a focused instrumentation test for the critical auth path; a logcat excerpt showing no credential or token leakage.

### Step 2 — CAT-002 — Android catalog browsing

**Status:** `[ ]`

**Depends on:** FND-001, FND-006, UI-002, APP-002, DOM-002, NET-002, DES-001

**Contract dependency:** FND-004 (accepted `openapi.yaml`). **Runtime integration:** CAT-001 — exercised by QUAL-001, not blocking this task.

**Prerequisites to start:**
- All Phase 0 steps complete (FND-006, UI-002, APP-002, DOM-002, NET-002)
- **FND-004 complete** (Backend: OpenAPI contract accepted)
- **DES-001 complete** (Project: Screen specifications with M-01, M-02, M-03, M-04, M-05 finalized)
- Shell navigation ready (Home/Explore fragments) from APP-002
- Design tokens, product card styles, image loading ready from UI-002
- Money formatting, Product/Category models ready from DOM-002
- API client with catalog endpoints ready from NET-002
- Mock server or real API available for testing

**Screens:** M-01, M-02, M-03, M-04, M-05

**Spec:** [`android/PHASE-1-SPEC.md`](android/PHASE-1-SPEC.md) §E

**Scope:** home and category browsing, product list and detail, search, sort, pagination, and API-backed image display.

**Acceptance criteria:**

- [ ] **a.** A customer can browse products, open details, search, sort, and navigate categories using the reviewed API contract, and filter changes reset pagination correctly.
- [ ] **b.** Loading, empty, image-failure, offline/server-error, and retry states are handled; a failed image never breaks the surrounding layout.
- [ ] **c.** USD amounts are formatted consistently without binary floating-point calculations; all user-facing copy is English.
- [ ] **d.** The screen set contains no ratings, favourites or heart actions, discount stickers, product variants, or promotional hero content, matching the exclusion rules in the screen specification.
- [ ] **e.** ViewModel/repository mapping has unit tests and the browse-to-detail path has a focused UI test.

**Verification:** unit and API-mapping tests, a focused UI smoke test against deterministic fixtures, and screenshots of the list, detail, empty, and error states.

## Phase 2 — Cart, Checkout, and Orders

**Cross-component release check:** see [Phase 2 in the Project Roadmap](ROADMAP.md) during final project validation. This is not an additional Android task assignment.

**Entry criteria:** Phase 1 complete; the cart and order state machine in the accepted contract understood and matched.

All three steps in this phase may run concurrently: they own disjoint packages, graphs, and string files, and they navigate only to the graph ids and arguments frozen by APP-002. Fixture-driven verification is what removes the earlier task-to-task dependency.

### Step 1 — CART-002 — Android cart experience

**Status:** `[ ]`

**Depends on:** FND-001, FND-006, UI-002, APP-002, DOM-002, NET-002, AUTH-002, DES-001

**Contract dependency:** FND-004. **Runtime integration:** CART-001 — exercised by QUAL-001, not blocking this task.

**Prerequisites to start:**
- All Phase 0 and Phase 1 foundation complete
- AUTH-002 complete (authentication screens, token management working)
- **FND-004 complete** (Backend: OpenAPI cart endpoints defined)
- **DES-001 complete** (Project: Screen M-10 finalized)
- Cart navigation graph defined in APP-002
- QuantityStepperView ready from UI-002
- Cart models, Money formatting ready from DOM-002
- API client with cart endpoints ready from NET-002
- Shell cart badge API available from APP-002

**Screen:** M-10

**Spec:** [`android/PHASE-2-SPEC.md`](android/PHASE-2-SPEC.md) §D

**Scope:** display and edit the signed-in customer's cart, render server-confirmed totals, and recover from stock and ownership conflicts.

**Acceptance criteria:**

- [ ] **a.** The customer can inspect items, update quantity, remove items, and see server-confirmed totals; no screen computes a subtotal, shipping amount, or total.
- [ ] **b.** Empty, loading, offline, error, and stock-changed states are handled, and a retry does not silently duplicate a mutation; a line whose price changed or became unavailable is explained rather than silently changed.
- [ ] **c.** Cart state and money presentation are covered by unit tests, and the critical add/update/remove actions have focused UI coverage.
- [ ] **d.** The cart count supplied to the shell badge comes from the server response, not from local state.

**Verification:** unit and API-mapping tests, cart flow UI tests, and screenshots of the populated, conflict, and empty states.

### Step 2 — CHECK-001 — Android checkout and simulated payment flow

**Status:** `[ ]`

**Depends on:** FND-001, FND-006, UI-002, APP-002, DOM-002, NET-002, AUTH-002, DES-001

**Contract dependency:** FND-004. **Runtime integration:** ORD-001 — exercised by QUAL-001, not blocking this task.

**Prerequisites to start:**
- All Phase 0 and Phase 1 foundation complete
- AUTH-002 complete (authentication working, user context available)
- **FND-004 complete** (Backend: OpenAPI order/checkout endpoints defined)
- **DES-001 complete** (Project: Screens M-11, M-12, M-13, M-14 finalized)
- Checkout navigation graph defined in APP-002
- Form styles, StateContainerView ready from UI-002
- Order models, Address models, Money formatting ready from DOM-002
- API client with order endpoints, idempotency interceptor ready from NET-002

**Screens:** M-11, M-12, M-13, M-14

**Spec:** [`android/PHASE-2-SPEC.md`](android/PHASE-2-SPEC.md) §E

**Scope:** signed-in address capture, server-priced order review, explicit demo outcome selection, idempotent order creation, and result display.

**Acceptance criteria:**

- [ ] **a.** Checkout validates required address fields against the contract, displays server-confirmed USD totals, and clearly labels the payment step as simulated.
- [ ] **b.** The UI collects no card number, security code, expiry, or live payment credential, and no provider branding is shown.
- [ ] **c.** Success, failure, timeout, retry, and duplicate-submit states map correctly to server order state; a timeout is never presented as success, and a new user attempt uses a new idempotency key while a transport retry reuses the existing one.
- [ ] **d.** A failed simulated payment leaves the order retryable and is never presented as paid.
- [ ] **e.** Tests cover validation, state transitions, idempotency behaviour, and safe retry; the double-tap case creates no second order.

**Verification:** unit and API-mapping tests, focused UI smoke tests for the success and failure outcomes, and evidence of one deliberately interrupted request that created no duplicate order.

### Step 3 — ORD-002 — Android order history and details

**Status:** `[ ]`

**Depends on:** FND-001, FND-006, UI-002, APP-002, DOM-002, NET-002, AUTH-002, DES-001

**Contract dependency:** FND-004. **Runtime integration:** ORD-001 — exercised by QUAL-001, not blocking this task.

**Prerequisites to start:**
- All Phase 0 and Phase 1 foundation complete
- AUTH-002 complete (authentication working, user context available)
- **FND-004 complete** (Backend: OpenAPI order history/detail endpoints defined)
- **DES-001 complete** (Project: Screens M-15, M-16 finalized)
- Account navigation graph defined in APP-002
- StatusBadgeView, order list styles ready from UI-002
- Order models, OrderStatus enums, Money formatting ready from DOM-002
- API client with order history endpoints ready from NET-002

**Screens:** M-15, M-16

**Spec:** [`android/PHASE-2-SPEC.md`](android/PHASE-2-SPEC.md) §F

**Scope:** customer-owned order list and detail with immutable snapshots and server-supplied status.

**Acceptance criteria:**

- [ ] **a.** A customer can view only their own orders and sees immutable item, address, USD total, payment result, and current status snapshots; no snapshot field is editable.
- [ ] **b.** Empty, loading, network, error, and not-found states are handled with accessible English copy, and a not-owned order is indistinguishable from a missing one.
- [ ] **c.** Tests cover parsing, status and payment-status display, pagination, and authorization/error handling.

**Verification:** unit and API-mapping tests, an order-history UI smoke test, and screenshots of populated, empty, completed-payment, failed-payment, and not-found states.

## Phase 3 — Quality and Portfolio Delivery

**Cross-component release check:** see [Phase 3 in the Project Roadmap](ROADMAP.md) during final project validation. This is not an additional Android task assignment.

### Step 1 — QUAL-001 — Component contribution

**Canonical task:** [QUAL-001 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Supply Android-side deterministic purchase-flow regression evidence and resolve Android-specific failures. This is the step that exercises the app against the **real** backend instead of the mock.

### Step 2 — QUAL-002 — Component contribution

**Canonical task:** [QUAL-002 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Review Android accessibility, responsive layouts, visual consistency, and screen states.

### Step 3 — QUAL-003 — Component contribution

**Canonical task:** [QUAL-003 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Review Android token storage, sensitive-data logging, client-side input handling, transport configuration, and dependency risks.

### Step 4 — QUAL-004 — Component contribution

**Canonical task:** [QUAL-004 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Verify Android setup, run commands, demo behavior, and limitations in the handoff guide.

### Step 5 — QUAL-005 — Component contribution

**Canonical task:** [QUAL-005 in the Project Roadmap](ROADMAP.md). The task scope, status, acceptance criteria, and verification record live only in that canonical entry.

**Component contribution:** Run documented Android checks from a clean checkout and report exact commands and outcomes.

## External dependencies owned by other components

These are the only cross-component items the Android tasks depend on. They are listed here **once** for
coordination; their canonical scope, status, and criteria live in their own roadmaps and must not be
reported as Android work.

| Dependency | Canonical location | Effect on Android |
|------------|--------------------|-------------------|
| FND-004 — OpenAPI contract | [Backend Roadmap — Phase 0 — Step 2](BACKEND_ROADMAP.md) | Blocks starting any Phase 1/2 client integration until accepted |
| AUTH-001 — Customer account API | [Backend Roadmap — Phase 1 — Step 1](BACKEND_ROADMAP.md) | Deferred to QUAL-001 integration; not a build blocker thanks to the mock strategy |
| CAT-001 — Catalog read API and seed data | [Backend Roadmap — Phase 1 — Step 2](BACKEND_ROADMAP.md) | Deferred to QUAL-001 integration |
| CART-001 — Customer cart API | [Backend Roadmap — Phase 2 — Step 1](BACKEND_ROADMAP.md) | Deferred to QUAL-001 integration |
| ORD-001 — Order creation and simulated payment API | [Backend Roadmap — Phase 2 — Step 2](BACKEND_ROADMAP.md) | Deferred to QUAL-001 integration |
| FND-005 — Setup documentation and CI | [Project Roadmap — Phase 0 — Step 4](ROADMAP.md) | Owns cross-component CI consolidation after FND-006 adds the Android job |

## Open decisions affecting Android

| ID | Question | Owner decision needed before |
|----|----------|------------------------------|
| OPEN-5 | Local mock server runner (Python 3 stdlib recommended) | NET-002 completion |
| OPEN-6 | Token storage mechanism | DATA-002 |
| OPEN-9 | `allowBackup` for the demo app | DATA-002 |
| OPEN-10 | Room deferred for the first release | DATA-002 |
| OPEN-1..4 | Contract decisions listed in [`openapi.yaml`](openapi.yaml) | FND-004 acceptance |
