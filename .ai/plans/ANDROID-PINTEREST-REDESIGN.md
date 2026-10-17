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
- [ ] **Step 13: Replace 'E' Letter with Sun/Moon Icon in Circular Theme Toggle on Home**
  - Update `layout_theme_toggle` in `fragment_home.xml` and `HomeFragment.java` to replace the "E" TextView with an ImageView showing `ic_dark_mode` / `ic_light_mode`.
  - Preserve the circular border and container styling.
  - Wire toggle action and reflect current Day/Night mode.
- [ ] **Step 14: Adjust Home Hero Banner Light Mode Palette to Warm Cream with High-Contrast Text**
  - Update `bg_hero_editorial.xml` to use Soft Cream (`@color/color_soft_cream` `#F0E8D9`) or Soft Sage in light mode instead of dark green.
  - Retain dark slate `#202720` in dark mode.
  - Verify and update text colors inside the hero banner (`tv_hero_title`, `tv_hero_subtitle`, `btn_hero_cta`) to high-contrast Ink (`@color/color_ink` `#212621`) and Botanical Green button.
- [ ] **Step 15: Redesign Explore Screen Product Cards to Match Figma (Node `1:58`)**
  - Match Figma Explore product card structure and styling (`item_product_card.xml` / explore grid).
- [ ] **Step 16: Synchronize Home Mood Circles with Explore Tags & Direct Navigation**
  - Synchronize categories/tags between Home circular chips and Explore filter chips (exact same labels, count, and order).
  - Make horizontal chips scrollable if dynamic, and tapping a chip navigates to Explore with that category/tag pre-selected.
- [ ] **Step 17: Account Screen Direct Sign-In on Guest State**
  - If user is not signed in (guest), show the Sign In view directly rather than a separate greeting prompt, allowing instant login and a link to create an account.
- [ ] **Step 18: Add 'Continue with Google' Button with Google Logo to Auth Screens**
  - Add Google logo vector drawable `ic_google_logo.xml`.
  - Add 52dp pill "Continue with Google" button to `fragment_sign_in.xml` and `fragment_register.xml`.

---

### Phase 5: Verification, Git Remote Sync & Pull Request
- [ ] **Unit Tests:** `./gradlew testDebugUnitTest` passing 100%.
- [ ] **Android Lint:** `./gradlew :app:lintDebug` passing with 0 errors.
- [ ] **Git Remote Sync & PR:** Commit, push to `origin/feature/android-pinterest-redesign`, and verify PR status.

