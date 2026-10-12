# .ai/ — AI Agent Workspace

## Purpose

This directory provides structured workspace areas for AI coding agents. It is **not** a replacement for GitHub Issues, Pull Requests, or Git history.

---

## Directory Structure

```
.ai/
├── README.md           # This file
├── ENVIRONMENT_VERIFICATION_TODO.md
├── state/              # High-level project state
│   └── PROJECT_STATE.md
├── tasks/              # Task-specific context and specifications
│   └── README.md
├── plans/              # Implementation plans for tasks
│   └── README.md
├── reviews/            # Code review notes and findings
│   └── README.md
└── handoffs/           # Agent handoff documentation
    └── README.md
```

---

## Responsibilities

### `state/`
- Contains `PROJECT_STATE.md` — a periodic high-level summary of project status.
- Updated occasionally (not after every commit).
- Provides quick context for agents starting a new session.

### `tasks/`
- Task-specific files: `.ai/tasks/<TASK-ID>.md`
- Contains supplementary requirements and context for specific tasks; acceptance criteria live only in the component roadmaps (do not copy them here).
- References the corresponding GitHub Issue when one exists.
- **Not a replacement for GitHub Issues** — use Issues for tracking, use these files for detailed context that helps agents.

### `plans/`
- Implementation plans for tasks: `.ai/plans/<TASK-ID>.md`
- Contains the agent's planned approach before implementation.
- Useful for complex tasks requiring design before coding.

### `reviews/`
- Code review notes: `.ai/reviews/<PR-ID>.md`
- Contains findings from code review, suggestions, and decisions.
- Supplements (does not replace) GitHub PR review comments.

### `handoffs/`
- Agent handoff documentation: `.ai/handoffs/<TASK-ID>.md`
- Created when an agent cannot complete a task and another agent (or human) will continue.
- Contains: current status, completed work, remaining work, known issues, branch name, relevant file paths.

---

## Guidelines

- **Do not create files unless they contain useful persistent information.**
- **Do not use this directory as a project management tool** — use GitHub Issues for that.
- **Keep files focused and concise** — avoid dumping entire conversation histories.
- **Name files with task or PR identifiers** for easy cross-referencing.
- **Clean up obsolete files** when tasks are completed and merged.
