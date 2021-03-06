package com.androidtitan.culturedapp.main.toparticle.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.NavUtils;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidtitan.culturedapp.ArticleHelper;
import com.androidtitan.culturedapp.R;
import com.androidtitan.culturedapp.common.FileManager;
import com.androidtitan.culturedapp.common.SessionManager;
import com.androidtitan.culturedapp.common.structure.MvpActivity;
import com.androidtitan.culturedapp.main.newsfeed.ui.NewsDetailActivity;
import com.androidtitan.culturedapp.main.newsfeed.ui.NewsFeedActivity;
import com.androidtitan.culturedapp.main.toparticle.TopArticleAdapter;
import com.androidtitan.culturedapp.main.toparticle.TopArticleMvp;
import com.androidtitan.culturedapp.main.toparticle.TopArticlePresenter;
import com.androidtitan.culturedapp.model.newyorktimes.Article;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.androidtitan.culturedapp.common.Constants.ARTICLE_BOOKMARKED;
import static com.androidtitan.culturedapp.common.Constants.ARTICLE_EXTRA;
import static com.androidtitan.culturedapp.common.Constants.ARTICLE_GEO_FACETS;
import static com.androidtitan.culturedapp.main.newsfeed.ui.NewsDetailActivity.SAVED_MULTIMEDIA;

public class TopArticleActivity extends MvpActivity<TopArticlePresenter, TopArticleMvp.View>
        implements TopArticleMvp.View, TopArticleAdapter.OnClick, FileManager.FileCallback {
    private final String TAG = TopArticleActivity.class.getSimpleName();

    TopArticlePresenter presenter;

    @BindView(R.id.topArticleRecyclerView)
    RecyclerView recyclerView;

    @BindView(R.id.pleaseWaitTextView)
    TextView pleaseWaitText;

    @BindView(R.id.fab)
    FloatingActionButton refreshFab;

    private LinearLayoutManager linearLayoutManager;
    private TopArticleAdapter topArticleAdapter;

    private List<Article> adapterArticleList;
    private boolean isTopArticleMode;

    private HashMap<String, Boolean> bookmarkedArticleRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        presenter = new TopArticlePresenter(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.top_article_activity);

        ButterKnife.bind(this);

        // TODO: 10/14/17 we need to pass out `isTopArticleMode` value in savedInstanceState on rotation

        bookmarkedArticleRef = SessionManager.getInstance().getBookmarkedArticles();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.getInt(NewsFeedActivity.TOP_ARTICLE_MODE) == NewsFeedActivity.TOP_ARTICLE_TOP) {
                isTopArticleMode = true;
                setupTopArticle();
            } else {
                isTopArticleMode = false;
                setupOfflineMode();
            }
        }
    }

    private void setupTopArticle() {
        setupToolbar(getString(R.string.top_article_string));
        setupRecyclerView(true);

        refreshFab.setOnClickListener(v -> presenter.loadArticles(isTopArticleMode));
    }

    private void setupOfflineMode() {
        setupToolbar(getString(R.string.offline_string));
        setupRecyclerView(false);

        refreshFab.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_delete, null));
        refreshFab.setOnClickListener(v -> buildAlertDialogue(this).show());
    }

    private AlertDialog buildAlertDialogue(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.offline_delete_title);
        builder.setMessage(R.string.offline_delete_message);
        builder.setPositiveButton(R.string.offline_delete_confirm, (dialog, which) -> {
            //clears our text file containing our offline articles
            FileManager.getInstance(TopArticleActivity.this).deleteFile(TopArticleActivity.this, TopArticleActivity.this);
            dialog.dismiss();
            finish();
        });
        builder.setNegativeButton(R.string.offline_delete_cancel, (dialog, which) -> {
            dialog.dismiss();
        });
        return builder.create();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //todo: store the data for the list on Orientation changed
    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public TopArticlePresenter getPresenter() {
        return presenter;
    }

    @Override
    public TopArticleMvp.View getMvpView() {
        return this;
    }

    @Override
    public void updateArticles(List<Article> articleList) {

        if(articleList != null && articleList.size() > 0) {
            adapterArticleList.clear();
            adapterArticleList.addAll(articleList);

            //delaying the setting of our Adapter because if an recyclerview is set with an empty adapter
            //the views will not have been instantiated meaning they cannot be updated...
            if(recyclerView.getAdapter() == null) {
                recyclerView.setAdapter(topArticleAdapter);
            } else {
                topArticleAdapter.notifyDataSetChanged();
            }

            pleaseWaitText.setVisibility(View.GONE);
        }
    }

    @Override
    public void setLoading() {
// TODO: 10/14/17
    }

    @Override
    public void displayDataNotAvailable() {
// TODO: 10/14/17
    }

    @Override
    public void displayDataEmpty() {
// TODO: 10/14/17
    }

    public void setupToolbar(String upToolbar) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(upToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void setupRecyclerView(boolean isTopArticleMode) {

        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        adapterArticleList = new ArrayList<>();
        topArticleAdapter = new TopArticleAdapter(this, this, adapterArticleList);
        presenter.loadArticles(isTopArticleMode);
    }

    @Override
    public void sendDetailActivity(Article article, ImageView imageView) {
        Intent intent = new Intent(this, NewsDetailActivity.class);
        intent.putExtra(ARTICLE_EXTRA, article);
        intent.putStringArrayListExtra(ARTICLE_GEO_FACETS, ArticleHelper.getGeoFacetArrayList(article));
        intent.putExtra(ARTICLE_BOOKMARKED, ArticleHelper.isArticleBookmarked(bookmarkedArticleRef, article.getTitle()));

        intent.putExtra(SAVED_MULTIMEDIA, ArticleHelper.multimediaToJsonString(article.getMultimedia()));

        startActivity(intent);
    }

    @Override
    public void onFileWriteComplete(String response, boolean hasError) {
        //no op
    }

    @Override
    public void onFileDeleteComplete() {
        topArticleAdapter.notifyDataSetChanged();
    }
}