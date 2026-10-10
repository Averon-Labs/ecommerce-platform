# Project Roadmap and Shared Task Backlog

## Purpose and source of truth

This is the project-level index for phase gates, cross-component tasks, and Task ID ownership. It is not the task list for Android, backend, or admin implementation. Assign component work from [ANDROID_ROADMAP.md](ANDROID_ROADMAP.md), [BACKEND_ROADMAP.md](BACKEND_ROADMAP.md), or [ADMIN_ROADMAP.md](ADMIN_ROADMAP.md); those files hold canonical task scope, dependencies, acceptance criteria, and verification expectations.

Each component roadmap resets step numbering to Step 1 at the start of every phase. Assign work by the roadmap name, phase, and step, or by the stable Task ID. If the component is not identified, ask which roadmap. Cross-component work is listed here and referenced by component roadmaps without copying its acceptance criteria. Acceptance criteria use lowercase letter labels that restart at `a` in each criteria block; these labels identify criteria for discussion and verification, not separate work assignments.

### Steps and acceptance criteria

The numbered Step is the roadmap unit and has one canonical Task ID. Assign an agent by Task ID or roadmap/phase/step. Each acceptance criterion is labeled with a lowercase English letter, restarting at `a` for every criteria block. Use the label to refer to a requirement (for example, “criterion b”); it does not define a separate assignment. Work that needs its own lifecycle or dependency graph must have a separate Task ID.

Each Task gets one branch and PR; all specialist stages for that Task use the same branch. See [AGENTS.md](../AGENTS.md) for the branch and commit naming format.

Product boundaries are in [PRODUCT.md](PRODUCT.md), architecture choices in [ARCHITECTURE.md](ARCHITECTURE.md) and [decisions/](decisions/), shared UI rules in [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) and [SCREEN_SPECIFICATIONS.md](SCREEN_SPECIFICATIONS.md), API conventions in [API_CONTRACT.md](API_CONTRACT.md), and test/security rules in [TESTING.md](TESTING.md) and [SECURITY.md](SECURITY.md). Those documents define standards; the canonical roadmap task entry tracks implementation.

Do not create a separate .ai/tasks/<TASK-ID>.md or .ai/plans/<TASK-ID>.md for ordinary work. Use those files only for substantial persistent context that cannot fit the canonical task; link them from that task and do not duplicate acceptance criteria.

## Project goal

Deliver a polished, end-to-end, single-store shopping demo for an international portfolio audience: an English-only Android customer app in Java/XML, a Laravel/MySQL REST API, and a React/TypeScript admin dashboard. Prices are sample USD amounts, data is fictional, and payment outcomes are simulated. This is not a production commerce launch.

## Branch and release model

- develop is the shared integration branch. Each assigned Task gets one short-lived branch from develop and a PR targeting develop; builder, tester, and debugger use that branch sequentially.
- Delete a task branch after its PR is accepted and merged. Keep develop permanently.
- At a phase/release boundary, complete phase exit criteria and run release checks on develop. Promote a release only through a reviewed develop-to-main PR; tag the released commit.
- The current main contains earlier merged bootstrap/documentation work and has no formal release tag. Preserve that history as the one-time pre-policy baseline; do not reset or rewrite main.
- See [AGENTS.md](../AGENTS.md) for branch naming, PR, review, and cleanup rules. Markdown does not change GitHub settings; verify actual remote configuration.

## Verified starting point

Last reviewed: 2026-10-09. Recheck source and Git state before starting each Task.

## Authority and component scope

This roadmap is for explicitly cross-component work and final project/release validation. It does not replace or extend a component's execution backlog. When an owner assigns Android work, use `ANDROID_ROADMAP.md` as the sole source for Android tasks, statuses, scopes, acceptance criteria, and progress reports; similarly, use `BACKEND_ROADMAP.md` or `ADMIN_ROADMAP.md` for those components. Do not report another component's open tasks as work remaining in the assigned component. A dependency owned by another component can block a task; report only that dependency and its effect on the assigned task. Review this project roadmap when coordinating cross-component work or deciding whether the full project/release gate is met.

