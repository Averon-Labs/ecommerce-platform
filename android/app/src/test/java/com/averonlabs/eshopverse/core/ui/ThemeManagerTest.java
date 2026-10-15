package com.averonlabs.eshopverse.core.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.appcompat.app.AppCompatDelegate;

import org.junit.Before;
import org.junit.Test;

public class ThemeManagerTest {

    private Context context;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Resources resources;
    private Configuration configuration;

    @Before
    public void setUp() {
        context = mock(Context.class);
        Context appContext = mock(Context.class);
        sharedPreferences = mock(SharedPreferences.class);
        editor = mock(SharedPreferences.Editor.class);
        resources = mock(Resources.class);
        configuration = new Configuration();

        when(context.getApplicationContext()).thenReturn(appContext);
        when(appContext.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putInt(anyString(), anyInt())).thenReturn(editor);

        when(context.getResources()).thenReturn(resources);
        when(resources.getConfiguration()).thenReturn(configuration);
    }

    @Test
    public void testGetAndSetSavedMode() {
        when(sharedPreferences.getInt(eq("key_night_mode"), anyInt())).thenReturn(AppCompatDelegate.MODE_NIGHT_YES);
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ThemeManager.getSavedMode(context));

        ThemeManager.setSavedMode(context, AppCompatDelegate.MODE_NIGHT_NO);
        verify(editor).putInt("key_night_mode", AppCompatDelegate.MODE_NIGHT_NO);
        verify(editor).apply();
    }

    @Test
    public void testIsDarkModeExplicitYes() {
        when(sharedPreferences.getInt(eq("key_night_mode"), anyInt())).thenReturn(AppCompatDelegate.MODE_NIGHT_YES);
        assertTrue(ThemeManager.isDarkMode(context));
    }

    @Test
    public void testIsDarkModeExplicitNo() {
        when(sharedPreferences.getInt(eq("key_night_mode"), anyInt())).thenReturn(AppCompatDelegate.MODE_NIGHT_NO);
        assertFalse(ThemeManager.isDarkMode(context));
    }

    @Test
    public void testIsDarkModeFallbackToSystem() {
        when(sharedPreferences.getInt(eq("key_night_mode"), anyInt())).thenReturn(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        configuration.uiMode = Configuration.UI_MODE_NIGHT_YES;
        assertTrue(ThemeManager.isDarkMode(context));

        configuration.uiMode = Configuration.UI_MODE_NIGHT_NO;
        assertFalse(ThemeManager.isDarkMode(context));
    }

    @Test
    public void testToggleThemeFromLightToDark() {
        when(sharedPreferences.getInt(eq("key_night_mode"), anyInt())).thenReturn(AppCompatDelegate.MODE_NIGHT_NO);
        boolean newModeIsDark = ThemeManager.toggleTheme(context);
        assertTrue(newModeIsDark);
        verify(editor).putInt("key_night_mode", AppCompatDelegate.MODE_NIGHT_YES);
    }

    @Test
    public void testToggleThemeFromDarkToLight() {
        when(sharedPreferences.getInt(eq("key_night_mode"), anyInt())).thenReturn(AppCompatDelegate.MODE_NIGHT_YES);
        boolean newModeIsDark = ThemeManager.toggleTheme(context);
        assertFalse(newModeIsDark);
        verify(editor).putInt("key_night_mode", AppCompatDelegate.MODE_NIGHT_NO);
    }
}
