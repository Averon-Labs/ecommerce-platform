package com.averonlabs.eshopverse.catalog;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for managing customer favorite/wishlist products.
 * Persists favorites in SharedPreferences as JSON and exposes reactive LiveData.
 */
public class FavoritesRepository {

    private static final String PREFS_NAME = "eshop_favorites_prefs";
    private static final String KEY_FAVORITES_JSON = "key_favorites_json";

    private static volatile FavoritesRepository sInstance;

    private final SharedPreferences prefs;
    private final Gson gson;
    private final Map<Integer, ProductSummary> favoritesMap = new LinkedHashMap<>();
    private final MutableLiveData<List<ProductSummary>> favoritesLiveData = new MutableLiveData<>(Collections.emptyList());

    public static synchronized FavoritesRepository getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new FavoritesRepository(context.getApplicationContext());
        }
        return sInstance;
    }

    @VisibleForTesting
    public static synchronized void setInstance(@Nullable FavoritesRepository instance) {
        sInstance = instance;
    }

    public FavoritesRepository(@NonNull Context context) {
        this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE), new Gson());
    }

    public FavoritesRepository(@NonNull SharedPreferences prefs, @NonNull Gson gson) {
        this.prefs = prefs;
        this.gson = gson;
        loadFromPrefs();
    }

    private synchronized void loadFromPrefs() {
        favoritesMap.clear();
        String json = prefs.getString(KEY_FAVORITES_JSON, null);
        if (json != null && !json.trim().isEmpty()) {
            try {
                Type type = new TypeToken<List<ProductSummary>>() {}.getType();
                List<ProductSummary> list = gson.fromJson(json, type);
                if (list != null) {
                    for (ProductSummary product : list) {
                        if (product != null) {
                            favoritesMap.put(product.getId(), product);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        notifyObservers();
    }

    private synchronized void persistToPrefs() {
        try {
            List<ProductSummary> list = new ArrayList<>(favoritesMap.values());
            String json = gson.toJson(list);
            prefs.edit().putString(KEY_FAVORITES_JSON, json).apply();
        } catch (Exception ignored) {
        }
        notifyObservers();
    }

    private void notifyObservers() {
        favoritesLiveData.postValue(Collections.unmodifiableList(new ArrayList<>(favoritesMap.values())));
    }

    public synchronized boolean isFavorite(int productId) {
        return favoritesMap.containsKey(productId);
    }

    public synchronized void toggleFavorite(@NonNull ProductSummary product) {
        if (favoritesMap.containsKey(product.getId())) {
            removeFavorite(product.getId());
        } else {
            addFavorite(product);
        }
    }

    public synchronized void addFavorite(@NonNull ProductSummary product) {
        favoritesMap.put(product.getId(), product);
        persistToPrefs();
    }

    public synchronized void removeFavorite(int productId) {
        if (favoritesMap.remove(productId) != null) {
            persistToPrefs();
        }
    }

    @NonNull
    public LiveData<List<ProductSummary>> getFavorites() {
        return favoritesLiveData;
    }

    @NonNull
    public synchronized List<ProductSummary> getFavoritesSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(favoritesMap.values()));
    }

    public synchronized int getFavoritesCount() {
        return favoritesMap.size();
    }

    public synchronized void clear() {
        favoritesMap.clear();
        persistToPrefs();
    }
}
