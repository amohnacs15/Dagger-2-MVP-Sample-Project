package com.androidtitan.culturedapp.main.newsfeed.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import com.google.android.material.appbar.AppBarLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.appcompat.widget.Toolbar;
import android.transition.Explode;
import android.transition.Transition;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.androidtitan.culturedapp.ArticleHelper;
import com.androidtitan.culturedapp.R;
import com.androidtitan.culturedapp.common.SessionManager;
import com.androidtitan.culturedapp.common.structure.MvpActivity;
import com.androidtitan.culturedapp.main.CulturedApp;
import com.androidtitan.culturedapp.main.firebase.PreferenceStore;
import com.androidtitan.culturedapp.main.newsfeed.adapter.NewsFeedAdapter;
import com.androidtitan.culturedapp.main.newsfeed.NewsFeedMvp;
import com.androidtitan.culturedapp.main.newsfeed.NewsFeedPresenter;
import com.androidtitan.culturedapp.main.preferences.PreferencesActivity;
import com.androidtitan.culturedapp.main.toparticle.ui.TopArticleActivity;
import com.androidtitan.culturedapp.main.provider.DatabaseContract;
import com.androidtitan.culturedapp.model.newyorktimes.Article;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.androidtitan.culturedapp.common.Constants.ARTICLE_BOOKMARKED;
import static com.androidtitan.culturedapp.common.Constants.ARTICLE_EXTRA;
import static com.androidtitan.culturedapp.common.Constants.ARTICLE_GEO_FACETS;
import static com.androidtitan.culturedapp.common.Constants.PREFERENCES_APP_FIRST_RUN;
import static com.androidtitan.culturedapp.common.Constants.PREFERENCES_SYNCING_PERIODICALLY;
import static com.androidtitan.culturedapp.main.newsfeed.ui.NewsDetailActivity.SAVED_MULTIMEDIA;

