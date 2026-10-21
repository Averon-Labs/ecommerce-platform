package com.averonlabs.eshopverse.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.recyclerview.widget.RecyclerView;

import com.averonlabs.eshopverse.core.model.ProductImage;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Unit tests for {@link ImageCarouselAdapter} gallery management.
 */
public class ImageCarouselAdapterTest {

    private ImageCarouselAdapter adapter;

    @Before
    public void setUp() {
        adapter = new ImageCarouselAdapter();
    }

    @Test
    public void testInitialState_empty() {
        assertNotNull(adapter);
        assertEquals(0, adapter.getItemCount());
    }

    @Test
    public void testSubmitList_updatesItemCount() {
        ProductImage img1 = new ProductImage("https://example.com/shoe_front.jpg", 1);
        ProductImage img2 = new ProductImage("https://example.com/shoe_side.jpg", 2);
        ProductImage img3 = new ProductImage("https://example.com/shoe_sole.jpg", 3);

        adapter.submitList(Arrays.asList(img1, img2, img3));
        assertEquals(3, adapter.getItemCount());
    }

    @Test
    public void testSubmitList_replaceList() {
        ProductImage img1 = new ProductImage("https://example.com/1.jpg", 1);
        adapter.submitList(Collections.singletonList(img1));
        assertEquals(1, adapter.getItemCount());

        ProductImage img2 = new ProductImage("https://example.com/2.jpg", 1);
        ProductImage img3 = new ProductImage("https://example.com/3.jpg", 2);
        adapter.submitList(Arrays.asList(img2, img3));
        assertEquals(2, adapter.getItemCount());

        adapter.submitList(Collections.emptyList());
        assertEquals(0, adapter.getItemCount());
    }
}