- Android has a regenerated Java/XML Gradle scaffold: namespace/application ID com.averonlabs.eshopverse, compile SDK 36.1, minSdk 25, targetSdk 36, Java 11 source/target, AGP 9.2.1, Gradle 9.4.1. The complete Phase 0-2 dependency set (Material 1.14.0, AndroidX libraries, Retrofit, OkHttp, Glide, test dependencies) was declared in FND-006. Verified build/lint/test commands are documented in `docs/DEVELOPMENT.md`.
- Android Phase 0 was expanded by PLAN-005 with six cross-cutting foundation steps (FND-006, UI-002, APP-002, DOM-002, DATA-002, NET-002) that must complete before the Phase 1 screen tasks; `docs/openapi.yaml` now exists as a draft for FND-004 owner review, and contract-derived fixtures plus a local mock server let the Android tasks be verified without a running backend.
- Android Phase 1 and Phase 2 steps depend only on Android and shared steps. Their backend dependencies are recorded once, in the coordination table in `docs/ANDROID_ROADMAP.md`, with integration deferred to QUAL-001.
- `backend/` now contains the initialized Laravel 13 application (FND-002, 2026-10-11) with a hosted MySQL workflow; `admin/` still contains agent guidance only (FND-003).
- .github/workflows/ci.yml is a placeholder. Its active job checks repository structure; component checks are not active.
- GitHub develop was created from the current pre-policy main baseline. Default branch, auto-delete, and branch protection must be verified under GIT-001.
- `docs/openapi.yaml` exists as an OpenAPI 3.1 draft awaiting API-owner acceptance (FND-004). `docs/API_CONTRACT.md` records shared conventions; endpoint schemas and per-class rate limits are drafted in the OpenAPI file and become binding only on acceptance.
- docs/DATABASE.md is a logical model, not implemented migrations.

This is a repository snapshot, not a promise that the state remains unchanged. Confirm facts again when a Task starts.

## Status and completion rules

- Task status: [ ] not started, [~] in progress, [x] complete with evidence, [!] blocked with the reason recorded.
- Acceptance checkboxes are checked only after implementation and verification evidence are reviewed.
- A task is complete when every criterion passes, required checks are green, and the reviewer has exact commands, outcomes, and the commit tested. Follow the Definition of Done in [AGENTS.md](../AGENTS.md).
- If a task exposes a missing product or architecture decision, ask the owner and stop dependent work; do not silently broaden scope or contradict an accepted ADR.
- OpenAPI must be reviewed and accepted before implementing API endpoints or client integrations. The owner can review the draft as a concrete artifact; no additional product choice is needed to prepare it from accepted scope.

## How to assign and execute a task

Name the roadmap, phase, and step (step numbering restarts at 1 in each phase), or give the stable Task ID. `ROADMAP.md` defines project gates and shared tasks and indexes component work; `ANDROID_ROADMAP.md`, `BACKEND_ROADMAP.md`, and `ADMIN_ROADMAP.md` hold the canonical component task scopes, dependencies, letter-labeled acceptance criteria, and verification expectations. Use criterion letters to refer to requirements, not to create separate assignments. For assignment, reading, clarification, and execution rules, follow the root [AGENTS.md](../AGENTS.md).

## Phase 0 — Foundation (current)

**Final project validation gate (not a component task list):** all three components have repeatable, verified setup and check commands; the OpenAPI contract has been reviewed before endpoint work; CI runs the agreed component checks; GitHub's integration/release branch settings match GIT-001; the shared first-release UI specification is accepted before screen implementation; remaining choices that block the first customer-to-order flow are recorded and resolved. Component agents execute and report their own work from their respective component roadmaps. Evaluate this gate only during cross-component coordination or final project validation.


**Android steps:** [Android Roadmap — Phase 0](ANDROID_ROADMAP.md). **Backend steps:** [Backend Roadmap — Phase 0](BACKEND_ROADMAP.md). **Admin steps:** [Admin Roadmap — Phase 0](ADMIN_ROADMAP.md).

### Step 1 — GIT-001 — Establish the develop integration branch

**Status:** `[~]`

**Depends on:** none

**Scope:** establish the shared integration branch and align GitHub's branch workflow with the release policy without rewriting existing history.

**Acceptance criteria:**

