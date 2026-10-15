---
name: grill-with-docs
description: Combined interview + domain modeling engine. Manually merged from mattpocock/skills (grilling + domain-modeling) and adapted to this project's own ADR format and Task-ID-driven workflow defined in AGENTS.md.
source: https://github.com/mattpocock/skills
adapted_for: eshop-verse (see AGENTS.md, docs/decisions/README.md)
---

# Grill With Docs — Project-Adapted Version

این فایل موتور مصاحبه (`grilling`) و انضباط مدل‌سازی دامنه (`domain-modeling`)
را با فرمت ADR و گردش‌کار Task-ID این پروژه (طبق AGENTS.md) ترکیب می‌کند.

## ⚠️ جایگاه این Skill در گردش‌کار پروژه

این Skill **جایگزین** سیستم Builder → Tester → Debugger یا روش Task-ID محور
توضیح داده‌شده در AGENTS.md نیست. این فقط یک **مرحله‌ی صفر** است، پیش از
ساخت یک Task Entry رسمی:
۱. ابهام یا تصمیم نامشخصی هست؟
   ↓
۲. این Skill را اجرا کن (مصاحبه + GLOSSARY.md + در صورت لزوم ADR)
   ↓
۳. حالا یک Task Entry رسمی در roadmap مربوطه بساز
   ↓
۴. گردش‌کار استاندارد AGENTS.md (Builder/Tester/Debugger) شروع می‌شود

وقتی به ایجنت این فایل داده می‌شود، بگویید:
"Follow the Grill With Docs instructions in this file. Start the interview about <topic>."

> **مهم:** در حین اجرای یک تسکِ مشخص‌شده، هرگز جلسه‌ی مصاحبه‌ی طولانی (Grilling) راه نیندازید.
> طبق AGENTS.md فقط یک سوال مشخص و متمرکز با پیشنهاد پیش‌فرض بپرسید.

---

## PART 1 — Interview Engine

Interview the user relentlessly until you reach a shared understanding. Map this as a **design tree**: every decision branches into the decisions that hang off it.

Work the tree in **rounds**. The **frontier** is every decision whose prerequisites are already settled: the questions you can ask _now_ without guessing at answers you haven't heard yet. Ask the whole frontier in one round: number each question and give your recommended answer. Then wait for the user's answers before the next round.

Format a round like so:
```text
❓ Q1 - <question title>: <question body, might be multiple paragraphs, including multiple choices>

➡️ <your recommended answer>

❓ Q2 - <question title>: <question body, might be multiple paragraphs, including multiple choices>

➡️ <your recommended answer>
```

Word each question so "yes" accepts your recommended answer.

Each round the user answers reshapes the tree: settled decisions push the frontier outward and unblock questions that depended on them. Recompute the frontier and ask the next round. A question whose answer depends on another question still open in this round belongs to a _later_ round, not this one.

Finding _facts_ is your job, never the user's. When a frontier question needs a fact from the environment (filesystem, tools, etc.), dispatch a sub-agent to find it (or inspect the repo yourself); don't ask the user for anything you could look up yourself. Don't block on it: a running exploration is an unsettled prerequisite, so only the questions downstream of it wait for it to report; ask the rest of the frontier now. The _decisions_ are the user's: put each to them and wait.

The session is done when the frontier is empty: every branch of the design tree visited, nothing left silently assumed. Do not act on it until the user confirms you have reached a shared understanding.

---

## PART 2 — Domain Modeling Discipline

Actively build and sharpen the project's domain model as you design. This is the *active* discipline: challenging terms, inventing edge-case scenarios, and writing the glossary and decisions down the moment they crystallise.

### File structure (this project)
```text
/
├── GLOSSARY.md ← single, root-level glossary (all components for now)
├── docs/
│   ├── decisions/ ← ADRs live here, NOT docs/adr/
│   │   ├── ADR-001-....md
│   │   ├── ADR-002-....md
│   │   └── README.md ← format spec, do not duplicate its content elsewhere
│   └── DECISIONS.md ← index of all ADRs — MUST be updated when a new ADR is added
└── android / backend / admin / ...
```

