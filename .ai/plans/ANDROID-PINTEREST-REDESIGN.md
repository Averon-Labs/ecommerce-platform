# Implementation Plan: ANDROID-PINTEREST-REDESIGN — Pinterest / MUSE Style Redesign

## Overview
This plan specifies the complete redesign of the Android Customer Application based on the extracted Figma design:
`https://www.figma.com/design/onAP6WyN93TaqPLKzJtoFl/Pinterest-Style-E-commerce-Mobile-App`
Figma File: **"MUSE. / DESIGN SYSTEM & E-Commerce Mobile App"** (Node `6:80` foundations & Node `1:2` Discover/Home).

---

## Visual Design Foundations (Figma Source of Truth)

### 1. Color Palette Tokens
| Token Name | Hex Code | Semantic Role |
| :--- | :--- | :--- |
| **Botanical Green** | `#385740` | Primary / Key Action / Filled CTA |
| **Deep Olive** | `#303E31` | Pressed State / Active Dark Emphasis |
| **Warm Ivory** | `#F5F2EA` | App Background / Light Canvas |
| **Soft Cream** | `#F0E8D9` | Editorial Panels / Hero Containers |
| **Sandstone** | `#E8DEC7` | Secondary Surface / Inactive Chips |
| **Terracotta** | `#D4A894` | Accent / Warm Highlights / Badges |
| **Natural Taupe** | `#C7B596` | Supporting Accent / Borders |
| **Ink** | `#212621` | Primary High-Contrast Text |
| **Muted Olive** | `#6E7268` | Secondary Text / Hints / Unselected Nav |
| **Soft Sage** | `#E6EBE4` | Container Tint / Selected Chips |
| **Sage Light / Luminous** | `#A8C5A4` | Dark Theme Accent / Highlights |
| **Pure White** | `#FFFFFF` | Card Surfaces / Floating Nav Body |

### 2. Dark Mode Palette
- Canvas Background: `#151A16` (Deep Botanical Slate/Black)
- Card & Surface: `#202720` / `#29332A`
- Primary / Accent: `#A8C5A4` (Luminous Sage)
- Primary Text: `#F5F2EA` (Warm Ivory)
- Secondary Text: `#B4B9AE` / `#6E7268`
- Outline: `#394339`

### 3. Typography & Shapes
- Font: Inter (with fallback to system sans-serif)
- Corner Radii:
  - Small / Chips: `8dp`
  - Cards / Product Tiles: `16dp`
  - Search Input / Form Fields: `22dp`
  - Pill Buttons & Floating Bottom Nav: `28dp` / `999dp`

---

## Checklist & Progress Status

### Phase 1: Design Foundations, Home & Core Shell (Completed)
- [x] **Step 1: Design System & Specifications Documentation**
  - Updated `docs/DESIGN_SYSTEM.md` with Figma MUSE palette, contrast table, and floating nav tokens.
  - Updated `docs/SCREEN_SPECIFICATIONS.md` with Home M-01 and Explore M-02 specs.
- [x] **Step 2: Android Resource Tokens & Drawables**
  - Added hex colors to `values/colors.xml` and `values-night/colors.xml`.
  - Added dimension tokens (28dp pill radius, 16dp margins, 22dp search input) in `values/dimens.xml`.
  - Created drawables: `bg_floating_bottom_nav.xml`, `bg_pill_search.xml`, `bg_hero_editorial.xml`, `bg_circular_category.xml`, and category icons.
- [x] **Step 3: Floating Bottom Navigation Implementation**
  - Implemented `card_bottom_nav` floating pill in `activity_main.xml` and window insets in `MainActivity.java`.
- [x] **Step 4: Staggered Pinterest Product Grid & Catalog Layout**
  - Updated `item_product_card.xml` with 16dp rounded corners and subtle stroke.
  - Updated `ProductGridAdapter.java` with organic staggered masonry heights.
  - Configured `StaggeredGridLayoutManager` in `ExploreFragment`, `SearchResultsFragment`, and `CategoryProductsFragment`.