- [x] **a.** Create `develop` from the current `main` baseline and preserve all existing merged history; do not force-push or reset `main`.
- [ ] **b.** Set `develop` as the GitHub default branch so new task PRs default to the integration branch.
- [ ] **c.** Configure GitHub to automatically delete merged task branches; keep `develop` and `main`.
- [ ] **d.** Protect `develop` and `main` from direct/force pushes and require PR review. Review `develop` → `main` as a release PR only.
- [ ] **e.** Open/re-target current in-flight work as a PR into `develop`; do not merge it automatically.

**Verification:** inspect live GitHub branch refs, default branch, PR base, deletion setting, and protection rules. Confirm `main` was not rewritten.

### Step 2 — DES-001 — Define shared visual direction and first-release screen specifications

**Status:** `[x]`

**Depends on:** none

**Scope:** translate the owner's supplied visual reference into one shared Android/admin visual language and document first-release screens already required by the product and roadmap. The reference is style guidance only; it must not expand page, content, or behavior scope.

**Acceptance criteria:**

- [x] **a.** `docs/DESIGN_SYSTEM.md` defines the cross-platform coral/neutral palette, Lato typography, shared spacing/shapes/elevation, Android Material 3 rules, web dashboard patterns, responsive behavior, and accessibility requirements.
- [x] **b.** `docs/SCREEN_SPECIFICATIONS.md` describes all first-release Android and admin screens, navigation shells, primary content/actions, and relevant loading/empty/error/success behavior.
- [x] **c.** Screen specifications apply reference layout cues only where an existing screen has a matching role; other screens retain their product/roadmap content hierarchy and flow while using shared visual tokens.
- [x] **d.** Screen flows respect the product/database/API boundaries: authenticated cart, sample shipping and no tax, simulated payment without card data, seeded product images, and admin-only catalog/inventory/order capabilities.
- [x] **e.** Out-of-scope reference concepts are excluded; unresolved data/API conflicts (including category archive behavior) are explicitly recorded rather than invented.
- [x] **f.** Android/web agent guidance, product/roadmap references, and ADR index link to the shared design documents without duplicating the page inventory.

**Verification:** documentation review against `PRODUCT.md`, `DATABASE.md`, `API_CONTRACT.md`, `ARCHITECTURE.md`, and existing Android/admin Tasks; check internal links and `git diff --check`. No code or runtime test is applicable.

### Step 3 — PLAN-001 — Split component roadmaps and define task execution and clarification workflow

**Status:** `[x]`

**Depends on:** none

**Scope:** organize the canonical backlog into component roadmaps with phase-local step numbers and make task assignment, clarification, and sequential agent execution unambiguous.

**Acceptance criteria:**

- [x] **a.** Create separate Android, Backend, and Admin roadmaps; every phase numbers its steps from Step 1.
- [x] **b.** Keep each Task ID's scope, dependencies, acceptance criteria, and verification expectations in exactly one canonical entry; link shared work rather than copying it.
- [x] **c.** Update repository and component instructions and developer-facing entry points to tell agents which roadmap to read and how a step assignment maps to a Task ID.
- [x] **d.** Define when to proceed without a question, when a material ambiguity requires an owner question, and that work continues after the answer without a second confirmation.
- [x] **e.** Define the end-to-end sequential build, independent test, conditional debug, retest, review, and PR-ready workflow.
- [x] **f.** Verify roadmap task mapping, step numbering, internal links, and the documentation diff.

**Verification:** inspect the generated roadmaps and all updated references; run documentation link/Task ID consistency checks and git diff --check. Application build and runtime tests are not applicable.

### Step 4 — FND-005 — Make setup documentation and CI match the real projects

**Status:** `[ ]`

**Depends on:** FND-001, FND-002, FND-003, FND-004, GIT-001

**Scope:** replace placeholder-only project checks with repeatable component checks and document what CI actually enforces.

**Acceptance criteria:**

