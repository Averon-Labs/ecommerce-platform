# Image Loading Execution Plan — Android Customer Application

This document defines the actionable checklist and implementation steps for optimized image loading and text-first rendering priority in the **Android Customer Application (Java / XML / Material 3)**.

> **Related Documents:**
> - Architecture Law: [`docs/ARCHITECTURE.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/docs/ARCHITECTURE.md#L112-L142)
> - Android Rules: [`android/AGENTS.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/AGENTS.md#L121-L135)
> - Admin & Backend Execution Plan: [`docs/IMAGE_LOADING_ADMIN_BACKEND.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/docs/IMAGE_LOADING_ADMIN_BACKEND.md)
> - Agent Skill: [`.agents/skills/text-first-async-image-loading/SKILL.md`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/.agents/skills/text-first-async-image-loading/SKILL.md)

---

## Phase 1: Android Core Tooling

### Step 1: Resource cleanup and helper upgrade in `ImageLoaderHelper`
* **Target File:** [`android/app/src/main/java/com/averonlabs/eshopverse/core/ui/ImageLoaderHelper.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/core/ui/ImageLoaderHelper.java)
* **Execution Checklist:**
  - [x] Implement `clear(@Nullable ImageView imageView)` to cancel in-flight Glide requests for recycled views.
  - [x] Configure disk cache strategy explicitly to `DiskCacheStrategy.ALL` to store decoded and original binary data.
  - [x] Enable memory caching (`skipMemoryCache(false)`) to achieve 0ms retrieval during fast scrolling.
  - [x] Configure cross-fade transition smoothly at `DrawableTransitionOptions.withCrossFade(250)`.
  - [x] Enforce hardware decode sizing according to physical `ImageView` dimensions to prevent OutOfMemory errors.

### Step 2: Unit and integration tests for loader infrastructure
* **Target File:** [`android/app/src/test/java/com/averonlabs/eshopverse/core/ui/ImageLoaderHelperTest.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/test/java/com/averonlabs/eshopverse/core/ui/ImageLoaderHelperTest.java)
* **Execution Checklist:**
  - [x] Null safety tests pass for empty or `null` URLs and default error image is loaded.
  - [x] Batch preloading (`preloadBatch`) verified with mixed lists of valid, empty, and null items.
  - [x] Fixed time constant for CrossFade animation asserted at exactly `250ms`.
  - [x] Command `./gradlew testDebugUnitTest --tests ImageLoaderHelperTest` finishes without errors.

---

## Phase 2: Android Adapters & Layouts

### Step 3: Catalog & Product Grid
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

### Step 4: Favorites List (Wishlist)
* **Target Files:**
  - [`android/app/src/main/java/com/averonlabs/eshopverse/catalog/FavoritesAdapter.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/catalog/FavoritesAdapter.java)
  - [`android/app/src/main/res/layout/fragment_favorites.xml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/res/layout/fragment_favorites.xml)
* **Execution Checklist:**
  - [x] `onViewRecycled` implemented in `FavoritesAdapter` to cancel queued tasks for recycled cards.
  - [x] Titles and prices of saved items rendered immediately from local database/cache without waiting for remote images.
  - [x] Shimmer placeholder visible immediately upon card binding.

### Step 5: Cart Screen
* **Target Files:**
  - [`android/app/src/main/java/com/averonlabs/eshopverse/cart/CartAdapter.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/cart/CartAdapter.java)
  - [`android/app/src/main/res/layout/item_cart_line.xml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/res/layout/item_cart_line.xml)
* **Execution Checklist:**
  - [x] Fixed thumbnail dimensions in XML (e.g., `80dp x 80dp`) with shimmer background to eliminate layout shift.
  - [x] Unit price, line total, price change notices, and item title prioritized at the top of `bind()`.
  - [x] `onViewRecycled` implemented to cancel cart item image downloads during scrolling or removal.

### Step 6: Checkout Review & Order Confirmation
* **Target Files:**
  - [`android/app/src/main/java/com/averonlabs/eshopverse/checkout/ReviewItemsAdapter.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/checkout/ReviewItemsAdapter.java)
  - [`android/app/src/main/res/layout/item_review_line.xml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/res/layout/item_review_line.xml)
* **Execution Checklist:**
  - [x] Render invoice breakdown and order item summaries without delay or network dependencies (<50ms).
  - [x] Implement `onViewRecycled` for order item thumbnails.
  - [x] In case of network error or offline state, invoice renders reliably with default error placeholder.

### Step 7: Product Detail Image Carousel
* **Target Files:**
  - [`android/app/src/main/java/com/averonlabs/eshopverse/catalog/ImageCarouselAdapter.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/catalog/ImageCarouselAdapter.java)
  - [`android/app/src/main/res/layout/item_product_image.xml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/res/layout/item_product_image.xml)
  - [`android/app/src/main/res/layout/fragment_product_detail.xml`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/res/layout/fragment_product_detail.xml)
* **Execution Checklist:**
  - [x] All text specifications (product name, technical specs, brand, price, stock status) rendered immediately from navigation arguments or cache.
  - [x] Carousel viewport at top has fixed `4:5` or `1:1` aspect ratio with shimmer placeholder.
  - [x] `onViewRecycled` implemented to cancel large image requests during rapid swiping.

---

## Phase 3: Android Prefetching & Scroll Optimization

### Step 8: Active prefetching based on scroll direction
* **Target Files:**
  - [`android/app/src/main/java/com/averonlabs/eshopverse/catalog/fragment/ExploreFragment.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/catalog/fragment/ExploreFragment.java)
  - [`android/app/src/main/java/com/averonlabs/eshopverse/shell/HomeFragment.java`](file:///d:/Ham/Code___/for_fiverr/eshop-verse/android/app/src/main/java/com/averonlabs/eshopverse/shell/HomeFragment.java)
* **Execution Checklist:**
  - [ ] Catalog scroll listener detects scroll direction.
  - [ ] On downward scroll, prefetch 4 to 6 upcoming product images into cache via `ImageLoaderHelper.preloadBatch()`.
  - [ ] Prefetch tasks run at Low Priority to ensure on-screen bandwidth is not degraded.

---

## Android Progress Tracking & Phase 11 Mapping

All remaining Android implementation items are tracked canonically in **[Android Roadmap Phase 11](ANDROID_ROADMAP.md)**:
- [x] **Step 1:** Resource cleanup method in `ImageLoaderHelper.java`
- [x] **Step 2:** Unit test validation in `ImageLoaderHelperTest.java`
- [x] **Step 3:** Implementation in `ProductGridAdapter.java` and grid layouts
- [x] **Step 4:** Implementation in `FavoritesAdapter.java`
- [x] **Step 5:** Implementation in `CartAdapter.java` and cart line layout
- [x] **Step 6:** Implementation in `ReviewItemsAdapter.java` (token polish in **IMG-004**)
- [x] **Step 7 (IMG-002):** Optimistic text-first transition in `ProductDetailFragment.java` & arguments
- [ ] **Step 8 (IMG-003):** Lookahead prefetching in fragments (low-priority tuning in **IMG-003**)
