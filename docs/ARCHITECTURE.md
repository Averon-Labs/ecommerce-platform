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

Modeled after industry-leading consumer platforms (Netflix, Pinterest, Digikala, Instagram), all client presentation layers must strictly follow these core progressive loading principles:

1. **Immediate Data-First Rendering & Zero Layout Shift (CLS = 0):**
   - Textual and structural metadata (titles, prices, discounts, stock status, ratings, category badges) must render immediately (<100ms) upon receiving local or network payloads. UI rendering must never block or wait for remote image assets to download.
   - All image viewports must enforce fixed aspect ratios (1:1 square for catalog cards, 16:9 for banners, 4:5 for detail galleries) via `ConstraintLayout` dimension ratios or fixed dimensions. The layout must never jump, reflow, or jitter when an image completes loading.

2. **Curated Shimmer / Skeleton Placeholders (Never Blank Boxes):**
   - While image bytes are streaming and decoding, viewports must display a subtle, elegant shimmer or skeleton placeholder (`bg_placeholder_shimmer`) harmonized with the brand palette (warm ivory to soft sandstone in light mode; muted charcoal in dark mode).
   - Static states use an editorial boutique brand watermark silhouette (`@drawable/ic_image_placeholder`), giving the user a visual sense of life and polish rather than a raw, empty container.

3. **Smooth Asynchronous Cross-Fade Transitions (200–300ms):**
   - When the byte stream is decoded into a hardware bitmap, it must seamlessly replace the placeholder using a hardware-accelerated cross-fade (`DrawableTransitionOptions.withCrossFade(250)`).
   - Abrupt image popping, flickering, or white flashes during load are strictly prohibited.

4. **Multi-Tier Intelligent Caching (RAM + Disk):**
   - **Tier 1 (RAM Memory Cache):** Fast LRU cache in system RAM (`skipMemoryCache(false)`) providing 0ms instantaneous display when scrolling back and forth or switching tabs.
   - **Tier 2 (Disk Cache):** Persistent on-disk cache (`DiskCacheStrategy.AUTOMATIC`) ensuring previously viewed assets are available offline and across app restarts without redundant network calls.
   - **Hardware Downsampling:** Images must decode strictly to the physical pixel bounds of the target ImageView to eliminate wasted memory and prevent OutOfMemory (OOM) exceptions.

5. **Proactive Scroll Prefetching & Incremental Lazy Loading:**
   - Catalog queries must be paginated in manageable 20-item batches.
   - As the user scrolls near the end of the loaded batch (e.g. items 15–18), the next page slice must automatically trigger in the background, appending new items without disrupting user scroll position or momentum.
   - Off-screen images (the next 4–6 visible items in the user's scroll direction) must be proactively prefetched into Glide's cache so that they are already in memory when entering the viewport.

6. **Editorial Graceful Error Fallbacks (No Ugly Broken Icons):**
   - Network errors, 404s, or decoding failures must never render a raw broken image icon, red alert marker, or blank hole.
   - Failed images must display an elegant, muted luxury fallback vector (`@drawable/ic_image_placeholder_error`) in harmonious brand tints, maintaining visual dignity and clean aesthetic continuity.

---

## Key Architectural Decisions

Significant decisions are tracked as Architecture Decision Records in `docs/decisions/`.

See [DECISIONS.md](DECISIONS.md) for the index.

The first-release stack and its rationale are recorded in [ADR-001](decisions/ADR-001-portfolio-stack.md).