- [x] **Step 5: Redesigned Home Screen (M-01 Discover / Home)**
  - Implemented editorial header, 22dp pill search, hero editorial card, circular category mood chips, and trending staggered grid in `fragment_home.xml` and `HomeFragment.java`.
- [x] **Step 6: Adaptive & Legacy App Launcher Icons**
  - Extracted foreground artwork with transparent background and safe zone padding (64.5%).
  - Generated all mipmap densities (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi) for adaptive and legacy round/square icons.
  - Updated `ic_launcher_background.xml` to `#FAF2E6`.
- [x] **Step 7: Verification & Quality Checks**
  - `./gradlew testDebugUnitTest` passed (100%).
  - `./gradlew :app:lintDebug` passed (0 errors).

---

### Phase 2: Feature Screens Redesign (Figma Screens)
> **App Identity & Refactoring Guideline (User Instruction):**
> The Figma template represents the visual UI/UX language (colors, bento layout, pill navigation, tactile cards), but the application is **EShop Verse**. All titles, brand wordmarks, subtitles, headers, and domain models must strictly reflect **EShop Verse** rather than placeholder template names (e.g. "MUSE"). Screens and components must be refactored to align with real project entities and architecture.

- [x] **Step 8: Redesign Explore / Search Screen (`02 — Explore / Search` - Node `1:58`)**
  - Canvas background: Warm Ivory (`#F5F2EA` in light, `#151A16` in dark).
  - Search input: 22dp pill styling matching MUSE (`bg_pill_search.xml` / `corner_search_pill`).
  - Category chips: Horizontal scroll with Soft Sage selected state and Sandstone hairline.
  - Sort icon button: 48dp pill button with white background and hairline border.
  - Results and category product screens updated with 100dp bottom padding for floating pill navigation.
- [x] **Step 9: Redesign Product Detail Screen (`03 — Product detail` - Node `1:122`)**
  - Canvas background: Warm Ivory (`#F5F2EA` light, `#151A16` dark).
  - Editorial image gallery: Bento card container with 20dp rounded corners (`@dimen/corner_bento`), subtle stroke, and pill indicator.
  - Typography & Brand tokens: Added uppercase category tag (`tv_product_category`), bold ink product title, and botanical green price (`@color/color_botanical_green`).
  - Tactile card container for quantity stepper (`layout_quantity`) with `@dimen/corner_card_pinterest`.
  - Sticky elevated bottom action bar with 52dp botanical green pill "Add to Bag" button (`@dimen/corner_pill`), status notice, and delivery note matching Figma (`Free shipping on orders over $100`).
  - Unit tests (`./gradlew testDebugUnitTest`) and lint (`./gradlew :app:lintDebug`) verified with 0 errors.
- [x] **Step 10: Redesign Shopping Bag / Cart Screen (`05 — Shopping bag / Checkout` - Node `1:201`)**
  - Canvas background: Warm Ivory (`#F5F2EA` light, `#151A16` dark).
  - Editorial header: EShop Verse brand subtitle and "Your Bag" title.
  - Cart line items: 16dp rounded card (`@dimen/corner_card_pinterest`) with 12dp rounded image container, bold ink product title, and botanical green line total.
  - Order summary Bento card: 20dp rounded corners (`@dimen/corner_bento`), hairline border, and 52dp botanical green pill checkout CTA button (`@dimen/corner_pill`).
  - Scroll container: 120dp bottom padding ensuring content floats cleanly above the 28dp pill bottom navigation bar.
