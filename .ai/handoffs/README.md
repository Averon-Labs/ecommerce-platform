# .ai/handoffs/ — Agent Handoff Documentation

## Purpose

This directory contains handoff files created when an agent cannot complete a task. The handoff file provides context for the next agent or human who will continue the work.

## Naming Convention

```
<TASK-ID>.md
```

## Template

```markdown
# Handoff: <TASK-ID> — <Title>

## Branch
- `<branch-name>`

## Status
- What was completed:
  - ...
- What remains:
  - ...

## Known Issues
- ...

## Relevant Files
- ...

## Notes
- ...
```

## Guidelines

Create a handoff only when work is blocked or transferred outside the normal build → test → debug → retest sequence. Follow the [handoff rules in `AGENTS.md`](../../AGENTS.md) for required status, commit, and push details; the Component-Specific Git Ownership Override still applies — Backend tasks are committed and pushed by the owner (by an agent only when explicitly authorized), while Android and Admin agents manage their phase-branch Git. Remove obsolete handoff files when the transfer is complete.
