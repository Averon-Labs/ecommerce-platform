# Portfolio Handoff Guide — EShop Verse

A short, reviewer-facing guide to set up and evaluate the **EShop Verse portfolio demo** — backend API, Android customer app, and React admin dashboard — from a clean checkout. Commands are copied from [`DEVELOPMENT.md`](DEVELOPMENT.md); that document holds the full detail and the verified command outputs.

> **Payment is simulated and this is not a production system.** No payment provider is contacted and no card data is ever requested, accepted, logged, or stored. See [`SECURITY.md`](SECURITY.md) § Portfolio Demo Boundaries and [`SECURITY_REVIEW.md`](SECURITY_REVIEW.md).

---

## What this component is

A Laravel 13 (PHP 8.4) REST API on MySQL 8.x — the single integration boundary for the Android customer app and the React admin dashboard. It implements the accepted contract in [`openapi.yaml`](openapi.yaml): customer auth, a browsable catalog, a server-priced cart, checkout quote, idempotent order creation with immutable snapshots, an explicitly simulated payment, and the admin catalog/inventory/order operations. Money is fixed-precision decimal strings; the server computes every total.

---

## Clean setup (backend)

Prerequisites: PHP 8.4 (with `curl`, `fileinfo`, `mbstring`, `openssl`, `pdo_mysql`), Composer 2, and a reachable MySQL 8.x with two databases (`eshopverse_dev`, `eshopverse_test`). The verified local workflow is the zip-based MySQL in [`DEVELOPMENT.md`](DEVELOPMENT.md).

```powershell
Set-Location backend
composer install                                   # uses the committed composer.lock
Copy-Item .env.example .env                        # then set DB_HOST / DB_PORT / DB_DATABASE / DB_USERNAME / DB_PASSWORD
php artisan key:generate
php artisan migrate:fresh --seed                   # build + seed the development database
php artisan test                                   # runs against the disposable eshopverse_test database
vendor\bin\pint --test                             # style/static check
php artisan serve                                  # http://127.0.0.1:8000  (health: GET /up)
```

`.env` is git-ignored and holds the only real credentials; `.env.example` contains placeholders only.

---

## Demo data and roles

`php artisan migrate:fresh --seed` seeds a fixed, fictional store (no real personal data): 5 categories, 12 products (10 active — one inactive, one out of stock, one archived), 12 inventory rows, and deterministic placeholder images.

| Role | Sign-in | Password | Notes |
|------|---------|----------|-------|
| Customer | `demo@example.com` | `Password123` | Local-only demo shopper for manual sign-in. |
| Administrator | `demo-admin@eshopverse.test` | from `DEMO_ADMIN_PASSWORD` in `backend/.env`, or a random per-install password if unset | Never a committed public credential; the admin SPA signs in through the Sanctum session/CSRF flow. |

Seeded counts after a fresh seed: **2 users (1 admin), 5 categories, 12 products (10 active), 12 inventories.**

---

## Reset procedure

To wipe local data and rebuild the demo store from scratch:

```powershell
php artisan migrate:fresh --seed
```

This is safe against the development database only. Tests never touch it — `phpunit.xml` is hard-wired to `eshopverse_test`, and `RefreshDatabase` runs each test in a transaction.

---

## Trying the purchase flow

With the server running (`php artisan serve`), a reviewer can walk the whole journey over the API (or through the Android/admin clients):

1. Sign in — `POST /api/v1/auth/login` with the demo customer above.
2. Browse — `GET /api/v1/products`, `GET /api/v1/products/{id}`.
3. Cart — `POST /api/v1/cart/items`, `PATCH /api/v1/cart/items/{productId}`.
4. Quote — `POST /api/v1/checkout/quote` (server-priced, returns a `shippingAddressId` fingerprint).
5. Order — `POST /api/v1/orders` with an `Idempotency-Key` header (immutable snapshots; cart emptied).
6. Pay (simulated) — `POST /api/v1/orders/{id}/payment` with `{"outcome":"success"}` (or `"failure"`; a failure stays retryable with a new key). Success confirms the order and decrements stock transactionally.
7. Read the order — `GET /api/v1/orders/{id}` (status, payment, timeline).

