# API Contract

## Purpose

This document defines the conventions and standards for the REST API that serves as the integration boundary between all client applications (Android app, Admin dashboard) and the backend.

Both the Android application and the backend must treat this contract as their shared interface. Changes to the API contract require review and agreement from both frontend and backend developers.

---

## Base URL

**Status: DECIDED**

The base URL is:

```
https://<domain>/api/v1/
```

Versioning is embedded in the URL path. The production-like demo uses HTTPS; local development may use HTTP.

---

## Naming Conventions

- Use **lowercase** with **hyphens** for URL paths: `/api/v1/order-items`
- Use **plural nouns** for resource collections: `/products`, `/categories`
- Use **camelCase** for JSON field names: `firstName`, `createdAt`
- Use **consistent naming** across all endpoints

---

## Versioning Strategy

**Status: DECIDED**

- API version is included in the URL path: `/api/v1/`, `/api/v2/`
- Breaking changes require a new API version.
- Non-breaking additions (new optional fields, new endpoints) do not require a version bump.
- Deprecated versions will be supported for a documented transition period.

---

## Request Conventions

### HTTP Methods

| Method | Usage |
|--------|-------|
| GET | Retrieve resources (never modifies state) |
| POST | Create new resources |
| PUT | Full replacement of a resource |
| PATCH | Partial update of a resource |
| DELETE | Remove a resource |

### Request Headers

```
Content-Type: application/json
Accept: application/json
Authorization: Bearer <token>    (for authenticated endpoints)
```

### Request Body

- Use JSON for all request bodies.
- Field names use camelCase.

---

## Response Conventions

### Successful Responses

**Single resource:**
```json
{
  "data": {
    "id": 1,
    "name": "Product Name"
  }
}
```

**Collection:**
```json
{
  "data": [
    { "id": 1, "name": "Product A" },
    { "id": 2, "name": "Product B" }
  ],
  "pagination": {
    "currentPage": 1,
    "perPage": 20,
    "totalItems": 100,
    "totalPages": 5
  }
}
```

**Action confirmation (no body needed):**
```
HTTP 204 No Content
```

---

## Error Response Conventions

**Status: DECIDED**

All error responses follow a consistent format:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "The request contains invalid fields.",
    "details": [
      {
        "field": "email",
        "message": "Email is required."
      }
    ]
  }
}
```

- `error.code` — Machine-readable error code.
- `error.message` — Human-readable description.
- `error.details` — Optional array of field-level errors.

---

## Authentication Conventions

**Status: DECIDED — Laravel Sanctum**

- Android authenticates with revocable Sanctum bearer tokens in the `Authorization` header.
- The first-party React admin uses Sanctum's cookie/session flow and CSRF protections; it must share a compatible top-level domain with the API.
- The demo requires an account for checkout. There is no guest checkout or social login in the first release.
- The backend enforces customer ownership and admin roles on every protected resource. Hiding a control in the UI is not authorization.

---

## Pagination Conventions

**Status: DECIDED**

- Use query parameters: `?page=1&perPage=20`
- Default page size: 20 items.
- Maximum page size: 100 items.
- Pagination metadata included in response body under `pagination` key.

---

## Status Code Conventions

| Code | Meaning |
|------|---------|
| 200 | Success — resource returned |
| 201 | Created — new resource created |
| 204 | No Content — action successful, no body |
| 400 | Bad Request — malformed request or invalid syntax |
| 401 | Unauthorized — authentication required |
| 403 | Forbidden — insufficient permissions |
| 404 | Not Found — resource does not exist |
| 409 | Conflict — resource conflict (e.g., duplicate) |
| 422 | Unprocessable Entity — syntactically valid request with invalid field values |
| 429 | Too Many Requests — rate limited |
| 500 | Internal Server Error — unexpected server failure |

---

## Contract Source and First-Release Resources

`API_CONTRACT.md` defines shared conventions. Before implementing an endpoint, create and review `docs/openapi.yaml` as the machine-readable source of truth for paths, schemas, authentication, errors, and examples. Keep this document focused on cross-endpoint rules; do not maintain duplicate endpoint schemas in prose.

The first-release contract must cover:

- Registration, login, logout, and the authenticated customer profile.
- Public product/category listing, search, sorting, and product details.
- Authenticated cart read/update operations.
- Order creation, customer order history/details, and a simulated payment outcome.
- Admin-only product/category/inventory management and order status updates.

Order creation must be safe against accidental duplicate submission (for example, an idempotency key). Prices and totals use fixed-precision decimal values and include the `USD` currency code; clients must not use binary floating-point arithmetic for money.

---

## Contract Change Workflow

1. Update the OpenAPI description and identify compatibility impact before changing backend or clients.
2. Get review from the API owner and affected client owners for breaking changes.
3. Implement server and client changes against the same contract revision.
4. Run contract/API tests and retain compatibility for non-breaking additions. Breaking changes require a documented version migration.

English is the only supported API-facing locale in the first release. Error messages shown to end users are English; machine-readable error codes remain stable.

---

## Open Decisions and Resolution Status

- [x] **Reviewed and accepted by the API owner (FND-004):** [`openapi.yaml`](openapi.yaml) is the approved OpenAPI 3.1 machine-readable contract, covering customer registration/login/logout/profile/password reset, public category and product read/search/sort/pagination, the authenticated cart, checkout quote and order creation with idempotency, the simulated payment outcome, customer order history and detail, admin session authentication, admin product/category/inventory management, admin order status transitions, the shared error envelope and error-code list, and the status-code contract. Endpoint and client integration must follow it; any contract change is made in the contract first (canonical task: Backend FND-004). Full backend implementation completed across Phase 1–3 (`c42dbd0`).
- [x] **Rate limits (QUAL-003):** Implemented in backend `AppServiceProvider` and `bootstrap/app.php` using `x-rate-limit-classes` in [`openapi.yaml`](openapi.yaml) (global 300/min/ip plus per-operation throttles).
- [x] **Category lifecycle (OPEN-2):** Resolved as archived-only (`status: active | archived`). Backend `AdminCategoryController` enforces refusal to archive while active products remain assigned.
- [x] **Flat sample shipping rate (OPEN-3):** Resolved as USD 5.00 flat rate per order (`CartPricing::SAMPLE_SHIPPING_CENTS = 500`).
- [x] **Failed payment order state (OPEN-4):** Resolved as remaining in `pending_payment` on simulated failure, leaving stock intact and allowing client payment retry.
- [ ] **Production API Host (OPEN-1):** Placeholder `api.example.test` remains for local/demo runs; real host to be specified prior to production deployment.
