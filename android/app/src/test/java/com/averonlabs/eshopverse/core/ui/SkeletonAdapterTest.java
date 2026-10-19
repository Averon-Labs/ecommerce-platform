package com.averonlabs.eshopverse.core.ui;

import static org.junit.Assert.assertEquals;

import com.averonlabs.eshopverse.R;

import org.junit.Test;

/**
 * Unit tests for SkeletonAdapter item counts, resource resolution, and edge cases.
 */
public class SkeletonAdapterTest {

    @Test
    public void testItemCount() {
        SkeletonAdapter adapter6 = new SkeletonAdapter(R.layout.item_skeleton_product_card, 6);
        assertEquals(6, adapter6.getItemCount());

        SkeletonAdapter adapter5 = new SkeletonAdapter(R.layout.item_skeleton_category_chip, 5);
        assertEquals(5, adapter5.getItemCount());

        SkeletonAdapter adapterZero = new SkeletonAdapter(R.layout.item_skeleton_category_chip, 0);
        assertEquals(0, adapterZero.getItemCount());
    }

    @Test
    public void testNegativeCountClampedToZero() {
        SkeletonAdapter adapterNegative = new SkeletonAdapter(R.layout.item_skeleton_product_card, -5);
        assertEquals(0, adapterNegative.getItemCount());
    }
}
