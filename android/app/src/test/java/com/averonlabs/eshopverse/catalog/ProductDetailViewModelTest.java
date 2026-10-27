package com.averonlabs.eshopverse.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.os.Bundle;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.catalog.fragment.ProductDetailFragment;
import com.averonlabs.eshopverse.core.model.Currency;
import com.averonlabs.eshopverse.core.model.ProductDetail;
import com.averonlabs.eshopverse.core.model.ProductImage;
import com.averonlabs.eshopverse.core.model.ProductStatus;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.state.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JVM unit tests for {@link ProductDetailViewModel} and {@link ProductDetailFragment}
 * verifying optimistic summary state and transition behavior (IMG-002).
 */
public class ProductDetailViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private CatalogRepository repository;
    private ProductDetailViewModel viewModel;

    @Before
    public void setUp() {
        repository = mock(CatalogRepository.class);
        viewModel = new ProductDetailViewModel(repository);
    }

    private ProductSummary createDummySummary(int id, String name, String price) {
        return new ProductSummary(
                id,
                name,
                price,
                Currency.USD,
                1,
                "Ceramics",
                "https://example.com/item.jpg",
                true,
                ProductStatus.ACTIVE
        );
    }

    private ProductDetail createDummyDetail(int id, String name, String price) {
        List<ProductImage> images = new ArrayList<>();
        images.add(new ProductImage("https://example.com/item1.jpg", 1));
        images.add(new ProductImage("https://example.com/item2.jpg", 2));
        return new ProductDetail(
                id,
                name,
                price,
                Currency.USD,
                1,
                "Ceramics",
                "https://example.com/item1.jpg",
                true,
                ProductStatus.ACTIVE,
                "Detailed handcrafted ceramic vase.",
                "SKU-CER-01",
                15,
                images
        );
    }

    @Test
    public void test_initialState_isIdle() {
        assertNull(viewModel.getProductState().getValue());
        assertNull(viewModel.getOptimisticSummary().getValue());
        assertEquals(Integer.valueOf(1), viewModel.getQuantity().getValue());
    }

    @Test
    public void test_setOptimisticSummary_updatesOptimisticSummaryLiveData() {
        ProductSummary summary = createDummySummary(42, "Handmade Mug", "24.00");
        viewModel.setOptimisticSummary(summary);

        assertNotNull(viewModel.getOptimisticSummary().getValue());
        assertEquals(42, viewModel.getOptimisticSummary().getValue().getId());
        assertEquals("Handmade Mug", viewModel.getOptimisticSummary().getValue().getName());
        assertEquals("24.00", viewModel.getOptimisticSummary().getValue().getPrice());
    }

    @Test
    public void test_loadProduct_success_emitsLoadingThenContent() {
        ProductDetail detail = createDummyDetail(42, "Handmade Vase", "89.00");

        doAnswer(invocation -> {
            CatalogRepository.CatalogCallback<ProductDetail> callback = invocation.getArgument(1);
            callback.onSuccess(detail);
            return null;
        }).when(repository).getProduct(eq(42L), any());

        viewModel.loadProduct(42);

        UiState<ProductDetail> state = viewModel.getProductState().getValue();
        assertNotNull(state);
        assertEquals(UiState.Status.CONTENT, state.getStatus());
        assertEquals(detail, state.getData());
        verify(repository, times(1)).getProduct(eq(42L), any());
    }

    @Test
    public void test_loadProduct_error_emitsErrorState() {
        doAnswer(invocation -> {
            CatalogRepository.CatalogCallback<ProductDetail> callback = invocation.getArgument(1);
            callback.onError(new RuntimeException("Network timeout"));
            return null;
        }).when(repository).getProduct(eq(99L), any());

        viewModel.loadProduct(99);

        UiState<ProductDetail> state = viewModel.getProductState().getValue();
        assertNotNull(state);
        assertEquals(UiState.Status.ERROR, state.getStatus());
        assertEquals(R.string.catalog_error_load_details, state.getMessageRes());
    }

    @Test
    public void test_quantity_stepper_bounds() {
        viewModel.setQuantity(5);
        assertEquals(Integer.valueOf(5), viewModel.getQuantity().getValue());

        viewModel.setQuantity(1);
        assertEquals(Integer.valueOf(1), viewModel.getQuantity().getValue());
    }

    @Test
    public void test_createArgs_withProductSummary_setsAllFields() {
        ProductSummary summary = createDummySummary(101, "Linen Throw", "45.00");
        Bundle bundle = mock(Bundle.class);
        ProductDetailFragment.populateArgs(bundle, summary);

        verify(bundle).putLong(ProductDetailFragment.ARG_PRODUCT_ID, 101L);
        verify(bundle).putString(ProductDetailFragment.ARG_PRODUCT_NAME, "Linen Throw");
        verify(bundle).putString(ProductDetailFragment.ARG_PRODUCT_PRICE, "45.00");
        verify(bundle).putString(ProductDetailFragment.ARG_PRODUCT_CURRENCY, "USD");
        verify(bundle).putString(ProductDetailFragment.ARG_PRODUCT_CATEGORY, "Ceramics");
        verify(bundle).putString(ProductDetailFragment.ARG_PRODUCT_IMAGE_URL, "https://example.com/item.jpg");
        verify(bundle).putBoolean(ProductDetailFragment.ARG_PRODUCT_IN_STOCK, true);
        verify(bundle).putSerializable(ProductDetailFragment.ARG_PRODUCT_SUMMARY, summary);

        // Also test createArgs factory produces non-null bundle
        assertNotNull(ProductDetailFragment.createArgs(summary));
    }

    @Test
    public void test_createArgs_withProductIdOnly_setsId() {
        Bundle bundle = mock(Bundle.class);
        ProductDetailFragment.populateArgs(bundle, 555L);
        verify(bundle).putLong(ProductDetailFragment.ARG_PRODUCT_ID, 555L);

        // Also test createArgs factory produces non-null bundle
        assertNotNull(ProductDetailFragment.createArgs(555L));
    }
}
