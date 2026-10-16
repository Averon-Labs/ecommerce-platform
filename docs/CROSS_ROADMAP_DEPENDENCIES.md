# Cross-Roadmap Dependencies Summary

## Purpose

This document highlights the key cross-component dependencies where work in one roadmap **blocks** or is **required by** work in another roadmap. Use this to coordinate with your colleagues on Backend and Admin work.

## Critical Blocking Dependencies

### 🚨 FND-004 (Backend) — THE BIG BLOCKER — RESOLVED 2026-10-12

**Task:** Draft and review the OpenAPI contract (`docs/openapi.yaml`)

**Owner:** Backend team

**Blocks:**
- **ALL Android Phase 1 tasks** (AUTH-002, CAT-002)
- **ALL Android Phase 2 tasks** (CART-002, CHECK-001, ORD-002)
- Android DOM-002, NET-002 in Phase 0
- **ALL Backend Phase 1 implementation tasks** (AUTH-001, CAT-001, CAT-003)
- **ALL Backend Phase 2 implementation tasks** (CART-001, ORD-001)
- **ALL Admin Phase 1 tasks** (AUTH-003, CAT-004)
- **ALL Admin Phase 2 tasks** (ORD-003)

**Impact:** This is the **single most critical cross-component dependency**. Until the OpenAPI contract is accepted, no API integration work can start in any component. Android can work on Phase 0 tasks that don't need the contract (FND-006, UI-002, APP-002, DATA-002), but Phase 1 and 2 are completely blocked.

**Status:** ✅ **Accepted** — `docs/openapi.yaml` (OpenAPI 3.1) passed structural validation and was reviewed and accepted by the API owner on 2026-10-12 (FND-004 complete; recorded in `docs/API_CONTRACT.md`). This blocker is cleared for every component; any contract change is made in `openapi.yaml` first and re-accepted.

---

## Phase 0 Dependencies

### Android Dependencies on Other Teams

| Android Task | Requires from Backend | Requires from Project | Notes |
|--------------|----------------------|----------------------|-------|
| DOM-002 | **FND-004** complete | - | Needs OpenAPI contract to generate models |
| NET-002 | **FND-004** complete | - | Needs OpenAPI contract to define endpoints |
| FND-006 | - | - | Can proceed independently |
| UI-002 | - | DES-001 (screen specs) | Needs design decisions |
| APP-002 | - | - | Can proceed after UI-002 |
| DATA-002 | - | - | Can proceed independently |

### Backend Dependencies on Other Teams

| Backend Task | Requires | Notes |
|--------------|----------|-------|
| FND-002 | None | **Complete (2026-10-11, re-verified 2026-10-12):** Laravel 13 app + local MySQL 8.4 dev/test workflow (the hosted free service was deleted by its provider; a hosted MySQL 8+ TLS service remains a documented drop-in replacement); exact commands in `docs/DEVELOPMENT.md` |
| FND-004 | Product/API/DB/Security docs | Must complete before **anyone** does API work |

### Admin Dependencies on Other Teams

| Admin Task | Requires | Notes |
|------------|----------|-------|
| FND-003 | None | **Complete (2026-10-16):** React 19 + TypeScript 6 + Vite 8 admin app initialized with a committed lockfile, Vitest/React Testing Library tests, ESLint, and the documented Sanctum SPA origin; verification record in `docs/ADMIN_ROADMAP.md` |

---

## Phase 1 Dependencies

### Android Dependencies on Other Teams

| Android Task | Requires from Backend | Requires from Admin | Requires from Project | Notes |
|--------------|----------------------|---------------------|----------------------|-------|
| AUTH-002 | **FND-004** (contract) | - | **DES-001** (screens M-06—M-09, M-17) | Backend doesn't need AUTH-001 done thanks to mock strategy |
| CAT-002 | **FND-004** (contract) | - | **DES-001** (screens M-01—M-05) | Backend doesn't need CAT-001 done thanks to mock strategy |

### Backend Dependencies on Other Teams

| Backend Task | Requires from Admin | Notes |
|--------------|---------------------|-------|
| AUTH-001 | - | Independent |
| CAT-001 | - | Independent |
| CAT-003 | **AUTH-003** backend portion | Admin auth must work for admin catalog to authorize |

