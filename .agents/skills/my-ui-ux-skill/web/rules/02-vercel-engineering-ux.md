# Rule 02: Vercel Engineering & UX Quality Standards

A beautiful interface is broken if clicking a button triggers layout shift, if users cannot paste passwords, or if keyboard navigation is invisible. This rulebook brings Vercel's rigorous web interface standards directly into your React codebase.

---

## 1. Zero Cumulative Layout Shift (CLS < 0.1)

Layout shift occurs when visual elements jump while images, fonts, or dynamic data load asynchronously.

### Mandatory Rules for Visual Stability
1. **Always declare explicit aspect ratio or dimensions on media**:
   ```tsx
   {/* WRONG: Triggers layout shift upon image download */}
   <img src="/avatar.jpg" className="w-12 rounded-full" />

   {/* CORRECT: Reserves exact space in the layout tree before bytes arrive */}
   <img src="/avatar.jpg" width={48} height={48} className="w-12 h-12 rounded-full aspect-square object-cover" />
   ```
2. **Skeleton screens must match the exact dimensions of final content**:
   ```tsx
   // Skeleton placeholder with precise height and rounded corner match
   <div className="h-6 w-32 bg-neutral-800 animate-pulse rounded-md" />
   ```
3. **Use `font-display: swap`** to prevent Flash of Invisible Text (FOIT).

---

## 2. Keyboard Focus States & Interaction Integrity

### The Focus-Visible Mandate
Never remove outlines with `outline-none` or `focus:outline-none` without a high-visibility `:focus-visible` ring:

```tsx
// Standard Accessible Focus Ring in Tailwind
const FOCUS_RING_CLASSES = "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2 focus-visible:ring-offset-neutral-950";
```

### Key Focus Rules
- Use `:focus-visible` instead of `:focus` so mouse clicks do not leave distracting rings on buttons.
- Use `:focus-within` on compound input containers so the outer border illuminates when the inner field is active.
- Ensure sticky headers, floating notifications, or banners never occlude or cover focused form fields.

---

## 3. Tabular Numbers for Data, Currencies & Timers

Default proportional fonts vary character widths (e.g., "1" is narrower than "8"). When counters, stock prices, or countdowns tick, proportional numbers jitter and jump horizontally.

```tsx
// Mandatory for all pricing cards, tables, dashboards, and timers
<span className="font-mono tabular-nums text-2xl font-bold">
  $2,481.50
</span>
```
In CSS/Tailwind: always apply `tabular-nums` (`font-variant-numeric: tabular-nums`).

---

## 4. Defensive Layout & Text Truncation

Long URLs, email addresses, or international localized strings can blow out flexible containers unless defensive styles are in place:

```tsx
{/* CRITICAL: min-w-0 on flex items is required to allow child truncation */}
<div className="flex items-center gap-3 min-w-0">
  <Avatar />
  <div className="min-w-0 flex-1">
    <p className="text-sm font-medium text-white truncate">
      user_with_an_exceptionally_long_email_address_here@domain.com
    </p>
    <p className="text-xs text-neutral-400 line-clamp-2">
      Multi-line descriptions should be clamped cleanly rather than breaking layout bounds.
    </p>
  </div>
</div>
```

---

## 5. Unblocking Native Browser Primitives

Never break the web's fundamental user expectations:

1. **NEVER prevent paste in input fields**: Never attach `onPaste={(e) => e.preventDefault()}` on password or credit card fields. Users must be able to use password managers.
2. **Always use real `<a>` or `<Link>` tags for navigation**: Never use `<div onClick={() => router.push(...)}>`. This destroys middle-click (open in new tab), Cmd/Ctrl+Click, and right-click "Copy link address".
3. **URL as the Single Source of Truth**:
   Tabs, search filters, pagination, and modal dialog states should be synced with URL query parameters so users can bookmark and share exact views.
4. **Kill Mobile Tap Delay**:
   Always apply `touch-action: manipulation` to interactive controls to eliminate the mobile browser 300ms double-tap delay.