- [ ] **a.** `docs/DEVELOPMENT.md` contains copyable setup/build/lint/static-check/test commands for Android, backend, admin, and OpenAPI validation; each command is verified against the checked-in tooling.
- [ ] **b.** Update `.github/workflows/ci.yml` to run the documented required checks for each initialized component on task PRs to `develop` and release PRs to `main`, using isolated test data and no committed secrets.
- [ ] **c.** Preserve failures as failures: do not skip, disable, or mask a required check to make CI green.
- [ ] **d.** Verify the workflow on a pull request or equivalent run and record its result. Inspect repository default-branch, auto-delete, and branch-protection settings against GIT-001; configure required status checks only after verifying their names, and document any setting that is not enforced.
- [ ] **e.** Remove stale “not initialized” comments/status once they are false; never claim a workflow or branch rule is active without evidence.

**Verification:** run the exact documented commands locally or in CI; inspect a successful workflow run and separately report any external branch-protection setting that remains unenforced.

### Step 5 — PLAN-002 — Separate agent role workflows and remove duplicated process guidance

**Status:** `[x]`

**Depends on:** PLAN-001

**Scope:** keep shared repository policy in the root instructions, put actionable build, test, and debug procedures in role-specific workflow guides, and remove repeated process descriptions from supporting Markdown without merging distinct product or technical references.

**Acceptance criteria:**

- [x] **a.** Add concise build, independent-test, and conditional-debug workflow guides under `.agents/workflows/`; each defines its entry conditions, scope boundaries, required evidence, and handoff to the next stage.
- [x] **b.** Keep `AGENTS.md` as the shared entry point and route each specialist to the correct guide; preserve the sequential same-task-branch flow, distinct builder/tester/debugger roles, independent retest, and no-repeat-without-new-evidence rule.
- [x] **c.** Use a focused local commit as each editing stage's handoff snapshot: builder commits implementation, tester commits any test-only additions, and debugger commits a fix; report the exact SHA verified and do not push during normal stage transitions.
- [x] **d.** Keep `docs/TESTING.md` focused on test strategy and test conventions; keep component setup details in `docs/DEVELOPMENT.md` and component-specific agent instructions in each component `AGENTS.md`.
- [x] **e.** Remove duplicated agent-stage procedures from supporting documents while preserving unique component, test-design, setup, contribution, and handoff requirements.
- [x] **f.** Update documentation entry points and links; confirm each roadmap Task ID remains canonical in one entry and verify Markdown links and the final documentation diff.

**Verification:** review the shared and role-specific instructions for conflicts, check all changed internal Markdown links and roadmap references, and run `git diff --check`. Application builds and runtime tests are not applicable to this documentation-only task.

### Step 6 — PLAN-003 — Define assignable sub-steps and component-aware Git naming (superseded)

**Status:** `[x]`

**Historical note:** Superseded by PLAN-004. The Sub-step assignment model below is retained only as the record of the earlier approach; it is no longer active guidance. Component-aware branch and commit naming remains in effect, with the format defined by current `AGENTS.md`.

**Depends on:** PLAN-001, PLAN-002

**Scope:** distinguish acceptance criteria from independently assignable roadmap sub-steps and standardize branch and commit names so they identify component, phase, step, optional sub-step, and Task ID.

**Acceptance criteria:**

- [x] **a.** Define when a nested roadmap item remains an acceptance criterion and when it merits a numbered, independently assignable sub-step; preserve current nested acceptance checklists unless they meet that rule.
- [x] **b.** Define phase-local step and optional sub-step numbering consistently across the project and component roadmaps.
- [x] **c.** Update repository Git guidance with a consistent branch and Conventional Commit format identifying component, phase, step, optional sub-step, and Task ID.
- [x] **d.** Keep branch type aligned with work (`feature`, `fix`, `docs`, `chore`, etc.) and define how an independently assigned sub-step maps to scope, branch, PR, and parent Task completion.
- [x] **e.** Verify roadmap hierarchy, examples, internal links, naming consistency, and `git diff --check`; no application build or runtime test applies.

**Verification:** independently reviewed the updated instructions and roadmap examples at commit `e6dd539b9fbfd1a0fad9637ab986084efe51b7ba`; Markdown relative-link, roadmap hierarchy/naming consistency, and `git diff --check` checks passed. Application builds and runtime tests are not applicable to this documentation-only task.

### Step 7 — CI-001 — Refresh GitHub Actions runtimes and least-privilege permissions

**Status:** `[x]`

**Depends on:** none

**Scope:** update active and illustrative GitHub Actions references that target deprecated Node runtimes, and limit the repository-validation workflow to the permissions and credentials it needs.

