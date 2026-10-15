package com.averonlabs.eshopverse.core.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Manages application light and dark theme mode preferences.
 */
public final class ThemeManager {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_NIGHT_MODE = "key_night_mode";

    private ThemeManager() {
        // Utility class
    }

    /**
     * Applies the stored theme preference. Should be called on Activity creation.
     */
    public static void applyTheme(@NonNull Context context) {
        int savedMode = getSavedMode(context);
        AppCompatDelegate.setDefaultNightMode(savedMode);
    }

    /**
     * Checks if dark mode is currently active.
     */
    public static boolean isDarkMode(@NonNull Context context) {
        int savedMode = getSavedMode(context);
        if (savedMode == AppCompatDelegate.MODE_NIGHT_YES) {
            return true;
        } else if (savedMode == AppCompatDelegate.MODE_NIGHT_NO) {
            return false;
        }
        // Fallback to current system configuration
        int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Toggles between light and dark modes and persists the selection.
     *
     * @return true if new mode is dark, false if light
     */
    public static boolean toggleTheme(@NonNull Context context) {
        boolean currentlyDark = isDarkMode(context);
        int newMode = currentlyDark ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;
        setSavedMode(context, newMode);
        AppCompatDelegate.setDefaultNightMode(newMode);
        return !currentlyDark;
    }

    public static int getSavedMode(@NonNull Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void setSavedMode(@NonNull Context context, int mode) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_NIGHT_MODE, mode).apply();
    }
}
