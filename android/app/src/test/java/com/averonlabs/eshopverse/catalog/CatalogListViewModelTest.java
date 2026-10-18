package com.averonlabs.eshopverse.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.error.ApiErrorCodes;
import com.averonlabs.eshopverse.core.model.Currency;
import com.averonlabs.eshopverse.core.model.Pagination;
import com.averonlabs.eshopverse.core.model.ProductStatus;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.net.ApiException;
import com.averonlabs.eshopverse.core.net.PagedResponse;
import com.averonlabs.eshopverse.core.state.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JVM unit tests for {@link CatalogListViewModel} per docs/android/PHASE-1-SPEC.md §E.7.
 * Proves loading -> content -> empty states, append vs reset pagination, error and retry,
 * and confirms that duplicate in-flight requests are prevented.
 */
public class CatalogListViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private CatalogRepository repository;
    private CatalogListViewModel viewModel;

    @Before
    public void setUp() {
        repository = mock(CatalogRepository.class);
        viewModel = new CatalogListViewModel(repository);
    }

    private ProductSummary createDummyProduct(int id, String name, String price) {
        return new ProductSummary(
                id,
                name,
                price,
                Currency.USD,
                1,
                "Footwear",
                "https://images.example.com/item.jpg",
                true,
                ProductStatus.ACTIVE
        );
    }

    @Test
    public void testLoadFirstPageContentTransition() {
        List<ProductSummary> dummyList = new ArrayList<>();
        dummyList.add(createDummyProduct(1, "Sneaker A", "80.00"));
        dummyList.add(createDummyProduct(2, "Sneaker B", "90.00"));
        PagedResponse<ProductSummary> pagedResponse = new PagedResponse<>(
                dummyList,
                new Pagination(1, 20, 2, 1)
        );

        doAnswer(invocation -> {
            CatalogRepository.CatalogCallback<PagedResponse<ProductSummary>> cb = invocation.getArgument(1);
            cb.onSuccess(pagedResponse);
            return null;
        }).when(repository).listProducts(any(), any());

        viewModel.loadFirstPage();

        UiState<List<ProductSummary>> state = viewModel.getProductsState().getValue();
        assertNotNull(state);
        assertTrue(state.isContent());
        assertEquals(2, state.getData().size());
        assertEquals("Sneaker A", state.getData().get(0).getName());
        assertFalse(viewModel.hasMore());
        assertEquals(Integer.valueOf(2), viewModel.getTotalCount().getValue());
    }

    @Test
    public void testEmptyResultsTransition() {
        PagedResponse<ProductSummary> emptyResponse = new PagedResponse<>(
                Collections.emptyList(),
                new Pagination(1, 20, 0, 0)
        );

        doAnswer(invocation -> {
            CatalogRepository.CatalogCallback<PagedResponse<ProductSummary>> cb = invocation.getArgument(1);
            cb.onSuccess(emptyResponse);
            return null;
        }).when(repository).listProducts(any(), any());

        viewModel.loadFirstPage();

        UiState<List<ProductSummary>> state = viewModel.getProductsState().getValue();
        assertNotNull(state);
        assertTrue(state.isEmpty());
        assertNull(state.getData());
        assertFalse(viewModel.hasMore());
    }

    @Test
    public void testPaginationAppendVsReset() {
        // Page 1
        List<ProductSummary> page1List = Collections.singletonList(createDummyProduct(1, "P1", "10.00"));
        PagedResponse<ProductSummary> page1Response = new PagedResponse<>(
                page1List,
                new Pagination(1, 20, 2, 2)
        );

        doAnswer(invocation -> {
            CatalogRepository.CatalogCallback<PagedResponse<ProductSummary>> cb = invocation.getArgument(1);
            cb.onSuccess(page1Response);
            return null;
        }).when(repository).listProducts(any(), any());

        viewModel.loadFirstPage();

        UiState<List<ProductSummary>> state1 = viewModel.getProductsState().getValue();
        assertNotNull(state1);
        assertEquals(1, state1.getData().size());
        assertTrue(viewModel.hasMore());

        // Page 2
        List<ProductSummary> page2List = Collections.singletonList(createDummyProduct(2, "P2", "20.00"));
        PagedResponse<ProductSummary> page2Response = new PagedResponse<>(
                page2List,
                new Pagination(2, 20, 2, 2)
        );

        doAnswer(invocation -> {
            CatalogRepository.CatalogCallback<PagedResponse<ProductSummary>> cb = invocation.getArgument(1);
            cb.onSuccess(page2Response);
            return null;
        }).when(repository).listProducts(any(), any());

        viewModel.loadNextPage();

        UiState<List<ProductSummary>> state2 = viewModel.getProductsState().getValue();
        assertNotNull(state2);
        // Page 2 appends to Page 1
        assertEquals(2, state2.getData().size());
        assertEquals("P1", state2.getData().get(0).getName());
        assertEquals("P2", state2.getData().get(1).getName());
        assertFalse(viewModel.hasMore());

        // Filter modification resets to page 1
        List<ProductSummary> resetList = Collections.singletonList(createDummyProduct(3, "Filtered P3", "30.00"));
        PagedResponse<ProductSummary> resetResponse = new PagedResponse<>(
                resetList,
                new Pagination(1, 20, 1, 1)
        );

        doAnswer(invocation -> {
            CatalogRepository.CatalogCallback<PagedResponse<ProductSummary>> cb = invocation.getArgument(1);
            cb.onSuccess(resetResponse);
            return null;
        }).when(repository).listProducts(any(), any());

        viewModel.setSort(ProductParams.SORT_PRICE_ASC);

        UiState<List<ProductSummary>> stateReset = viewModel.getProductsState().getValue();
        assertNotNull(stateReset);
        assertEquals(1, stateReset.getData().size());
        assertEquals("Filtered P3", stateReset.getData().get(0).getName());
        assertEquals(1, viewModel.getCurrentParams().getPage());
    }

    @Test
    public void testErrorAndRetry() {
        ApiException apiException = new ApiException(422, ApiErrorCodes.CATEGORY_NOT_FOUND, "Category not found", Collections.emptyMap());

        doAnswer(invocation -> {
            CatalogRepository.CatalogCallback<PagedResponse<ProductSummary>> cb = invocation.getArgument(1);
            cb.onError(apiException);
            return null;
        }).when(repository).listProducts(any(), any());

        viewModel.loadFirstPage();

        UiState<List<ProductSummary>> errorState = viewModel.getProductsState().getValue();
        assertNotNull(errorState);
        assertTrue(errorState.isError());
        assertEquals(R.string.error_category_not_found, errorState.getMessageRes());
        assertEquals(ApiErrorCodes.CATEGORY_NOT_FOUND, viewModel.getErrorCode().getValue());

        // Now test retry with success
        List<ProductSummary> recoveryList = Collections.singletonList(createDummyProduct(1, "Recovered", "50.00"));
        PagedResponse<ProductSummary> recoveryResponse = new PagedResponse<>(
                recoveryList,
                new Pagination(1, 20, 1, 1)
        );

        doAnswer(invocation -> {
            CatalogRepository.CatalogCallback<PagedResponse<ProductSummary>> cb = invocation.getArgument(1);
            cb.onSuccess(recoveryResponse);
            return null;
        }).when(repository).listProducts(any(), any());

        viewModel.retry();

        UiState<List<ProductSummary>> recoveredState = viewModel.getProductsState().getValue();
        assertNotNull(recoveredState);
        assertTrue(recoveredState.isContent());
        assertEquals("Recovered", recoveredState.getData().get(0).getName());
    }

    @Test
    public void testPreventDuplicateInFlightRequests() {
        // Intentionally do NOT immediately invoke callback to simulate in-flight request
        viewModel.loadFirstPage();

        assertTrue(viewModel.isInFlight());
        assertEquals(UiState.Status.LOADING, viewModel.getProductsState().getValue().getStatus());

        // Attempt a duplicate load while in flight
        viewModel.loadFirstPage();
        viewModel.loadNextPage();
        viewModel.retry();

        // Repository was called only once!
        verify(repository, times(1)).listProducts(any(), any());
    }

    @Test
    public void testSetQueryTriggersSearchAndReset() {
        PagedResponse<ProductSummary> response = new PagedResponse<>(
                Collections.emptyList(),
                new Pagination(1, 20, 0, 0)
        );
        doAnswer(invocation -> {
            CatalogRepository.CatalogCallback<PagedResponse<ProductSummary>> cb = invocation.getArgument(1);
            cb.onSuccess(response);
            return null;
        }).when(repository).listProducts(any(), any());

        viewModel.setQuery("sneaker");
        assertEquals("sneaker", viewModel.getCurrentParams().getQ());
        verify(repository, times(1)).listProducts(any(), any());

        // Same query does not trigger duplicate load
        viewModel.setQuery("sneaker");
        verify(repository, times(1)).listProducts(any(), any());

        // Clearing query resets to null and triggers reload
        viewModel.setQuery("");
        assertNull(viewModel.getCurrentParams().getQ());
        verify(repository, times(2)).listProducts(any(), any());
    }

    @Test
    public void testReloadCallsFirstPage() {
        viewModel.reload();
        verify(repository, times(1)).listProducts(any(), any());
        assertEquals(1, viewModel.getCurrentParams().getPage());
    }
}
