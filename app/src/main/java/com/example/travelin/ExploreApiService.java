package com.example.travelin;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ExploreApiService {
    @GET("explore_destinations.json")
    Call<List<ExploreDestination>> getExploreDestinations();
}