An end-to-end regression test exercises exactly this journey against the seeded data: `php artisan test --filter=PurchaseFlowRegressionTest`.

---

## Known limitations

- **Simulated payment only** — no provider, no card data, no refunds; `payment` is a demo outcome row.
- **Flat sample shipping** — a single configured USD 5.00 rate per order (`x-money-rules`, OPEN-3).
- **No production readiness is claimed** — a real launch needs a threat model, privacy/legal review, deployment controls, monitoring, and backup/recovery (see `SECURITY.md`).
- **Local development runs over loopback HTTP** (no TLS); a hosted deployment terminates HTTPS and uses a TLS MySQL connection.
- Deferred/open items are tracked as `OPEN-n` decisions in `openapi.yaml` `x-open-decisions`.

---

## Component status

This guide covers the **backend REST API**, the native **Android customer application**, and the **React admin dashboard**.

---

## Android Customer Application

### What this component is

A native Android application built with **Java 17, Material 3, MVVM, Repository Pattern, ViewBinding, and Jetpack Navigation**. It delivers a complete e-commerce customer journey adhering to the design specifications in [`docs/SCREEN_SPECIFICATIONS.md`](SCREEN_SPECIFICATIONS.md) and tokens in [`docs/DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md):
- In-place category exploration and search filtering with fixed 16dp outer screen margin and 8dp card gutters.
- Server-confirmed cart calculations and quantity stepper.
- Safe, simulated checkout flow with zero credit card or PII collection.
- Order history with immutable order detail snapshots.
- Multi-layer security: Keystore AES-GCM token encryption, cloud/ADB backup exclusions, and logcat redaction.

---

### Clean setup (Android)

Prerequisites:
- **JDK 17+** (verified on OpenJDK 17 and Oracle JDK 21).
- **Android SDK Platform 36** (minSdk 25, targetSdk 36).
- An Android device or emulator running API 25+ (recommended: Pixel emulator running API 34+).

#### 1. Building and running automated checks via CLI

From the project root:

```powershell
Set-Location android

# Compile and run all unit and integration tests (150+ tests):
.\gradlew.bat testDebugUnitTest --no-daemon

# Run Android Lint static analysis:
.\gradlew.bat lintDebug --no-daemon

# Assemble the debug APK:
.\gradlew.bat assembleDebug --no-daemon

# Install onto an active emulator or connected device:
.\gradlew.bat installDebug
```

#### 2. Running with the embedded offline mock mode (standalone)

By default, the debug build contains an `OfflineMockInterceptor` that serves mock responses for catalog, cart, checkout, and orders automatically without needing any external server. You can immediately launch and test the app offline on any emulator or device:
```powershell
.\gradlew.bat installDebug
```

#### 3. Running against the Python mock server

To test network roundtrips with the shared JSON fixture mock server:
```powershell
# In terminal 1:
python tools/mock-api/server.py 8081

# In terminal 2:
Set-Location android
.\gradlew.bat installDebug -PapiBaseUrl="http://10.0.2.2:8081/api/v1"
```

#### 4. Running against the live Laravel backend

To test end-to-end integration against the real seeded database:
```powershell
# In terminal 1 (start MySQL and Laravel server):
Set-Location backend
php artisan serve --port=8000

# In terminal 2:
Set-Location android
.\gradlew.bat installDebug -PapiBaseUrl="http://10.0.2.2:8000/api/v1"
```

---

### Demo Customer Account

For testing authenticated features (Cart, Checkout, Order History, Profile):

| Role | Email | Password | Notes |
|---|---|---|---|
| Customer | `demo@example.com` | `Password123` | Pre-seeded customer account in local database and mock responses |

---

### Screen-by-Screen Walkthrough (M-01 through M-17)

Follow this structured path to review all 17 customer screens:

| ID | Screen Name | Route / Navigation Path | Key Controls & Behaviors |
|---|---|---|---|
| **M-01** | **Home** | Bottom Nav: **Home** | Welcome banner, quick category filters, featured products grid. |
| **M-02** | **Explore** | Bottom Nav: **Explore** | Top category chips (All, Footwear, Apparel, Accessories, Electronics) for in-place filtering, search bar, 2-column product grid with 8dp gutters. |
| **M-03** | **Search Results** | Enter keyword in Search Bar | Query persistence, live filtered product grid, empty state with "Clear search" CTA. |
| **M-04** | **Category Products** | Select Category from Catalog | Category-focused product grid with sort menu. |
| **M-05** | **Product Details** | Tap any product card | Glide image gallery, formatted USD price, stock status badge, accessible QuantityStepper, sticky coral "Add to Cart" action. |
| **M-06** | **Sign In** | Bottom Nav: **Account** (when logged out) | Email/password input with validation, secure login, link to registration. |
| **M-07** | **Create Account** | Tap "Create Account" on Sign In | Full name, email, password fields with real-time validation and error containers. |
| **M-08** | **Request Password Reset** | Tap "Forgot password?" on Sign In | Email input with confirmation feedback. |
| **M-09** | **Set New Password** | Follow reset token link | Token verification, new password submission. |
| **M-10** | **Cart** | Bottom Nav: **Cart** | Item rows with thumbnails, dynamic quantity stepper, item removal dialog, server-priced USD totals, "Proceed to Checkout" CTA. |
| **M-11** | **Shipping Address** | Tap "Checkout" in Cart | Recipient name, street address, city, state, postal code, country input form with strict validation. |
| **M-12** | **Order Review** | Tap "Continue" on Address screen | Read-only itemized snapshot, snapshotted shipping address, subtotal, sample shipping ($5.00), total, simulated payment notice. |
| **M-13** | **Simulated Payment Choice** | Tap "Continue to Demo Payment" | Explicit demo outcome selection: **"Simulate Success"** or **"Simulate Failure"** (zero card credentials or provider branding). |
| **M-14** | **Payment Result** | Submit simulated payment | Distinct success (green) or failure (red) feedback card, order reference, "View Order" CTA. |
| **M-15** | **Order History** | Bottom Nav: **Account** → "My Orders" | Paginated list of user's past orders with status badges (`pending_payment`, `processing`, `completed`, `cancelled`). |
| **M-16** | **Order Details** | Tap any order in Order History | Immutable snapshot of purchased items, prices, delivery address, timeline, and payment outcome. |
| **M-17** | **Account Overview** | Bottom Nav: **Account** (when logged in) | Customer name, email, membership status, and secure "Sign Out" action. |

---

### Visual State Showcase (Screen States)

Every screen is engineered with clear state handling via `StateContainerView`:

1. **Loading State:**
   - Smooth progress indicator / shimmer loading placeholders while API requests are in flight.
2. **Populated Content State:**
   - Clean Material 3 cards, 16dp outer screen margin, 8dp card gutters, and 48x48dp minimum touch targets.
   - High-contrast typography conforming to WCAG AA (>= 4.5:1 ratio).
3. **Empty State:**
   - Dedicated illustrations and contextual English messaging:
     - Empty Cart: *"Your cart is empty"* with *"Explore Products"* button.
     - Empty Orders: *"No orders yet"* with link to catalog.
     - Empty Search: *"No products found"* with *"Clear search"* button.
4. **Error & Offline State:**
   - Network failure or server error prompts display user-friendly copy and a clear *"Retry"* button.
   - Safe retry semantics: mutations are never duplicated automatically upon transport retry.

---

## Admin Dashboard

### What this component is

A React 19 + TypeScript + Vite single-page dashboard for store operations, styled with Tailwind CSS 4 on the shared coral/neutral design system (self-hosted Lato). It talks to the backend through the admin REST endpoints only, authenticating with the first-party Laravel Sanctum session (HttpOnly cookie + echoed `XSRF-TOKEN`; no auth token in browser storage). The shell is a static sidebar at `≥768px` and a drawer below it; the four sections cover the first-release scope: products, categories, inventory, and orders. A persisted light/dark theme toggle is available in the shell header and on sign-in; only the non-sensitive theme preference is stored in `localStorage`.

The optional free-hosted demo is recorded in [`DEPLOYMENT.md`](DEPLOYMENT.md); the local setup below remains the reproducible path.

### Clean setup (Admin)

Prerequisites: **Node.js 20.19+ (22.12+) and npm**, plus the backend running and seeded (see above).

```powershell
Set-Location admin
npm ci                              # uses the committed package-lock.json
Copy-Item .env.example .env.local   # only if the API is not at http://localhost:8000/api/v1
npm run dev                         # http://localhost:5173 (port is pinned)
```

Open the SPA at `http://localhost:5173` with the backend on `http://localhost:8000`; the checked-in backend defaults already accept that origin (`SANCTUM_STATEFUL_DOMAINS`, `config/cors.php`). Do not mix `localhost` and `127.0.0.1` — the `SameSite=Lax` session cookie only travels within one site. Full origin arrangement: [`DEVELOPMENT.md`](DEVELOPMENT.md) § Admin Dashboard.

Admin checks (from `admin/`):

```powershell
npm run typecheck        # tsc --noEmit
npm run lint             # eslint .
npm test                 # Vitest + React Testing Library behavior tests
npm run check:contrast   # WCAG 2.2 check for the light + dark admin token pairs
npm run build            # type-check + production build (dist/)
```

### Admin account

| Role | Sign-in | Password | Notes |
|------|---------|----------|-------|
| Administrator | `demo-admin@eshopverse.test` | from `DEMO_ADMIN_PASSWORD` in `backend/.env` (random per-install if unset) | Local-only; the SPA has no self-registration — accounts are provisioned by the backend seed. |

### Screen-by-Screen Walkthrough (A-01 through A-07)

| ID | Screen | How to reach it | Key behaviors |
|----|--------|-----------------|---------------|
| **A-01** | **Admin sign in** | Open `http://localhost:5173` | Email + password with inline validation; throttle messages carry the retry hint; non-admin or expired sessions show a notice; the light/dark toggle is at the top-right and its choice persists. |
| **A-02** | **Products** | Sidebar: **Products** | Search plus category/status filters, API pagination, row actions **Edit**/**Archive** (confirmation), responsive table-to-card layout on narrow screens. |
| **A-03** | **Create/edit product** | **Add product** / row **Edit** | Grouped form (name, description, USD price, optional SKU, category, status, ordered demo image URLs with preview); unsaved-change guard; stock is managed in A-05, not here. |
| **A-04** | **Categories** | Sidebar: **Categories** | Create/edit, archive/restore; archiving is refused with the server's conflict message while active products remain assigned. |
| **A-05** | **Inventory** | Sidebar: **Inventory** | Product/SKU/quantity/availability table; **Adjust stock** dialog validates a nonnegative integer; quantity zero reads **Out of stock** in catalog views. |
| **A-06** | **Orders** | Sidebar: **Orders** | Search by reference or customer, status/date filters, API pagination, payment badge, responsive rows. |
| **A-07** | **Order details** | Open any order in A-06 | Immutable item/address snapshots, simulated payment outcome, server timeline, and a status action offering only the transitions the API currently allows; the view refreshes from the server after a change. |

### Admin workflows

- **Catalog:** add an active product, edit its price, then archive it; create a category and try archiving it while a product still uses it to see the conflict message.
- **Inventory:** adjust a product's stock — zero quantity renders **Out of stock** wherever the product is listed.
- **Orders:** complete a simulated purchase (Android app or the API steps above) and, as the administrator, move the paid order through **Processing → Shipped → Delivered**. A `pending_payment` order can be cancelled instead. Invalid transitions are rejected by the server with `409 INVALID_TRANSITION`; the UI offers only the allowed moves.
- **Reset:** `php artisan migrate:fresh --seed` (above) rebuilds the store and the admin account together.

### Known limitations (Admin)

- **Demo image URLs only** — no binary upload; product images are ordered URL strings with previews.
- **Stock lives in Inventory (A-05)** and is never edited on the product form.
- Desktop widths are the primary target; narrow screens fall back to the documented labelled card lists.
- The light/dark preference is the only value stored in `localStorage` (`eshop-admin-theme`) and is non-sensitive; auth tokens and session state remain in Sanctum cookies. Deferred by product scope: analytics/overview, customer administration, CMS, and wishlist.
- Local development runs over loopback HTTP; the session cookie gains `Secure` only in a TLS deployment (see [`SECURITY_REVIEW.md`](SECURITY_REVIEW.md) § Accepted non-production limitations).

