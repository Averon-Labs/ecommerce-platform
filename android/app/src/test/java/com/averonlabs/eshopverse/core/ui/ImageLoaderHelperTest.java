package com.averonlabs.eshopverse.core.ui;

import static org.junit.Assert.assertEquals;

import com.averonlabs.eshopverse.R;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for ImageLoaderHelper constants, configurations, and null safety.
 */
public class ImageLoaderHelperTest {

    @Test
    public void testDefaultConstants() {
        assertEquals(250, ImageLoaderHelper.DEFAULT_CROSS_FADE_DURATION_MS);
        assertEquals(R.drawable.bg_placeholder_shimmer, ImageLoaderHelper.DEFAULT_PLACEHOLDER_RES);
        assertEquals(R.drawable.bg_placeholder_error, ImageLoaderHelper.DEFAULT_ERROR_RES);
    }

    @Test
    public void testNullSafetyOnPreloadAndClear() {
        // Must execute cleanly without NullPointerExceptions
        ImageLoaderHelper.preload(null, null);
        ImageLoaderHelper.preload(null, "https://example.com/test.jpg");
        ImageLoaderHelper.preloadBatch(null, null);
        ImageLoaderHelper.preloadBatch(null, Collections.emptyList());

        List<String> urlsWithNulls = new ArrayList<>();
        urlsWithNulls.add(null);
        urlsWithNulls.add("");
        urlsWithNulls.add("   ");
        ImageLoaderHelper.preloadBatch(null, urlsWithNulls);

        ImageLoaderHelper.clear(null);
    }
}
