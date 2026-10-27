package com.averonlabs.eshopverse.core.ui;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.widget.ImageView;

import com.averonlabs.eshopverse.R;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for ImageLoaderHelper constants, configurations, lifecycle guards, and null safety.
 */
public class ImageLoaderHelperTest {

    @Test
    public void testDefaultConstants() {
        assertEquals(250, ImageLoaderHelper.DEFAULT_CROSS_FADE_DURATION_MS);
        assertEquals(R.drawable.bg_placeholder_shimmer, ImageLoaderHelper.DEFAULT_PLACEHOLDER_RES);
        assertEquals(R.drawable.bg_placeholder_error, ImageLoaderHelper.DEFAULT_ERROR_RES);
        assertEquals(com.bumptech.glide.Priority.LOW, ImageLoaderHelper.PREFETCH_PRIORITY);
    }

    @Test
    public void testPreloadOnDestroyedOrFinishingActivitySafelyHandled() {
        Activity mockDestroyedActivity = mock(Activity.class);
        when(mockDestroyedActivity.isDestroyed()).thenReturn(true);
        when(mockDestroyedActivity.isFinishing()).thenReturn(false);

        ImageLoaderHelper.preload(mockDestroyedActivity, "https://example.com/item.jpg");

        Activity mockFinishingActivity = mock(Activity.class);
        when(mockFinishingActivity.isDestroyed()).thenReturn(false);
        when(mockFinishingActivity.isFinishing()).thenReturn(true);

        ImageLoaderHelper.preload(mockFinishingActivity, "https://example.com/item.jpg");
    }

    @Test
    public void testPreloadBatchOnDestroyedOrFinishingActivitySafelyHandled() {
        List<String> urls = Collections.singletonList("https://example.com/item.jpg");

        Activity mockDestroyedActivity = mock(Activity.class);
        when(mockDestroyedActivity.isDestroyed()).thenReturn(true);
        when(mockDestroyedActivity.isFinishing()).thenReturn(false);

        ImageLoaderHelper.preloadBatch(mockDestroyedActivity, urls);

        Activity mockFinishingActivity = mock(Activity.class);
        when(mockFinishingActivity.isDestroyed()).thenReturn(false);
        when(mockFinishingActivity.isFinishing()).thenReturn(true);

        ImageLoaderHelper.preloadBatch(mockFinishingActivity, urls);
    }

    @Test
    public void testNullSafetyOnPreloadAndClear() {
        // Must execute cleanly without NullPointerExceptions
        ImageLoaderHelper.preload(null, null);
        ImageLoaderHelper.preload(null, "");
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

    @Test
    public void testClearOnDestroyedActivitySafelyHandled() {
        ImageView mockImageView = mock(ImageView.class);
        Activity mockActivity = mock(Activity.class);
        when(mockActivity.isDestroyed()).thenReturn(true);
        when(mockActivity.isFinishing()).thenReturn(false);
        when(mockImageView.getContext()).thenReturn(mockActivity);

        // Should return early safely without calling Glide or throwing
        ImageLoaderHelper.clear(mockImageView);
    }

    @Test
    public void testClearOnFinishingActivitySafelyHandled() {
        ImageView mockImageView = mock(ImageView.class);
        Activity mockActivity = mock(Activity.class);
        when(mockActivity.isDestroyed()).thenReturn(false);
        when(mockActivity.isFinishing()).thenReturn(true);
        when(mockImageView.getContext()).thenReturn(mockActivity);

        ImageLoaderHelper.clear(mockImageView);
    }

    @Test
    public void testClearOnNullContextSafelyHandled() {
        ImageView mockImageView = mock(ImageView.class);
        when(mockImageView.getContext()).thenReturn(null);

        ImageLoaderHelper.clear(mockImageView);
    }

    @Test
    public void testLoadImageOnDestroyedActivitySafelyHandled() {
        ImageView mockImageView = mock(ImageView.class);
        Activity mockActivity = mock(Activity.class);
        when(mockActivity.isDestroyed()).thenReturn(true);
        when(mockImageView.getContext()).thenReturn(mockActivity);

        ImageLoaderHelper.loadImage(mockImageView, "https://example.com/sample.jpg");
        ImageLoaderHelper.loadImage(mockImageView, null);
        ImageLoaderHelper.loadImageFitCenter(mockImageView, "https://example.com/sample.jpg");
    }
}