- [x] **Step 11: Redesign Account & Authentication Screens (`06`, `07`, `08`, `09`)**
  - Updated button and input design tokens in `styles.xml`: 52dp Botanical Green filled pill buttons (`@dimen/corner_pill`), 52dp outlined pill buttons, 22dp pill inputs (`@dimen/corner_search_pill`), and 20dp Bento card panels with hairline border.
  - Redesigned `fragment_account.xml` (Figma `06` & `07`):
    - Signed-in profile hero Bento card with 60dp Soft Sage circular avatar container (`bg_avatar_circle`), bold Ink name, email, and Botanical Green status badge.
    - Profile details Bento card with 22dp pill inputs for first/last name and 52dp filled pill "Save Profile" button.
    - Guest welcome Bento card with 72dp Soft Sage circular icon, warm editorial prompt, 52dp filled "Sign In" pill button, and 52dp outlined "Create Account" pill button.
    - 120dp bottom padding on scroll container to float cleanly above the 28dp pill floating bottom navigation.
  - Redesigned `fragment_sign_in.xml` (Figma `08`):
    - Editorial brand header ("ESHOP VERSE", "Welcome back.", subtitle).
    - Bento card with 22dp pill inputs, password visibility toggle, forgot password action, and 52dp Botanical Green filled pill button.
  - Redesigned `fragment_register.xml` (Figma `09`):
    - Editorial header ("ESHOP VERSE", "Create an account.", subtitle).
    - Bento card with horizontal name fields, email, password, confirm password in 22dp pill styling, and 52dp filled pill button.
  - Redesigned `fragment_request_password_reset.xml` & `fragment_set_new_password.xml`:
    - Warm Ivory canvas, editorial typography, Bento cards, 22dp pill inputs, and 52dp pill buttons.

---

### Phase 4: UI Refinements & User Edits
- [x] **Step 12: Remove Top Toolbar across App Shell**
  - Removed `AppBarLayout` and `MaterialToolbar` from `activity_main.xml`.
  - Removed `appbar_scrolling_view_behavior` from `content_main.xml`.
  - Updated `MainActivity.java` window insets listener to apply status bar padding directly to root, removed ActionBar registration and options menu inflation.
  - Verified with `./gradlew testDebugUnitTest` (100% pass) and `./gradlew :app:lintDebug` (0 errors).
- [x] **Step 13: Replace 'E' Letter with Sun/Moon Icon in Circular Theme Toggle on Home**
  - Replaced the "E" TextView in `fragment_home.xml` with `ImageView iv_theme_toggle_icon` (22dp, centered) inside the 40dp circular frame (`btn_profile_avatar`).
  - Updated `HomeFragment.java` with `updateThemeIcon()` dynamically loading `ic_light_mode` (in dark theme) or `ic_dark_mode` (in light theme).
  - Wired circular button click to `ThemeManager.toggleTheme(requireContext())`.
  - Verified with `./gradlew testDebugUnitTest` (100% pass) and `./gradlew :app:lintDebug` (0 errors).
- [x] **Step 14: Adjust Home Hero Banner Light Mode Palette to Warm Cream with High-Contrast Text**
  - Added semantic hero banner tokens `color_hero_banner` (`#EAEFE8` pale fresh sage-cream in light, `#1F2720` in dark) and `color_hero_banner_stroke` (`#D2DDD0` in light, `#303D32` in dark).
  - Updated `bg_hero_editorial.xml` to use `@color/color_hero_banner` and `@dimen/corner_bento`.
  - Updated `card_hero_editorial` in `fragment_home.xml` with `@color/color_hero_banner` background and `@color/color_hero_banner_stroke` border.
  - Adjusted text colors: `tv_hero_curation` to `@color/color_botanical_green` (AAA contrast), `tv_hero_title` to `@color/color_ink`, `tv_hero_subtitle` to `@color/color_muted_olive`, and `btn_hero_cta` text to `?attr/colorOnPrimary`.
  - Verified with `./gradlew testDebugUnitTest` (100% pass) and `./gradlew :app:lintDebug` (0 errors).
- [x] **Step 15: Redesign Explore Screen Product Cards to Match Figma (Node `1:58`)**
  - Updated `item_product_card.xml`: 0dp flat elevation, hairline border (`@color/border_hairline`), 16dp rounded corners (`@dimen/corner_card_pinterest`), soft sage image backing (`@color/color_soft_sage`), bold ink product title with natural 1-2 line wrapping, and signature botanical green price (`@color/color_botanical_green`).
  - Updated `PriceTextView.java` to support explicit XML `android:textColor` overrides.
  - Updated `ProductGridAdapter.java` and `ProductCardView.java` to bind botanical green price text dynamically.
  - Verified with `./gradlew testDebugUnitTest` (100% pass) and `./gradlew :app:lintDebug` (0 errors).
