# First-Release Screen Specifications

## Purpose and authority

This document translates approved product and roadmap tasks into a consistent visual treatment for the Android customer application and web admin dashboard. It complements, and does not duplicate, the shared tokens and reusable component rules in [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md). Product behavior and scope remain governed by [`PRODUCT.md`](PRODUCT.md), the API contract, and the canonical Task entries in the relevant component roadmap or [`ROADMAP.md`](ROADMAP.md) for shared work. This document does not authorize new pages or features.

The supplied visual reference ([Pinterest pin](https://de.pinterest.com/pin/775956210819327781/)) contributes a visual language: white surfaces, a soft gray canvas, coral accents, near-black typography, Lato, rounded cards, compact product grids, and a clean admin navigation/content shell. Borrow a composition pattern only where it fits an existing EShop Verse screen, such as catalog browsing, product detail, cart, or the admin shell. The pictured analytics overview is not an EShop Verse feature. For screens without a matching reference page, use the shared visual tokens and components while preserving the flow, content hierarchy, and controls established by their product/roadmap requirements. Do not copy reference-only labels, data, controls, or pages.

All user-facing copy is English. Customer price and total displays use en-US USD formatting. The first release uses fictional catalog and account data, and simulated payment only.

## Shared rules

- Use the design tokens and accessibility rules in [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md) on every page. Individual screens may change content hierarchy, not brand styling.
- Treat the screen inventory below as a mapping of existing product tasks, not as permission to add scope. When a detailed layout is not supported by an existing product/API requirement, preserve the task-defined flow and apply only the shared visual system.
- Every network-backed page defines loading, populated, empty, recoverable error/retry, and success feedback as applicable. Keep errors close to the affected content and preserve user input when safe.
- Use real API-backed data only. Never draw fake revenue, visitors, ratings, reviews, sales counts, discounts, or inventory thresholds to fill empty space.
- Product images are seeded/demo URLs. Image loading uses a neutral placeholder and a recoverable image-error treatment; no image upload is in scope.
- Do not add wishlists/favorites, product reviews/ratings, coupons, taxes, saved-address books, customer-management screens, notifications, CMS, or real payment fields. These features are deferred by the product/database scope.
- Customer browsing is available without an account. A cart belongs to a signed-in customer. Do not create or persist a guest cart. For an auth-required action, follow the existing auth and return-navigation requirements; make no cart mutation until the customer is signed in.
- Checkout shows subtotal plus the configured sample shipping amount; do not show a tax calculation or invent a discount.
- Payment result and order status always reflect the server response. A failed or timed-out request must not be shown as success or trigger an unsafe duplicate order.
- Category lifecycle follows the accepted contract shape: categories are archived and restored, never deleted, and archiving is refused with `409 CATEGORY_NOT_EMPTY` while non-archived products remain (`docs/openapi.yaml`, decision OPEN-2 closed 2026-10-11). Category screens support list/create/edit/reorder plus archive/restore; there is no delete action. The `status` column and endpoints arrive with Backend CAT-003, and until the API serves them the archive/restore control stays hidden or disabled with an explanation.

## Android customer application

### App shell and navigation

- Use one Material 3 app theme, one contextual top app bar, and a four-item bottom navigation: **Home**, **Explore**, **Cart**, **Account**. Show an item-count badge on Cart when nonzero.
- Keep bottom navigation visible on the four top-level destinations. Hide it on focused checkout/auth/detail steps when it would compete with the task; use normal Android back navigation.
- Home top bar shows the text wordmark **EShop Verse**, a search entry point, and a profile/account icon. Use initials rather than inventing a profile photo when the API has no avatar.
- Use a 16dp horizontal content gutter, 12dp card/grid gaps, and sticky bottom actions only when the page needs a primary action. Respect system bars and keyboard insets.
- The reference's heart, discount sticker, shoe size/color selectors, and FAB are not copied: wishlist, discounts, and product variants are not part of the first-release model; actions use the appropriate Material 3 button or navigation item.

### Screen inventory

| ID | Screen | Structure and primary behavior |
|----|--------|--------------------------------|
| M-01 | Home | Use the existing catalog-browsing entry points defined for CAT-002. Apply the reference's product-forward cards and whitespace only to catalog content that the app already presents; do not add a promotional/editorial hero or new content sections to imitate the reference. |
| M-02 | Explore / all products | Screen title, search field, category navigation, supported sort control, and product grid. Cards show product image, name, and USD price, with availability only when the API provides it. Tapping a product opens M-05; do not add ratings, favorite hearts, or a card-level add action. |
| M-03 | Search results | Search field retains the query; result count only when returned by pagination metadata; sort menu uses API-supported options; results reuse M-02 cards. Empty results explain that no match was found and offer **Clear search** plus category browsing. |
| M-04 | Category products | Category title, optional one-line description only if the API supplies it, sort control, and the same product grid. Empty category state links back to Explore. |
| M-05 | Product details | Large edge-to-edge product image/gallery when multiple seeded URLs exist; product name and USD price; concise description; stock/availability; quantity stepper only when available; sticky coral **Add to cart** action. No size/color/variant selectors unless the product model is explicitly extended later. |
| M-06 | Sign in | Use the existing sign-in fields and actions. Apply shared typography, surfaces, fields, buttons, focus, and error styles; keep the route and form flow defined by AUTH-002. Preserve the originating route for return navigation when the task requires it. |
| M-07 | Create account | Use the registration fields and actions established by AUTH-002. Apply the shared form, validation, loading, and error patterns without introducing additional fields or account features. |
| M-08 | Request password reset | Use the reset request flow established by AUTH-002, with shared labeled-field, action, confirmation, and error patterns. |
| M-09 | Set new password | Use the reset-token flow established by AUTH-002, with shared labeled-field, action, validation, and invalid/expired-token patterns. |
| M-10 | Cart | Signed-in customer's product rows with thumbnail, name, current price, quantity control, remove action, and server availability/conflict feedback. Bottom summary shows subtotal, configured sample shipping when known, and total; **Continue to checkout** is disabled for an empty or invalid cart. |
| M-11 | Shipping address | Checkout form for recipient name, address lines, city, region/state, postal code, and country, using the fields required by the reviewed API contract. Explain that the address is snapshotted for this order; saved-address management is not implied. **Continue** validates before proceeding. |
| M-12 | Order review | Read-only item/quantity summary, shipping-address summary, subtotal, sample shipping, and total. Provide **Edit cart**, **Edit address**, and **Continue to demo payment**. State clearly that payment is simulated and no card data is requested. |
| M-13 | Simulated payment choice | Explain this is a demo-only outcome selector; provide explicit **Simulate success** and **Simulate failure** actions. No card number, expiry, security code, or real provider logo/field. Disable both actions during submission and use the order idempotency behavior from the reviewed contract. |
| M-14 | Payment/order result | Distinct success and failure heading, server-returned order reference/status, and next steps. Success offers **View order** and **Continue shopping**; failure offers only a safe retry/recovery path consistent with server state and **View order** when one exists. Never infer success from a timeout. |
| M-15 | Order history | Signed-in customer's paginated order cards showing order reference/date, USD total, and server status. Empty state says no orders exist yet and links to Explore; each row opens M-16. |
| M-16 | Order details | Immutable order item/price snapshots, shipping-address snapshot, subtotal/shipping/total, payment result, current status, and order timeline only for transitions supplied by the API. No customer-side status editing. |
| M-17 | Account | Provide only the account/profile basics supported by PRODUCT.md and the customer API. Apply shared typography, surfaces, and action styles; do not imply customer administration, saved addresses, or other account features. |

M-03 and M-04 reuse the M-02 catalog-listing pattern as search/category result states; they do not require extra bottom-navigation destinations or new features.

M-17 Account belongs to AUTH-002 together with M-06–M-09: it presents only the account/profile basics that PRODUCT.md and the customer API support, and it is the destination that hosts sign-out.

### Android interaction and state details

- Product card tap opens M-05. The product-detail **Add to cart** action updates the signed-in cart once and announces success accessibly. Rejected/insufficient-stock responses update availability inline and keep the customer on the current page.
- Search is submitted with the keyboard action or search icon. Sort/category changes reset the result page to the beginning. Preserve the query when navigating to a product and back.
- Empty cart/orders/catalog use the shared empty-state illustration/icon, one sentence, and one relevant action. Do not create a different empty-state visual treatment for each feature.
- Authenticated API `401` clears protected session state and returns to M-06 with a concise explanation; network/server failures retain safe form/search state and show retry.
- The system back action returns to the previous logical step and does not resubmit a payment or order mutation.

## Admin dashboard (React web)

### Admin shell and navigation

- Use the reference's white navigation surface, soft-gray content canvas, and coral active-state treatment for the existing admin area. The navigation shell is a visual pattern and does not imply a dashboard landing page.
- Sidebar destinations are **Products**, **Categories**, **Inventory**, and **Orders**, matching the approved admin capabilities. Sign out is in the account menu. Do not add destinations for overview/analytics, customers, notifications, transactions, or CMS.
- Keep list pages task-oriented: filters above the table, primary action aligned with the page heading, row actions visible and keyboard accessible, and status expressed as text plus color.
- Desktop uses a fixed 248px sidebar and content area capped near 1440px. Tablet collapses the sidebar; below 768px it becomes a drawer and tables reflow into labeled cards.

### Screen inventory

| ID | Screen | Structure and primary behavior |
|----|--------|--------------------------------|
| A-01 | Admin sign in | Use the existing admin sign-in fields and actions. Apply the shared palette, typography, form, focus, loading, and error patterns; keep the authentication flow defined by AUTH-003. No admin self-registration. |
| A-02 | Products | Heading plus **Add product**; search, category/status filters only when supported by API; table columns: product/image, SKU, category, USD price, stock, status, actions. Pagination follows API metadata. Row actions open edit/details; archive follows the reviewed server behavior. |
| A-03 | Create/edit product | One-column form on narrow screens and grouped two-column form on wide screens. Fields: name, description, USD price, optional SKU, category, active/inactive status, and ordered demo image URLs with preview. Stock is managed in A-05, not duplicated here. Show field validation, unsaved-change warning, **Save**, and **Cancel**. No binary upload control. |
| A-04 | Categories | Heading plus **Add category**; searchable/sortable list with category name and display order; show product count only if the API returns it. Edit and reorder follow server support. Lifecycle follows the contract: **Archive**/**Restore** (`adminArchiveCategory`/`adminRestoreCategory`), never delete, shown once Backend CAT-003 serves them; until then hide/disable archive with an explanation. |
| A-05 | Inventory | Table of product, SKU, current quantity, availability, and **Adjust stock** action. Adjustment dialog shows current and proposed quantity, validates a nonnegative integer, and submits only the new quantity supported by API. Quantity zero is **Out of stock**; do not invent a low-stock threshold or adjustment audit history. |
| A-06 | Orders | Heading, search, date/status filters when supported, and table columns: order reference, customer, date, USD total, order status, payment status, action. Pagination uses API metadata. Status filters and labels match the reviewed API enums. |
| A-07 | Order details | Read-only customer and order reference, immutable shipping address and line items, subtotal/shipping/total, payment outcome, and current status. A status action is shown only when the API permits a transition; confirm the next state and refresh from the server after save. |

### Admin interaction and state details

- Product save errors preserve entered values. Archive and stock changes use an explicit confirmation, show in-progress state, and refresh the authoritative row after success.
- List pages define loading skeleton, no-data state with one in-scope CTA, no-filter-results state with **Clear filters**, error/retry state, and success feedback after mutation.
- `401` ends the admin session and returns to A-01. `403` shows an access-denied page/state; a hidden control is not an authorization boundary.
- Order status updates never optimistically claim success. Display the server's resulting status and explain a `409` conflict with a refresh/retry action.

## Screen-to-roadmap map

| Screens | Primary Tasks |
|---------|---------------|
| M-06–M-09, M-17 | AUTH-002 |
| M-01–M-05 | CAT-002 |
| A-01 | AUTH-003 |
| A-02–A-05 | CAT-004 |
| M-10 | CART-002 |
| M-11–M-14 | CHECK-001 |
| M-15–M-16 | ORD-002 |
| A-06–A-07 | ORD-003 |
| Shared accessibility, responsive, and consistency review | QUAL-002 |

## Open contract alignment item - Resolved by the FND-004 contract

The question recorded here during documentation drafting is resolved: `docs/openapi.yaml` (decision OPEN-2, closed 2026-10-11) defines the category lifecycle. Categories are archived and restored, never deleted; archiving is refused with `409 CATEGORY_NOT_EMPTY` while any non-archived product still references the category; the archive response returns the category including its `status`. The UI has no category delete action, and archive/restore controls appear once Backend CAT-003 implements the endpoints.

Historical background: `DATABASE.md` still models categories with `name` and `sort_order` but no status column, while Backend CAT-003 and Admin CAT-004 include category archive behavior. The status column and the endpoints arrive with CAT-003; until the API serves them, the UI hides or disables archive/restore with an explanation, which is what this specification required while the contract was unsettled.
