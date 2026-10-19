# Architecture Decision Records — Index

## Purpose

This file indexes all Architecture Decision Records (ADRs) for the project.

ADRs document significant technical decisions. Each ADR is a separate file in `docs/decisions/`.

---

## Conventions

See [docs/decisions/README.md](decisions/README.md) for ADR conventions.

---

## Decision Log

| ID | Title | Status | Date |
|----|-------|--------|------|
| [ADR-001](decisions/ADR-001-portfolio-stack.md) | Portfolio stack and first-release boundaries | ACCEPTED | 2026-10-06 |
| [ADR-002](decisions/ADR-002-shared-visual-direction.md) | Shared coral-and-neutral visual direction | ACCEPTED (updated 2026-10-19) | 2026-10-07 |
| [ADR-003](decisions/ADR-003-secure-token-storage.md) | Secure token storage mechanism | ACCEPTED | 2026-10-10 |
| [ADR-004](decisions/ADR-004-defer-room-persistence.md) | Defer Room local persistence for initial release | ACCEPTED | 2026-10-10 |
| [ADR-005](decisions/ADR-005-local-mock-api-runner.md) | Local mock API server runner | ACCEPTED | 2026-10-10 |
| [ADR-006](decisions/ADR-006-in-app-offline-mock-interceptor.md) | In-App OfflineMockInterceptor for Android Independent Development | PROPOSED | 2026-10-15 |
| [ADR-007](decisions/ADR-007-dual-authentication-strategy.md) | Dual Authentication Strategy: Stateful SPA Sessions and Revocable Mobile Bearer Tokens | PROPOSED | 2026-10-15 |

---

## How to Add a Decision

1. Create a new file: `docs/decisions/ADR-NNN-short-title.md`
2. Use the ADR template in `docs/decisions/README.md`.
3. Add an entry to the table above.
4. Submit via Pull Request.
