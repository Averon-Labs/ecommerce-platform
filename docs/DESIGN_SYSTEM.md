# Design System

## Purpose

This document defines the shared visual language for the Android customer app and React admin dashboard, governed by the unified design intelligence in [`.agents/skills/my-ui-ux-skill`](../.agents/skills/my-ui-ux-skill/SKILL.md). Android uses XML Views, Material 3, and modern mobile patterns; the web dashboard uses semantic, responsive React and Tailwind with Vercel engineering standards. Page-by-page composition and behavior are in [`SCREEN_SPECIFICATIONS.md`](SCREEN_SPECIFICATIONS.md).

The first release is English-only, en-US, and USD. Store customer-facing Android copy in string resources and web copy in the app's central message/constants layer so the UI can be localized later without layout rewrites.

The visual direction fuses the owner's supplied e-commerce reference (bright coral highlights, clean surfaces, near-black text, Lato typography) with modern **Dribbble visual craft (asymmetric Bento grids, frosted glassmorphism, micro-border depth, pill buttons)** and strict **WCAG 2.2 AA accessibility (4.5:1 text contrast, 48dp Android / 44px Web touch targets, zero layout shifts)**. See [ADR-002](decisions/ADR-002-shared-visual-direction.md) and `.agents/skills/my-ui-ux-skill/`.

Keep the tokens below synchronized across Android resources and web CSS variables. Change them centrally through a reviewed design update; never introduce unrelated per-screen palettes.

## Visual Consistency Rules

- This is the single visual language for every Android and admin screen. New screens extend the patterns and tokens here and in `SCREEN_SPECIFICATIONS.md`; they do not introduce an unrelated screen-specific design language.
- Apply a reference layout cue only to an existing project screen with a matching role. For other screens, use shared tokens and components while preserving the content hierarchy and flow defined by the product and roadmap. Do not add pages, controls, data, or behavior just because they appear in the reference.
- Build Android UI with XML Views, Material 3 components, and modern patterns from `.agents/skills/my-ui-ux-skill/android/`. Use the shared app theme and Android resources for colors, type, dimensions, shapes, and component styles. Support both Light and deep Dark / OLED modes with full tonal parity.
- Build admin UI with semantic, responsive React web patterns and Tailwind from `.agents/skills/my-ui-ux-skill/web/`. Use shared CSS variables/design tokens for color, type, spacing, shape, elevation, and focus styles; do not imitate Android's bottom navigation in the desktop dashboard.
- Do not hardcode color, spacing, corner radius, text appearance, or elevation in individual screens. A deliberate token change updates this document and the shared Android/web resources together.
- Use Material 3 components and shared patterns for Android app bars, bottom navigation, buttons, fields, cards, dialogs, snackbars, and progress. Use consistent web navigation, buttons, fields, tables, cards, dialogs, and status badges in the dashboard.
- Every data-driven screen defines loading, content, empty, error/retry, and success feedback as applicable. Reuse consistent layout, wording, icon treatment, and action placement across the app or dashboard.
- Prefer clear hierarchy, restrained decoration, readable typography, consistent alignment and whitespace, and purposeful product imagery. Avoid charts or decoration that competes with content or creates inconsistent density.
- Support small and large screens, system font scaling, accessible contrast, minimum 48dp Android targets / 44 CSS px web targets, semantic labels, TalkBack, keyboard navigation, visible focus, and usable error text.
- Before implementing a screen, inspect existing screens and both design documents. Extend a shared component or pattern when possible. Introduce a new reusable pattern only for a real product need and document it here.

### XML Implementation Checklist

For each new or materially changed screen, verify during implementation/review that:

1. The root and child views use the common Material 3 app theme and shared styles.
2. All user-visible strings come from string resources; colors and dimensions come from shared resources.
3. Material 3 components are used for standard interactions instead of custom lookalikes.
4. Screen structure, spacing, typography, and state handling match existing screens and this system.
5. Touch targets, contrast, content descriptions, font scaling, and keyboard/focus behavior are addressed.

---

## Color System

**Status: ACCEPTED — Light and Dark/OLED palettes derived from owner reference and `my-ui-ux-skill`**

