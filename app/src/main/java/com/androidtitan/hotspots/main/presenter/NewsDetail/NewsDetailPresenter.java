package com.androidtitan.hotspots.main.presenter.newsdetail;

import android.widget.ImageView;

import com.androidtitan.hotspots.main.model.newyorktimes.Multimedium;

import java.util.List;

/**
 * Created by amohnacs on 3/26/16.
 */
public interface NewsDetailPresenter {

    void getHeaderImage(List<Multimedium> mediaList, ImageView image, int pixelWidth, int pixelHeight);
    String formatDESUrl(String facet);
    String formatPERUrl(String facet);
    String formatORgUrl(String facet);
    String formatGEOUrl(String facet);
    void startMusicActivity(String searcher);
}
