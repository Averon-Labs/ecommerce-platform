package com.averonlabs.eshopverse.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.SharedPreferences;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.averonlabs.eshopverse.core.model.Currency;
import com.averonlabs.eshopverse.core.model.ProductStatus;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

public class FavoritesRepositoryTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private SharedPreferences mockPrefs;
    private SharedPreferences.Editor mockEditor;
    private Gson gson;
    private FavoritesRepository repository;

    private ProductSummary createSampleProduct(int id, String name) {
        return new ProductSummary(
                id,
                name,
                "49.99",
                Currency.USD,
                1,
                "Apparel",
                "https://example.com/img.jpg",
                true,
                ProductStatus.ACTIVE
        );
    }

    @Before
    public void setUp() {
        mockPrefs = mock(SharedPreferences.class);
        mockEditor = mock(SharedPreferences.Editor.class);
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);
        when(mockPrefs.getString(anyString(), anyString())).thenReturn(null);
        gson = new Gson();

        repository = new FavoritesRepository(mockPrefs, gson);
    }

    @Test
    public void testInitialStateIsEmpty() {
        assertFalse(repository.isFavorite(1));
        assertEquals(0, repository.getFavoritesCount());
        assertTrue(repository.getFavoritesSnapshot().isEmpty());
    }

    @Test
    public void testAddFavoritePersistsAndUpdatesLiveData() {
        ProductSummary p1 = createSampleProduct(101, "Organic Cotton Tee");

        repository.addFavorite(p1);

        assertTrue(repository.isFavorite(101));
        assertEquals(1, repository.getFavoritesCount());
        assertEquals(1, repository.getFavoritesSnapshot().size());
        assertEquals(p1, repository.getFavoritesSnapshot().get(0));

        List<ProductSummary> liveDataValue = repository.getFavorites().getValue();
        assertNotNull(liveDataValue);
        assertEquals(1, liveDataValue.size());
        assertEquals(101, liveDataValue.get(0).getId());

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockEditor).putString(ArgumentCaptor.forClass(String.class).capture(), jsonCaptor.capture());
        assertTrue(jsonCaptor.getValue().contains("Organic Cotton Tee"));
        verify(mockEditor).apply();
    }

    @Test
    public void testToggleFavoriteAddsAndRemoves() {
        ProductSummary p1 = createSampleProduct(202, "Linen Overshirt");

        // First toggle: adds item
        repository.toggleFavorite(p1);
        assertTrue(repository.isFavorite(202));
        assertEquals(1, repository.getFavoritesCount());

        // Second toggle: removes item
        repository.toggleFavorite(p1);
        assertFalse(repository.isFavorite(202));
        assertEquals(0, repository.getFavoritesCount());
        assertTrue(repository.getFavoritesSnapshot().isEmpty());
    }

    @Test
    public void testRemoveFavorite() {
        ProductSummary p1 = createSampleProduct(301, "Canvas Tote");
        ProductSummary p2 = createSampleProduct(302, "Ceramic Mug");

        repository.addFavorite(p1);
        repository.addFavorite(p2);
        assertEquals(2, repository.getFavoritesCount());

        repository.removeFavorite(301);
        assertFalse(repository.isFavorite(301));
        assertTrue(repository.isFavorite(302));
        assertEquals(1, repository.getFavoritesCount());
    }

    @Test
    public void testClearFavorites() {
        ProductSummary p1 = createSampleProduct(401, "Minimalist Watch");
        repository.addFavorite(p1);
        assertEquals(1, repository.getFavoritesCount());

        repository.clear();
        assertFalse(repository.isFavorite(401));
        assertEquals(0, repository.getFavoritesCount());
        assertTrue(repository.getFavoritesSnapshot().isEmpty());
    }

    @Test
    public void testLoadsExistingFromPreferencesOnInit() {
        ProductSummary p1 = createSampleProduct(501, "Saved Hoodie");
        String initialJson = gson.toJson(List.of(p1));

        SharedPreferences prefsWithData = mock(SharedPreferences.class);
        when(prefsWithData.getString("key_favorites_json", null)).thenReturn(initialJson);

        FavoritesRepository repoWithData = new FavoritesRepository(prefsWithData, gson);

        assertTrue(repoWithData.isFavorite(501));
        assertEquals(1, repoWithData.getFavoritesCount());
        assertEquals("Saved Hoodie", repoWithData.getFavoritesSnapshot().get(0).getName());
    }
}
