# ADR-002: Shared Coral-and-Neutral Visual Direction

## Status

ACCEPTED

## Date

2026-10-07

## Context

The owner supplied an e-commerce mobile and admin-dashboard reference and requested its visual pattern and palette for both EShop Verse clients. The existing Android-only design guide used a blue palette and did not define the dashboard or page-by-page layouts. Android is Java/XML with Material 3; the admin is a React web application.

## Options Considered

### Keep the existing blue Android palette and define a separate dashboard style

- Pros: no change to the existing Android guide.
- Cons: creates two brand languages and does not follow the supplied direction.

### Apply the supplied coral-and-neutral direction across both clients

- Pros: one recognizable brand, compatible with Material 3 on Android, and suitable for responsive web administration.
- Cons: requires coordinating Android and web tokens and checking contrast for the bright coral.

### Reproduce the reference screens literally

- Pros: closest visual match to the screenshots.
- Cons: includes controls and dashboard analytics outside the agreed product scope, and may not fit the project's data/API model.

## Decision

- Use a stable light palette of white, soft gray, near-black, and coral with Lato typography, rounded surfaces, clear hierarchy, and restrained elevation.
- Use coral `#F7767E` for filled emphasis with near-black foreground, and darker coral `#B92F3D` for text/icons on light backgrounds. Keep colors, typography, spacing, shapes, and status semantics shared between the Android app and admin dashboard.
- Use Material 3 XML/View components for Android. Use responsive web navigation, tables, forms, and cards for the admin while sharing brand tokens rather than forcing a mobile layout onto desktop.
- Treat the supplied reference as visual inspiration, not a screen-by-screen specification. Only implement the page inventory and behavior within `PRODUCT.md`, `DATABASE.md`, and the reviewed API contract. Deferred functions such as wishlists and analytics remain deferred.
- Maintain the detailed page compositions and states in `docs/SCREEN_SPECIFICATIONS.md`; maintain the reusable token/component rules in `docs/DESIGN_SYSTEM.md`.

## Consequences

- UI tasks depend on DES-001 and must use the shared screen/design documents.
- Android font assets must be bundled locally with the applicable license notice when implemented; web font loading must not require a third-party runtime request.
- The light palette is the first-release baseline. Dark mode and user-selected dynamic palettes require a later design task.
- Verify real component contrast and keyboard/screen-reader behavior during implementation and QUAL-002; palette values alone are not accessibility evidence.

## Update (2026-10-19)

The owner approved a follow-up admin theme task (UI-003). The admin dashboard now exposes a persisted
light/dark toggle using the existing coral/neutral dark tokens; light remains the baseline, and the choice
follows the OS preference until the user sets one. The implementation, contrast evidence, and accessibility
behavior are recorded in [`DESIGN_SYSTEM.md`](../DESIGN_SYSTEM.md) § Admin Web Theme and the Admin Roadmap
Phase 3 Step 6. Android dark mode and user-selected dynamic palettes remain deferred; this update does not
change the Android visual direction.
