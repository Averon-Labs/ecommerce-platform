# Build Workflow

Use this guide only when assigned the builder stage for a roadmap task. The shared entry point is [`../../AGENTS.md`](../../AGENTS.md); the canonical task entry and its linked project guidance define what to build.

## Entry

- The coordinator has assigned the Task ID or roadmap, phase, and step, its scope and acceptance criteria, and the current task-branch commit.
- Resolve material ambiguities before implementing; do not guess at missing requirements.
- Read only the role guide for this stage. Use the task-linked technical guidance, component instructions, [`../../docs/DEVELOPMENT.md`](../../docs/DEVELOPMENT.md) for verified commands, and [`../../docs/TESTING.md`](../../docs/TESTING.md) for test design when adding tests. Do not read the test or debug workflow guide unless assigned that role.

## Work

- Implement only the assigned scope on the existing task branch. Do not broaden the task or edit unrelated work.
- Add or update tests for changed behavior. Use verified commands and task-specific verification expectations; never invent a command or hide a failure.
- Run the checks appropriate to the task and report any unrun check with its reason. Follow the task-start and pre-commit checkpoints in the [canonical Git synchronization lifecycle](../../AGENTS.md#remote-synchronization-and-task-branch-lifecycle); create a focused local commit on the shared task branch and report its SHA. Do not publish a separate branch or push between normal stages.
- **Component scope of these Git steps** comes from the Component-Specific Git Ownership Override in AGENTS.md and never widens it: Android and Admin agents run the checkpoints and commit as written, on the phase branch (`<type>/<component>-phase-<N>`); Backend agents run **no Git commands** (owner-managed, unless the owner explicitly authorizes a specific operation for one task) and hand off the verified working tree without committing.

## Handoff to test

Stop editing before the independent tester begins. Report the Task ID, branch and commit SHA, changed files, acceptance criteria addressed, assumptions or unresolved issues, and exact commands with outcomes. Give the tester the task's named checks and any reproducible failure evidence. Do not claim completion while a required check fails.
