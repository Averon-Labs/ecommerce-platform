---
name: handoff
description: Maintain a verified project handoff so coding agents can resume work without repeated discovery, false assumptions, or scope violations.
---

# HANDOFF Skill

Use at the start/end of substantial work, when switching agents, resuming after a break, or when asked for project status.

## Non-negotiable safeguards
- Work in the local working copy only.
- Never push to GitHub or any remote. Do not run `git push`, `git pull`, `git fetch`, remote-mutating commands, or GitHub write/API actions. Do not change remotes, remote branches, issues, PRs, or GitHub settings.
- Do not create commits unless explicitly requested.
- Never run destructive commands such as `git reset --hard`, `git clean`, force checkout, or broad deletion to resolve workspace issues.
- Preserve pre-existing modified and untracked files. Inspect status and relevant diffs before editing.
- Respect project instructions and task scope; do not touch unrelated areas.
- Never store passwords, API keys, tokens, private keys, cookies, recovery codes, or other secret values in handoff notes.
- Never claim a file was inspected, a test was run, a build passed, or a change was made unless it actually happened.

## Start-of-session procedure
1. Read root `HANDOFF.md`, if present.
2. Locate and read applicable instructions and docs, such as `AGENTS.md`, `CLAUDE.md`, `README.md`, architecture docs, contribution rules, and relevant roadmaps. Only report files that exist and were actually read.
3. Establish the project root and inspect local state using safe, read-only operations. Prefer `git status --short --branch`, `git diff --stat`, and relevant `git diff`. Do not contact remotes.
4. Identify pre-existing modifications and untracked files before editing; preserve them.
5. Verify handoff claims against code, docs, local diffs, and test evidence. Treat handoff notes as a guide, not unquestionable truth.
6. Identify current task, scope, exclusions, acceptance criteria, dependencies, and risks.
7. Record discrepancies between the handoff and repository evidence. Follow applicable instruction precedence and ask if consequential ambiguity remains.
8. Resume from the highest-priority incomplete action; do not repeat completed work unless verification finds a defect.

## During work
- Keep a factual record of meaningful file changes, decisions, blockers, and checks.
- For each changed file, track path, purpose, summary, and verification status.
- Record useful failed attempts: command/action, exact error or observed behavior, lesson learned, and whether the cause is confirmed or only suspected.
- Run relevant tests, lint, type checks, builds, or manual checks when feasible. Use `PASS`, `FAIL`, `BLOCKED`, or `NOT RUN`; include exact commands and concise results.
- Code written does not automatically mean a feature is complete. Verify acceptance criteria.
- Do not paste huge logs or whole source files into the handoff. Include short error excerpts and paths instead.
- Do not broaden scope just to improve documentation.

## End-of-session procedure
1. Review local status and relevant diffs again using read-only operations.
2. Update root `HANDOFF.md` from verified evidence and actual work.
3. Keep **active files** separate from **files changed this session**; active files may be unchanged.
4. Record completed/incomplete work, checks and results, useful failed attempts, bugs, blockers, decisions, and the next actionable task.
5. Distinguish `Complete`, `In progress`, `Planned`, `Blocked`, `Unverified`, and `Not started`.
6. Preserve useful history. Replace stale current-state claims only after verification. If too long, move older sessions to a separate local history file and link it; do not discard unresolved issues or lessons.
7. Do not commit or publish the handoff. Leave it local.
8. Report changed files, checks and results, blockers, and next step. Never say the handoff was updated unless it was actually written and checked.

## Required HANDOFF.md coverage
Maintain the sections in the project template, adapted to actual evidence:
- project goal, users/workflow, architecture, and entry points
- applicable instructions, allowed scope, exclusions, and constraints
- current task and acceptance criteria
- current state/completion matrix
- active files and their purpose
- files changed this session and why
- exact tests/builds/checks and outcomes
- failed attempts, exact errors, and lessons learned
- bugs, blockers, risks, open questions, and workarounds
- important decisions and rationale
- environment/dependencies/configuration (no secret values)
- local branch/status, pre-existing changes, and untracked files
- prioritized next steps, compact session history, source-of-truth docs

## Quality rules
- Never guess the goal, architecture, branch, files, test status, or next task. Use `Unknown — verify` when evidence is missing.
- Put quick orientation first: goal, task, state, constraints, next action.
- Prefer paths, symbols, exact commands, concise error text, and acceptance criteria over vague summaries.
- Reference README/docs/roadmaps by repository-relative path; do not duplicate them wholesale.
- Record dates only when the environment supplies reliable date/time.
- Do not manufacture a session entry when no meaningful work occurred.
- If `HANDOFF.md` is absent, create it from the provided template. If it exists, read and carefully update it; never replace useful history with a blank template.
- Clearly mark unknowns and state what must be inspected next.

## Safe local Git inspection examples
Allowed read-only examples: `git status --short --branch`, `git diff --stat`, `git diff -- path/to/file`, `git log -5 --oneline` (local history only). Do not query or mutate remotes.

## Invocation
Start: “Use the handoff skill. Read `HANDOFF.md` and applicable project instructions, verify local state and key claims, then resume the highest-priority incomplete task. Work locally only; do not modify GitHub or remotes.”

End: “Use the handoff skill to update `HANDOFF.md` from verified changes and actual test results. Preserve local changes and useful history. Do not commit or publish anything.”