The palette is rooted in vibrant coral primary actions, crisp surfaces, near-black or deep-slate contrast text, and subtle pastels for merchandising. It implements the full 29 Material 3 semantic roles across Light, Dark, and OLED Dark themes.

### Material 3 Light Roles

| Role token | Value | Use |
|------------|-------|-----|
| Primary / on-primary | `#F7767E` / `#161616` | Coral actions and their foregrounds |
| Primary container / on-primary-container | `#FFF0F1` / `#8D2531` | Selected surfaces and soft coral emphasis |
| Secondary / on-secondary | `#7651B5` / `#FFFFFF` | Secondary emphasis, lavender accent |
| Secondary container / on-secondary-container | `#EEE7F7` / `#392653` | Soft lavender chips and selected surfaces |
| Tertiary / on-tertiary | `#287E87` / `#FFFFFF` | Aqua emphasis and promo accents |
| Tertiary container / on-tertiary-container | `#DDF3F4` / `#164E53` | Soft aqua surfaces |
| Error / on-error | `#B3261E` / `#FFFFFF` | Destructive and error emphasis |
| Error container / on-error-container | `#FCEEEE` / `#8C1D18` | Error messages and fields |
| Background / on-background | `#F5F6F7` / `#161616` | App canvas and default text |
| Surface / on-surface | `#FFFFFF` / `#161616` | Cards, sheets, fields, dialogs, and text |
| Surface variant / on-surface-variant | `#F1F1F3` / `#6B6B6B` | Supporting surfaces and secondary text |
| Surface containers (lowest → highest) | `#FFFFFF`, `#FAFAFB`, `#F5F6F7`, `#EFF0F2`, `#E7E8EA` | Nested neutral surface hierarchy |
| Outline / outline variant | `#767676` / `#D8D8DE` | Strong control boundary / decorative hairline divider |
| Surface tint | `#F7767E` | Material tonal elevation tint |
| Inverse surface / inverse-on-surface | `#28242C` / `#F7F1FA` | Temporary inverse surfaces such as snackbars |
| Inverse primary | `#FFB6BC` | Primary emphasis on inverse surfaces |
| Scrim | `#000000` | Modal dimming, with component-provided opacity |

### Material 3 Dark & Deep OLED Roles

| Role token | Dark Value | OLED Value | Use |
|------------|------------|------------|-----|
| Primary / on-primary | `#FFB3B8` / `#5F121C` | `#FFB3B8` / `#5F121C` | Luminous coral in dark mode |
| Primary container / on-primary-container | `#8D2531` / `#FFDADF` | `#8D2531` / `#FFDADF` | Dark coral container |
| Secondary / on-secondary | `#D0BCFF` / `#381E72` | `#D0BCFF` / `#381E72` | Luminous lavender |
| Secondary container / on-secondary-container | `#4F378B` / `#EADDFF` | `#4F378B` / `#EADDFF` | Dark lavender container |
| Tertiary / on-tertiary | `#7BD0D9` / `#00363B` | `#7BD0D9` / `#00363B` | Luminous aqua |
| Tertiary container / on-tertiary-container | `#1F4E53` / `#97ECEF` | `#1F4E53` / `#97ECEF` | Dark aqua container |
| Error / on-error | `#FFB4AB` / `#690005` | `#FFB4AB` / `#690005` | Dark mode error indicator |
| Error container / on-error-container | `#93000A` / `#FFDAD6` | `#93000A` / `#FFDAD6` | Dark error container |
| Background / on-background | `#121212` / `#E2E2E6` | `#000000` / `#F0F0F4` | Pitch black OLED canvas |
| Surface / on-surface | `#18181A` / `#E2E2E6` | `#0D0D0E` / `#F0F0F4` | Dark mode cards and sheets |
| Surface variant / on-surface-variant | `#2D3037` / `#C4C6D0` | `#1A1C20` / `#C4C6D0` | Dark surface variant |
| Outline / outline variant | `#8E9099` / `#44474F` | `#8E9099` / `#2D3037` | Crisp hairline strokes |

### Reference-only merchandising surfaces

