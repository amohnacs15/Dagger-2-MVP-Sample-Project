package com.androidtitan.hotspots.main.ui;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.androidtitan.hotspots.R;
import com.androidtitan.hotspots.main.presenter.newsdetail.NewsDetailPresenter;
import com.androidtitan.hotspots.main.ui.activities.NewsDetailActivity;

import butterknife.Bind;
import butterknife.ButterKnife;


public class WikiFragment extends Fragment {
    private final String TAG = getClass().getSimpleName();

    private NewsDetailPresenter presenter;

    @Bind(R.id.webview)
    WebView articleWebView;

    private String url;


    public WikiFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            url = getArguments().getString(NewsDetailActivity.NEWS_DETAIL_WIKI_URL);
            Log.e(TAG, url);
        }
        else {
            Log.e(TAG, "stump");
        }

        presenter = ((NewsDetailActivity)getActivity()).getPresenter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_wiki, container, false);
        ButterKnife.bind(this, v);

        articleWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                articleWebView.loadUrl(url);
                return true;
            }
        });

        articleWebView.getSettings().setLoadsImagesAutomatically(true);
        articleWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        articleWebView.getSettings().setJavaScriptEnabled(true);
        articleWebView.getSettings().setBuiltInZoomControls(true);
        articleWebView.loadUrl(url);

        return v;
    }

}