- [x] **Step 16: Synchronize Home Mood Circles with Explore Tags & Direct Navigation**
  - Created `item_category_mood_circle.xml` with 56dp circular frame (`@drawable/bg_circular_category`), icon, and label.
  - Replaced static category layout in `fragment_home.xml` with horizontal `rv_home_categories` RecyclerView.
  - Updated `HomeFragment.java` to observe `CategoryViewModel.getCategoriesState()`, rendering exact same categories (and 'All' at index 0) in identical order and labels as Explore.
  - Configured navigation arguments (`categoryId`, `categoryName`) on `action_homeFragment_to_exploreFragment` in `nav_main.xml`.
  - Updated `ExploreFragment.java` with `handleIncomingArguments()` to automatically select the passed category chip and filter the product catalog immediately upon navigation.
  - Verified with `./gradlew testDebugUnitTest` (100% pass) and `./gradlew :app:lintDebug` (0 errors).
- [x] **Step 17: Account Screen Direct Sign-In on Guest State**
  - Replaced guest welcome greeting card in `fragment_account.xml` with direct Sign-In Bento Card including email input, password input with toggle, forgot password action, botanical green Sign-In pill CTA, and "Create Account" text link.
  - Updated `AccountFragment.java` to manage direct login via `AuthViewModel`, binding input validation errors, lockout timers, progress indicator, and instant session transition upon login/sign-out.
  - Added navigation actions in `nav_main.xml` from `accountFragment` to `registerFragment` and `requestPasswordResetFragment`.
  - Updated instrumentation test `SignInFlowTest.java` to test direct sign-in on `accountFragment`.
  - Verified with `./gradlew testDebugUnitTest` (100% pass) and `./gradlew :app:lintDebug` (0 errors).
- [x] **Step 18: Add 'Continue with Google' Button with Google Logo to Auth Screens**
  - Created authentic multi-color Google logo vector drawable `ic_google_logo.xml`.
  - Added `Widget.EShopVerse.Button.Google` style (52dp pill, white/surface surface, hairline border, ink text, untinted Google logo).
  - Added subtle "or" divider and 52dp "Continue with Google" button across `fragment_sign_in.xml`, `fragment_register.xml`, and `fragment_account.xml`.
  - Wired interactive click handlers in `SignInFragment.java`, `RegisterFragment.java`, and `AccountFragment.java`.
  - Verified with `./gradlew testDebugUnitTest` (100% pass) and `./gradlew :app:lintDebug` (0 errors).

---

### Phase 5: Verification, Git Remote Sync & Pull Request
- [x] **Unit Tests:** `./gradlew testDebugUnitTest` passing 100%.
- [x] **Android Lint:** `./gradlew :app:lintDebug` passing with 0 errors.
- [x] **Git Remote Sync & PR:** Commit, push to `origin/feature/android-pinterest-redesign`, and verify PR status.

---

### Phase 6: Real Catalog Seed, Sectioned Home, Square Grids & Explore Polish
- [x] **Step 19: Save & Parse `fake_products.md` and Setup Product Images Asset Pipeline**
  - Saved and parsed 10 categories and 50 products from `temp_data/fake_products.md`.
  - Copied all 50 product images from `temp_data/*.jpg` to `android/app/src/main/assets/products/*.jpg` for instant, reliable offline loading via `file:///android_asset/products/<id>.jpg`.
