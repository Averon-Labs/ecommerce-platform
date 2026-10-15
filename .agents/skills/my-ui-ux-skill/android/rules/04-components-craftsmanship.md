# Rule 04: Component Craftsmanship & Architecture

Modern Android applications rely on Google's Material Components (`com.google.android.material.*`), but stock implementations often feel generic. This guide shows how to customize them for bespoke, high-end aesthetics.

---

## 1. MaterialCardView: The Building Block

Never use raw `CardView` (from `androidx.cardview`). Always use `com.google.android.material.card.MaterialCardView` for vector border strokes and dynamic theme color matching.

### The 3 Modern Card Variants

#### A. The Outlined Card (Default for Clean Modern UI)
```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="20dp"
    app:cardElevation="0dp"
    app:strokeWidth="1dp"
    app:strokeColor="?attr/colorOutlineVariant"
    app:cardBackgroundColor="?attr/colorSurface"
    android:clickable="true"
    android:focusable="true"
    app:rippleColor="?attr/colorControlHighlight">
    
    <!-- Card Content -->
</com.google.android.material.card.MaterialCardView>
```

#### B. The Filled Tonal Card (For Grouped Content)
```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="20dp"
    app:cardElevation="0dp"
    app:strokeWidth="0dp"
    app:cardBackgroundColor="?attr/colorSurfaceVariant">
    
    <!-- Card Content -->
</com.google.android.material.card.MaterialCardView>
```

#### C. The Floating Elevated Card (For Overlays and Modals)
```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="24dp"
    app:cardElevation="4dp"
    app:strokeWidth="1dp"
    app:strokeColor="?attr/colorOutlineVariant"
    app:cardBackgroundColor="?attr/colorSurface">
    
    <!-- Card Content -->
</com.google.android.material.card.MaterialCardView>
```

---

## 2. Buttons & Extended FABs

### Modern Pill Button (Full Height & Rounded Corners)
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btnPrimaryAction"
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:text="Continue to Payment"
    android:textAllCaps="false"
    android:textSize="16sp"
    android:fontFamily="sans-serif-medium"
    app:cornerRadius="28dp"
    app:elevation="0dp"
    app:backgroundTint="?attr/colorPrimary"
    android:textColor="?attr/colorOnPrimary"
    app:icon="@drawable/ic_arrow_forward"
    app:iconGravity="textEnd"
    app:iconTint="?attr/colorOnPrimary" />
```

### Modern Floating Action Button (Extended FAB)
```xml
<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
    android:id="@+id/fabCreate"
    android:layout_width="wrap_content"
    android:layout_height="56dp"
    android:text="New Transaction"
    android:textAllCaps="false"
    android:textColor="?attr/colorOnPrimaryContainer"
    app:backgroundTint="?attr/colorPrimaryContainer"
    app:icon="@drawable/ic_add_24"
    app:iconTint="?attr/colorOnPrimaryContainer"
    app:cornerRadius="28dp"
    app:elevation="4dp"
    android:layout_margin="24dp" />
```

---

## 3. High-Craft Bottom Sheets (28dp Corner + Edge-to-Edge)

A proper Dribbble-style bottom sheet features a visible drag handle, rounded top corners, and full keyboard inset accommodation:

### Bottom Sheet Layout (`layout/fragment_modern_bottom_sheet.xml`)
```xml
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bg_bottom_sheet_surface"
    android:padding="24dp">

    <!-- Subtle Drag Handle Indicator -->
    <View
        android:id="@+id/sheetDragHandle"
        android:layout_width="36dp"
        android:layout_height="4dp"
        android:background="@drawable/bg_pill_handle"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <TextView
        android:id="@+id/tvSheetTitle"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:text="Select Destination"
        android:textAppearance="?attr/textAppearanceTitleLarge"
        android:textColor="?attr/colorOnSurface"
        android:layout_marginTop="20dp"
        app:layout_constraintTop_toBottomOf="@id/sheetDragHandle"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### Drag Handle Shape (`res/drawable/bg_pill_handle.xml`)
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="?attr/colorOutlineVariant" />
    <corners android:radius="999dp" />
</shape>
```

---

## 4. Modern Input Fields (TextInputLayout)

Never use raw `EditText`. Always wrap in `com.google.android.material.textfield.TextInputLayout`:

```xml
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/tilEmail"
    style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="Email Address"
    app:boxCornerRadiusTopStart="16dp"
    app:boxCornerRadiusTopEnd="16dp"
    app:boxCornerRadiusBottomStart="16dp"
    app:boxCornerRadiusBottomEnd="16dp"
    app:boxStrokeWidth="1dp"
    app:boxStrokeColor="@color/selector_input_stroke"
    app:startIconDrawable="@drawable/ic_mail_24"
    app:startIconTint="?attr/colorOnSurfaceVariant"
    app:helperText="We will never share your email"
    app:helperTextTextColor="?attr/colorOnSurfaceVariant"
    app:errorEnabled="true">

    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/etEmail"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="textEmailAddress"
        android:textColor="?attr/colorOnSurface"
        android:textSize="16sp" />
</com.google.android.material.textfield.TextInputLayout>
```

---

## 5. Horizontal Filter Chips (Anti-Clipping Rule)

When displaying horizontally scrollable chip sets, **always set `clipToPadding="false"`**:

```xml
<HorizontalScrollView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:scrollbars="none"
    android:clipToPadding="false"
    android:paddingStart="16dp"
    android:paddingEnd="16dp">

    <com.google.android.material.chip.ChipGroup
        android:id="@+id/chipGroupFilters"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:singleLine="true"
        app:chipSpacingHorizontal="8dp">

        <com.google.android.material.chip.Chip
            style="@style/Widget.Material3.Chip.Filter"
            android:layout_width="wrap_content"
            android:layout_height="40dp"
            android:text="All Items"
            app:chipCornerRadius="20dp" />

        <com.google.android.material.chip.Chip
            style="@style/Widget.Material3.Chip.Filter"
            android:layout_width="wrap_content"
            android:layout_height="40dp"
            android:text="Analytics"
            app:chipCornerRadius="20dp" />
    </com.google.android.material.chip.ChipGroup>
</HorizontalScrollView>
```
*Why `clipToPadding="false"`?* It allows chips to start with a comfortable 16dp margin from the screen edge while still scrolling cleanly all the way across the display boundary.
