# Product Definition

## Purpose

EShop Verse is a portfolio project that demonstrates a complete single-store shopping flow across an Android customer app, a REST API, and an admin dashboard. It is a demonstration, not a live commerce service.

## First Release Scope

- Audience: international portfolio viewers.
- Customer-facing language: English only.
- Display locale: `en-US` for the demo's English text and USD formatting.
- Display currency: USD sample prices; no live settlement or currency conversion.
- Store model: one store, not a multi-vendor marketplace.
- Checkout: signed-in customers only, with explicitly labelled simulated success and failure scenarios. Do not collect or store real payment credentials.
- Shipping: one configured sample rate and an address snapshot on each order; no carrier integration or tax calculation.
- Seed data: fictional products, users, and orders only.

### Customer Journey

1. Browse categories and products; search and sort the catalog.
2. Open product details and manage a cart.
3. Register or sign in, provide a shipping address, and review the order total.
4. Choose a clearly labelled simulated success or failure and see the matching order result.
5. View the order and its status in order history.

### In Scope

- Android app in Java and XML using Material 3, MVVM, and repositories.
- Laravel REST API backed by MySQL.
- React and TypeScript admin dashboard that accesses backend functionality only through the API.
- Admin product, category, inventory, and order-status workflows.

### Deferred

- Real payment processing, guest checkout, multiple currencies or languages, tax calculation, and carrier integration.
- Multi-vendor support, coupons, bulk import/export, advanced analytics, reviews, wishlists, and push notifications.
- Production operations and public launch requirements. Those need a separate deployment and operations plan.

---

## Target Users

### Customers
- Mobile shoppers using Android devices
- Users who want to browse products, place orders, and view order status

### Administrators / Merchants
- Store owners and staff managing product catalog, inventory, and orders
- Store staff managing products, inventory, and order status

---

## Customer Application

### First-Release Requirements
- Registration, sign-in, and profile basics.
- Google Sign-In as an additional customer sign-in method (owner-approved AUTH-004; admin accounts are never created this way).
- Password reset through the backend's configured email flow.
- Product browsing, categories, search, sorting, and product details.
- Cart management and signed-in checkout.
- Simulated payment outcomes and order history/status.
- English UI and sample USD prices.

---

## Admin Capabilities

### First-Release Requirements
- Admin-only sign-in and authorization.
- Product and category management.
- Inventory updates.
- Order viewing and status updates.
- Persisted light/dark theme preference for the admin interface (owner-requested UI-003, 2026-10-19).

Customer account administration and analytics are deferred.

The shared visual direction and detailed first-release page layouts/states are defined in [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md) and [`SCREEN_SPECIFICATIONS.md`](SCREEN_SPECIFICATIONS.md).

---

## Major Business Domains

| Domain | Description | Status |
|--------|-------------|--------|
| Authentication | Customer and admin sign-in, role checks | IN SCOPE |
| Product Catalog | Products, categories, product images | IN SCOPE |
| Search | Search and sort the catalog | IN SCOPE |
| Cart | Signed-in cart management | IN SCOPE |
| Checkout | Shipping snapshot and sample total | IN SCOPE |
| Payment | Simulated success/failure only | IN SCOPE |
| Orders | Customer history and admin status updates | IN SCOPE |
| Wishlist, reviews, notifications | Deferred features | DEFERRED |
| Analytics, coupons, multi-vendor | Deferred features | DEFERRED |