- [x] **Step 20: Replace Categories & Vector Icons in Explore and Home**
  - Updated `/categories` endpoint in `OfflineMockInterceptor.java` to return the 10 catalog categories (`Shirts & Tops`, `Pants & Jeans`, `Dresses`, `Jackets & Coats`, `T-Shirts`, `Activewear`, `Shoes`, `Accessories`, `Leather & Suede`, `Bags & Backpacks`).
  - Created bespoke vector drawables in `res/drawable/`: `ic_category_shirts.xml`, `ic_category_pants.xml`, `ic_category_dresses.xml`, `ic_category_jackets.xml`, `ic_category_tshirts.xml`, `ic_category_activewear.xml`, `ic_category_shoes.xml`, `ic_category_accessories.xml`, `ic_category_leather.xml`, `ic_category_bags.xml`.
  - Implemented `CategoryIconResolver.java` to resolve matching vector icons for all categories with robust fallbacks.
  - Updated Explore horizontal chips in `ExploreFragment.java` to display matching icons alongside labels with proper selection tinting.
  - Updated Home mood circles in `HomeFragment.java` to use `CategoryIconResolver` with existing styling and navigation to Explore.
  - Added unit test `CategoryIconResolverTest.java` and updated `OfflineMockInterceptorTest.java`.
  - Verified with `./gradlew testDebugUnitTest` (100% pass) and `./gradlew :app:lintDebug` (0 errors).
- [x] **Step 21: Redesign Product Placeholder Vector (Dribbble-Grade Minimalist Aesthetic)**
  - Replaced the generic "landscape photo" icon in `ic_image_placeholder.xml` with an atelier boutique apparel silhouette featuring an elegant curved coat hanger with soft rounded joints, a subtle ambient editorial aura backdrop (`@color/color_botanical_green` with 10% alpha), and minimalist Dribbble-grade sparkle accents.
  - Automatically inherited across product cards, cart items, order review items, product detail carousels, and `ProductCardView`.
  - Verified with `./gradlew testDebugUnitTest` (100% pass) and `./gradlew :app:lintDebug` (0 errors).
- [x] **Step 22: Clean Old Mock Catalog & Populate New Data in `OfflineMockInterceptor.java`**
  - Completely purged all 16 old mock products and obsolete categories.
  - Populated all 50 products from `temp_data/fake_products.md` with IDs (101–150), SKUs matching asset filenames (`shirt_001`–`bag_005`), exact catalog prices, rich boutique descriptions, and category associations.
  - Linked images directly to offline Android assets via `file:///android_asset/products/<id>.jpg` for instant, reliable loading.
  - Updated pagination (`perPage: 60`), product detail query handling (supporting both numeric IDs and string SKUs), and cart mock.
  - Updated `OfflineMockInterceptorTest.java` assertions for 50-item catalog, 5-item category filter, and updated product details.
  - Verified with `./gradlew testDebugUnitTest` (100% pass) and `./gradlew :app:lintDebug` (0 errors).
- [x] **Step 23: Uniform 1:1 Square Image Aspect Ratio & Equal Height Product Grid**
  - Updated `item_product_card.xml` with `ConstraintLayout` root and `container_image` set to `0dp` x `0dp` with strict `app:layout_constraintDimensionRatio="1:1"`.
  - Configured `tv_product_name` with `minLines="2"` and `maxLines="2"` guaranteeing perfectly equal heights for every card across the grid.
  - Removed dynamic staggered masonry height overrides in `ProductGridAdapter.java`.
  - Migrated `ExploreFragment.java`, `CategoryProductsFragment.java`, and `SearchResultsFragment.java` from `StaggeredGridLayoutManager` to clean, synchronized `GridLayoutManager(requireContext(), 2)`.
  - Verified with `./gradlew testDebugUnitTest` (100% pass) and `./gradlew :app:lintDebug` (0 errors).
- [x] **Step 24: Fix Explore Loading Spinner Layering Bug**
  - Updated `StateContainerView.java`:
    - Overrode `onFinishInflate()` to automatically adopt direct XML child views into `contentHost` (preventing child views from being inflated at a higher z-index above `messagePanel`).
    - Added `messagePanel.bringToFront()` on non-CONTENT state transitions to ensure progress spinners and error messages are never obscured by card views.
  - Added explicit `binding.stateContainer.setContentView(binding.rvProducts)` across `ExploreFragment.java`, `CategoryProductsFragment.java`, and `SearchResultsFragment.java`.
  - Verified with `./gradlew testDebugUnitTest` (100% pass) and `./gradlew :app:lintDebug` (0 errors).