| Token | Value | Use |
|-------|-------|-----|
| Product lime | `#EFF5C8` | Product tile backing only |
| Product blush | `#F8E3E3` | Product tile backing only |
| Product aqua | `#DDF3F4` | Product tile backing only |
| Product lavender | `#F0E5F7` | Product tile backing only |

These are non-semantic image backings, never status colors or text backgrounds. Use the standard surface and on-surface roles for copy and controls placed over them unless a separately measured foreground pair is added here.

### Status semantics

| Semantic | Foreground | Container |
|----------|------------|-----------|
| Success / delivered / completed | `#256B4B` | `#E8F3ED` |
| Warning / pending | `#8A5300` | `#FBF1E3` |
| Error / failed / cancelled | `#B3261E` | `#FCEEEE` |
| Neutral / informational | `#505050` | `#EFF0F2` |

Status must always have a visible text label; color alone never communicates meaning. Dark mode maps status roles to their corresponding high-contrast dark tonal variants.

### Measured Contrast Ratios (WCAG AA)

Measured contrast table covering every foreground/background pair the application and starter screens use:

| Foreground on background | Contrast | Requirement |
|--------------------------|----------|-------------|
| `#161616` on coral `#F7767E` (primary action) | 6.76:1 | Passes normal text |
| `#8D2531` on `#FFF0F1` (primary container) | 7.78:1 | Passes normal text |
| white on lavender `#7651B5` (secondary action) | 5.81:1 | Passes normal text |
| `#392653` on `#EEE7F7` (secondary container) | 11.05:1 | Passes normal text |
| white on aqua `#287E87` (tertiary action) | 4.75:1 | Passes normal text |
| `#164E53` on `#DDF3F4` (tertiary container) | 8.09:1 | Passes normal text |
| white on error `#B3261E` | 6.54:1 | Passes normal text |
| `#8C1D18` on `#FCEEEE` (error container) | 8.07:1 | Passes normal text |
| `#161616` on white surface | 18.10:1 | Passes normal text |
| `#161616` on canvas `#F5F6F7` | 16.72:1 | Passes normal text |
| `#6B6B6B` on white surface | 5.33:1 | Passes normal text |
| `#6B6B6B` on `#F1F1F3` surface variant | 4.72:1 | Passes normal text |
| `#6B6B6B` on `#EFF0F2` surface container high | 4.67:1 | Passes normal text |
| coral-strong `#B92F3D` on white / canvas | 5.94:1 / 5.49:1 | Passes normal text |
| outline `#767676` on white / canvas | 4.54:1 / 4.20:1 | Passes 3:1 component boundary |
| success `#256B4B` on `#E8F3ED` | 5.63:1 | Passes normal text |
| warning `#8A5300` on `#FBF1E3` | 5.66:1 | Passes normal text |
| error `#B3261E` on `#FCEEEE` | 5.79:1 | Passes normal text |
| neutral `#505050` on `#EFF0F2` | 7.07:1 | Passes normal text |

---

## Typography

**Status: ACCEPTED**

Use Lato to match the supplied visual direction, with Android/web fallback to the system sans-serif. Android bundles licensed static Lato Regular, Semibold, and Bold font files locally and retains the SIL Open Font License notice at [`android/licenses/Lato-OFL.txt`](../android/licenses/Lato-OFL.txt). Use the same weight hierarchy across Android and web:

| Role | Android size/weight | Web size/weight | Usage |
|------|---------------------|-----------------|-------|
| Large title | 28sp / 700 | 28px / 700 | Optional prominent heading when the existing screen content calls for it |
| Page title | 24sp / 700 | 24px / 700 | Screen title |
| Section title | 18sp / 700 | 18px / 700 | Content group heading |
| Component title | 16sp / 600 | 16px / 600 | Product/order/card title |
| Body | 14sp / 400 | 14px / 400 | Main content and forms |
| Supporting | 12sp / 400 | 12px / 400 | Metadata and helper copy |
| Action label | 14sp / 700 | 14px / 600 | Buttons, tabs, navigation |

Do not use ultra-light weights for small text. Allow font scaling; let long English content wrap rather than clip or shrink below the role size.

---

## Spacing

**Status: DECIDED**

