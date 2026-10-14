# Portfolio Handoff Guide — EShop Verse

A short, reviewer-facing guide to set up and evaluate the **backend API** of the EShop Verse portfolio demo from a clean checkout. Commands are copied from [`DEVELOPMENT.md`](DEVELOPMENT.md); that document holds the full detail and the verified command outputs.

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

This guide covers the **backend**. The Android customer app and the React admin dashboard are separate components with their own setup, screens, and a visual walkthrough contributed by their QUAL-004 work; see [`ROADMAP.md`](ROADMAP.md) Phase 3 and the component roadmaps for that portion.
