# .ai/plans/ — Implementation Plans

## Purpose

This directory contains implementation plans created by agents before starting complex tasks.

## Naming Convention

```
<TASK-ID>.md
```

## Template

```markdown
# Plan: <TASK-ID> — <Title>

## Approach
- ...

## Files to Create/Modify
- ...

## Steps
1. ...
2. ...

## Testing Plan
- ...

## Risks
- ...
```

## Active Implementation Plans

| Component | Plan Document | Scope & Focus |
| :--- | :--- | :--- |
| **Android** | [`ANDROID-PINTEREST-REDESIGN.md`](ANDROID-PINTEREST-REDESIGN.md) | Pinterest / MUSE visual redesign, Warm Ivory theme, floating nav, auth & catalog. |
| **Backend** | [`BACKEND-GOOGLE-AUTH-INTEGRATION.md`](BACKEND-GOOGLE-AUTH-INTEGRATION.md) | Google Sign-In token verification (`POST /api/v1/auth/google`), MySQL schema, and Sanctum bearer sessions (see the plan's implementation notes). Implemented and verified as AUTH-004. |
| **Admin** | [`ADMIN-DASHBOARD-INTEGRATION.md`](ADMIN-DASHBOARD-INTEGRATION.md) | Customer OAuth provider badges, order views, and Bento design system alignment. |

## Guidelines

- Create plans for non-trivial tasks before implementing.
- Keep plans concise and actionable.
- Update or remove plans as tasks are completed.

