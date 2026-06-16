package com.example.travelin;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {
    // Replace this URL when the real travelin-api repository is available.
    public static final String BASE_URL =
            "https://raw.githubusercontent.com/mon-compte/travelin-api/main/";

    private static ExploreApiService exploreApiService;

    private RetrofitClient() {
    }

    public static synchronized ExploreApiService getExploreApiService() {
        if (exploreApiService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            exploreApiService = retrofit.create(ExploreApiService.class);
        }
        return exploreApiService;
    }
}
