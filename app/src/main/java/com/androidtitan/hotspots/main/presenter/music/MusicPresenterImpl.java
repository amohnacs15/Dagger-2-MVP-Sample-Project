package com.androidtitan.hotspots.main.presenter.music;

import android.content.Context;
import android.util.Log;

import com.androidtitan.hotspots.main.domain.retrofit.DaggerSpotifyRetrofitComponent;
import com.androidtitan.hotspots.main.domain.retrofit.SpotifyEndpointInterface;
import com.androidtitan.hotspots.main.domain.retrofit.SpotifyRetrofit;
import com.androidtitan.hotspots.main.model.spotify.Item;
import com.androidtitan.hotspots.main.model.spotify.SpotifyResponse;
import com.androidtitan.hotspots.main.presenter.news.NewsView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Created by amohnacs on 3/15/16.
 */
public class MusicPresenterImpl implements MusicPresenter {
    private final String TAG = getClass().getSimpleName();

    @Inject
    SpotifyRetrofit spotifyRetrofit;

    private Retrofit retrofit;
    private Context context;
    private MusicView musicView;
    private NewsView newsView;

    @Inject
    public MusicPresenterImpl(Context context, MusicView musicView) {

        DaggerSpotifyRetrofitComponent.create()
                .inject(this);

        this.context = context;
        this.musicView = musicView;
    }

    @Override
    public List<Item> querySpotifyTracks(String search, int count) {
        retrofit = spotifyRetrofit.getRetrofit();

        final ArrayList<Item> itemList = new ArrayList<Item>();

        SpotifyEndpointInterface spotifyService = retrofit.create(SpotifyEndpointInterface.class);
        final Call<SpotifyResponse> call = spotifyService.tracks(search, "track", count);

        call.enqueue(new Callback<SpotifyResponse>() {
            @Override
            public void onResponse(Call<SpotifyResponse> call, Response<SpotifyResponse> response) {
                if(response.isSuccessful()) {

                    SpotifyResponse resp = response.body();

                    for(Item item : resp.getTracks().getItems()) {
                        itemList.add(item);
                        musicView.updateImageAdapter();

                    }
                    //Log.e(TAG, resp.getTracks().getItems().get(0).getName());
                } else {
                    Log.e(TAG, "response fail");
                }
            }

            @Override
            public void onFailure(Call<SpotifyResponse> call, Throwable t) {

            }
        });

        return itemList;
    }

}