**Acceptance criteria:**

- [x] **a.** Upgrade active and commented first-party actions to current supported major versions that run on Node 24; retain third-party action versions that remain supported and current.
- [x] **b.** Limit the read-only validation job's token permissions to repository contents read and prevent checkout from persisting credentials it does not use.
- [x] **c.** Keep uninitialized component jobs disabled; do not declare or install a Node.js version for the Admin application before its package/toolchain exists.
- [x] **d.** Validate workflow syntax and review the final diff; report that hosted CI must be observed on the next pull request if one is not created for this task.

**Verification:** Python/PyYAML parsed the workflow; static assertions confirmed the intended action versions, read-only permissions, disabled component jobs, and absence of setup-node; `git diff --check` passed. `actionlint` was unavailable. The hosted `Validate Repository` check passed on PR #13. No application build or runtime test applies to this workflow-only change.

### Step 8 — PLAN-004 — Label roadmap acceptance criteria and remove Sub-step assignment rules

**Status:** `[x]`

**Depends on:** PLAN-003, CI-001

**Scope:** make every roadmap acceptance criterion addressable by a lowercase English letter within its Task, remove Sub-step as an assignment or branch-naming concept, and retain Phase/Step/Task as the roadmap hierarchy.

**Acceptance criteria:**

- [x] **a.** Label every acceptance criterion in the project and component roadmaps `a`, `b`, `c`, and so on; restart at `a` for each Task's acceptance-criteria section.
- [x] **b.** Update shared instructions and roadmap introductions so assignments can reference Task criteria by their letters without introducing Sub-steps.
- [x] **c.** Remove optional Sub-step coordinates from branch and Conventional Commit formats; retain component, phase, step, Task ID, and change type.
- [x] **d.** Mark PLAN-003's Sub-step approach as superseded while preserving its historical record and the component-aware naming guidance that remains valid.
- [x] **e.** Verify labels restart correctly for every acceptance-criteria block, no active Sub-step rules remain, roadmap/task references and Markdown links are valid, and `git diff --check` passes.

**Verification:** independently reviewed all roadmap acceptance criteria and assignment/Git rules; checked Markdown links, criterion-label consistency, and `git diff --check`. The CI-001 dependency and its hosted CI check have also been confirmed. No application build or runtime test applies.

### Step 9 — PLAN-005 — Plan the Android execution structure and phase specifications

**Status:** `[~]`

**Depends on:** PLAN-001, PLAN-002, DES-001, FND-001

**Scope:** make the Android backlog executable phase by phase by a sequence of independent agents: add the missing cross-cutting foundation steps, freeze file ownership and the navigation interface, define the contract and its local mock, and write the phase-level implementation specifications.

**Rationale.** The Android backlog previously moved from a verified empty scaffold directly to customer-authentication screens. Five cross-cutting layers that every screen depends on — dependencies and test harness, design tokens and component styles, app shell and navigation, network and API client, money/models/secure storage — had no owning step, so screen tasks would each have re-implemented them and two parallel tasks would have edited the same files. The Phase 1–2 steps were also blocked on backend implementations they do not actually need, because the integration boundary is the reviewed contract rather than a running server.

**Acceptance criteria:**

- [x] **a.** Every cross-cutting layer the Phase 1–2 screen tasks depend on has a canonical Android step with an owning scope and no file overlap with a concurrent step.
- [x] **b.** Android Phase 1 and Phase 2 steps list only Android and shared steps as blocking dependencies; external backend/admin dependencies are recorded once in a coordination table with their integration point.
- [x] **c.** A machine-readable contract draft exists for owner review, resolves the recorded open contract decisions (including category lifecycle), and passes structural validation.
- [x] **d.** A task-brief format exists that defines the per-task reading set, file ownership, and definition of done, so an agent does not read the whole documentation tree.
- [x] **e.** Phase implementation specifications exist for Android Phases 0, 1, and 2 and reference, without duplicating, the canonical acceptance criteria in the component roadmap.
- [x] **f.** Contract-derived fixture and local mock behaviour is specified so client steps can be verified and demonstrated before the backend exists.
- [ ] **g.** The Task ID index, component guidance, and stale status statements are consistent with the restructured roadmaps, and the owner has reviewed and accepted the contract draft.

