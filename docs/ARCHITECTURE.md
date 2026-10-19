# Architecture

## Overview

EShop Verse follows a client-server architecture with clearly separated components communicating through a RESTful API.

```
┌─────────────────┐     ┌─────────────────┐
│   Android App   │     │ Admin Dashboard  │
│   (Customer)    │     │     (Web)        │
└────────┬────────┘     └────────┬─────────┘
         │                       │
         └───────────┬───────────┘
                     │ HTTPS
              ┌──────▼──────┐
              │  REST API   │
              │   (PHP)     │
              └──────┬──────┘
                     │
              ┌──────▼──────┐
              │   MySQL     │
              │  Database   │
              └─────────────┘
```

---

## Components

### Android Customer Application

| Aspect | Decision | Status |
|--------|----------|--------|
| Language | Java | DECIDED |
| UI | XML layouts with Material 3 | DECIDED |
| Architecture | MVVM (Model-View-ViewModel) | DECIDED |
| Data Layer | Repository Pattern | DECIDED |
| Local Storage | Deferred for initial release; server is single source of truth (ADR-004). Secure tokens use KeystoreTokenStore (ADR-003). | DECIDED |
| Networking | REST API client | DECIDED |
| HTTP Client | Retrofit | DECIDED |
| Image Loading | Glide | DECIDED |
| Dependency Injection | Constructor injection/manual wiring for the first release | DECIDED |
| Navigation | AndroidX Navigation | DECIDED |
| Min SDK | 25, matching the checked-in Android scaffold | DECIDED |

### Backend API

| Aspect | Decision | Status |
|--------|----------|--------|
| Language | PHP | DECIDED |
| API Style | RESTful | DECIDED |
| Database | MySQL | DECIDED |
| Framework | Laravel; use the supported stable release selected when implementation begins | DECIDED |
| Authentication | Laravel Sanctum: bearer tokens for Android; cookie/session auth for the first-party admin SPA | DECIDED |
| Schema Changes | Laravel migrations | DECIDED |
| API Versioning | `/api/v1/` URL prefix | DECIDED |

### Admin Dashboard

| Aspect | Decision | Status |
|--------|----------|--------|
| Type | Web application | DECIDED |
| Technology | React and TypeScript | DECIDED |
| Backend access | REST API only; no direct database access or embedded business rules | DECIDED |
| Authentication | Laravel Sanctum first-party SPA session authentication | DECIDED |
| Hosting | Separate frontend build under a domain compatible with the API's first-party cookie session | DECIDED |

### Database

| Aspect | Decision | Status |
|--------|----------|--------|
| Engine | MySQL | DECIDED |
| Schema migrations | Laravel migrations | DECIDED |
| Character set | utf8mb4 | DECIDED |

---

## Component Boundaries

### Rules

1. The Android app communicates with the backend **only** through the REST API.
2. The Admin dashboard communicates with the backend **only** through the REST API (or a shared API with admin-specific endpoints).
3. No component directly accesses another component's database or internal state.
4. The API contract (documented in `API_CONTRACT.md`) is the integration boundary.
5. Components can be developed, tested, and deployed independently.

### Directory Mapping

```
android/    → Android customer application
backend/    → PHP REST API and database migrations
admin/      → Admin dashboard
docs/       → Shared documentation
```

---

## Cross-Cutting Concerns

| Concern | Approach | Status |
|---------|----------|--------|
| Authentication | Sanctum-managed API tokens for Android; secure cookie session for admin SPA | DECIDED |
| Error Handling | Consistent JSON error envelope from `API_CONTRACT.md` | DECIDED |
| Logging | Server-side structured logs; never include credentials or payment data | DECIDED |
| Monitoring | Out of scope for the portfolio demo; define before public production use | DEFERRED |
| Caching | HTTP/client caching only where useful; no distributed cache in the first release | DECIDED |
| File Storage | Seeded image URLs or local development storage; production storage is out of scope | DECIDED |

---

## UI & Data Loading Principles (Core Architectural Rules)

1. **Immediate Data Rendering & Non-Blocking Image Loading:**
   - Text and structural layout data (titles, prices, badges, metadata) must render immediately upon receiving network/local data. UI rendering must never block or wait for images to download.
   - Image containers must instantly show a curated, lightweight, Pinterest-style placeholder (`@drawable/ic_image_placeholder`) with fixed aspect ratio.
   - Images must decode and cross-fade smoothly into place asynchronously (`DrawableTransitionOptions.withCrossFade()`) as each completes downloading.

2. **Incremental Lazy Loading & Smart Pre-fetching:**
   - Catalog lists and product grids must be paginated in manageable batches (default 20 items per page).
   - Scroll listeners must detect proximity to the end of the loaded dataset (e.g. approaching item 18 or 19 of a 20-item page) and trigger the next page request automatically in the background, appending new items without disrupting user scroll position.

---

## Key Architectural Decisions

Significant decisions are tracked as Architecture Decision Records in `docs/decisions/`.

See [DECISIONS.md](DECISIONS.md) for the index.

The first-release stack and its rationale are recorded in [ADR-001](decisions/ADR-001-portfolio-stack.md).
