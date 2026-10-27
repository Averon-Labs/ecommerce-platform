# Image Loading & Text-First Rendering — Execution Plan

This document defines the actionable, checkable implementation plan for applying the **Text-First Immediate Rendering & Independent Asynchronous Image Loading** architectural pattern across the entire **EShop Verse** platform.

> **Related Architectural Rules & Skills:**
> - Core Architecture Law: [`docs/ARCHITECTURE.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/docs/ARCHITECTURE.md#L112-L142)
> - Android Rules: [`android/AGENTS.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/AGENTS.md#L121-L135)
> - Dedicated Agent Skill: [`.agents/skills/text-first-async-image-loading/SKILL.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/.agents/skills/text-first-async-image-loading/SKILL.md)
>
> **Quick Links to Platform-Specific Plans:**
> - 📱 **[Dedicated Android Plan: docs/IMAGE_LOADING_ANDROID.md](file:///d:/Ham/Code___/for_fiverr/eshop-verse/docs/IMAGE_LOADING_ANDROID.md)**
> - 💻 **[Dedicated Admin & Backend Plan: docs/IMAGE_LOADING_ADMIN_BACKEND.md](file:///d:/Ham/Code___/for_fiverr/eshop-verse/docs/IMAGE_LOADING_ADMIN_BACKEND.md)**
> - 🔗 [Jump to Part 2 (Admin & Backend) on this page](#part-2-admin-dashboard--backend-api)

---

# Part 1: Android Customer Application

This part covers all implementation steps for image loading infrastructure, adapters, XML layouts, and scroll optimization in the Android app.

---

### Phase 1: Android Core Tooling

#### Step 1: Resource cleanup and helper upgrade in `ImageLoaderHelper`
* **Target File:** [`android/app/src/main/java/com/averonlabs/eshopverse/core/ui/ImageLoaderHelper.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/core/ui/ImageLoaderHelper.java)
* **Execution Checklist:**
  - [x] Call `clear(@Nullable ImageView imageView)` to cancel in-flight Glide requests for recycled views.
  - [x] Configure disk cache strategy explicitly to `DiskCacheStrategy.ALL` to store decoded and original binary data.
  - [x] Enable memory caching (`skipMemoryCache(false)`) to achieve 0ms retrieval during fast scrolling.
  - [x] Configure cross-fade transition smoothly at `DrawableTransitionOptions.withCrossFade(250)`.
  - [x] Enforce hardware decode sizing according to physical `ImageView` dimensions to prevent OutOfMemory errors.

#### Step 2: Unit and integration tests for loader infrastructure
* **Target File:** [`android/app/src/test/java/com/averonlabs/eshopverse/core/ui/ImageLoaderHelperTest.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/test/java/com/averonlabs/eshopverse/core/ui/ImageLoaderHelperTest.java)
* **Execution Checklist:**
  - [x] Null safety tests pass for empty or `null` URLs and default error image is loaded.
  - [x] Batch preloading (`preloadBatch`) verified with mixed lists of valid, empty, and null items.
  - [x] Fixed time constant for CrossFade animation asserted at exactly `250ms`.
  - [x] Command `./gradlew testDebugUnitTest --tests ImageLoaderHelperTest` finishes without errors.

---

### Phase 2: Android Adapters & Layouts

#### Step 3: Catalog & Product Grid
* **Target Files:**
  - [`android/app/src/main/java/com/averonlabs/eshopverse/catalog/ProductGridAdapter.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/catalog/ProductGridAdapter.java)
  - [`android/app/src/main/res/layout/item_product_card.xml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/res/layout/item_product_card.xml)
  - [`android/app/src/main/res/layout/item_product_linear_card.xml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/res/layout/item_product_linear_card.xml)
* **Execution Checklist:**
  - [x] In `bind()`, product name, formatted price, stock badge, and accessibility content description are bound synchronously on the first lines.
  - [x] `onViewRecycled` in `ProductGridAdapter` is overridden and calls `ImageLoaderHelper.clear(holder.binding.ivProductImage)` to cancel out-of-viewport downloads:
    ```java
    @Override
    public void onViewRecycled(@NonNull ProductViewHolder holder) {
        super.onViewRecycled(holder);
        ImageLoaderHelper.clear(holder.binding.ivProductImage);
    }
    ```
  - [x] In XML layouts, `app:layout_constraintDimensionRatio="1:1"` is set on image viewports to prevent Layout Shift.
  - [x] Strictly avoid `wrap_content` on image container width or height.

#### Step 4: Favorites List (Wishlist)
* **Target Files:**
  - [`android/app/src/main/java/com/averonlabs/eshopverse/catalog/FavoritesAdapter.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/catalog/FavoritesAdapter.java)
  - [`android/app/src/main/res/layout/fragment_favorites.xml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/res/layout/fragment_favorites.xml)
* **Execution Checklist:**
  - [x] `onViewRecycled` implemented in `FavoritesAdapter` to cancel queued tasks for recycled cards.
  - [x] Titles and prices of saved items rendered immediately from local database/cache without waiting for remote images.
  - [x] Shimmer placeholder visible immediately upon card binding.

#### Step 5: Cart Screen
* **Target Files:**
  - [`android/app/src/main/java/com/averonlabs/eshopverse/cart/CartAdapter.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/cart/CartAdapter.java)
  - [`android/app/src/main/res/layout/item_cart_line.xml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/res/layout/item_cart_line.xml)
* **Execution Checklist:**
  - [x] Fixed thumbnail dimensions in XML (e.g., `80dp x 80dp`) with shimmer background to eliminate layout shift.
  - [x] Unit price, line total, price change notices, and item title prioritized at the top of `bind()`.
  - [x] `onViewRecycled` implemented to cancel cart item image downloads during scrolling or removal:
    ```java
    @Override
    public void onViewRecycled(@NonNull CartViewHolder holder) {
        super.onViewRecycled(holder);
        ImageLoaderHelper.clear(holder.binding.ivCartItemImage);
    }
    ```

#### Step 6: Checkout Review & Order Confirmation
* **Target Files:**
  - [`android/app/src/main/java/com/averonlabs/eshopverse/checkout/ReviewItemsAdapter.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/checkout/ReviewItemsAdapter.java)
  - [`android/app/src/main/res/layout/item_review_line.xml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/res/layout/item_review_line.xml)
* **Execution Checklist:**
  - [x] Render invoice breakdown and order item summaries without delay or network dependencies (<50ms).
  - [x] Implement `onViewRecycled` for order item thumbnails.
  - [x] In case of network error or offline state, invoice renders reliably with default error placeholder.

#### Step 7: Product Detail Image Carousel
* **Target Files:**
  - [`android/app/src/main/java/com/averonlabs/eshopverse/catalog/ImageCarouselAdapter.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/catalog/ImageCarouselAdapter.java)
  - [`android/app/src/main/res/layout/item_product_image.xml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/res/layout/item_product_image.xml)
  - [`android/app/src/main/res/layout/fragment_product_detail.xml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/res/layout/fragment_product_detail.xml)
* **Execution Checklist:**
  - [x] All text specifications (product name, technical specs, brand, price, stock status) rendered immediately from navigation arguments or cache.
  - [x] Carousel viewport at top has fixed `4:5` or `1:1` aspect ratio with shimmer placeholder.
  - [x] `onViewRecycled` implemented to cancel large image requests during rapid swiping:
    ```java
    @Override
    public void onViewRecycled(@NonNull ImageViewHolder holder) {
        super.onViewRecycled(holder);
        ImageLoaderHelper.clear(holder.binding.ivCarouselImage);
    }
    ```

---

### Phase 3: Android Prefetching & Scroll Optimization

#### Step 8: Active prefetching based on scroll direction
* **Target Files:**
  - [`android/app/src/main/java/com/averonlabs/eshopverse/catalog/fragment/ExploreFragment.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/catalog/fragment/ExploreFragment.java)
  - [`android/app/src/main/java/com/averonlabs/eshopverse/shell/HomeFragment.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/shell/HomeFragment.java)
* **Execution Checklist:**
  - [ ] Catalog scroll listener detects scroll direction.
  - [ ] On downward scroll, prefetch 4 to 6 upcoming product images into cache via `ImageLoaderHelper.preloadBatch()`.
  - [ ] Prefetch tasks run at Low Priority to ensure on-screen bandwidth is not degraded.

---

# Part 2: Admin Dashboard & Backend API

This part covers all implementation and validation steps in the Admin Web Dashboard and Backend REST API contracts.

---

### Phase 4: Admin Web Dashboard

#### Step 9: Standards for image loading and text rendering in Admin Dashboard
* **Target Directory:** `admin/` (Web project with React / TypeScript / Tailwind)
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

### Phase 5: Backend REST API

#### Step 10: Validation and refinement of backend JSON responses
* **Target Files:**
  - [`docs/API_CONTRACT.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/docs/API_CONTRACT.md)
  - [`docs/openapi.yaml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/docs/openapi.yaml)
  - Backend routes and controllers in `backend/`
* **Execution Checklist:**
  - [ ] All collection endpoints (`GET /api/v1/products`, `GET /api/v1/cart`, `GET /api/v1/orders`) return only string URLs (`imageUrl` / `thumbnailUrl`).
  - [ ] Sending raw image binaries, streams, or heavy Base64 strings inside JSON payloads is strictly prevented and filtered.
  - [ ] When possible, include image aspect ratio or reference dimensions (`width` and `height`) as metadata in the model so clients know dimensions before downloading.
  - [ ] HTTP cache headers for static image hosting are set with high expiry (`Cache-Control: public, max-age=31536000, immutable`).

---

## Progress Summary & Roadmap Mapping

All remaining tasks are consolidated under **Android Roadmap Phase 11** (`feature/android-phase-11`):
* **IMG-002 (Step 7):** Optimistic text-first transition for Product Detail screen.
* **IMG-003 (Step 8 tuning):** Low-priority prefetch bandwidth optimization in `ImageLoaderHelper`.
* **IMG-004 (Step 6 refinement):** Layout token & active shimmer alignment in `item_review_line.xml`.
* **IMG-005 (Step 9):** Web admin zero-CLS and lazy loading standards.

### Android Customer Application
- [x] **Step 1:** Resource cleanup method in `ImageLoaderHelper.java`
- [x] **Step 2:** Unit test validation in `ImageLoaderHelperTest.java`
- [x] **Step 3:** Implementation in `ProductGridAdapter.java` and grid layouts
- [x] **Step 4:** Implementation in `FavoritesAdapter.java`
- [x] **Step 5:** Implementation in `CartAdapter.java` and cart line layout
- [x] **Step 6:** Implementation in `ReviewItemsAdapter.java` (refinement in IMG-004)
- [ ] **Step 7 (IMG-002):** Implementation of optimistic text-first transition in `ProductDetailFragment.java`
- [x] **Step 8 (IMG-003):** Active lookahead prefetching implemented in fragments (priority tuning in IMG-003)

### Admin & Backend
- [ ] **Step 9 (IMG-005):** Implementation of lazy loading and fixed container in Admin Dashboard (React)
- [ ] **Step 10:** Validation of JSON contract and cache headers in Backend (REST API)
