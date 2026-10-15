# Rule 07: Typography & UX Microcopy

Great visual design fails if the typography is illegible or the microcopy reads like robotic corporate jargon. Typography governs reading rhythm; microcopy guides human emotion.

---

## 1. Typographic Ergonomics

Adhering to Robert Bringhurst’s *The Elements of Typographic Style*:

1. **Optimal Measure (Line Length)**:
   - Body text line length must remain between **60 and 75 characters per line** (`max-w-prose` in Tailwind, $\approx 65\text{ch}$).
   - Anything over 85 characters causes fatigue as the eye struggles to find the next line start.
2. **Line Height (Leading)**:
   - Headings (large text): Tight line height (`leading-tight` or `leading-snug`, 1.1–1.25).
   - Body copy (small text): Spacious line height (`leading-relaxed`, 1.5–1.75) for effortless scanning.
3. **Preventing Widows & Orphans**:
   - Apply `text-wrap: balance` on all hero headlines (`<h1>`–`<h3>`) to equalize line widths automatically.
   - Apply `text-wrap: pretty` on paragraphs to prevent single dangling words on the final line.

```tsx
<h1 className="text-4xl sm:text-6xl font-bold tracking-tight text-white text-balance">
  Deterministic Infrastructure for High-Velocity Engineering
</h1>
<p className="mt-4 text-neutral-400 text-base sm:text-lg max-w-2xl leading-relaxed text-pretty">
  Deploy scalable microservices with cryptographic identity verification and automated zero-downtime rollouts.
</p>
```

---

## 2. Active Voice & Actionable Microcopy

Vercel Writing Standards mandate direct, user-focused language over abstract passive descriptions:

| Passive / System Jargon | Active User-Centric Alternative |
| :--- | :--- |
| "The deployment has been initiated" | **"Deploying your project..."** |
| "Webhook configuration interface" | **"Manage API webhooks"** |
| "Submit" / "Execute" | **"Save changes" / "Create project"** |
| "Invalid credential input detected" | **"Incorrect email or password. Please try again."** |
| "Click here to proceed" | **"View documentation"** |

---

## 3. The Banned Vocabulary List

Never use patronizing, condescending, or empty filler words in user interfaces:

- ❌ **"Simply click Settings"**: Never use *simply* or *just*. If the task is confusing to the user, calling it "simple" frustrates them.
  - ✅ *"Click Settings."*
- ❌ **"Easily integrate your database"**: Never promise *easy* or *quick*.
  - ✅ *"Connect your PostgreSQL database in two steps."*
- ❌ **"Obviously required"**: Never imply the user lacks common sense.
- ❌ **"Oops! Something went wrong"**: Cutesy error copy feels unprofessional during high-stress moments (like failed payments).
  - ✅ *"We could not process your card. Please check your card details and try again."*

---

## 4. Empathetic Error Message Anatomy

Every production error message must answer three questions:
1. **What happened?** (Clear, non-technical explanation).
2. **Why did it happen?** (Root cause without exposing stack traces).
3. **How does the user fix it?** (Concrete next step or action button).

```tsx
<div role="alert" className="rounded-2xl bg-rose-500/10 border border-rose-500/20 p-4">
  <div className="flex items-start gap-3">
    <AlertTriangleIcon className="w-5 h-5 text-rose-400 flex-shrink-0 mt-0.5" />
    <div className="space-y-1">
      <h4 className="text-sm font-semibold text-rose-200">Payment Authorization Failed</h4>
      <p className="text-xs text-rose-300/80 leading-relaxed">
        Your bank declined the transaction due to insufficient funds.
      </p>
      <div className="pt-2">
        <button className="text-xs font-semibold text-white underline hover:no-underline">
          Update payment method →
        </button>
      </div>
    </div>
  </div>
</div>
```