- [x] **Step 25: Sectioned Home Screen Layout & Full Click Navigation**
  - Sectioned `fragment_home.xml` into 4 curated rows with distinct visual rhythms:
    - Section 1: "Trending this week" (`rv_trending_carousel`) — horizontal linear carousel.
    - Section 2: "Popular & Best Sellers" (`rv_bestsellers_grid`) — 2-column grid.
    - Section 3: "Special Offers" (`rv_special_offers_carousel`) — horizontal linear carousel.
    - Section 4: "Curated Essentials" (`rv_essentials_grid`) — 2-column grid.
  - Enhanced `ProductGridAdapter.java`:
    - Added `isCarousel` mode support to dynamically size carousel items to `164dp` (`@dimen/product_carousel_item_width`) for horizontal rows while retaining standard grid sizing for 2-column sections.
    - Attached click listeners to both `cardProduct` and `ivProductImage` ensuring all touch interactions reliably trigger product navigation.
  - Updated `HomeFragment.java` to set up and bind all 4 section adapters, slice catalog products cleanly across sections, and wire all section "See all" buttons to `ExploreFragment`.
  - Configured explicit Jetpack Navigation actions to `productDetailFragment` across `HomeFragment.java`, `ExploreFragment.java`, `CategoryProductsFragment.java`, and `SearchResultsFragment.java`.
  - Verified with `./gradlew testDebugUnitTest` (100% pass) and `./gradlew :app:lintDebug` (0 errors).
- [x] **Step 26: Verification, Unit Tests & Android Lint**
  - `./gradlew testDebugUnitTest` passing 100% (26 tasks executed, 0 failures).
  - `./gradlew :app:lintDebug` passing with 0 errors.
  - Phase 6 (Real Catalog Seed, Sectioned Home, Square Grids & Explore Polish) completed and verified.

---

### Phase 7: UX Polish, Data Loading Architecture, Stability & Real-time Search

> **Architectural Non-Negotiables:**
> 1. **Immediate Data Rendering & Non-blocking Pinterest Placeholders:** Product info, titles, prices, and cards render immediately without waiting for images. Every image uses `@drawable/ic_image_placeholder` with smooth async Glide cross-fade.
> 2. **Incremental Lazy Loading & Smart Pre-fetch:** 20 items per page with background prefetching triggered around item 18/19 without disruption.

- [x] **Step 27: Architecture Principles & Non-blocking Placeholder/Lazy Loading Pipeline**
  - Documented the two core principles in `docs/ARCHITECTURE.md`, `android/AGENTS.md`, and plan.
  - Enhanced Glide image loading across `ProductGridAdapter.java`, `CartAdapter.java`, `ImageCarouselAdapter.java`, and `ReviewItemsAdapter.java` with `DrawableTransitionOptions.withCrossFade()`.
  - Updated `OfflineMockInterceptor.java` to handle pagination parameters (`page`, `per_page=20`), delivering realistic page slices and prefetching.
  - Updated `OfflineMockInterceptorTest.java` with pagination verification; `./gradlew testDebugUnitTest` and `:app:lintDebug` passed with 0 errors.