public class NewsFeedActivity extends MvpActivity<NewsFeedPresenter, NewsFeedMvp.View> implements NewsFeedMvp.View, ErrorFragmentInterface,
        DevConsoleDialogFragment.DevConsoleCallback, NewsFeedAdapter.OnClick {
    private final String TAG = getClass().getSimpleName();

    private static final String SENDER_ID = "612691836045";
    public static final String ACCOUNT_TYPE = "com.androidtitan";
    public static final String ACCOUNT = "dummyaccount";

    private static final String SAVED_INSTANCE_STATE_ARTICLES = "newsfeedactivity.savedinstancestatearticles";

    // Sync interval constants...one hour
    public static final long SECONDS_PER_MINUTE = 60L;
    public static final long SYNC_INTERVAL_IN_MINUTES = 180L;
    public static final long SYNC_INTERVAL =
            SYNC_INTERVAL_IN_MINUTES *
                    SECONDS_PER_MINUTE;

    public static final String TOP_ARTICLE_MODE = "newsactivity.toparticlemode";
    public static final int TOP_ARTICLE_TOP = 100;
    public static final int TOP_ARTICLE_OFFLINE = 101;

    private static final int LOADING_ANIM_TIME = 700;

    public static final String ERROR_MESSAGE = "errorfragment.errormessage";
    public static final String ERROR_MAP = "errorfragment.errormap";

    ErrorFragment errorFragment;

    NewsFeedPresenter presenter;

    private Handler handler;

    private Animation fadeAnim;

    private Account account;

    Toolbar supportActionBar;

    ActionBarDrawerToggle drawerToggle;

    @BindView(R.id.colorBgView)
    View bgView;

    @BindView(R.id.loadingTextView)
    TextView loadingTitleText;

    @BindView(R.id.welcomeTextView)
    TextView welcomeText;

    @BindView(R.id.newsList)
    RecyclerView recyclerView;

    @BindView(R.id.refreshFloatingActionButton)
    FloatingActionButton refreshFab;

    @BindView(R.id.drawerLayout)
    DrawerLayout drawerLayout;

    @BindView(R.id.drawer_navigation_view)
    NavigationView navigationView;

    @BindView(R.id.navigation_icon)
    ImageView navImage;

    SharedPreferences sharedPreferences;

    private LinearLayoutManager linearLayoutManager;

    private StaggeredGridLayoutManager staggeredLayoutManager;
    private NewsFeedAdapter adapter;

    private AppBarLayout appBarLayout;

    private List<Article> articles;

    private HashMap<String, Boolean> bookMarkedArticles;

    private boolean isSyncingPeriodically;

    private boolean loading = true;

    private int pastVisibleItems;

    private int visibleItemCount;

    private int totalItemCount;

    private int devConsoleCount = 0;

    public int adapterLoadOffset = 6;

    int screenSize;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        presenter = new NewsFeedPresenter(this);
        initializeTranstionsAndAnimations();
        //initialize dummy account
        account = createSyncAccount(this);
        sharedPreferencesSetup();
        initFCM();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.newsfeed_activity);
        ButterKnife.bind(this);

        setupUserPreferences();

        sessionManager = SessionManager.getInstance();

        articles = new ArrayList<>();
        bookMarkedArticles = sessionManager.getBookmarkedArticles();
        presenter.checkTopArticlesPresent();

        loadingTitleText.setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT) {
            loadingTitleText.setContentDescription(this.getResources().getString(R.string.accessability_loading));
        }

        initializeAnimation();

        screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        setUpActionBar();

        if (savedInstanceState != null) {
            articles = savedInstanceState.getParcelableArrayList(SAVED_INSTANCE_STATE_ARTICLES);
            hiddenFirstLoadAnimation();

        } else {
            if (screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
                articles = presenter.loadArticles(10);
            } else {
                articles = presenter.loadArticles(5);
            }
        }

        navImage.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawers();
            } else {
                drawerLayout.openDrawer(navigationView);
            }
        });

        navigationView.getHeaderView(0).setOnClickListener((header) -> {

            if (devConsoleCount == 6) {
                if (!navigationView.getMenu().getItem(3).isVisible()) {
                    Toast.makeText(this, R.string.prime_dev_console_text, Toast.LENGTH_SHORT).show();
                    navigationView.getMenu().getItem(3).setVisible(true);
                } else {
                    Toast.makeText(this, R.string.bye_dev_console, Toast.LENGTH_SHORT).show();
                    navigationView.getMenu().getItem(3).setVisible(false);
                }

                devConsoleCount = 0;
            } else {
                devConsoleCount++;
            }
        });

        navigationView.setNavigationItemSelectedListener((item) -> {
            drawerLayout.closeDrawers();

            switch (item.getItemId()) {

                case R.id.onboarding_card_generator:

                    if (!adapter.getSharedPreferences().getBoolean(PREFERENCES_APP_FIRST_RUN, false)
                            && !adapter.getAboutStatus()) {
                        adapter.resetOnboardingCard();
                    }

                    break;

                case R.id.about_card_generator:

                    if (!adapter.getSharedPreferences().getBoolean(PREFERENCES_APP_FIRST_RUN, false)
                            && !adapter.getAboutStatus()) {
                        adapter.showAboutCard();
                    }

                    break;

                case R.id.support_mail:

                    launchMailIntent();

                    break;

                case R.id.settings:

                    startActivity(new Intent(this, PreferencesActivity.class));

                    break;

                case R.id.devConsole:
                    //display a dialog fragment
                    showDialog();

                default:

                    Log.e(TAG, "Incorrect navigation drawer item selected");
            }
            return true;
        });

        initializeRecyclerView();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int[] array = null;
                scrollViewParallax(dy);

                // logic for hiding and showing the actionbar shadow when the list is fully scrolled down
                try {
                    if (linearLayoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                        appBarLayout.setElevation(0);
                    }
                } catch (Exception e) {
                    array = staggeredLayoutManager.findFirstCompletelyVisibleItemPositions(array);

                    if (array[0] == 0 || array[1] == 1) {
                        appBarLayout.setElevation(0);
                    }
                }

                if (dy > 0) { //check for active scrolling
                    hideToolbarBy(dy);

                    if (screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE ||
                            getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

                        visibleItemCount = staggeredLayoutManager.getChildCount();
                        totalItemCount = staggeredLayoutManager.getItemCount();
                        int[] firstVisibleItems = null;
                        firstVisibleItems = staggeredLayoutManager.findFirstVisibleItemPositions(firstVisibleItems);

                        if (firstVisibleItems != null && firstVisibleItems.length > 0) {
                            pastVisibleItems = firstVisibleItems[0];
                        }

                        if (loading) {

                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                loading = false;

                                presenter.loadOffsetArticles(10, adapterLoadOffset);
                                adapterLoadOffset += 10;

                                showColoredSnackbar();
                            }
                        }
                    } else {

                        visibleItemCount = linearLayoutManager.getChildCount();
                        totalItemCount = linearLayoutManager.getItemCount();
                        pastVisibleItems = linearLayoutManager.findFirstCompletelyVisibleItemPosition();

                        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

                            if (loading) {

                                if ((visibleItemCount + pastVisibleItems + 1) >= totalItemCount) {
                                    //the +1 accounts for having one less card visible to add

                                    loading = false;
                                    Log.d(TAG, "appending data...");

                                    presenter.loadOffsetArticles(5, adapterLoadOffset);
                                    adapterLoadOffset += 5;

                                    showColoredSnackbar();
                                }
                            }

                        } else {

                            if (loading) {

                                if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {

                                    loading = false;
                                    Log.d(TAG, "appending data...");

                                    presenter.loadOffsetArticles(5, adapterLoadOffset);
                                    adapterLoadOffset += 5;

                                    showColoredSnackbar();
                                }
                            }
                        }
                    }
                } else {
                    showToolbarBy(dy);
                }
            }
        });

        refreshFab.setOnClickListener(v -> {
            presenter.newsArticlesRefresh(articles, 5);
            loading = true;
            showColoredSnackbar();
        });

    }

    /**
     * Calculates the completion height our scaling background view and animation and immediately sets it.
     * Informing the user that no loading has taken place.
     */
    private void hiddenFirstLoadAnimation() {
        ViewTreeObserver viewTreeObserver = bgView.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    bgView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    int viewWidth = bgView.getWidth();
                    int viewHeight = bgView.getHeight();

                    Log.e(TAG, "X : " + viewWidth + " , Y : " + viewHeight);

                    final int bgViewHeight = viewHeight / 3;

                    Log.e(TAG, "modified Y : " + bgViewHeight);

                    CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) bgView.getLayoutParams();
                    params.height = bgViewHeight;
                    bgView.setLayoutParams(params);
                }
            });
        }
    }

    private void launchMailIntent() {
        Intent feedbackIntent = new Intent(Intent.ACTION_SEND);
        feedbackIntent.setType("plain/text");
        feedbackIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"adrian.mohnacs@gmail.com"});
        feedbackIntent.putExtra(Intent.EXTRA_SUBJECT, "Cultured Feedback");
        feedbackIntent.putExtra(Intent.EXTRA_TEXT, "Hi Adrian,\nHere\'s what I think about Cultured...");
        //chooser
        String title = getResources().getString(R.string.chooser_text);
        Intent chooser = Intent.createChooser(feedbackIntent, title);
        if (feedbackIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooser);
        }
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    /*
    Navigation Drawer listening for open and close event with the app icon
    */
    private void setUpActionBar() {
        // Attaching the layout to the toolbar object
        supportActionBar = (Toolbar) findViewById(R.id.toolbar);
        appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);

        setSupportActionBar(supportActionBar);
        getSupportActionBar().setTitle("");
        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                R.string.drawer_open,
                R.string.drawer_closed
        ) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };
        // Set the drawer toggle as the DrawerListener
        drawerLayout.addDrawerListener(drawerToggle);

        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    public NewsFeedPresenter getPresenter() {
        return presenter;
    }

    @Override
    public NewsFeedMvp.View getMvpView() {
        return this;
    }

    @Override
    public void onResume() {
        super.onResume();

        setWelcomeText();
    }

    private void showDialog() {
        DialogFragment newFragment = DevConsoleDialogFragment.newInstance();
        newFragment.show(getSupportFragmentManager(), "DevConsoleDialogFragment");
    }

    private void sharedPreferencesSetup() {

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        isSyncingPeriodically = sharedPreferences.getBoolean(PREFERENCES_SYNCING_PERIODICALLY, false);
    }

    private void setupUserPreferences() {

        setWelcomeText();
/*
        String continentString = getString(R.string.pref_key_continent);
        switch (sharedPreferences.getString(continentString, "")) {
            case "North America":

                break;

            case "South America":

                break;

            default:
//                throw new IllegalArgumentException("Invalid case for switch statement");
        }
        */
    }

    private void setWelcomeText() {
        String userName = sharedPreferences.getString(getString(R.string.pref_key_name), "");
        welcomeText.setText("Welcome " + userName);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (isSyncingPeriodically) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(PREFERENCES_SYNCING_PERIODICALLY, false).apply();

            ContentResolver.removePeriodicSync(account, DatabaseContract.AUTHORITY, Bundle.EMPTY);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.news_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent passingIntent = new Intent(this, TopArticleActivity.class);

        switch (item.getItemId()) {
//            case R.id.menu_item_toparticle:
//
//                passingIntent.putExtra(TOP_ARTICLE_MODE, TOP_ARTICLE_TOP);
//                startActivity(passingIntent);
//
//                break;

            case R.id.menu_item_offline:

                passingIntent.putExtra(TOP_ARTICLE_MODE, TOP_ARTICLE_OFFLINE);
                startActivity(passingIntent);

                break;

            default:
                throw new IllegalArgumentException("Invalid options item: " + item.getItemId());

        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Save all appropriate fragment states.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Let's decide if we want to keep it or not
//        outState.putParcelableArrayList(SAVED_STATE_ARTICLE_LIST,
//                (ArrayList<? extends Parcelable>) articles);

        outState.putParcelableArrayList(SAVED_INSTANCE_STATE_ARTICLES,
                (ArrayList<? extends Parcelable>) articles);
    }

    private Account createSyncAccount(Context context) {

        Account newAccount = new Account(ACCOUNT, ACCOUNT_TYPE);
        AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        return newAccount;

    }


    @Override
    public void onLoadComplete() {

        if (CulturedApp.isNewsFeedFirstLoad()) {
            firstLoadCompleteAnimation();
        }
        CulturedApp.setNewsFeedFirstLoad(false);

    }

    @Override
    public void appendAdapterItem(Article article) {
        adapter.appendToAdapter(article);
    }


    @Override
    public void insertAdapterItem(int index, Article article) {
        adapter.insertIntoAdapter(index, article);
    }

    @Override
    public void insertAdapterItems(int index, ArrayList<Article> articles) {
        adapter.insertIntoAdapter(index, articles);
    }

    @Override
    public List<Article> getArticles() {
        return articles;
    }

    @Override
    public void displayError(String message, Map<String, Object> additionalProperties) {

        Bundle args = new Bundle();
        args.putString(ERROR_MESSAGE, message);

        errorFragment = ErrorFragment.newInstance(message, additionalProperties);
        errorFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.main_content, errorFragment).commit();

    }

    // todo: this might be getting called too much onResume(). take a look at it when convenient
    @Override
    public void doTopArticlesExist(boolean articlesExist) {
        if (!articlesExist) {
            getTopArticlesOnIntialLaunch();
        }
    }

    @Override
    public void restartArticleLoad() {
        getSupportFragmentManager().beginTransaction().remove(errorFragment).commit();
        presenter.loadArticles(10);
    }

    private void initializeRecyclerView() {

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                && screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE) {

            // using StaggeredGrid as the layout manager    
            staggeredLayoutManager = new StaggeredGridLayoutManager(3, 1);
            recyclerView.setLayoutManager(staggeredLayoutManager);

        } else if (screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE ||
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

            staggeredLayoutManager = new StaggeredGridLayoutManager(2, 1);
            recyclerView.setLayoutManager(staggeredLayoutManager);

        } else {

            linearLayoutManager = new LinearLayoutManager(this);
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(linearLayoutManager);
        }

        // specify an adapter to use with your RecyclerView    
        adapter = new NewsFeedAdapter(this, this, articles);
        recyclerView.setAdapter(adapter);
    }

    //animation and UI methods
    private void scrollViewParallax(int dy) { // divided by three to scroll slower
        bgView.setTranslationY(bgView.getTranslationY() - dy / 3);
        welcomeText.setTranslationY(welcomeText.getTranslationY() - dy / 2);
    }

    public void initializeAnimation() {

        handler = new Handler();
        fadeAnim = AnimationUtils.loadAnimation(this, R.anim.fade_out);

    }

    public void firstLoadCompleteAnimation() {

        final ScaleAnimation scale = new ScaleAnimation((float) 1.0, (float) 1.0, (float) 1.0, (float) 0.33);
        scale.setFillAfter(true);
        scale.setDuration(LOADING_ANIM_TIME);

        loadingTitleText.setVisibility(View.GONE);
        loadingTitleText.startAnimation(fadeAnim);
        handler.postDelayed(() -> bgView.startAnimation(scale), LOADING_ANIM_TIME);

        handler.postDelayed(() -> {

            adapter.notifyDataSetChanged();
        }, LOADING_ANIM_TIME * 2);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void initializeTranstionsAndAnimations() {
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            // inside your activity (if you did not enable transitions in your theme)
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
            // set an exit transition
            Transition plode = new Explode();
            getWindow().setEnterTransition(plode);
            getWindow().setExitTransition(plode);

        } else {
            // do something for phones running an API before lollipop
        }
    }

    private void showToolbarBy(int dy) {

        if (cannotShowMore(dy)) {
            appBarLayout.setTranslationY(0);

        } else {
            appBarLayout.setTranslationY(appBarLayout.getTranslationY() - dy);

            if (dy < 0) {
                appBarLayout.setElevation(8);
            } else {
                appBarLayout.setElevation(0);
            }
        }

    }

    private void hideToolbarBy(int dy) {
        if (cannotHideMore(dy)) {
            appBarLayout.setTranslationY(-appBarLayout.getBottom());
        } else {
            appBarLayout.setTranslationY(appBarLayout.getTranslationY() - dy);
        }
    }

    private boolean cannotHideMore(int dy) {
        return Math.abs(appBarLayout.getTranslationY() - dy) > appBarLayout.getBottom();
    }

    private boolean cannotShowMore(int dy) {
        return appBarLayout.getTranslationY() - dy > 0;
    }

    private void initFCM() {
        PreferenceStore preferenceStore = PreferenceStore.get(this);
        String currentToken = preferenceStore.getFcmToken();

        if (currentToken == null) {
            new FCMRegistrationTask().execute();
        } else {
            Log.d(TAG, "Have token: " + currentToken);

            if (!isSyncingPeriodically) {
                setupPeriodicSync();
            }
        }
    }

    private void showColoredSnackbar() {
        Snackbar loadingSnackbar = Snackbar.make(recyclerView,
                getResources().getString(R.string.simple_loading),
                Snackbar.LENGTH_LONG);
        View snackbarView = loadingSnackbar.getView();
        snackbarView.setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.colorPrimary));
        loadingSnackbar.show();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void sendDetailActivity(Article article, ImageView imageView) {

        Intent intent = new Intent(this, NewsDetailActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        intent.putExtra(ARTICLE_EXTRA, article);
        intent.putStringArrayListExtra(ARTICLE_GEO_FACETS, ArticleHelper.getGeoFacetArrayList(article));
        intent.putExtra(ARTICLE_BOOKMARKED, ArticleHelper.isArticleBookmarked(bookMarkedArticles, article.getTitle()));

        intent.putExtra(SAVED_MULTIMEDIA, ArticleHelper.multimediaToJsonString(article.getMultimedia()));

        startActivity(intent);
    }

    //starting our SyncAdapter
    private class FCMRegistrationTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {

            int googleApiAvailable = GoogleApiAvailability.getInstance()
                    .isGooglePlayServicesAvailable(NewsFeedActivity.this);
            if (googleApiAvailable != ConnectionResult.SUCCESS) {
                Log.e(TAG, "Play services not available, cannot register for GCM");
                return null;
            }

            /*InstanceID instanceID = InstanceID.getInstance(NewsFeedActivity.this);

            try {
                String token = instanceID.getToken(SENDER_ID, FirebaseMessaging.INSTANCE_ID_SCOPE, null);
                Log.d(TAG, "Got token: " + token);
                return token;

            } catch (IOException e) {
                Log.e(TAG, "Failed to get token from InstanceID", e);
                return null;
            }*/
            return null;
        }

        @Override
        protected void onPostExecute(String token) {
            super.onPostExecute(token);

            if (token == null) {
                setupPeriodicSync();
            } else {
                PreferenceStore.get(NewsFeedActivity.this).setFcmToken(token);
            }
        }
    }

    private void setupPeriodicSync() {

        isSyncingPeriodically = true;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREFERENCES_SYNCING_PERIODICALLY, true).apply();

        ContentResolver.setIsSyncable(account, DatabaseContract.AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(
                account, DatabaseContract.AUTHORITY, true);
        ContentResolver.addPeriodicSync(
                account, DatabaseContract.AUTHORITY, Bundle.EMPTY, SYNC_INTERVAL);
        ContentResolver.requestSync(account, DatabaseContract.AUTHORITY, Bundle.EMPTY);
    }

    private void getTopArticlesOnIntialLaunch() {
        //todo: check to ensure that it is the first launch...we should have this in shared preferences
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_FORCE, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(null, DatabaseContract.AUTHORITY, bundle);
    }


}
