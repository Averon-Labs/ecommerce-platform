# Rule 03: Modern Dribbble & Behance Visual Patterns

The secret to transforming generic "boring enterprise" Android screens into slick, viral Dribbble designs lies in **four architectural techniques**:
1. **Asymmetric Bento Grids** (Dynamic visual rhythm)
2. **Layered Strokes & Micro-Border Depth** (Crisp lines over muddy shadows)
3. **Glassmorphism & Frosted Surfaces** (Translucent backgrounds + soft blur)
4. **Floating Island Controls** (Detached bottom bars and pill actions)

---

## 1. Mobile Bento Grid Layout Architecture

A Bento Grid organizes varied content into asymmetric, rounded cards that create an instant feeling of craftsmanship.

```
┌──────────────────────────────────────────────┐
│  FEATURE HERO CARD (2 Columns Wide, 180dp)   │
│  • Large stat / Accent illustration          │
│  • Gradient or primary-container tint        │
├──────────────────────┬───────────────────────┤
│  SQUARE CARD A       │  SQUARE CARD B        │
│  (1 Col, 140dp)      │  (1 Col, 140dp)       │
│  • Circular Progress │  • Quick Action Switch│
├──────────────────────┴───────────────────────┤
│  WIDE METRIC ROW (2 Columns Wide, 88dp)      │
│  • Mini sparkline / live status tracker      │
└──────────────────────────────────────────────┘
```

### Complete Bento Layout in ConstraintLayout
```xml
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="16dp">

    <!-- HERO BENTO (Span 2 Columns) -->
    <com.google.android.material.card.MaterialCardView
        android:id="@+id/cardBentoHero"
        android:layout_width="0dp"
        android:layout_height="180dp"
        app:cardCornerRadius="24dp"
        app:cardElevation="0dp"
        app:strokeWidth="1dp"
        app:strokeColor="?attr/colorOutlineVariant"
        app:cardBackgroundColor="?attr/colorPrimaryContainer"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <androidx.constraintlayout.widget.ConstraintLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:padding="20dp">

            <TextView
                android:id="@+id/tvHeroBadge"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="PRO OVERVIEW"
                android:textAppearance="?attr/textAppearanceLabelSmall"
                android:textColor="?attr/colorPrimary"
                android:background="@drawable/bg_pill_badge"
                android:paddingHorizontal="10dp"
                android:paddingVertical="4dp"
                app:layout_constraintTop_toTopOf="parent"
                app:layout_constraintStart_toStartOf="parent" />

            <TextView
                android:id="@+id/tvHeroTitle"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:text="$24,850.00"
                android:fontFamily="sans-serif-black"
                android:textSize="32sp"
                android:textColor="?attr/colorOnPrimaryContainer"
                android:layout_marginTop="12dp"
                app:layout_constraintTop_toBottomOf="@id/tvHeroBadge"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintEnd_toEndOf="parent" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="+18.4% vs last month"
                android:textColor="?attr/colorOnPrimaryContainer"
                android:alpha="0.8"
                android:textSize="13sp"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintBottom_toBottomOf="parent" />
        </androidx.constraintlayout.widget.ConstraintLayout>
    </com.google.android.material.card.MaterialCardView>

    <!-- SQUARE CARD LEFT (Span 1 Column) -->
    <com.google.android.material.card.MaterialCardView
        android:id="@+id/cardBentoLeft"
        android:layout_width="0dp"
        android:layout_height="140dp"
        android:layout_marginTop="12dp"
        android:layout_marginEnd="6dp"
        app:cardCornerRadius="20dp"
        app:cardElevation="0dp"
        app:strokeWidth="1dp"
        app:strokeColor="?attr/colorOutlineVariant"
        app:cardBackgroundColor="?attr/colorSurface"
        app:layout_constraintTop_toBottomOf="@id/cardBentoHero"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toStartOf="@id/cardBentoRight" />

    <!-- SQUARE CARD RIGHT (Span 1 Column) -->
    <com.google.android.material.card.MaterialCardView
        android:id="@+id/cardBentoRight"
        android:layout_width="0dp"
        android:layout_height="140dp"
        android:layout_marginTop="12dp"
        android:layout_marginStart="6dp"
        app:cardCornerRadius="20dp"
        app:cardElevation="0dp"
        app:strokeWidth="1dp"
        app:strokeColor="?attr/colorOutlineVariant"
        app:cardBackgroundColor="?attr/colorSurface"
        app:layout_constraintTop_toBottomOf="@id/cardBentoHero"
        app:layout_constraintStart_toEndOf="@id/cardBentoLeft"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## 2. Glassmorphism & Translucency on Android

True frosted glass on Android requires handling two API tiers:
1. **API 31+ (Android 12+)**: Hardware-accelerated `RenderEffect.createBlurEffect()`.
2. **Pre-API 31**: High-fidelity semi-transparent shape gradient with 1dp crisp stroke.

### Universal Glass Drawable (`res/drawable/bg_glass_card.xml`)
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <!-- Semi-transparent luminous tint -->
    <solid android:color="#26FFFFFF" /> <!-- 15% white opacity -->
    
    <!-- 1dp crisp border creates the glass edge highlight -->
    <stroke
        android:width="1dp"
        android:color="#4DFFFFFF" /> <!-- 30% white stroke -->
        
    <corners android:radius="24dp" />
</shape>
```

### Applying Native Hardware Blur in Java (API 31+)
```java
public static void applyFrostedGlass(@NonNull View targetView, float blurRadius) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Native GPU blur effect
        RenderEffect blurEffect = RenderEffect.createBlurEffect(
            blurRadius, 
            blurRadius, 
            Shader.TileMode.CLAMP
        );
        targetView.setRenderEffect(blurEffect);
    }
}
```

---

## 3. Floating Island Navigation (Bottom Pill)

Instead of the full-width edge-stuck bottom navigation bar from 2015, Dribbble designs use a **floating, detached navigation pill**:

```xml
<com.google.android.material.card.MaterialCardView
    android:id="@+id/navIslandContainer"
    android:layout_width="wrap_content"
    android:layout_height="64dp"
    android:layout_marginBottom="24dp"
    app:cardCornerRadius="32dp"
    app:cardElevation="6dp"
    app:strokeWidth="1dp"
    app:strokeColor="?attr/colorOutlineVariant"
    app:cardBackgroundColor="?attr/colorSurface"
    app:layout_constraintBottom_toBottomOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent">

    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottomNavIsland"
        android:layout_width="wrap_content"
        android:layout_height="match_parent"
        android:background="@android:color/transparent"
        app:elevation="0dp"
        app:labelVisibilityMode="unlabeled"
        app:itemIconSize="24dp"
        app:itemIconTint="@color/selector_nav_icon_tint"
        app:itemRippleColor="?attr/colorControlHighlight"
        app:menu="@menu/menu_island_nav" />
</com.google.android.material.card.MaterialCardView>
```

---

## 4. Glowing Accent Gradients

To add a signature Dribbble ambient glow behind a hero card or metric:

```xml
<!-- res/drawable/bg_ambient_glow.xml -->
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <gradient
        android:type="radial"
        android:gradientRadius="160dp"
        android:startColor="#4D6366F1" <!-- 30% Indigo glow -->
        android:centerColor="#1A6366F1" <!-- 10% Fade -->
        android:endColor="#00000000"   <!-- 0% Pure transparent --> />
</shape>
```
Place an empty `View` with this background right behind your primary hero card with `-20dp` margin offset to generate a luxury ambient bloom.
