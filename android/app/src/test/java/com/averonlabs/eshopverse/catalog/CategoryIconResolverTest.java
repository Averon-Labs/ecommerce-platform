package com.averonlabs.eshopverse.catalog;

import static org.junit.Assert.assertEquals;

import com.averonlabs.eshopverse.R;

import org.junit.Test;

public class CategoryIconResolverTest {

    @Test
    public void testAllCategory() {
        assertEquals(R.drawable.ic_category_all, CategoryIconResolver.getCategoryIcon("All"));
        assertEquals(R.drawable.ic_category_all, CategoryIconResolver.getCategoryIcon(null));
        assertEquals(R.drawable.ic_category_all, CategoryIconResolver.getCategoryIcon("Unknown Category"));
    }

    @Test
    public void testTenCatalogCategories() {
        assertEquals(R.drawable.ic_category_shirts, CategoryIconResolver.getCategoryIcon("Shirts & Tops"));
        assertEquals(R.drawable.ic_category_pants, CategoryIconResolver.getCategoryIcon("Pants & Jeans"));
        assertEquals(R.drawable.ic_category_dresses, CategoryIconResolver.getCategoryIcon("Dresses"));
        assertEquals(R.drawable.ic_category_jackets, CategoryIconResolver.getCategoryIcon("Jackets & Coats"));
        assertEquals(R.drawable.ic_category_tshirts, CategoryIconResolver.getCategoryIcon("T-Shirts"));
        assertEquals(R.drawable.ic_category_activewear, CategoryIconResolver.getCategoryIcon("Activewear"));
        assertEquals(R.drawable.ic_category_shoes, CategoryIconResolver.getCategoryIcon("Shoes"));
        assertEquals(R.drawable.ic_category_accessories, CategoryIconResolver.getCategoryIcon("Accessories"));
        assertEquals(R.drawable.ic_category_leather, CategoryIconResolver.getCategoryIcon("Leather & Suede"));
        assertEquals(R.drawable.ic_category_bags, CategoryIconResolver.getCategoryIcon("Bags & Backpacks"));

        // Single-word streamlined category names
        assertEquals(R.drawable.ic_category_shirts, CategoryIconResolver.getCategoryIcon("Shirts"));
        assertEquals(R.drawable.ic_category_pants, CategoryIconResolver.getCategoryIcon("Pants"));
        assertEquals(R.drawable.ic_category_jackets, CategoryIconResolver.getCategoryIcon("Jackets"));
        assertEquals(R.drawable.ic_category_leather, CategoryIconResolver.getCategoryIcon("Leather"));
        assertEquals(R.drawable.ic_category_bags, CategoryIconResolver.getCategoryIcon("Bags"));
    }

    @Test
    public void testGetStreamlinedName() {
        assertEquals("All", CategoryIconResolver.getStreamlinedName(null));
        assertEquals("All", CategoryIconResolver.getStreamlinedName("All"));
        assertEquals("Shirts", CategoryIconResolver.getStreamlinedName("Shirts & Tops"));
        assertEquals("Pants", CategoryIconResolver.getStreamlinedName("Pants & Jeans"));
        assertEquals("Dresses", CategoryIconResolver.getStreamlinedName("Dresses"));
        assertEquals("Jackets", CategoryIconResolver.getStreamlinedName("Jackets & Coats"));
        assertEquals("T-Shirts", CategoryIconResolver.getStreamlinedName("T-Shirts"));
        assertEquals("Activewear", CategoryIconResolver.getStreamlinedName("Activewear"));
        assertEquals("Shoes", CategoryIconResolver.getStreamlinedName("Shoes"));
        assertEquals("Accessories", CategoryIconResolver.getStreamlinedName("Accessories"));
        assertEquals("Leather", CategoryIconResolver.getStreamlinedName("Leather & Suede"));
        assertEquals("Bags", CategoryIconResolver.getStreamlinedName("Bags & Backpacks"));
    }
}
