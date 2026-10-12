# Conditional Debug Workflow

Use this guide only when the independent tester has supplied evidence of a reproducible failure. The shared entry point is [`../../AGENTS.md`](../../AGENTS.md); the canonical task entry defines the permitted fix scope.

## Entry

- A failure is reproducible and its tested commit, exact command/input, environment, and observed result have been handed off.
- Be a third, distinct agent from both the builder and the independent tester. Work on the same task branch after the previous agent has stopped editing.
- Read the task-linked technical guidance, component instructions, [`../../docs/TESTING.md`](../../docs/TESTING.md), and only the debug workflow guide for role-specific procedure.

## Reproduce and diagnose

- Inspect the current commit and existing attempt record. Reproduce the reported failure before changing code.
- For each approach, record the code state or commit, hypothesis, exact command/input, observed result, and whether the evidence supports or rejects the hypothesis.
- Do not repeat an identical command against unchanged code, inputs, and environment unless new evidence changes the hypothesis. After three distinct unsuccessful hypotheses, stop and report the failure and evidence to the coordinator for a decision.

## Fix and retest handoff

- Make the smallest in-scope root-cause fix and add a regression test, following [`../../docs/TESTING.md`](../../docs/TESTING.md). Do not edit unrelated behavior or weaken a check.
- Follow the pre-commit checkpoint in the [canonical Git synchronization lifecycle](../../AGENTS.md#remote-synchronization-and-task-branch-lifecycle); create a focused local fix commit on the shared task branch and do not push during the normal transition.
- **Component scope of these Git steps** comes from the Component-Specific Git Ownership Override in AGENTS.md and never widens it: Android and Admin agents create the focused fix commit as written on the phase branch; Backend debuggers run **no Git commands** — they hand off the fixed working tree without committing (unless the owner explicitly authorizes one).
- Report the fix, changed files, fix commit SHA, regression test, exact commands and outcomes, and the complete attempt record. Hand the branch back to the independent tester for retesting; do not claim success until the required checks pass.
