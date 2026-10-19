# Database Design

## Overview

The platform uses **MySQL** as its primary relational database. This document describes the first-release data model for the single-store portfolio demo.

> **Note:** This is a logical design, not a finalized physical schema. Implement schema changes as reviewed Laravel migrations. The demo uses fictional data and simulated payment only.

---

## Conceptual Entities

### Users

| Field | Type | Notes |
|-------|------|-------|
| id | Primary Key | Auto-increment |
| email | String | Unique, required |
| password_hash | String | Hashed, never stored in plain text; nullable for Google-only accounts |
| google_id | String | Nullable, unique; Google account subject (`sub`), added by AUTH-004 |
| first_name | String | Required |
| last_name | String | Required |
| role | Enum | customer, admin |
| status | Enum | active, suspended, deleted |
| created_at | Timestamp | |
| updated_at | Timestamp | |

### Products

| Field | Type | Notes |
|-------|------|-------|
| id | Primary Key | |
| name | String | Required |
| description | Text | |
| price | Decimal | Required; sample USD amount |
| sku | String | Unique, optional |
| category_id | Foreign Key | References Categories |
| status | Enum | active, inactive, archived |
| created_at | Timestamp | |
| updated_at | Timestamp | |

All first-release prices are USD, so a per-row currency column is unnecessary. Money values use fixed-precision storage and are never calculated with binary floating point. The API returns a decimal string with the `USD` currency code.

### Product Images

| Field | Type | Notes |
|-------|------|-------|
| id | Primary Key | |
| product_id | Foreign Key | References Products |
| url | String | Seeded/demo image URL |
| sort_order | Integer | Orders images for product display |

Product images are separate rows so each product can have an ordered image set. Uploading images is deferred; seeded URLs are sufficient for the demo.

### Categories

| Field | Type | Notes |
|-------|------|-------|
| id | Primary Key | |
| name | String | Required |
| sort_order | Integer | |
| status | Enum (`active`, `archived`) | Category lifecycle; implemented in the Backend catalog migrations (CAT-001) with archive/restore endpoints (CAT-003) |
| created_at | Timestamp | |
| updated_at | Timestamp | |

Categories are flat in the first release; hierarchical categories are deferred. Categories are archived and restored, never deleted (decision OPEN-2, closed 2026-10-11 in `docs/openapi.yaml`); archiving is refused while non-archived products still reference the category.

### Inventory

| Field | Type | Notes |
|-------|------|-------|
| id | Primary Key | |
| product_id | Foreign Key | References Products |
| quantity | Integer | Current stock level |
| updated_at | Timestamp | |

**Relationship:** One inventory row per product, enforced by a unique `product_id`. Update stock and order state transactionally when a simulated payment succeeds.

### Cart

| Field | Type | Notes |
|-------|------|-------|
| id | Primary Key | |
| user_id | Foreign Key | References Users |
| created_at | Timestamp | |
| updated_at | Timestamp | |

### Cart Items

| Field | Type | Notes |
|-------|------|-------|
| id | Primary Key | |
| cart_id | Foreign Key | References Cart |
| product_id | Foreign Key | References Products |
| quantity | Integer | |
| unit_price | Decimal(11,2) | Price snapshot taken when the line was added; compared with the current product price on every read to expose the contract's `priceChanged` flag |
| added_at | Timestamp | |

There is one cart per signed-in user in the first release. A cart contains at most one row per product. The `unit_price` snapshot column was added on 2026-10-14 for CART-001 so the accepted contract's `CartItem.priceChanged` behavior is observable (the binding contract supersedes the earlier price-less logical model).

### Orders

| Field | Type | Notes |
|-------|------|-------|
| id | Primary Key | |
| user_id | Foreign Key | References Users |
| reference | String | Unique order reference `ESV-NNNNNN`, derived from the primary key |
| status | Enum | pending_payment, confirmed, processing, shipped, delivered, cancelled |
| subtotal_amount | Decimal | Sum of order item totals |
| shipping_amount | Decimal | Fixed sample shipping amount; USD |
| total_amount | Decimal | Subtotal plus shipping |
| shipping_address_snapshot | JSON | Required immutable copy of the address used for this order |
| notes | Text | Optional |
| timeline | JSON | Recorded status transitions with UTC timestamps, oldest first (contract `Order.timeline`) |
| created_at | Timestamp | |
| updated_at | Timestamp | |

Three recorded decisions for ORD-001 (2026-10-14): `reference` is generated as `ESV-` plus the zero-padded primary key (matching the contract's `ESV-000017` example) and set inside the creating transaction. `payment_status` is deliberately not a column — it is derived from the latest `demo_payments` row and answers `pending` when none exists, so it can never drift from the payment records. `timeline` is an append-only JSON array of `{status, at}` entries written at creation and on every applied transition. Stock is never reserved at order creation; inventory is decremented only inside the successful simulated-payment transaction, which locks the inventory rows and re-checks availability first (see Inventory above).

### Order Items

| Field | Type | Notes |
|-------|------|-------|
| id | Primary Key | |
| order_id | Foreign Key | References Orders |
| product_id | Foreign Key | References Products |
| product_name | String | Product name snapshot at purchase time |
| quantity | Integer | |
| unit_price | Decimal | Price at time of order |
| total_price | Decimal | |

### Demo Payments

| Field | Type | Notes |
|-------|------|-------|
| id | Primary Key | |
| order_id | Foreign Key | References Orders |
| amount | Decimal | |
| method | Enum | `demo` only |
| status | Enum | pending, completed, failed |
| transaction_id | String | Fictional demo reference; never a real payment credential |
| created_at | Timestamp | |

### Idempotency Keys

| Field | Type | Notes |
|-------|------|-------|
| id | Primary Key | |
| user_id | Foreign Key | References Users |
| endpoint | String | Operation scope: `orders.create` or `orders.payment:<orderId>` |
| key | String | Client-supplied `Idempotency-Key` header (UUID) |
| hash | String | SHA-256 of the JSON request body |
| response_code | Integer | Stored response status replayed on duplicate submission |
| response_body | JSON | Stored response body replayed on duplicate submission |
| created_at | Timestamp |

Added on 2026-10-14 for ORD-001 because the accepted contract requires an `Idempotency-Key` header on `POST /orders` and `POST /orders/{orderId}/payment`, and the earlier logical model had no store for it. Rows are unique per (user, endpoint, key): the same key with the same body replays the stored response without repeating the mutation, while reuse with a different body answers `409 IDEMPOTENCY_KEY_REUSED`. The row is written inside the mutation's own transaction, so a concurrent duplicate insert rolls the whole mutation back instead of applying it twice.

### Deferred Domains

Reviews, notifications, coupons, saved addresses, and wishlists are outside the first-release schema. Add them only with approved product requirements.

---

## Deferred Decisions

- Production soft-delete and audit-history requirements.
- Coupon, refund, tax, and real payment-provider models.
- Production image storage and retention.
- Production deployment, backup, and recovery requirements.

Use `utf8mb4`, indexes for foreign keys and frequently queried fields, and migrations for every schema change. Review final indexes and constraints against actual query patterns.
