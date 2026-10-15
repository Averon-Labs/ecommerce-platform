# EShop Verse — Domain Glossary

Shared domain vocabulary across Android, Backend, and Admin components.

## Roles and Accounts

**Customer**:
A person who registers, signs in, browses the product catalog, and places orders through the Android customer application.
_Avoid_: Client, buyer, shopper

**Admin**:
A store administrator who accesses the web dashboard to manage catalog products, categories, stock levels, and order fulfillment states.
_Avoid_: Superuser, manager, operator

**User**:
The underlying authentication and identity entity stored in the database representing either a Customer or an Admin distinguished by role.
_Avoid_: Member, account holder

## Catalog and Products

**Product**:
A distinct retail item available for purchase, containing name, description, USD price, SKU, status, and category association.
_Avoid_: Article, good, listing

**Category**:
A flat grouping that organizes products for exploration and admin management, lifecycle-managed through active and archived statuses.
_Avoid_: Department, section, collection

## Shopping and Checkout

**Cart**:
A signed-in customer's active, server-persisted collection of products and quantities intended for purchase.
_Avoid_: Basket, shopping bag

**Checkout Quote**:
A server-authoritative calculation of subtotal, flat sample shipping, and total for a customer's cart before order submission.
_Avoid_: Estimate, invoice preview

**Order**:
An immutable purchase record created from an accepted checkout quote, containing line-item snapshots, a shipping address snapshot, and payment outcomes.
_Avoid_: Purchase, transaction

## Status and Lifecycle

**OrderStatus**:
The discrete lifecycle state of an order: `pending_payment` (awaiting payment simulation), `confirmed` (payment succeeded and stock decremented), `processing` (fulfillment in progress), `shipped` (in transit), `delivered` (terminal success), or `cancelled` (terminal void).
_Avoid_: `pending`, `in_progress`

**CategoryStatus**:
The state of a category: `active` (visible in catalog and eligible for product assignment) or `archived` (soft-retired from catalog; protected against deletion while active products remain).
_Avoid_: `deleted`

## Financial Representation

**Money**:
A fixed-precision decimal representation in USD with exactly two fraction digits, serialized as a decimal string over the API and handled with exact decimal types (`BigDecimal` / integer cents) in code.
_Avoid_: Floating-point (float, double), client-side price math
