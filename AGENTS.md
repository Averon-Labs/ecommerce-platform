# AGENTS.md — Repository-Wide Agent Instructions

## Component-Specific Git Ownership Override

This section takes precedence over every other Git, branch, commit, push, and PR instruction in this file.

- **Universal Phase-Based Branching:** Across all components (Android, Backend, Admin, and Project), task branches are maintained **per phase** (`feature/<component>-phase-<N>`), housing all steps of that phase together in a single branch. Individual steps are NOT separate branches; each step is committed sequentially as a focused Conventional Commit (`<type>(<component>-pNN-sNN-<task-id>): <imperative summary>`) directly within the phase branch. When all steps of a phase are completed and verified, the phase branch is reviewed and merged into `develop`.
- **Android tasks:** agents are authorized to manage Git operations on phase branches (`feature/android-phase-<N>`), including inspecting Git state, branch creation from `develop`, committing steps sequentially, pushing, and preparing PRs into `develop` following the agent Git workflow.
- **Backend tasks:** the repository owner performs all Git operations. Backend feature branches are maintained per phase (`feature/backend-phase-<N>`), housing all steps of that phase together. Agents must not run any Git command, including `status`, `diff`, `log`, `fetch`, `pull`, `branch`, `switch`, `add`, `commit`, `push`, `reset`, or GitHub CLI commands. Sequential agents hand off verified working-tree state without commits.
- **Admin tasks:** continue to follow the Git, branch, commit, push, and PR workflow described elsewhere in this file, operating on phase-based branches (`feature/admin-phase-<N>`) where each step is committed sequentially.
- The owner may authorize additional specific Git operations as an exception for one particular task.

> This file is the **primary instruction set** for all AI coding agents working in this repository.
> It is provider-neutral and applies equally to Claude Code, OpenAI Codex, Google Gemini/Antigravity, GitHub Copilot, and any other compatible coding agent.

---

## Project Purpose

This repository contains **EShop Verse**, a production-oriented e-commerce platform consisting of:

- **Android Customer Application** — Java, XML, Material 3, MVVM, Repository Pattern
- **RESTful Backend** — PHP, MySQL
- **Admin Dashboard** — Web-based administration interface
- **Database** — MySQL relational database

The platform is developed collaboratively by human developers and AI coding agents.

---

## Source of Truth (Priority Order)

1. **Explicit human requirements** — direct instructions from project owners
2. **Source code** — the actual implementation
3. **Automated tests** — verified behavior
4. **Git history** — commit log and branch state
5. **Architecture Decision Records** — `docs/decisions/`
6. **Project documentation and canonical backlog** — the relevant component roadmap (`docs/ANDROID_ROADMAP.md`, `docs/BACKEND_ROADMAP.md`, or `docs/ADMIN_ROADMAP.md`) is the sole execution backlog for work assigned to that component. `docs/ROADMAP.md` owns only explicitly cross-component tasks and final project/release validation; it is not an extra component task list.
7. **Supplementary task context** — `.ai/tasks/` only when a roadmap task links to it
8. **AI-generated plans and notes** — `.ai/plans/`, `.ai/handoffs/`

> **AI conversation history is NEVER authoritative project state.**
> The repository is the source of truth. Always verify against the repository.

---

## Sequential Agent Workflow

Specialist agents work one at a time on the same task branch. Unless the owner narrows the request, assigning a roadmap step or Task ID covers the full workflow through a PR ready for human review. The coordinator assigns each stage its scope, acceptance criteria, and current commit; do not start another stage while an agent is editing. For Android and Admin work, each editing stage creates a focused local commit before handoff so the next stage has a stable snapshot; normal stage transitions do not push. For Backend work, the repository owner handles all Git operations; agents must not invoke Git commands for Backend and must hand off the verified working-tree state sequentially without making commits.

Each specialist reads only the workflow guide for its assigned role, in addition to the shared and task-relevant guidance:

