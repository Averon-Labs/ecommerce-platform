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

## Implementation Steps

### Step 1: Design System & Specifications Documentation [COMPLETED]
- Updated `docs/DESIGN_SYSTEM.md` with:
  - The MUSE / Botanical Green (`#385740`) & Warm Ivory (`#F5F2EA`) color system and WCAG 2.2 AA contrast ratings.
  - Floating pill navigation specifications (28dp radius, 16dp bottom, 24dp horizontal margins).
  - Staggered Pinterest masonry card specifications (16dp radius, dynamic organic aspect ratios).
  - Pill button and editorial hero guidelines.
- Updated `docs/SCREEN_SPECIFICATIONS.md` to reflect:
  - M-01 (Home): Editorial hero banner, circular category mood chips, trending staggered grid.
  - M-02 (Explore): Staggered masonry catalog grid, pill search bar, pill category chips.
  - Navigation shell: Floating bottom pill navigation.

### Step 2: Android Resource Tokens & Drawables [COMPLETED]
- `res/values/colors.xml`: Added exact Figma color tokens (`color_botanical_green`, `color_warm_ivory`, `color_sandstone`, `color_terracotta`, `color_ink`, etc.) and mapped to Material 3 semantic color attributes.
- `res/values-night/colors.xml`: Added dark theme equivalents (`#151A16` canvas, `#A8C5A4` luminous sage, `#202720` surfaces).
- `res/values/dimens.xml`: Added dimensions for corner radii (16dp, 22dp, 28dp), floating nav margins and elevations.
- `res/values/styles.xml`: Updated button, card, and text styles to match the new tokens.
- Drawables:
  - `bg_floating_bottom_nav.xml`: Pill shape (28dp radius) with white/ivory surface and subtle outline.
  - `bg_pill_search.xml`: 22dp radius input background.
  - `bg_hero_editorial.xml`: Soft cream / botanical rounded container with 20dp radius.
  - `bg_circular_category.xml`: Circular pastel mood chips.
  - Category vectors: `ic_category_all.xml`, `ic_category_fashion.xml`, `ic_category_objects.xml`, `ic_category_beauty.xml`.

### Step 3: Floating Bottom Navigation Implementation [COMPLETED]
- `res/layout/activity_main.xml`:
  - Wrapped `BottomNavigationView` inside a `com.google.android.material.card.MaterialCardView` (`@+id/card_bottom_nav`) floating pill container.
  - Margins: 16dp bottom, 24dp start/end, 28dp corner radius, 8dp elevation, 1dp subtle hairline stroke.
- `MainActivity.java`:
  - Handled window insets dynamically: added system nav bar insets to `card_bottom_nav` bottom margin.
  - Updated visibility toggles on top-level vs child screens.
  - Updated content bottom padding calculation with safe breathing room.

### Step 4: Staggered Pinterest Product Grid & Catalog [COMPLETED]
- `res/layout/item_product_card.xml`:
  - 16dp rounded corners (`cardCornerRadius="16dp"`).
  - Clean tactile surface (`?attr/colorSurface`), subtle sandstone stroke.
  - Added `@+id/container_image`.
- `ProductGridAdapter.java`:
  - Added dynamic organic image heights (`150dp`, `195dp`, `170dp`, `220dp`, `185dp`) based on product ID for the Pinterest masonry look.
- `ExploreFragment.java` & `SearchResultsFragment.java` & `CategoryProductsFragment.java`:
  - Replaced `GridLayoutManager` with `StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)`.
  - Set gap handling: `GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS`.

### Step 5: Redesigned Home Screen (Discover / Home) [COMPLETED]
- `res/layout/fragment_home.xml`:
  - Brand header: Wordmark "EShop Verse", tagline "Curated lifestyle & design", and avatar initial with quick theme toggle.
  - Pill search bar: 22dp rounded input with search icon navigating to Explore.
  - Hero Editorial Card: "Make room for beautiful things", "Objects with soul. Made to last.", and pill CTA button "EXPLORE THE EDIT ↗".
  - Circular Category Mood Chips: "All", "Home", "Fashion", "Objects", "Beauty" with circular icons.
  - "Trending this week" header with 2-column staggered Pinterest product grid.
- `HomeFragment.java`:
  - Connected `CatalogListViewModel` to populate trending items.
  - Handled clicks for all category chips, hero CTA, pill search, and product details.
  - Added quick theme toggle button.

### Step 6: Verification & Quality Checks [COMPLETED]
- Ran `./gradlew testDebugUnitTest`: SUCCESSFUL (100% test pass).
- Ran `./gradlew :app:lintDebug`: SUCCESSFUL (0 errors).

---

## Testing Plan
- Unit tests: Verify `ThemeManagerTest`, catalog tests, and existing tests remain 100% green.
- Lint tests: Verify no deprecations, no hardcoded colors/dimensions, no minSdk violations.
- Visual verification: Verify floating navigation padding, touch targets (>= 48dp), and contrast ratios (>= 4.5:1).
