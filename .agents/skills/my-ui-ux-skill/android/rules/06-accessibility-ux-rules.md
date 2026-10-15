# Rule 06: Accessibility, Insets & Ergonomic Standards

An interface that looks stunning on a designer's static canvas is a failure if users struggle to tap its buttons, if text gets cut off when font size is increased, or if the virtual keyboard hides input fields.

---

## 1. The 48×48dp Minimum Touch Target Rule

WCAG 2.2 and Google Play Accessibility scanners mandate that all interactive elements have a touch target of at least **48×48dp**.

### How to Style 24dp Icons with a 48dp Hit Target
Even if the visible icon is only 24dp, its clickable footprint must be 48dp:

```xml
<!-- CORRECT: 24dp vector icon with 48dp touch footprint -->
<com.google.android.material.button.MaterialButton
    android:id="@+id/btnBack"
    style="@style/Widget.Material3.Button.IconButton"
    android:layout_width="48dp"
    android:layout_height="48dp"
    app:icon="@drawable/ic_arrow_back_24"
    app:iconSize="24dp"
    app:iconGravity="textStart"
    android:contentDescription="@string/desc_navigate_back" />
```

### Expanding Hit Areas in Java with `TouchDelegate`
If an element cannot be widened in XML due to dense layout constraints, expand its touch bounds programmatically:

```java
public static void expandTouchArea(final View parent, final View child, final int extraPaddingDp) {
    parent.post(() -> {
        Rect rect = new Rect();
        child.getHitRect(rect);
        int px = (int) (extraPaddingDp * parent.getResources().getDisplayMetrics().density);
        rect.top -= px;
        rect.bottom += px;
        rect.left -= px;
        rect.right += px;
        parent.setTouchDelegate(new TouchDelegate(rect, child));
    });
}
```

---

## 2. Modern Edge-to-Edge & System Insets (Android 15 Ready)

Modern Android devices use gesture bars and camera cutouts. Content must flow seamlessly underneath system bars, while interactive controls must respect system insets.

### In Java `Activity.onCreate()`:
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // 1. Enable Edge-to-Edge drawing
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    
    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    // 2. Consume Insets (Status bar, Navigation bar, and Keyboard IME)
    ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, windowInsets) -> {
        Insets systemBars = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
        );
        Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());

        // Apply status bar top padding to header
        binding.appBarContainer.setPadding(0, systemBars.top, 0, 0);

        // Apply navigation bar or keyboard bottom padding to bottom controls
        int bottomPadding = Math.max(systemBars.bottom, ime.bottom);
        binding.bottomContainer.setPadding(0, 0, 0, bottomPadding);

        return WindowInsetsCompat.CONSUMED;
    });
}
```

---

## 3. Typography Scaling & No-Truncation Safety

1. **Always use `sp` for `android:textSize`**: Never use `dp` or `px`.
2. **Defensive Layout**: Never set fixed heights (`android:layout_height="40dp"`) on `TextView` containers. Always use `wrap_content` with `android:maxLines` or allow dynamic wrapping.
3. **Ellipsize Strategy**:
   ```xml
   <TextView
       android:layout_width="0dp"
       android:layout_height="wrap_content"
       android:maxLines="2"
       android:ellipsize="end"
       app:layout_constraintEnd_toStartOf="@id/ivAction" />
   ```

---

## 4. Internationalization & RTL Discipline

1. **Never use `android:layout_marginLeft` or `android:layout_marginRight`**:
   - Always use `layout_marginStart` and `layout_marginEnd`.
2. **Never use `android:gravity="left"`**:
   - Always use `android:gravity="start"`.
3. In `AndroidManifest.xml`:
   ```xml
   <application
       android:supportsRtl="true" ... >
   ```

---

## 5. Strict Banned Anti-Patterns (NEVER DO THIS)

| Forbidden Anti-Pattern | Why It Breaks UX | Correct Solution |
| :--- | :--- | :--- |
| **`#FFFFFF` hardcoded in XML** | Blinds the user when dark mode is active | Use `?attr/colorSurface` or `?attr/android:colorBackground` |
| **Blocking Paste on input fields** | Ruins password managers and clipboard access | Never set `android:longClickable="false"` on `EditText` |
| **ImageView without `contentDescription`** | Blind users get generic "Unlabelled button" | Provide descriptive string or `android:importantForAccessibility="no"` |
| **Using emojis as UI action icons** | Render inconsistently and break vector scalability | Use clean Vector Drawables (`VectorAsset`) |
| **Nested `ScrollView` inside `ScrollView`** | Breaks touch momentum and vertical scroll physics | Use a single `NestedScrollView` with a unified `ConstraintLayout` |
| **Button press with 0ms transition** | Makes interface feel unresponsive and frozen | Add `SpringTouchFeedback` or standard Material Ripple |