**Verification:** structural validation of the contract draft (no tabs, no duplicate keys, no unresolved `$ref`), Markdown link and Task-ID consistency checks, and owner review of the contract and the open decisions listed in `docs/openapi.yaml`. No application build or runtime test applies to this planning task.

## Phase 1 — Accounts and Catalog

**Entry criteria:** Phase 0 exit criteria are met, including acceptance of DES-001 before implementing customer/admin screens. Implement the shared contract; update and review OpenAPI before changing endpoint behavior.


Task steps are component-specific: see [Android](ANDROID_ROADMAP.md), [Backend](BACKEND_ROADMAP.md), and [Admin](ADMIN_ROADMAP.md).

**Phase 1 exit criteria:** a customer can register/sign in and browse/search seeded products; the admin can sign in and manage the catalog/inventory; server-side authorization, API/UI failure states, and the listed component tests pass.


## Phase 2 — Cart, Checkout, and Orders

**Entry criteria:** Phase 1 exit criteria are met. All money, stock, ownership, idempotency, and state transitions follow the reviewed contract.


Task steps are component-specific: see [Android](ANDROID_ROADMAP.md), [Backend](BACKEND_ROADMAP.md), and [Admin](ADMIN_ROADMAP.md).

**Phase 2 exit criteria:** a signed-in customer can complete the sample purchase flow through the API and database, including explicit simulated success/failure and safe duplicate submission; the customer can view the order and an admin can manage its status. Required ownership, stock, payment-state, and regression checks pass.


## Phase 3 — Quality and Portfolio Delivery

**Entry criteria:** Phase 2 customer purchase flow and admin order workflow are complete.


### Step 1 — QUAL-001 — Cross-component purchase-flow regression

**Status:** `[ ]`

**Depends on:** AUTH-002, CAT-002, CART-002, CHECK-001, ORD-002

**Scope:** a small number of deterministic end-to-end/smoke checks across Android, API, and database for the core customer journey.

**Acceptance criteria:**

- [ ] **a.** A clean seeded customer can browse, add/update cart items, complete simulated success, and view the resulting order.
- [ ] **b.** Simulated failure, duplicate submission, and at least one authorization boundary are covered by automated integration/regression checks at the most appropriate layer.
- [ ] **c.** Tests use isolated disposable data and cannot charge/contact a real provider.
- [ ] **d.** The suite runs locally and in CI with documented commands and actionable failure output.

**Verification:** run the smoke/regression suite from a clean database state and verify its CI job.

### Step 2 — QUAL-002 — Accessibility and UI quality pass

**Status:** `[ ]`

**Depends on:** DES-001, CAT-002, CAT-004, CART-002, CHECK-001, ORD-002, ORD-003

**Scope:** inspect and fix first-release accessibility, responsive behavior, consistency, and user-facing state gaps.

**Acceptance criteria:**

- [ ] **a.** Critical Android and admin flows have labels, focus/keyboard behavior, readable contrast/text, and usable narrow-screen layouts.
- [ ] **b.** Screens consistently handle loading, empty, error, success, and retry states without dead ends.
- [ ] **c.** Findings are recorded with a reproducible check or screenshot where useful; no unrelated redesign is introduced.

**Verification:** repeatable manual checklist plus focused automated accessibility/UI checks where supported by existing tooling.

### Step 3 — QUAL-003 — Security and privacy review

**Status:** `[ ]`

**Depends on:** AUTH-001, AUTH-003, CAT-003, CART-001, ORD-001, ORD-003

**Scope:** verify implemented first-release controls against [`SECURITY.md`](SECURITY.md) and the relevant OWASP API risks.

**Acceptance criteria:**

- [ ] **a.** Review object/function-level authorization, authentication, input validation, rate limiting, CSRF/CORS, secret handling, logging, and dependency advisories.
- [ ] **b.** Record each finding with severity, evidence, and resolution or an explicitly accepted non-production limitation; fix in-scope high-risk findings before release.
- [ ] **c.** Regression tests cover every fixed authorization/security defect; no secrets or real personal/payment data exist in repository history or demo seeds.
- [ ] **d.** The demo notice accurately states simulated payment and non-production limits.