Use a 4-unit base across Android dp and web CSS px; the numeric values in this table map directly to dp on Android and px on web:

| Token | Value |
|-------|-------|
| xs | 4dp |
| sm | 8dp |
| md-sm | 12dp |
| md | 16dp |
| lg | 24dp |
| xl | 32dp |
| xxl | 40dp |
| xxxl | 48dp |

Consistent spacing should be applied to padding, margins, and gaps.

---

## Shapes

**Status: DECIDED**

Following Material 3 shape scale:

| Shape | Corner Radius | Usage |
|-------|---------------|-------|
| Extra small | 8dp / px | Chips, fields, compact buttons |
| Small | 12dp / px | Product and data cards |
| Medium | 16dp / px | Large cards, forms, dialogs |
| Large | 20dp / px | Large panels only when required by existing screen content |
| Pill | 999dp / px | Chips and fully rounded icon actions |

---

## Elevation

**Status: DECIDED**

Use subtle separation rather than heavy shadows:

| Level | Usage |
|-------|-------|
| Level 0 | Canvas and flat lists |
| Level 1 | Cards and top bars |
| Level 2 | Menus, sheets, sticky actions |
| Level 3 | Dialogs and modal overlays |

---

## Component Specifications

### Buttons

| Type | Usage |
|------|-------|
| Filled | Primary actions (Add to Cart, Place Order) |
| Outlined | Secondary actions |
| Text | Tertiary actions (View All, Cancel) |
| Icon | Toolbar and utility actions |

### Inputs

| Type | Usage |
|------|-------|
| Outlined Text Field | Forms (login, registration, search) |
| Filled Text Field | Alternative style where appropriate |
| Dropdown / Exposed Menu | Category selection, sorting |
| Quantity Selector | Cart item quantity |

### Cards

| Type | Usage |
|------|-------|
| Product Card | Product grid/list items |
| Order Card | Order history items |
| Category Card | Category browsing |

### Product Components

- Product image carousel
- Price display in USD
- Add to cart button
- Availability badge only when the API exposes that state
- Quantity selector

### Web Dashboard Components

| Component | Shared behavior |
|-----------|-----------------|
| Sidebar / mobile navigation | Persistent active item; collapses to a drawer on narrow screens |
| Data table | Clear column headings, aligned values, row actions, keyboard-focusable controls; becomes stacked cards on narrow screens |
| Search and filters | Persistent visible labels, removable filter chips, clear/reset action when filters are active |
| Status badge | Text label plus semantic color; never color alone |
| Form section | Consistent label, helper/error text, field spacing, and save/cancel placement |
| Confirmation dialog | Names the target and consequence; primary/secondary actions are consistent |

---

## State Patterns

### Loading States

| Pattern | Usage |
|---------|-------|
| Skeleton screens | Initial content loading |
| Circular progress | Action in progress |
| Linear progress | Page-level loading |
| Shimmer effect | Not required; use static skeletons in the first release |

### Empty States

| Screen | Message |
|--------|---------|
| Empty cart | Illustration + "Your cart is empty" + CTA |
| Empty orders | Illustration + "No orders yet" + CTA |
| No search results | Illustration + "No results found" + suggestions |

### Error States

| Type | Handling |
|------|----------|
| Network error | Retry button + offline message |
| Server error | Generic error message + retry |
| Validation error | Inline field-level errors |
| Not found | 404-style message |
| Session expired | Redirect to login |

---

## Modern Visual Craftsmanship (Dribbble & Vercel Standards)

Per `.agents/skills/my-ui-ux-skill/`, EShop Verse bridges visual grandeur with strict software engineering:

### 1. Mobile & Web Bento Grids
- **Android Home / Explore:** Group diverse product highlights, category spotlights, and promotional tiles into asymmetric Bento cards (`bg_bento_item.xml`) with a 2-column wide Hero card (180dp) anchored over dual 1-column square cards (140dp).
- **Web Admin Dashboard:** Organize core metrics into an asymmetric Bento layout (wide 8-col performance/revenue hero card + 4-col live inventory satellite cards + 3-col activity cards) using `BentoGrid.tsx`.
- **Micro-Border Depth:** Eliminate muddy drop shadows; use 1dp hairline strokes (`app:strokeWidth="1dp"`, `app:strokeColor="?attr/colorOutlineVariant"` in Android, `border border-neutral-800` in Web) to produce razor-sharp edges.

