# Image Loading Execution Plan — Admin & Backend

This document defines the actionable checklist and implementation steps for optimized image loading and text-first rendering priority in the **Admin Dashboard (React)** and **Backend REST API contracts**.

> **Related Documents:**
> - Architecture Law: [`docs/ARCHITECTURE.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/docs/ARCHITECTURE.md#L112-L142)
> - Android Execution Plan: [`docs/IMAGE_LOADING_ANDROID.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/docs/IMAGE_LOADING_ANDROID.md)
> - Agent Skill: [`.agents/skills/text-first-async-image-loading/SKILL.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/.agents/skills/text-first-async-image-loading/SKILL.md)

---

## Phase 1: Admin Web Dashboard

### Step 1 (Overall Step 9): Standards for image loading and text rendering in Admin Dashboard
* **Target Directory:** `admin/` (Web project with React / TypeScript / Tailwind)
* **Goal:** Guarantee immediate rendering of table text data without blocking on image loading and completely prevent Cumulative Layout Shift (CLS = 0) on the web.
* **Execution Checklist:**
  - [ ] In Product Management Table, render textual data columns (SKU, title, category, price, stock quantity) immediately without delay.
  - [ ] Use browser native `loading="lazy"` and `decoding="async"` attributes for small table thumbnails:
    ```tsx
    <div className="relative aspect-square w-12 overflow-hidden rounded-md bg-sandstone-100">
      <img
        src={product.imageUrl}
        alt={product.name}
        loading="lazy"
        decoding="async"
        className="h-full w-full object-cover transition-opacity duration-200"
      />
    </div>
    ```
  - [ ] Set fixed aspect ratio (e.g. `aspect-square` or explicit pixel dimensions `w-12 h-12`) on all images to prevent Cumulative Layout Shift (CLS) on the web.
  - [ ] In product create/edit forms, display image preview with a skeleton pulse effect until fully loaded.

---

## Phase 2: Backend REST API

### Step 2 (Overall Step 10): Validation and refinement of backend JSON responses
* **Target Files:**
  - [`docs/API_CONTRACT.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/docs/API_CONTRACT.md)
  - [`docs/openapi.yaml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/docs/openapi.yaml)
  - Backend routes and controllers in `backend/`
* **Goal:** Completely decouple textual metadata from binary image data in API responses to guarantee sub-100ms response times.
* **Execution Checklist:**
  - [ ] All collection endpoints (`GET /api/v1/products`, `GET /api/v1/cart`, `GET /api/v1/orders`) return only string URLs (`imageUrl` / `thumbnailUrl`).
  - [ ] Sending raw image binaries, streams, or heavy Base64 strings inside JSON payloads is strictly prevented and filtered.
  - [ ] When possible, include image aspect ratio or reference dimensions (`width` and `height`) as metadata in the model so clients know dimensions before downloading.
  - [ ] HTTP cache headers for static image hosting are set with high expiry (`Cache-Control: public, max-age=31536000, immutable`).

---

## Admin & Backend Progress Tracking

- [ ] **Step 9 (Admin):** Implementation of lazy loading and fixed container in Admin Dashboard (React)
- [ ] **Step 10 (Backend):** Validation of JSON contract and cache headers in Backend (REST API)
