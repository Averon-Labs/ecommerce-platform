package com.averonlabs.eshopverse.checkout;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.averonlabs.eshopverse.core.model.CartItem;
import com.averonlabs.eshopverse.core.model.Currency;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ReviewItemsAdapter} and its diff callback logic.
 */
public class ReviewItemsAdapterTest {

    private ReviewItemsAdapter adapter;

    @Before
    public void setUp() {
        adapter = new ReviewItemsAdapter();
    }

    @Test
    public void testAdapterInitialization() {
        assertNotNull(adapter);
    }

    @Test
    public void testDiffCallback_areItemsTheSame() {
        CartItem item1 = new CartItem(
                101,
                "Linen Shirt",
                "https://example.com/linen.jpg",
                "45.00",
                Currency.USD,
                2,
                "90.00",
                true,
                10,
                false,
                false
        );

        CartItem item1Updated = new CartItem(
                101,
                "Linen Shirt",
                "https://example.com/linen.jpg",
                "45.00",
                Currency.USD,
                3,
                "135.00",
                true,
                10,
                false,
                false
        );

        CartItem item2 = new CartItem(
                102,
                "Leather Belt",
                null,
                "25.00",
                Currency.USD,
                1,
                "25.00",
                true,
                5,
                false,
                false
        );

        assertTrue(ReviewItemsAdapter.DIFF_CALLBACK.areItemsTheSame(item1, item1Updated));
        assertFalse(ReviewItemsAdapter.DIFF_CALLBACK.areItemsTheSame(item1, item2));
    }

    @Test
    public void testDiffCallback_areContentsTheSame() {
        CartItem item1 = new CartItem(
                101,
                "Linen Shirt",
                "https://example.com/linen.jpg",
                "45.00",
                Currency.USD,
                2,
                "90.00",
                true,
                10,
                false,
                false
        );

        CartItem item1Same = new CartItem(
                101,
                "Linen Shirt",
                "https://example.com/linen.jpg",
                "45.00",
                Currency.USD,
                2,
                "90.00",
                true,
                10,
                false,
                false
        );

        CartItem item1Different = new CartItem(
                101,
                "Linen Shirt",
                "https://example.com/linen.jpg",
                "45.00",
                Currency.USD,
                3,
                "135.00",
                true,
                10,
                false,
                false
        );

        assertTrue(ReviewItemsAdapter.DIFF_CALLBACK.areContentsTheSame(item1, item1Same));
        assertFalse(ReviewItemsAdapter.DIFF_CALLBACK.areContentsTheSame(item1, item1Different));
    }
}
