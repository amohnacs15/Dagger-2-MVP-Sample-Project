package com.androidtitan.hotspots.main.domain;

import com.androidtitan.hotspots.main.model.SpotifyResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by amohnacs on 3/16/16.
 */
public interface SpotifyEndpointInterface {

    //@GET("search?q={searchText}&type=track&limit={count}")
    @GET("search")
    Call<SpotifyResponse> tracks(
            @Query("q") String searchText,
            @Query("type") String type,
            @Query("limit") int count);

    /*@GET("h5/metadata/medals")
    Call<List<HaloMedal>> descList();

    @GET("h5/metadata/medals")
    Call<List<HaloMedal>> classList();

    @GET("h5/metadata/medals")
    Call<List<HaloMedal>> difficultList();*/



}