If the team later decides components need separate glossaries, introduce a `GLOSSARY-MAP.md` at the root and split into per-component `GLOSSARY.md` files. Do not do this unprompted — ask first if it seems warranted.

Create `GLOSSARY.md` lazily: only when the first term is resolved.

### During the session

**Challenge against the glossary.** When the user uses a term that conflicts with the existing language in `GLOSSARY.md`, call it out immediately.

**Sharpen fuzzy language.** Propose a precise canonical term when the user uses vague or overloaded terms.

**Discuss concrete scenarios.** Stress-test domain relationships with specific edge-case scenarios.

**Cross-reference with code.** Check whether the code agrees with what the user states. Surface contradictions.

**Update GLOSSARY.md inline.** Capture resolved terms the moment they crystallise. Use the format below.

`GLOSSARY.md` should be totally devoid of implementation details. It is a glossary and nothing else.

**Offer ADRs sparingly.** Only when all three are true:
1. **Hard to reverse**
2. **Surprising without context**
3. **The result of a real trade-off**

If any is missing, skip the ADR.

---

## PART 3 — GLOSSARY.md Format

### Structure

```md
# EShop Verse

Shared domain vocabulary across Android, Backend, and Admin components.

## Language

**Order**:
A customer's confirmed request to purchase one or more products.
_Avoid_: Purchase, transaction

**Customer**:
A person who places orders through the Android app.
_Avoid_: Client, buyer, account
```

### Rules
- Be opinionated. Pick the best term, list others under _Avoid_.
- Keep definitions tight. One or two sentences. Define what it IS, not what it does.
- Only project-specific terms. General programming concepts don't belong.
- Group under subheadings when natural clusters emerge.

---

## PART 4 — ADR Format (this project's official format — from docs/decisions/README.md)

⚠️ ADRs go in `docs/decisions/`, not `docs/adr/`. This overrides any generic ADR convention from other sources.

### File naming
```text
docs/decisions/ADR-NNN-short-title.md
```
Sequential numbering: ADR-001, ADR-002, etc. Never reuse numbers, even for deprecated decisions. Scan `docs/decisions/` for the highest existing number and increment by one.

### Template

```markdown
# ADR-NNN: Short Title

## Status

PROPOSED | ACCEPTED | DEPRECATED | SUPERSEDED by ADR-XXX

## Date

YYYY-MM-DD

## Context

What is the issue or question that prompted this decision?

## Options Considered

### Option A: ...
- Pros: ...
- Cons: ...

### Option B: ...
- Pros: ...
- Cons: ...

## Decision

What was decided and why.

## Consequences

- What becomes easier or harder as a result.
- What follow-up actions are needed.
```

### Status during a grilling session
Always create a new ADR with `Status: PROPOSED`. Per AGENTS.md, new architectural decisions require explicit human approval before being treated as final. Do not mark an ADR as ACCEPTED yourself — tell the user it is pending their approval.

### Mandatory index update
After creating or changing an ADR, update the index in `docs/DECISIONS.md` to add/reflect the new entry. An ADR that is not indexed there is considered incomplete.

### When to offer an ADR
All three must be true:
- **Hard to reverse**: the cost of changing your mind later is meaningful
- **Surprising without context**: a future reader will wonder "why did they do it this way?"
- **The result of a real trade-off**: there were genuine alternatives and you picked one for specific reasons

### What qualifies
- Architectural shape (monorepo structure, event-sourcing, etc.)
- Integration patterns between Android / Backend / Admin
- Technology choices with lock-in (database, auth, deployment target)
- Boundary/ownership decisions (which component owns which data)
- Deliberate deviations from the obvious path
- Constraints not visible in the code (compliance, SLAs, partner contracts)
- Non-obvious rejected alternatives

---

## PART 5 — Output Checklist (end of session)

Before ending a grill-with-docs session, confirm:

- [ ] `GLOSSARY.md` updated inline for every resolved term (if any)
- [ ] Any new ADR created in `docs/decisions/` using the exact template above
- [ ] New ADR has `Status: PROPOSED` (never self-approve)
- [ ] `docs/DECISIONS.md` index updated if an ADR was added
- [ ] No source code, configuration, or roadmap files were modified
- [ ] User explicitly confirmed shared understanding before the session ends
