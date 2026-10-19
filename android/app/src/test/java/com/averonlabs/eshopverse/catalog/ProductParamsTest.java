package com.averonlabs.eshopverse.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * JVM unit tests for {@link ProductParams} per docs/android/PHASE-1-SPEC.md §E.7.
 * Proves query construction for every sort/search/category/pagination combination,
 * whitespace sanitization, and verifies that any filter change resets the page to 1.
 */
public class ProductParamsTest {

    @Test
    public void testDefaultParameters() {
        ProductParams params = new ProductParams();
        assertEquals(1, params.getPage());
        assertEquals(20, params.getPerPage());
        assertNull(params.getQ());
        assertNull(params.getCategoryId());
        assertNull(params.getInStock());
        assertEquals(ProductParams.SORT_NEWEST, params.getSort());
    }

    @Test
    public void testQuerySanitization() {
        ProductParams params = new ProductParams().withQuery("   running   shoes   ");
        assertEquals("running shoes", params.getQ());

        ProductParams emptyParams = params.withQuery("    ");
        assertNull(emptyParams.getQ());

        ProductParams nullParams = params.withQuery(null);
        assertNull(nullParams.getQ());
    }

    @Test
    public void testSortOptions() {
        ProductParams defaultParams = new ProductParams();

        ProductParams priceAsc = defaultParams.withSort(ProductParams.SORT_PRICE_ASC);
        assertEquals(ProductParams.SORT_PRICE_ASC, priceAsc.getSort());

        ProductParams priceDesc = defaultParams.withSort(ProductParams.SORT_PRICE_DESC);
        assertEquals(ProductParams.SORT_PRICE_DESC, priceDesc.getSort());

        ProductParams nameAsc = defaultParams.withSort(ProductParams.SORT_NAME_ASC);
        assertEquals(ProductParams.SORT_NAME_ASC, nameAsc.getSort());

        ProductParams newest = defaultParams.withSort(ProductParams.SORT_NEWEST);
        assertEquals(ProductParams.SORT_NEWEST, newest.getSort());

        // Unknown fallback
        ProductParams unknown = defaultParams.withSort("invalid_sort");
        assertEquals(ProductParams.SORT_NEWEST, unknown.getSort());
    }

    @Test
    public void testFilterChangesResetPageToOne() {
        // Start on page 4
        ProductParams page4 = new ProductParams().withPage(4);
        assertEquals(4, page4.getPage());

        // Changing query resets page to 1
        ProductParams withQuery = page4.withQuery("boots");
        assertEquals(1, withQuery.getPage());
        assertEquals("boots", withQuery.getQ());

        // Advance to page 3
        ProductParams page3 = withQuery.withPage(3);
        assertEquals(3, page3.getPage());

        // Changing category resets page to 1
        ProductParams withCat = page3.withCategoryId(42L);
        assertEquals(1, withCat.getPage());
        assertEquals(Long.valueOf(42L), withCat.getCategoryId());

        // Advance to page 2
        ProductParams page2 = withCat.nextPage();
        assertEquals(2, page2.getPage());

        // Changing sort resets page to 1
        ProductParams withSort = page2.withSort(ProductParams.SORT_PRICE_DESC);
        assertEquals(1, withSort.getPage());
        assertEquals(ProductParams.SORT_PRICE_DESC, withSort.getSort());

        // Advance to page 5
        ProductParams page5 = withSort.withPage(5);
        assertEquals(5, page5.getPage());

        // Changing inStock resets page to 1
        ProductParams withStock = page5.withInStock(true);
        assertEquals(1, withStock.getPage());
        assertTrue(withStock.getInStock());
    }

    @Test
    public void testNextPageAdvancesPagination() {
        ProductParams params = new ProductParams().withQuery("sneakers").withSort(ProductParams.SORT_PRICE_ASC);
        assertEquals(1, params.getPage());

        ProductParams p2 = params.nextPage();
        assertEquals(2, p2.getPage());
        assertEquals("sneakers", p2.getQ());
        assertEquals(ProductParams.SORT_PRICE_ASC, p2.getSort());

        ProductParams p3 = p2.nextPage();
        assertEquals(3, p3.getPage());
    }

    @Test
    public void testEqualityAndHashCode() {
        ProductParams p1 = new ProductParams(1, 20, "test", 10L, true, ProductParams.SORT_NAME_ASC);
        ProductParams p2 = new ProductParams(1, 20, "test", 10L, true, ProductParams.SORT_NAME_ASC);
        ProductParams p3 = new ProductParams(2, 20, "test", 10L, true, ProductParams.SORT_NAME_ASC);

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(p1, p3);
    }

    @Test
    public void testPriceRangeFilterResetsPageToOne() {
        ProductParams page3 = new ProductParams().withPage(3);
        assertEquals(3, page3.getPage());

        ProductParams filtered = page3.withPriceRange(25.0, 150.0);
        assertEquals(1, filtered.getPage());
        assertEquals(Double.valueOf(25.0), filtered.getMinPrice());
        assertEquals(Double.valueOf(150.0), filtered.getMaxPrice());
    }

    @Test
    public void testActiveFilterCount() {
        ProductParams defaultParams = new ProductParams();
        assertEquals(0, defaultParams.getActiveFilterCount());

        ProductParams sortOnly = defaultParams.withSort(ProductParams.SORT_PRICE_ASC);
        assertEquals(1, sortOnly.getActiveFilterCount());

        ProductParams inStockOnly = sortOnly.withInStock(true);
        assertEquals(2, inStockOnly.getActiveFilterCount());

        ProductParams withMinPrice = inStockOnly.withPriceRange(30.0, 200.0);
        assertEquals(3, withMinPrice.getActiveFilterCount());

        ProductParams withBothPrices = inStockOnly.withPriceRange(30.0, 120.0);
        assertEquals(4, withBothPrices.getActiveFilterCount());
    }
}
