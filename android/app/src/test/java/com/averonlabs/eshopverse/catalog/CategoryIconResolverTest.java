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
    }
}
