# Rule 02: XML Layouts & Hierarchy Architecture

## 1. The Zero-Nesting Mandate

Deeply nested XML layouts (`LinearLayout` inside `RelativeLayout` inside `FrameLayout`) cause catastrophic frame drops (jank) during scroll and animations due to repeated recursive `onMeasure` and `onLayout` passes.

### Golden Performance Rules
1. **Target View Tree Depth**: Maximum depth $\le 4$ levels from root to deepest leaf.
2. **Never nest `layout_weight`**: Nested weights force $O(2^N)$ measure passes.
3. **Use ConstraintLayout 2.1+ Helpers**: Use `Guideline`, `Barrier`, and `Flow` instead of intermediate grouping containers.

---

## 2. Advanced ConstraintLayout Patterns

### A. The 8dp Responsive Guideline Grid
Always align your screen content using percentage or fixed dp guidelines to preserve breathing room:

```xml
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- 16dp Start Margin Guideline -->
    <androidx.constraintlayout.widget.Guideline
        android:id="@+id/guideStart"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        app:layout_constraintGuide_begin="16dp" />

    <!-- 16dp End Margin Guideline -->
    <androidx.constraintlayout.widget.Guideline
        android:id="@+id/guideEnd"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        app:layout_constraintGuide_end="16dp" />

    <!-- Views align strictly between guidelines -->
    <TextView
        android:id="@+id/tvScreenTitle"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:textAppearance="?attr/textAppearanceHeadlineMedium"
        android:textColor="?attr/colorOnBackground"
        app:layout_constraintStart_toStartOf="@id/guideStart"
        app:layout_constraintEnd_toEndOf="@id/guideEnd"
        app:layout_constraintTop_toTopOf="parent"
        android:layout_marginTop="24dp" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

### B. Dynamic Content Protection with `Barrier`
When dealing with dynamic user text (e.g. localized names or variable descriptions), elements must never collide or overlap:

```xml
<!-- Barrier prevents elements below from overlapping either title or subtitle -->
<androidx.constraintlayout.widget.Barrier
    android:id="@+id/barrierHeader"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:barrierDirection="bottom"
    app:constraint_referenced_ids="tvTitle,tvSubtitle,ivAvatar" />

<com.google.android.material.card.MaterialCardView
    android:id="@+id/cardContent"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:layout_marginTop="16dp"
    app:layout_constraintTop_toBottomOf="@id/barrierHeader"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent" />
```

---

### C. Replacing Nested `LinearLayout` with `Flow`
Instead of creating horizontal or vertical linear containers for chips, button rows, or metric badges, use `androidx.constraintlayout.helper.widget.Flow`:

```xml
<androidx.constraintlayout.helper.widget.Flow
    android:id="@+id/flowMetrics"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    app:constraint_referenced_ids="cardStat1,cardStat2,cardStat3"
    app:flow_wrapMode="chain"
    app:flow_horizontalStyle="spread"
    app:flow_horizontalGap="12dp"
    app:flow_verticalGap="12dp"
    app:layout_constraintStart_toStartOf="@id/guideStart"
    app:layout_constraintEnd_toEndOf="@id/guideEnd"
    app:layout_constraintTop_toBottomOf="@id/tvScreenTitle"
    android:layout_marginTop="16dp" />

<!-- Individual cards exist at the root level without intermediate wrappers! -->
<com.google.android.material.card.MaterialCardView
    android:id="@+id/cardStat1"
    android:layout_width="0dp"
    android:layout_height="wrap_content" />

<com.google.android.material.card.MaterialCardView
    android:id="@+id/cardStat2"
    android:layout_width="0dp"
    android:layout_height="wrap_content" />

<com.google.android.material.card.MaterialCardView
    android:id="@+id/cardStat3"
    android:layout_width="0dp"
    android:layout_height="wrap_content" />
```

---

## 3. High-Performance Inflation with `ViewStub`

Never inflate heavy UI elements (such as empty states, error states, or complex modal overlays) on initial screen launch:

```xml
<!-- Lightweight placeholder: 0 overhead until needed -->
<ViewStub
    android:id="@+id/stubEmptyState"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout="@layout/layout_empty_state_dribbble"
    app:layout_constraintTop_toTopOf="parent"
    app:layout_constraintBottom_toBottomOf="parent" />
```

In Java:
```java
// Inflate only when list is actually empty
if (dataset.isEmpty()) {
    View emptyView = binding.stubEmptyState.inflate();
    // Setup empty state actions
}
```

---

## 4. Dimension Unit Discipline

- **Layout dimensions, margins, paddings, corners**: Always `dp`.
- **Typography, text sizes**: Strictly `sp`.
- **Stroke widths**: `1dp` (thin crisp modern) or `2dp` (active/focus state). Never `3dp+` for standard card borders.
- **Corner Radii Tokens**:
  - `xs`: 4dp (Badge)
  - `sm`: 8dp (Chips, buttons)
  - `md`: 16dp (Standard card)
  - `lg`: 24dp (Bento card, modal header)
  - `xl`: 28dp (Bottom sheet top corners)
  - `full`: 999dp (Pills, FABs)