1. **Builder:** [`.agents/workflows/build.md`](.agents/workflows/build.md) — implement the task and add tests for changed behavior.
2. **Independent tester:** [`.agents/workflows/test.md`](.agents/workflows/test.md) — independently verify the builder's commit; it must be a different agent from the builder.
3. **Debugger:** [`.agents/workflows/debug.md`](.agents/workflows/debug.md) — a third, distinct agent, used only after a failure is reproduced.
4. **Independent retest:** the tester or another independent tester verifies a debug fix. The coordinator reviews evidence and the final diff before completion.

For Android and Admin work, every agent must inspect the current git status, branch, and recent commits. For Backend work, agents must not run Git commands; the owner manages Git state. Every agent must inspect the assigned Task entry in its canonical component roadmap; for explicitly cross-component work, use `docs/ROADMAP.md`. Read linked supplementary context and relevant component guidance before editing. When assigned Android work, do not execute or report backend/admin tasks. An incomplete external dependency may block the assigned Android task; report only the dependency ID and its blocking effect, then coordinate with its owner. Agents must respect the assigned file/scope boundary and preserve untracked or unrelated work.

Debuggers must keep an attempt record and must not repeat a failed approach without new evidence; follow the limits and evidence format in the debug guide. The coordinator passes that record between stages. A tester commits any test-only additions and reports the exact SHA verified; if it makes no changes, it reports the builder's SHA. Normal build → test → debug → retest transitions need no handoff file or intermediate push; use the handoff procedure below only when work is blocked or transferred outside this sequence. After independent verification and coordinator review, the coordinator publishes the task branch and prepares a PR for Android and Admin work; never merge automatically. For Backend work, the owner handles all Git publishing and PR operations, and sequential agents verify the shared working-tree state without commits.

---

## Planning Requirements

### Before Starting Any Task

1. **Read this file** (`AGENTS.md`) completely.
2. **Read the relevant component AGENTS.md** (`android/AGENTS.md`, `backend/AGENTS.md`, or `admin/AGENTS.md`).
3. **Inspect Git state for Android and Admin tasks** — check for uncommitted changes, current branch, and recent commits. For Backend tasks, do not invoke Git; the owner manages all Git operations and state.
4. **Inspect existing code** — understand the current state before modifying.
5. **Read the assigned Step and canonical Task entry** in the matching component roadmap; use `docs/ROADMAP.md` only when the assignment is explicitly cross-component or for final project/release validation. Inspect any supplementary `.ai/tasks/` file explicitly linked by the entry.
6. **Follow Ponytail principles** — see [`.agents/skills/ponytail/SKILL.md`](.agents/skills/ponytail/SKILL.md) (minimal code, strict YAGNI, reuse existing libs/code).
7. For substantial work, plan from the roadmap entry before implementing; keep the plan in the task discussion unless durable extra context is genuinely needed.

### Step Assignment and Clarification