- [x] **Step 28 (Priority): Product Detail Screen Crash Elimination, Navigation, Back Button & Add to Bag**
  - Fix crash on product click from Home and Explore:
    - In `nav_main.xml`, update `action_homeFragment_to_productDetailFragment` and `action_exploreFragment_to_productDetailFragment` to target `@id/nav_catalog` (the top-level nested graph id) instead of inner `@id/productDetailFragment`.
    - In `nav_catalog.xml`, add `<argument android:name="productId" android:defaultValue="-1L" app:argType="long" />` to the root `<navigation>` element so argument bundles passed to `nav_catalog` are automatically forwarded to start destination `productDetailFragment`.
    - In `HomeFragment.java` and `ExploreFragment.java`, add safe navigation and fallback handling `navController.navigate(R.id.nav_catalog, args)`.
  - Add top circular back button to `fragment_product_detail.xml` (40dp frosted/surface pill button with `ic_arrow_back` / `ic_back` and 48dp touch target) allowing user to pop back to previous screen.
  - In `ProductDetailFragment.java`:
    - Add null safety for `product.getImages()` (preventing NPE when image list is null).
    - Wire `btn_add_to_cart` to `CartRepository.addItem(...)` with quantity selection, visual confirmation feedback (Snackbar), and proper state handling.
  - Verify with `./gradlew testDebugUnitTest` and `:app:lintDebug`.
- [x] **Step 29: Cart Screen Quantity Stepper & Removal Stability**
  - Fix `QuantityStepperView.java`: replace bare text buttons with dedicated vector drawables/icons (`ic_minus.xml`, `ic_plus.xml`), ensure high-contrast tints (`@color/color_botanical_green` / `@color/text_primary_ink`), and remove text padding clipping.
  - Fix Cart trash / remove button: update `OfflineMockInterceptor.java` to handle `DELETE /cart/items/{productId}` and update the in-memory cart state dynamically so items are removed and totals recomputed.
- [x] **Step 30: Account Screen Google Sign-In & Forgot Password Crash Fix**
  - Anchored Snackbars across `AccountFragment.java` and `CartFragment.java` using `snackbar.setAnchorView(bottomNav)` (anchored to `card_bottom_nav`), ensuring feedback toasts float clearly above the floating pill bottom navigation bar.
  - Resolved Forgot Password and Register navigation:
    - Added first-class fragment destinations `registerFragment`, `requestPasswordResetFragment`, and `setNewPasswordFragment` directly in `nav_main.xml`.
    - Configured actions `action_requestPasswordResetFragment_to_setNewPasswordFragment`, `action_setNewPasswordFragment_to_requestPasswordResetFragment`, and `action_setNewPasswordFragment_to_signInFragment`.
    - Added safe navigation try-catch fallbacks across `AccountFragment.java`, `RequestPasswordResetFragment.java`, and `SetNewPasswordFragment.java`.
  - Added anchored success snackbar on profile update (`R.string.auth_feedback_profile_updated`).
  - Verified with `./gradlew testDebugUnitTest` (100% pass) and `./gradlew :app:lintDebug` (0 errors).
- [ ] **Step 31: Home Category Mood Circles & "All" Icon Aesthetic Upgrade**
  - Redesign `ic_category_all.xml` with a sleek, minimalist Dribbble/Pinterest-grade boutique bento/sparkle vector.
  - Fix mood circle spacing: set uniform fixed item width (`72dp`) and balanced horizontal margins (`10dp`) on `item_category_mood_circle.xml` so all circles are spaced strictly equally regardless of label width.
  - Streamline category labels to punchy, elegant single-word titles ("Shirts", "Pants", "Dresses", "Jackets", "T-Shirts", "Activewear", "Shoes", "Accessories", "Leather", "Bags") with centered text.
- [ ] **Step 32: Swipe-to-Refresh on Home, Explore, and Cart**
  - Wrap content layouts with `SwipeRefreshLayout` in `fragment_home.xml`, `fragment_explore.xml`, and `fragment_cart.xml`.
  - Style swipe spinners with `@color/color_botanical_green` indicator tint.
  - Connect pull-to-refresh to `CatalogListViewModel.reload()`, `CartViewModel.loadCart()`, and home feeds.
- [ ] **Step 33: Instant Explore Search Filtering & Crash Elimination**
  - Remove buggy cross-graph navigation on search input.
  - Implement real-time in-place grid filtering with `TextWatcher` and debounce (starts filtering immediately once 2 characters are typed).
  - Add null safety across search query and list updates.
- [ ] **Step 34: Verification, Unit Tests & Android Lint**
  - Verify clean test run (`testDebugUnitTest`) and lint check (`lintDebug`).

