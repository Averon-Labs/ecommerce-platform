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
- [x] **Step 8: Redesign Explore / Search Screen (`02 — Explore / Search` - Node `1:58`)**
  - Canvas background: Warm Ivory (`#F5F2EA` in light, `#151A16` in dark).
  - Search input: 22dp pill styling matching MUSE (`bg_pill_search.xml` / `corner_search_pill`).
  - Category chips: Horizontal scroll with Soft Sage selected state and Sandstone hairline.
  - Sort icon button: 48dp pill button with white background and hairline border.
  - Results and category product screens updated with 100dp bottom padding for floating pill navigation.
- [ ] **Step 9: Redesign Product Detail Screen (`03 — Product detail` - Node `1:122`)**
  - Editorial header, rounded image gallery with 20dp corners, warm ivory canvas.
  - Botanical green pill "Add to Bag" button and quantity stepper.
- [ ] **Step 10: Redesign Shopping Bag / Cart Screen (`05 — Shopping bag / Checkout` - Node `1:201`)**
  - Cart item cards with tactile surface and rounded styling.
  - Order summary container and pill checkout CTA.
- [ ] **Step 11: Redesign Account & Authentication Screens (`06`, `07`, `08`, `09`)**
  - Signed-in profile card and guest banner.
  - Form fields with 22dp pill inputs and filled primary buttons.
