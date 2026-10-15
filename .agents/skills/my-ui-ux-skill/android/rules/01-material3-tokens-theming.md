# Rule 01: Material 3 Tokens & Theming System (Java & XML)

## 1. The 29 Tonal Color Roles

Material Design 3 eliminates arbitrary color picking in favor of mathematically paired **Roles**. Every background role has an associated **"On"** role for text/icons, ensuring automatic WCAG contrast compliance.

### Core Token Hierarchy

| Token Role Attribute | Primary Usage | Light Theme Tone | Dark Theme Tone | OLED Dark Tone |
| :--- | :--- | :--- | :--- | :--- |
| `colorPrimary` | Key CTAs, prominent buttons, active indicator | Tone 40 | Tone 80 | Tone 80 |
| `colorOnPrimary` | Text & icons on top of `colorPrimary` | Tone 100 (#FFFFFF) | Tone 20 | Tone 20 |
| `colorPrimaryContainer` | Tonal buttons, chips, highlighted card fills | Tone 90 | Tone 30 | Tone 20 |
| `colorOnPrimaryContainer` | Text/icons on `colorPrimaryContainer` | Tone 10 | Tone 90 | Tone 90 |
| `colorSecondary` | Filter chips, floating badges, segmented tabs | Tone 40 | Tone 80 | Tone 80 |
| `colorOnSecondary` | Text & icons on top of `colorSecondary` | Tone 100 | Tone 20 | Tone 20 |
| `colorSecondaryContainer`| Subtle card fills, inactive selected pills | Tone 90 | Tone 30 | Tone 20 |
| `colorOnSecondaryContainer`| Text/icons on `colorSecondaryContainer` | Tone 10 | Tone 90 | Tone 90 |
| `colorTertiary` | Accent contrast, badges, creative callouts | Tone 40 | Tone 80 | Tone 80 |
| `colorOnTertiary` | Text on tertiary accent | Tone 100 | Tone 20 | Tone 20 |
| `colorTertiaryContainer`| Soft contrasting accents | Tone 90 | Tone 30 | Tone 20 |
| `colorOnTertiaryContainer`| Text on tertiary container | Tone 10 | Tone 90 | Tone 90 |
| `colorError` | Validation errors, destructive dialogs | #BA1A1A | #FFB4AB | #FFB4AB |
| `colorOnError` | Text on error surfaces | #FFFFFF | #690005 | #690005 |
| `colorErrorContainer` | Error banners, invalid input backgrounds | #FFDAD6 | #93000A | #93000A |
| `colorOnErrorContainer` | Text on error container | #410002 | #FFDAD6 | #FFDAD6 |
| `android:colorBackground`| Screen background behind all views | Tone 98 (#FDFBFF) | Tone 6 (#121212)| Tone 0 (#000000)|
| `colorOnBackground` | Primary body text on screen background | Tone 10 (#1A1C1E) | Tone 90 (#E2E2E6)| Tone 95 (#F0F0F4)|
| `colorSurface` | Default surface (cards, bottom sheets) | Tone 98 | Tone 6 | Tone 4 |
| `colorOnSurface` | Primary text on surfaces | Tone 10 | Tone 90 | Tone 95 |
| `colorSurfaceVariant` | Secondary surface, subtle divider tint | Tone 90 | Tone 30 | Tone 15 |
| `colorOnSurfaceVariant` | Secondary/subtitle text, inactive icons | Tone 30 (#44474E) | Tone 80 (#C4C6D0)| Tone 80 (#C4C6D0)|
| `colorOutline` | High-contrast borders, input field strokes | Tone 50 (#74777F) | Tone 60 (#8E9099)| Tone 60 (#8E9099)|
| `colorOutlineVariant` | Subtle card borders, dividers, card outlines | Tone 80 (#C4C6D0) | Tone 30 (#44474F)| Tone 20 (#2D3037)|

---

## 2. Defining Modern Themes in `themes.xml`

### Light Theme (`res/values/themes.xml`)
```xml
<resources xmlns:tools="http://schemas.android.com/tools">
    <!-- Base Application Theme -->
    <style name="Base.Theme.MyModernApp" parent="Theme.Material3.DayNight.NoActionBar">
        <!-- Brand Colors -->
        <item name="colorPrimary">@color/md_theme_light_primary</item>
        <item name="colorOnPrimary">@color/md_theme_light_onPrimary</item>
        <item name="colorPrimaryContainer">@color/md_theme_light_primaryContainer</item>
        <item name="colorOnPrimaryContainer">@color/md_theme_light_onPrimaryContainer</item>

        <item name="colorSecondary">@color/md_theme_light_secondary</item>
        <item name="colorOnSecondary">@color/md_theme_light_onSecondary</item>
        <item name="colorSecondaryContainer">@color/md_theme_light_secondaryContainer</item>
        <item name="colorOnSecondaryContainer">@color/md_theme_light_onSecondaryContainer</item>

        <item name="colorTertiary">@color/md_theme_light_tertiary</item>
        <item name="colorOnTertiary">@color/md_theme_light_onTertiary</item>

        <!-- Background & Surface -->
        <item name="android:colorBackground">@color/md_theme_light_background</item>
        <item name="colorOnBackground">@color/md_theme_light_onBackground</item>
        <item name="colorSurface">@color/md_theme_light_surface</item>
        <item name="colorOnSurface">@color/md_theme_light_onSurface</item>
        <item name="colorSurfaceVariant">@color/md_theme_light_surfaceVariant</item>
        <item name="colorOnSurfaceVariant">@color/md_theme_light_onSurfaceVariant</item>

        <!-- Borders & Dividers -->
        <item name="colorOutline">@color/md_theme_light_outline</item>
        <item name="colorOutlineVariant">@color/md_theme_light_outlineVariant</item>

        <!-- System Bar Edge-to-Edge Flags -->
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:windowLightNavigationBar">true</item>

        <!-- Default Component Styling Overrides -->
        <item name="materialCardViewStyle">@style/Widget.App.CardView</item>
        <item name="shapeAppearanceCornerMedium">@style/ShapeAppearance.App.Corner.Medium</item>
        <item name="shapeAppearanceCornerLarge">@style/ShapeAppearance.App.Corner.Large</item>
    </style>

    <style name="Theme.MyModernApp" parent="Base.Theme.MyModernApp" />
</resources>
```

### Dark / OLED Theme (`res/values-night/themes.xml`)
```xml
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Base.Theme.MyModernApp" parent="Theme.Material3.DayNight.NoActionBar">
        <!-- In Dark mode, invert tones as per M3 specs -->
        <item name="colorPrimary">@color/md_theme_dark_primary</item>
        <item name="colorOnPrimary">@color/md_theme_dark_onPrimary</item>
        <item name="colorPrimaryContainer">@color/md_theme_dark_primaryContainer</item>
        <item name="colorOnPrimaryContainer">@color/md_theme_dark_onPrimaryContainer</item>

        <!-- OLED Pitch Black or Deep Slate Surfaces -->
        <item name="android:colorBackground">@color/md_theme_dark_background</item>
        <item name="colorOnBackground">@color/md_theme_dark_onBackground</item>
        <item name="colorSurface">@color/md_theme_dark_surface</item>
        <item name="colorOnSurface">@color/md_theme_dark_onSurface</item>
        <item name="colorSurfaceVariant">@color/md_theme_dark_surfaceVariant</item>
        <item name="colorOnSurfaceVariant">@color/md_theme_dark_onSurfaceVariant</item>

        <item name="colorOutline">@color/md_theme_dark_outline</item>
        <item name="colorOutlineVariant">@color/md_theme_dark_outlineVariant</item>

        <!-- Dark System Bars: White icons -->
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar">false</item>
        <item name="android:windowLightNavigationBar">false</item>
    </style>
</resources>
```

---

## 3. Dynamic Color (Material You) in Java

To automatically adapt your app to the user's wallpaper palette on Android 12+ (API 31+), initialize in your `Application` subclass:

```java
package com.yourapp;

import android.app.Application;
import com.google.android.material.color.DynamicColors;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Applies wallpaper colors dynamically to all activities that extend M3 themes
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
```

---

## 4. Referencing Colors in XML (Crucial Rules)

1. **NEVER use hardcoded hex (`#FFFFFF`, `#121212`) in component XML**:
   ```xml
   <!-- WRONG -->
   <TextView android:textColor="#000000" />

   <!-- CORRECT -->
   <TextView android:textColor="?attr/colorOnSurface" />
   ```
2. **Use `?attr/colorOnSurfaceVariant` for subtitles and secondary captions**:
   This guarantees that when the user toggles dark mode, the subtitle never becomes invisible.
3. **Use `?attr/colorOutlineVariant` with 1dp borders for clean modern cards**:
   Instead of heavy 90s drop shadows, crisp modern UI uses 1dp subtle strokes to define boundary depth.