### 2. Glassmorphism & Frosted Translucency
- **Android Floating Surfaces:** Apply `bg_card_glass.xml` (semi-translucent surface fill + 1dp outline) on floating app bars, bottom navigation pills, and checkout summary sheets.
- **Web Frosted Cards:** Use `backdrop-blur-xl`, `bg-neutral-900/60`, and subtle ambient Aurora glow accents (`bg_gradient_glow.xml` / Tailwind radial gradients) on modal dialogs and sticky headers using `GlassCard.tsx`.

### 3. Motion, Haptics & Micro-Interactions
- **Touch Feedback:** Every interactive card and button must provide instant tactile feedback on touch:
  - Android: `scaleX(0.97f)` / `scaleY(0.97f)` on press with masked ripples, plus `SpringAnimation` for dynamic physical damping.
  - Web: Compositor-only transforms (`transform: scale(0.98)` / `transition: transform 150ms cubic-bezier(0.16, 1, 0.3, 1)`); never animate `width`, `height`, `top`, or `margin`.
- **View Transitions:** Utilize native W3C View Transitions for seamless shared-element card morphing between catalog browsing and product details.

### 4. Vercel Engineering Standards for Web Admin
- **Zero Cumulative Layout Shift (CLS < 0.1):** Every image, chart, and dynamic card must have explicit dimensions and dedicated skeleton loaders to prevent layout jumps.
- **Tabular Figures:** Always apply `tabular-nums` (font-variant-numeric: tabular-nums) on order IDs, USD totals, stock counts, and timestamps for perfectly aligned data tables.
- **Accessible Focus Rings:** Never remove focus rings without an accessible replacement; apply `focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2`.
- **Unblocked Browser Behavior:** Never block native clipboard paste in input fields, never break middle-click tab opening on links, and maintain deep-linkable URL state for modal dialogs and tabs.

---

## Navigation

**Status: DECIDED**

- Bottom Navigation Bar with four primary destinations
- Toolbar with contextual actions
- Back navigation following Android conventions
- Deep links are deferred from the first release

Bottom navigation items:
1. Home
2. Explore
3. Cart
4. Profile / Account

Do not add a wishlist/favorites destination or heart action in the first release. Wishlists are deferred in `PRODUCT.md`.

### Admin Dashboard Navigation

- Desktop sidebar destinations: Products, Categories, Inventory, Orders.
- Account menu provides the signed-in admin identity and Sign out action.
- Do not add customer administration, analytics, notifications, transaction history, or CMS destinations in the first release; those are outside the product scope.

---

## Responsive Layout

### Android

- Design and review the primary portrait flow at 360dp width; support narrow 320dp devices without horizontal scrolling.
- Use 16dp horizontal screen gutters, 12dp grid gaps, and a two-column product grid when each card remains at least 140dp wide; collapse to one column only when required by available width.
- Keep the four-destination bottom navigation and system insets clear of scrollable content and sticky checkout actions.
- On tablets/landscape, allow wider content and additional grid columns while keeping text and card widths readable.

### Admin Web

- Wide desktop (at least 1200 CSS px): 248px sidebar, top bar, and a centered content area capped near 1440px.
- Tablet (768–1199px): compact/collapsible sidebar and reduced table columns where secondary details move into row detail.
- Narrow screens (below 768px): sidebar becomes a drawer; multi-column forms become one column; tables reflow to labeled cards without hiding actions or status.
- Do not make small numeric or order status text depend on hover-only disclosure.

---

## Accessibility

- Minimum touch target: 48dp × 48dp
- Minimum web interactive target: 44 CSS px × 44 CSS px
- Minimum color contrast ratio: 4.5:1 (text), 3:1 (large text)
- Content descriptions for all interactive elements
- Support for TalkBack screen reader
- Support for font scaling
- Keyboard navigation support where applicable
- Focus indicators for interactive elements
