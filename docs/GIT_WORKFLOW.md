# Git Workflow Rules

## Universal Phase-Based Branching Model

Across the entire repository (Android, Backend, Admin, and Project-wide tasks):

- **One branch per phase:** Feature branches are created from `develop` **per phase**, not per individual step.
  - Branch naming: `<type>/<component>-phase-<N>`
  - Examples:
    - `feature/android-phase-1`
    - `feature/backend-phase-1`
    - `feature/admin-phase-1`
- **Steps as commits:** All steps within a given phase live and evolve together within that single branch. Each step is developed, verified, and committed sequentially as a focused Conventional Commit directly on the phase branch:
  - Commit format: `<type>(<component>-pNN-sNN-<task-id>): <imperative summary>`
  - Example: `feat(android-p01-s01-auth-002): customer authentication and profile`
- **PR Lifecycle:** When all steps of a phase are completed and verified, the phase branch is reviewed and merged into `develop` via a Pull Request.

---

## Component-Specific Git Responsibilities

### Android Component
- **Phase Branch:** Maintained per phase (`feature/android-phase-<N>`), created from `develop`.
- **AI Agent Authority:** Follows the agent Git workflow (phase branch management, sequential Conventional Commits per step, remote synchronization, push to origin, and PR preparation into `develop`). Never merge PRs automatically.

### Backend Component
- **Phase Branch:** Maintained per phase (`feature/backend-phase-<N>`), created by the owner from `develop`.
- **Git Authority:** Strictly owner-managed. Agents must not run any Git commands (read-only or mutating) unless specifically authorized as a one-off exception by the owner. Agents hand off verified working-tree state without commits.

### Admin Component
- **Phase Branch:** Maintained per phase (`feature/admin-phase-<N>`), created from `develop`.
- **Git Authority:** Follows the agent Git workflow (local commits per step, remote synchronization, and PR preparation).

---

## Branch Hierarchy Summary

```
main                                  # Released versions only
develop                               # Shared integration branch
feature/<component>-phase-<N>         # Phase branches housing all steps of phase N
fix/<component>-phase-<N>
refactor/<component>-phase-<N>
chore/<component>-phase-<N>
docs/<component>-phase-<N>
```