**Verification:** security test matrix, dependency audit commands from `docs/DEVELOPMENT.md`, and reviewed findings record.

### Step 4 — QUAL-004 — Demo data and portfolio handoff guide

**Status:** `[ ]`

**Depends on:** FND-002, FND-003, CAT-001, AUTH-003, ORD-001

**Scope:** make local evaluation understandable and reproducible for a portfolio reviewer.

**Acceptance criteria:**

- [ ] **a.** Document clean setup, migrations/seeding, running all components, available demo roles, and reset procedure.
- [ ] **b.** Demo credentials are generated or local-only and clearly marked; no reusable public secrets or real user data are committed.
- [ ] **c.** Include concise architecture/flow explanation, screenshots or a short visual walkthrough, and known limitations.
- [ ] **d.** Explain that payment is simulated and no production readiness is claimed.

**Verification:** a person unfamiliar with the repository follows the guide from a clean checkout without undocumented steps.

### Step 5 — QUAL-005 — Clean-checkout release rehearsal

**Status:** `[ ]`

**Depends on:** FND-005, QUAL-001, QUAL-002, QUAL-003, QUAL-004

**Scope:** final local/CI rehearsal and closeout for the non-deployed portfolio release.

**Acceptance criteria:**

- [ ] **a.** From a clean checkout, install/setup instructions work and all documented required builds, static checks, tests, OpenAPI validation, and CI checks pass.
- [ ] **b.** No placeholder instructions, false CI claims, unreviewed API-contract changes, committed secrets, or unexplained known failures remain.
- [ ] **c.** Record tested commit, exact commands, results, limitations, and any follow-up task IDs.
- [ ] **d.** The phase exit criteria are reviewed; mark the roadmap tasks complete only with evidence.

**Verification:** independent reviewer repeats the documented clean-checkout procedure and checks the CI run and final task evidence.

**Component evidence:** the three component roadmaps list expected contributions to these canonical shared tasks; those are references, not extra task copies.

**Phase 3 exit criteria:** an unfamiliar reviewer can set up and evaluate the complete local demo from a clean checkout; required CI/security/accessibility checks pass; demo limitations and any deferred follow-up work are clear.


## Canonical Task ID index

Each Task ID has exactly one canonical task entry. Status, dependencies, acceptance criteria, and verification expectations are maintained only there.