- Identify work by roadmap name, phase, and step (for example, `Android Roadmap — Phase 1 — Step 1`) or by stable Task ID. Step numbering restarts at 1 in every phase. If the owner names a phase and step without identifying a roadmap, ask which component they mean. An Android assignment means Android Roadmap only; never expand it to backend/admin work because those tasks appear in a project-level gate or dependency list.
- Acceptance criteria are labeled with lowercase English letters restarting at `a` in each criteria block. Use a letter to point to a specific criterion in discussion or verification; a criterion label does not create a separately assignable work unit. Assign work by Task ID or roadmap/phase/step.
- Read the canonical task, its dependencies, applicable component instructions, and every authoritative project document that governs the task's scope: the task-linked documents and any applicable product, architecture/ADR, API/database, design, security, and testing guidance. Inspect source, tests, and current configuration to resolve decisions already answered by repository evidence; do not bulk-read unrelated component documents or ask the owner to repeat a documented decision.
- If scope, acceptance criteria, product behavior, architecture, API/schema/security behavior, or a consequential library/dependency choice is materially missing or contradictory, ask a focused question. State the unresolved decision, give a recommendation and tradeoff when useful, and pause only the dependent work. Once the owner answers, continue the task without asking for another start or plan confirmation.
- If the task and authoritative evidence fully specify the work, proceed without an approval question or a redundant plan-confirmation turn. For example, Android image loading is already specified as Glide in `android/AGENTS.md`; do not ask whether to use Glide or Picasso.
- Do not silently guess at material requirements or mark uncertainty as resolved. If dependencies are incomplete, report the exact blocking Task ID and do not expand the assigned scope to bypass it.
- For Android and Admin tasks, follow the canonical remote synchronization lifecycle in [Git Rules](#remote-synchronization-and-task-branch-lifecycle). For Backend tasks, do not run Git commands at any point unless the owner explicitly authorizes a specific Git operation for that particular task.
- Never merge a PR automatically.

### For Non-Trivial Features

- Select one existing Task ID from the applicable component roadmap, or [`docs/ROADMAP.md`](docs/ROADMAP.md) only for an explicitly cross-component task; do not assign a whole phase as one implementation task.
- The Task entry is canonical in exactly one roadmap and contains scope, dependencies, acceptance criteria, and verification expectations. Component tasks are executed and reported from their own roadmap. `docs/ROADMAP.md` owns cross-component tasks and final integration/release validation; do not copy criteria into another roadmap or per-task file.
- If planned work is missing from the roadmap, add and review a Task entry there before implementation. Link a GitHub Issue if one exists.
- Create an optional `.ai/tasks/<TASK-ID>.md` or `.ai/plans/<TASK-ID>.md` only for substantial persistent context that cannot fit in the roadmap; link it from the Task entry and avoid duplicating acceptance criteria.

---

## Safe Editing Rules

- **Never modify `main` directly** for feature work. Create a branch.
- **Inspect before editing** — always read a file before modifying it.
- **Avoid concurrent file edits** — do not modify files that other agents are likely editing.
- **Preserve existing comments and documentation** unless explicitly instructed to change them.
- **Do not overwrite architectural decisions silently** — if you disagree with an ADR, document your concern; do not ignore it.
- **Do not delete files** unless explicitly instructed.
- **Do not rewrite files entirely** when a targeted edit suffices.

---

## Architecture Rules

- Follow the architecture documented in `docs/ARCHITECTURE.md`.
- Respect component boundaries:
  - `android/` — Android customer application
  - `backend/` — PHP REST API
  - `admin/` — Admin dashboard
- Communication between components happens **only through the REST API**.
- The API contract is defined in `docs/API_CONTRACT.md`.
- Do not introduce cross-component dependencies outside the API boundary.
- Consult `docs/decisions/` before making architectural changes.
- New architectural decisions must be proposed as ADRs in `docs/decisions/`.

### Android Visual Design

- All customer-app screens use the shared Material 3 XML/View system in `docs/DESIGN_SYSTEM.md`; do not mix unrelated visual styles between screens.
- Use the single app theme and shared color, typography, spacing, shape, elevation, and component tokens. Define or change tokens centrally and update the design-system document when approved.
- Prefer Material 3 components and established screen patterns for app bars, navigation, product cards, forms, actions, dialogs, and loading/empty/error/success states. Avoid ad hoc colors, dimensions, typography, custom controls, or one-off interaction patterns.
- Keep layouts responsive and accessible: minimum 48dp touch targets, scalable text, semantic labels, keyboard/focus support, and verified contrast.
- Before adding a new screen pattern or changing the visual language, check the design system and existing screens; extend shared patterns rather than creating a parallel style.

### Admin Web Visual Design

- Follow the shared tokens in `docs/DESIGN_SYSTEM.md` and page-level behavior/layout in `docs/SCREEN_SPECIFICATIONS.md` for every dashboard page.
- Reuse the same coral/neutral palette, Lato typography, spacing, shapes, status semantics, and responsive principles as the customer app. Use web navigation and tables appropriate to desktop; do not copy Android's bottom navigation into the dashboard.
- Keep dashboard pages limited to the first-release scope. Analytics, customer administration, notifications, transaction history, CMS, and wishlist are deferred unless a new product Task changes that scope.
- Ensure keyboard operation, persistent labels, visible focus, non-color-only status, and readable table/card behavior at supported narrow widths.

---

## Testing Rules

- **All new code must include tests** unless explicitly marked as a prototype.
- **Do not disable existing tests** to make your changes pass.
- **Do not skip tests** in CI configuration.
- **Do not mark tests as expected failures** without documenting the reason.
- Follow the testing strategy in `docs/TESTING.md`.
- Test naming convention: `test_<unit>_<scenario>_<expected>` or equivalent for the language.
- Follow the risk-based test levels and independent verification flow in `docs/TESTING.md`.

---

## Security Rules

- **Never commit secrets**, API keys, passwords, or credentials.
- **Never commit `.env` files** with real values.
- **Never disable security checks** to make a task pass.
- **Never log sensitive data** (passwords, tokens, PII).
- Use environment variables or secure vaults for configuration.
- Follow the security guidelines in `docs/SECURITY.md`.
- Validate all user input on the backend.
- Use parameterized queries — never concatenate SQL.

---

## Dependency Rules

- **Do not add dependencies without justification.**
- Prefer standard library solutions when reasonable.
- Document why a dependency is needed in the commit message or PR description.
- Pin dependency versions explicitly.
- Check dependencies for known vulnerabilities before adding.
- Do not add dependencies that duplicate existing functionality.

---

## Git Rules

### Remote Synchronization and Task Branch Lifecycle

The Git lifecycle below applies to Android and Admin tasks. Backend Git operations remain owner-managed: agents must not run any Git command for Backend unless explicitly authorized.

1. **At task start, before creating a branch:** run `git fetch --prune origin` and `git remote set-head origin -a`, then inspect `git status --short --branch`, `git branch -vv`, recent commits, and relevant remote refs. Create each new task branch from the latest `origin/develop`. For an existing shared task branch, confirm its upstream state before editing.
2. **Keep integration branches current only by fast-forward:** create a missing local `develop` with `git switch --track -c develop origin/develop`; update an existing clean `develop` or `main` with `git pull --ff-only origin <branch>`. Never commit or push directly from them. Never pull/rebase over uncommitted work; preserve it and reconcile deliberately.
3. **Before staging/committing:** fetch again and check whether the task branch's upstream moved. If it did, preserve the work in progress, integrate the newer commits deliberately, resolve conflicts, then review the diff and stage only task-scoped files. Do not include unrelated user changes.
4. **Before pushing:** fetch again and confirm the current branch and upstream. If GitHub deleted that task branch, check whether its commits are already in `origin/develop`: if merged, do not recreate or push it; if not merged, stop and ask the coordinator before restoring/recreating the branch. If the remote branch advanced, integrate its commits explicitly before retrying the push.
5. **After a task PR merges:** run `git fetch --prune origin`. Delete its local branch only after confirming it is merged and is not the current branch, using `git branch -d <branch>`. Never use `git reset --hard` or force-push as routine synchronization or cleanup.

If fetch, fast-forward, or push fails, capture the exact output, inspect divergence and branch protection, and resolve the cause; do not repeat an unchanged command without new evidence.

### Branches

Use this Gitflow-lite model:

- `main` contains only approved, completed releases. Never start a feature/task branch from it or merge a task branch directly into it.
- `develop` is the integration branch and the default base for phase PRs. Create it once from the current `main` baseline; keep it after releases.
- **Per-phase feature branches:** Create one branch from `develop` for each **Phase** of a component (Android, Backend, Admin, or Project), rather than creating branches per step. Use the work type, component, and phase number: `<type>/<component>-phase-<N>` (e.g. `feature/android-phase-1`, `feature/backend-phase-1`, `feature/admin-phase-1`). All steps within that phase live and evolve together within that single branch.
- **Steps as commits:** Individual steps within a phase are developed, verified, and committed sequentially as focused Conventional Commits directly within that phase branch: `<type>(<component>-pNN-sNN-<task-id>): <imperative summary>`.
- All sequential agents for a phase share that branch. After all steps of the phase pass build/test/debug/retest and review, merge its PR into `develop`; delete the phase branch after merge.
- When a phase/release is complete, run the release checks on `develop`. Promote a release only through a reviewed `develop` → `main` PR; tag the released commit. Never delete `develop`.
- If the repository has not yet established `develop`, the coordinator creates it once from `main`; do not create competing integration branches.

Branch names:

```
main                                  # Released versions only
develop                               # Shared integration branch
feature/<component>-phase-<N>         # Phase branches housing all steps of phase N
fix/<component>-phase-<N>
refactor/<component>-phase-<N>
chore/<component>-phase-<N>
docs/<component>-phase-<N>
```

### Commit Conventions

Use conventional commit prefixes:

```
feat:     New feature
fix:      Bug fix
refactor: Code refactoring (no behavior change)
test:     Adding or modifying tests
docs:     Documentation changes
chore:    Maintenance, tooling, config
build:    Build system changes
ci:       CI/CD changes
```

**Rules:**
- Keep commits focused and logically reversible.
- One logical change per commit.
- Write clear, descriptive commit messages.
- Use Conventional Commit scopes to identify the same work coordinates as the branch: `<type>(<component>-pNN-sNN-<task-id>): <imperative summary>`. Example: `chore(android-p00-s01-fnd-001): verify Android scaffold and toolchain`.
- Include the Task ID in the commit/PR title or description as applicable; reference a GitHub Issue if one exists. The branch and commit type must describe the actual change. Map `feature/` branches to the Conventional Commit type `feat`; other types use the same name where available (`fix`, `refactor`, `docs`, `chore`, `build`, or `ci`).

### Pull Requests

- Task branches target `develop`; only release PRs from `develop` target `main`.
- All changes to `develop` and `main` go through Pull Requests; no direct task commits to either branch.
- PRs must have a clear description of what changed and why.
- PRs must reference the roadmap Task ID; link a GitHub Issue when one exists.
- PRs must pass all CI checks before merging.
- PRs require at least one human review for non-trivial changes.
- Use the PR template at `.github/pull_request_template.md`.
- Delete short-lived task branches after their PR merges; retain `develop` and `main`.

---

## Human Approval Requirements

The following require explicit human approval before merging:

- Architectural changes (new ADRs)
- Database schema changes
- API contract changes
- Security-related changes
- Dependency additions or major version upgrades
- CI/CD pipeline changes
- Changes to `AGENTS.md` or component AGENTS files
- Changes to `main` or `develop` branch protection rules

---

## Agent Handoff Rules

When a task is blocked or work must be transferred outside the planned sequential role workflow, use this procedure. It does not apply to normal build → test → debug → retest transitions; those use local commits without a handoff file or push, as described above:

1. **Commit all work in progress** on the feature branch.
2. **Create a handoff file**: `.ai/handoffs/<TASK-ID>.md` containing:
   - Current status
   - What was completed
   - What remains
   - Known issues or blockers
   - Relevant file paths
   - Branch name
3. **Push the branch** so the next agent can access it.
4. **Do not leave uncommitted changes** in the working directory.

---

## Context Management

- Use `.ai/state/PROJECT_STATE.md` for high-level project status (updated periodically, not after every change).
- Use the applicable component roadmap as the execution and progress-reporting source for that component. Use `docs/ROADMAP.md` only for explicitly cross-component tasks and final project/release validation; its phase gates do not add backend/admin work to an Android assignment.
- Use `.ai/tasks/<TASK-ID>.md` or `.ai/plans/<TASK-ID>.md` only for linked supplementary context that would otherwise be lost or make the roadmap unwieldy.
- Use `.ai/reviews/<PR-ID>.md` for code review notes.
- **Do not create files in `.ai/` unless they contain genuinely useful persistent information.**
- **Do not use `.ai/` as a replacement for GitHub Issues or PRs.**

---

## Definition of Done

A code task is complete when:

- [ ] Code is implemented and follows project conventions.
- [ ] Tests are written and passing.
- [ ] No existing tests are broken.
- [ ] Code has been self-reviewed for obvious issues.
- [ ] Documentation is updated if behavior changed.
- [ ] API contract is updated if endpoints changed.
- [ ] Commit messages follow conventions.
- [ ] Branch is pushed and PR is created (or ready to create).
- [ ] No secrets, credentials, or sensitive data are committed.
- [ ] No linting errors or warnings are introduced.

For a documentation-only task, mark code/test checks as not applicable with a reason; review links, consistency, and scope instead.