### Admin Dependencies on Other Teams

| Admin Task | Requires from Backend | Requires from Project | Notes |
|------------|----------------------|----------------------|-------|
| AUTH-003 | **FND-002**, **FND-004** | **FND-005**, **DES-001** | Backend must provide Sanctum auth endpoints |
| CAT-004 | **CAT-003** | **DES-001** | Backend admin catalog API must exist |

**⚠️ AUTH-003 is SHARED:** This task has both a backend component (admin auth API endpoints, role middleware) and an admin component (React login UI, session handling). Backend colleague implements the API portion, Admin colleague implements the UI portion.

---

## Phase 2 Dependencies

### Android Dependencies on Other Teams

| Android Task | Requires from Backend | Requires from Project | Notes |
|--------------|----------------------|----------------------|-------|
| CART-002 | **FND-004** (contract) | **DES-001** (screen M-10) | Backend doesn't need CART-001 done |
| CHECK-001 | **FND-004** (contract) | **DES-001** (screens M-11—M-14) | Backend doesn't need ORD-001 done |
| ORD-002 | **FND-004** (contract) | **DES-001** (screens M-15—M-16) | Backend doesn't need ORD-001 done |

### Backend Dependencies on Other Teams

| Backend Task | Requires | Notes |
|--------------|----------|-------|
| CART-001 | AUTH-001, CAT-001 | Both must be complete |
| ORD-001 | AUTH-001, CAT-001, CART-001 | All three must be complete |

### Admin Dependencies on Other Teams

| Admin Task | Requires from Backend | Requires from Project | Notes |
|------------|----------------------|----------------------|-------|
| ORD-003 | **ORD-001** | **DES-001** | Backend order API must exist |

---

## Dependency Graph

### Critical Path Visualization

```
Backend FND-004 (OpenAPI Contract)
       ↓
       ├─────────────────────────────────────┐
       ↓                                     ↓
  Backend Phase 1                       Android DOM-002, NET-002
  (AUTH-001, CAT-001)                         ↓
       ↓                                 Android Phase 1
  Backend CAT-003 ←───┐                 (AUTH-002, CAT-002)
       ↓              │                       ↓
  Backend AUTH-003 ──┘                  Android Phase 2
  (backend portion)                     (CART-002, CHECK-001, ORD-002)
       ↓
  Admin AUTH-003
  (UI portion)
       ↓
  Admin CAT-004
```

---

## Coordination Rules

### For Android Developer (You)

1. **Phase 0:** You can work on FND-006, UI-002, APP-002, DATA-002 independently.
2. **Wait for Backend FND-004** before starting DOM-002 or NET-002.
3. **Wait for Backend FND-004 + Project DES-001** before starting any Phase 1 or Phase 2 tasks.
4. **You do NOT need to wait** for Backend AUTH-001, CAT-001, CART-001, or ORD-001 to be complete — the mock server strategy allows you to proceed with fixtures.
5. **Integration testing** (QUAL-001) is when you'll finally test against the real backend.

### For Backend Developer (Your Colleague)

**Status 2026-10-15: ✅ Backend Roadmap Phases 0–3 are complete — no backend step remains open.** Historical execution order: FND-002 first, then FND-004 (the contract above, accepted 2026-10-12), then Phase 1 (AUTH-001, CAT-001, the AUTH-003 backend portion, CAT-003), then Phase 2 (CART-001, then ORD-001 — which unlocked Admin ORD-003), then the Phase 3 quality contributions (records in `docs/BACKEND_ROADMAP.md`). The only backend-adjacent item left is the commented backend CI job in `.github/workflows/ci.yml`, owned by cross-component FND-005 and gated on owner approval for CI/CD changes.

### For Admin Developer (Your Colleague)

1. **Phase 0:** FND-003 is complete (2026-10-16) — the React app is initialized and verified (record in `docs/ADMIN_ROADMAP.md`).
2. **Phase 1:** Wait for Backend FND-002, FND-004, and coordinate on AUTH-003 backend portion. Then wait for Backend CAT-003 before doing CAT-004.
3. **Phase 2:** Wait for Backend ORD-001 before starting ORD-003.

---

## Open Decisions That Affect Android

These decisions are listed in Android roadmap but may need input from Backend/Admin:

