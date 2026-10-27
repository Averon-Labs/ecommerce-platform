---
name: text-first-async-image-loading
description: >
  Mandatory engineering and architectural rules for Text-First Immediate Rendering and
  Independent Asynchronous Image Loading across Android (Java/XML) and Web (React/Tailwind).
  Guarantees zero-blocking UI, zero Cumulative Layout Shift (CLS = 0), viewport-scoped lazy loading,
  and smooth progressive image transitions.
---

# Text-First Immediate Rendering & Independent Asynchronous Image Loading

This skill defines the non-negotiable architectural standard for loading list data, catalog items, cards, and detail views in EShop Verse. It eliminates network bottlenecks, UI thread stutter (jank), layout jumps (CLS), and ensures users instantly read product metadata while image assets stream independently in the background.

---

## 1. Core Principles

### 1.1 Strict Payload Separation
- **API Payloads:** Network endpoints (REST/JSON) must only transmit text, numerical metadata, and image URLs (`String`). Never bundle raw binary or base64 image data within list payloads.
- **Immediate Text Binding:** All textual components (Title, Price, Stock Status, Discounts, Badges) must be bound and rendered synchronously to the UI thread (<100ms) the moment data arrives from memory or network.

### 1.2 Zero Cumulative Layout Shift (CLS = 0)
- **Fixed Aspect Ratio Viewports:** Every `ImageView` or image container must have an explicit, immutable aspect ratio or fixed dimension before image bytes arrive.
  - Catalog Grid Cards: `1:1` square (`app:layout_constraintDimensionRatio="1:1"`).
  - Editorial Hero / Banners: `16:9` (`app:layout_constraintDimensionRatio="16:9"`).
  - Product Detail Gallery: `4:5` or `1:1`.
  - Cart & Order Line Thumbnails: Fixed dimensions (e.g. `80dp x 80dp`).
- **Never use `wrap_content` for width or height** on network image containers.

### 1.3 Independent Asynchronous Streaming
- Each image is treated as a separate, independent network job dispatched to a background thread pool (managed via `ImageLoaderHelper` / Glide).
- If one image fails, times out, or has high latency, it must **never** delay, stall, or prevent adjacent text or other images from displaying.

### 1.4 Curated Placeholder & Seamless Cross-Fade
- **Instant Placeholder:** A refined placeholder (such as brand shimmer `@drawable/bg_placeholder_shimmer` or watermark silhouette `@drawable/ic_image_placeholder`) is shown immediately when the view is bound.
- **Smooth Transition:** When the background bitmap decoding completes, it cross-fades into view smoothly (200–250ms) without flickering or white flashes.
- **Graceful Error:** Failed loads display an editorial vector (`@drawable/bg_placeholder_error`), never an empty void or broken link graphic.

### 1.5 Viewport-Scoped Lazy Loading & Request Cancellation
- Only items currently entering or inside the viewport issue image fetch requests.
- When an item scrolls out of view and its holder is recycled (`onViewRecycled`), pending network jobs for that view must be cancelled or deprioritized immediately (`ImageLoaderHelper.clear(imageView)`).

---

## 2. Android Implementation Standard (Java & XML)

### 2.1 Standard Adapter Binding Pattern

In every `RecyclerView.Adapter`, `onBindViewHolder` must adhere strictly to this sequence:

```java
@Override
public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
    ProductSummary product = items.get(position);

    // STEP 1: Synchronous Text & State Rendering (INSTANT - UI Thread)
    holder.binding.tvProductName.setText(product.getName());
    holder.binding.tvProductPrice.setFormattedPrice(MoneyFormat.format(product.getPriceMoney()));
    
    // Status badges & text-based conditions
    if (!product.isInStock()) {
        holder.binding.badgeOutOfStock.setVisibility(View.VISIBLE);
    } else {
        holder.binding.badgeOutOfStock.setVisibility(View.GONE);
    }

    // Accessibility content description (combines all text metadata)
    holder.itemView.setContentDescription(product.getName() + ", " + MoneyFormat.format(product.getPriceMoney()));

    // STEP 2: Independent, Non-blocking Image Loading via ImageLoaderHelper
    ImageLoaderHelper.loadImage(
        holder.binding.ivProductImage,
        product.getImageUrl(),
        R.drawable.bg_placeholder_shimmer,
        R.drawable.bg_placeholder_error,
        true // centerCrop
    );
}

@Override
public void onViewRecycled(@NonNull MyViewHolder holder) {
    super.onViewRecycled(holder);
    // STEP 3: Cancel pending Glide requests to free up background threads
    ImageLoaderHelper.clear(holder.binding.ivProductImage);
}
```

### 2.2 XML Layout Rules for Zero CLS

```xml
<!-- Example: Catalog Card Viewport with 1:1 Aspect Ratio -->
<androidx.constraintlayout.widget.ConstraintLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <com.google.android.material.imageview.ShapeableImageView
        android:id="@+id/ivProductImage"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:scaleType="centerCrop"
        android:background="@drawable/bg_placeholder_shimmer"
        app:layout_constraintDimensionRatio="1:1"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Text elements below are positioned relative to the fixed viewport -->
    <TextView
        android:id="@+id/tvProductName"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:layout_constraintTop_toBottomOf="@id/ivProductImage"
        ... />

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## 3. Web & Admin Implementation Standard (React / TypeScript / Tailwind)

For the Admin Dashboard and web components:

1. **Native Lazy Loading:** Always use `loading="lazy"` and `decoding="async"` on `<img>` tags.
2. **Fixed Aspect Ratios:** Use Tailwind aspect-ratio utilities (`aspect-square`, `aspect-video`) or explicit `width` and `height` attributes.
3. **Skeleton Pulse:** Use CSS shimmer or Tailwind `animate-pulse` on a background div until `onLoad` fires.

```tsx
<div className="relative aspect-square w-full overflow-hidden rounded-xl bg-sandstone-100">
  {/* Text is already mounted elsewhere in the card */}
  <img
    src={product.imageUrl}
    alt={product.name}
    loading="lazy"
    decoding="async"
    className={`h-full w-full object-cover transition-opacity duration-300 ${isLoaded ? 'opacity-100' : 'opacity-0'}`}
    onLoad={() => setIsLoaded(true)}
  />
</div>
```

---

## 4. Anti-Patterns to Strictly Avoid

- ❌ **NEVER block the UI thread** or await image downloads in Repositories, ViewModels, or Adapters.
- ❌ **NEVER use `wrap_content`** on network image views (causes jarring layout reflows when images arrive).
- ❌ **NEVER leave image views empty/blank** while loading; always provide an active shimmer placeholder.
- ❌ **NEVER show broken icon graphics** or alert dialogs when an image fails; quietly fallback to the editorial error vector.
- ❌ **NEVER download images in a global batch** upfront before rendering list text.

---

## 5. Execution Plan Reference

For the comprehensive 4-phase, 10-step implementation roadmap across all EShop Verse components (Android adapters, layouts, prefetching, and Admin web views), see the full specification:
👉 [`docs/IMAGE_LOADING_EXECUTION_PLAN.md`](../../docs/IMAGE_LOADING_EXECUTION_PLAN.md)

