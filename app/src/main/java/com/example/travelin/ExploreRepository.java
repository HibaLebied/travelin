package com.example.travelin;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExploreRepository {
    public static final String PREFS_NAME = "travelin_prefs";
    public static final String KEY_CACHED_JSON = "cached_explore_json";
    private static final String KEY_CACHE_VERSION = "cached_explore_version";
    private static final int CACHE_VERSION = 5;
    public static final String KEY_OFFLINE_MODE = "offline_mode_enabled";
    public static final String KEY_PREFERRED_THEME = "preferred_theme";
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_CURRENCY = "currency";

    private static final Type DESTINATION_LIST_TYPE =
            new TypeToken<List<ExploreDestination>>() { }.getType();

    private final Context appContext;
    private final SharedPreferences preferences;
    private final Gson gson = new Gson();

    public ExploreRepository(Context context) {
        appContext = context.getApplicationContext();
        preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (preferences.getInt(KEY_CACHE_VERSION, 0) != CACHE_VERSION) {
            preferences.edit()
                    .remove(KEY_CACHED_JSON)
                    .putInt(KEY_CACHE_VERSION, CACHE_VERSION)
                    .apply();
        }
    }

    public void getExploreDestinations(RepositoryCallback callback) {
        if (preferences.getBoolean(KEY_OFFLINE_MODE, false)) {
            List<ExploreDestination> cached = readCache();
            if (cached.isEmpty()) {
                callback.onError("Aucune donnée disponible hors ligne");
            } else {
                callback.onSuccess(cached, true);
            }
            return;
        }

        RetrofitClient.getExploreApiService()
                .getExploreDestinations()
                .enqueue(new Callback<List<ExploreDestination>>() {
                    @Override
                    public void onResponse(
                            Call<List<ExploreDestination>> call,
                            Response<List<ExploreDestination>> response
                    ) {
                        List<ExploreDestination> destinations = response.body();
                        if (response.isSuccessful()
                                && destinations != null
                                && !destinations.isEmpty()) {
                            saveCache(destinations);
                            callback.onSuccess(destinations, false);
                            return;
                        }
                        useFallback(callback);
                    }

                    @Override
                    public void onFailure(Call<List<ExploreDestination>> call, Throwable throwable) {
                        useFallback(callback);
                    }
                });
    }

    public List<ExploreDestination> readCache() {
        String json = preferences.getString(KEY_CACHED_JSON, null);
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<ExploreDestination> destinations = gson.fromJson(json, DESTINATION_LIST_TYPE);
            return destinations == null ? Collections.emptyList() : destinations;
        } catch (RuntimeException exception) {
            return Collections.emptyList();
        }
    }

    private void useFallback(RepositoryCallback callback) {
        List<ExploreDestination> cached = readCache();
        if (!cached.isEmpty()) {
            callback.onSuccess(cached, true);
            return;
        }

        // Temporary mock while BASE_URL still points to the example repository.
        List<ExploreDestination> mock = readBundledMock();
        if (!mock.isEmpty()) {
            saveCache(mock);
            callback.onSuccess(mock, true);
        } else {
            callback.onError("Impossible de charger les destinations");
        }
    }

    private void saveCache(List<ExploreDestination> destinations) {
        preferences.edit()
                .putString(KEY_CACHED_JSON, gson.toJson(destinations))
                .putInt(KEY_CACHE_VERSION, CACHE_VERSION)
                .apply();
    }

    private List<ExploreDestination> readBundledMock() {
        try (InputStream input = appContext.getAssets().open("explore_destinations.json");
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(input, StandardCharsets.UTF_8))) {
            List<ExploreDestination> destinations = gson.fromJson(reader, DESTINATION_LIST_TYPE);
            return destinations == null ? Collections.emptyList() : destinations;
        } catch (IOException | RuntimeException exception) {
            return Collections.emptyList();
        }
    }

    public interface RepositoryCallback {
        void onSuccess(List<ExploreDestination> destinations, boolean fromCache);

        void onError(String message);
    }
}