| ID | Question | Who Decides | Blocks |
|----|----------|-------------|--------|
| OPEN-1 to OPEN-4 | Contract decisions in openapi.yaml | Backend + Product Owner | FND-004 acceptance |
| OPEN-5 | Local mock server runner | Android + Backend | NET-002 |
| OPEN-6 | Token storage mechanism | Android | DATA-002 |
| OPEN-7 | Supporting-text token value | Android + Design | UI-002 |
| OPEN-8 | Lato weight for "600" | Android + Design | UI-002 |
| OPEN-9 | allowBackup for demo app | Android + Security | DATA-002 |
| OPEN-10 | Room deferred? | Android + Product Owner | DATA-002 |
| OPEN-11 | Lato font files placement | Android | UI-002 start |

---

## Task Assignment Pattern

### What You (Android Developer) Should Focus On

**Right now (Phase 0):**
- ✅ FND-001 (already complete)
- 🔨 FND-006 — Toolchain modernization (can start immediately)
- 🔨 UI-002 — Design tokens (can start after FND-006, needs DES-001 and design decisions)
- 🔨 APP-002 — App shell (can start after FND-006 + UI-002)
- 🔨 DATA-002 — Token storage (can start after FND-006, needs decisions)
- ⏸️ DOM-002 — Wait for Backend FND-004
- ⏸️ NET-002 — Wait for Backend FND-004 + DOM-002 + DATA-002

**Phase 1:**
- ⏸️ Wait for Backend FND-004 + Project DES-001
- Then: AUTH-002 and CAT-002 can run in parallel

**Phase 2:**
- ⏸️ Wait for Phase 1 Android complete
- Then: CART-002, CHECK-001, ORD-002 can run in parallel

### What Backend Colleague Should Focus On

**✅ Complete (2026-10-15) — Backend Roadmap Phases 0–3 all delivered:**
- Phase 0: FND-002, FND-004 (contract accepted 2026-10-12)
- Phase 1: AUTH-001, CAT-001, AUTH-003 backend portion, CAT-003
- Phase 2: CART-001, ORD-001 (unlocked Admin ORD-003)
- Phase 3: QUAL-001..QUAL-005 backend contributions (records in `docs/BACKEND_ROADMAP.md`; final verification 2026-10-15: 153 tests, 2062 assertions, 0 failures)

**Remaining backend-adjacent item:** enable the backend CI job template in `.github/workflows/ci.yml` (cross-component FND-005; CI/CD changes require owner approval).

### What Admin Colleague Should Focus On

**Right now (Phase 0):**
- ✅ FND-003 — React initialization (complete 2026-10-16; verification record in `docs/ADMIN_ROADMAP.md`)

**Phase 1:**
- ⏸️ Wait for Backend FND-002, FND-004
- 🔨 AUTH-003 UI portion (coordinate with Backend dev on backend portion)
- ⏸️ Wait for Backend CAT-003
- 🔨 CAT-004

**Phase 2:**
- ⏸️ Wait for Backend ORD-001
- 🔨 ORD-003

---

## Summary: What Blocks What

### Backend FND-004 Blocks:
- Android: DOM-002, NET-002, ALL Phase 1, ALL Phase 2
- Backend: ALL Phase 1, ALL Phase 2
- Admin: ALL Phase 1, ALL Phase 2

### Backend AUTH-001 Blocks:
- Backend: CART-001, ORD-001

### Backend CAT-001 Blocks:
- Backend: CAT-003, CART-001, ORD-001

### Backend AUTH-003 (backend portion) Blocks:
- Backend: CAT-003
- Admin: CAT-004

### Backend CAT-003 Blocks:
- Admin: CAT-004

### Backend CART-001 Blocks:
- Backend: ORD-001

### Backend ORD-001 Blocks:
- Admin: ORD-003

### Project DES-001 Blocks:
- Android: UI-002, AUTH-002, CAT-002, CART-002, CHECK-001, ORD-002
- Admin: AUTH-003, CAT-004, ORD-003

---

## Legend

- 🚨 **Critical blocker** — blocks multiple teams
- 🔨 **Can work on now** — prerequisites met
- ⏸️ **Blocked** — waiting on dependency
- ✅ **Complete** — done