| Task ID | Canonical location | Task |
|---------|--------------------|------|
| FND-001 | [Android Roadmap — Phase 0 — Step 1](ANDROID_ROADMAP.md) | Verify Android scaffold and toolchain |
| FND-006 | [Android Roadmap — Phase 0 — Step 2](ANDROID_ROADMAP.md) | Toolchain, dependency, and test-harness modernization |
| UI-002 | [Android Roadmap — Phase 0 — Step 3](ANDROID_ROADMAP.md) | Android design token and component style layer |
| APP-002 | [Android Roadmap — Phase 0 — Step 4](ANDROID_ROADMAP.md) | App shell and navigation |
| DOM-002 | [Android Roadmap — Phase 0 — Step 5](ANDROID_ROADMAP.md) | Money, API models, and shared UI state |
| DATA-002 | [Android Roadmap — Phase 0 — Step 6](ANDROID_ROADMAP.md) | Secure token storage and local persistence decision |
| NET-002 | [Android Roadmap — Phase 0 — Step 7](ANDROID_ROADMAP.md) | Network and API client core |
| FND-002 | [Backend Roadmap — Phase 0 — Step 1](BACKEND_ROADMAP.md) | Initialize Laravel API and local database workflow |
| FND-003 | [Admin Roadmap — Phase 0 — Step 1](ADMIN_ROADMAP.md) | Initialize the admin application and local workflow |
| FND-004 | [Backend Roadmap — Phase 0 — Step 2](BACKEND_ROADMAP.md) | Draft and review the OpenAPI contract |
| GIT-001 | [Project Roadmap — Phase 0 — Step 1](ROADMAP.md) | Establish the develop integration branch |
| FND-005 | [Project Roadmap — Phase 0 — Step 4](ROADMAP.md) | Make setup documentation and CI match the real projects |
| DES-001 | [Project Roadmap — Phase 0 — Step 2](ROADMAP.md) | Define shared visual direction and first-release screen specifications |
| AUTH-001 | [Backend Roadmap — Phase 1 — Step 1](BACKEND_ROADMAP.md) | Customer account API |
| AUTH-002 | [Android Roadmap — Phase 1 — Step 1](ANDROID_ROADMAP.md) | Android customer authentication and profile |
| AUTH-003 | [Admin Roadmap — Phase 1 — Step 1](ADMIN_ROADMAP.md) | Admin session authentication and API authorization |
| CAT-001 | [Backend Roadmap — Phase 1 — Step 2](BACKEND_ROADMAP.md) | Catalog read API and fictional seed data |
| CAT-002 | [Android Roadmap — Phase 1 — Step 2](ANDROID_ROADMAP.md) | Android catalog browsing |
| CAT-003 | [Backend Roadmap — Phase 1 — Step 3](BACKEND_ROADMAP.md) | Admin catalog and inventory API |
| CAT-004 | [Admin Roadmap — Phase 1 — Step 2](ADMIN_ROADMAP.md) | Admin catalog and inventory screens |
| CART-001 | [Backend Roadmap — Phase 2 — Step 1](BACKEND_ROADMAP.md) | Customer cart API |
| CART-002 | [Android Roadmap — Phase 2 — Step 1](ANDROID_ROADMAP.md) | Android cart experience |
| ORD-001 | [Backend Roadmap — Phase 2 — Step 2](BACKEND_ROADMAP.md) | Order creation and simulated payment API |
| CHECK-001 | [Android Roadmap — Phase 2 — Step 2](ANDROID_ROADMAP.md) | Android checkout and simulated payment flow |
| ORD-002 | [Android Roadmap — Phase 2 — Step 3](ANDROID_ROADMAP.md) | Android order history and details |
| ORD-003 | [Admin Roadmap — Phase 2 — Step 1](ADMIN_ROADMAP.md) | Admin order management |
| QUAL-001 | [Project Roadmap — Phase 3 — Step 1](ROADMAP.md) | Cross-component purchase-flow regression |
| QUAL-002 | [Project Roadmap — Phase 3 — Step 2](ROADMAP.md) | Accessibility and UI quality pass |
| QUAL-003 | [Project Roadmap — Phase 3 — Step 3](ROADMAP.md) | Security and privacy review |
| QUAL-004 | [Project Roadmap — Phase 3 — Step 4](ROADMAP.md) | Demo data and portfolio handoff guide |
| QUAL-005 | [Project Roadmap — Phase 3 — Step 5](ROADMAP.md) | Clean-checkout release rehearsal |
| PLAN-001 | [Project Roadmap — Phase 0 — Step 3](ROADMAP.md) | Split component roadmaps and define task execution and clarification workflow |
| PLAN-002 | [Project Roadmap — Phase 0 — Step 5](ROADMAP.md) | Separate agent role workflows and remove duplicated process guidance |
| PLAN-003 | [Project Roadmap — Phase 0 — Step 6](ROADMAP.md) | Define assignable sub-steps and component-aware Git naming |
| CI-001 | [Project Roadmap — Phase 0 — Step 7](ROADMAP.md) | Refresh GitHub Actions runtimes and least-privilege permissions |
| PLAN-004 | [Project Roadmap — Phase 0 — Step 8](ROADMAP.md) | Label roadmap acceptance criteria and remove Sub-step assignment rules |
| PLAN-005 | [Project Roadmap — Phase 0 — Step 9](ROADMAP.md) | Plan the Android execution structure and phase specifications |

## Optional follow-up — Public deployment

Public hosting is outside the required portfolio release. Do not block Phases 0–3 on it. If requested, create a separately scoped deployment Task after the local release is complete. Before execution, the owner must choose/approve the hosting provider, domain/origin arrangement, budget, secrets/configuration, and public demo-account policy. Deployment must use HTTPS, fictional data, and the simulated-payment boundary; production operations remain a separate project.

## Deferred product features

Wishlist, reviews, push notifications, coupons, analytics, guest checkout, multiple currencies/languages, real payment processing, taxes, carrier integrations, multi-vendor support, and production operations are not required for this release. Add any only through an explicitly accepted scope/ADR and a new Task ID in the appropriate canonical roadmap.
