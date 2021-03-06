package com.androidtitan.culturedapp.main.toparticle;

import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import androidx.annotation.NonNull;

import com.androidtitan.culturedapp.main.provider.LoaderHelper;
import com.androidtitan.culturedapp.main.provider.wrappers.ArticleCursorWrapper;
import com.androidtitan.culturedapp.main.provider.wrappers.FacetCursorWrapper;
import com.androidtitan.culturedapp.main.provider.wrappers.MultimediumCursorWrapper;
import com.androidtitan.culturedapp.model.newyorktimes.Article;
import com.androidtitan.culturedapp.model.newyorktimes.Facet;
import com.androidtitan.culturedapp.model.newyorktimes.FacetType;
import com.androidtitan.culturedapp.model.newyorktimes.Multimedium;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.androidtitan.culturedapp.common.Constants.ARTICLE_LOADER_ID;
import static com.androidtitan.culturedapp.common.Constants.TOP_ARTICLE_FACET_LOADER_ID;
import static com.androidtitan.culturedapp.common.Constants.TOP_ARTICLE_MEDIA_LOADER_ID;
import static com.androidtitan.culturedapp.model.newyorktimes.FacetType.DES;
import static com.androidtitan.culturedapp.model.newyorktimes.FacetType.GEO;
import static com.androidtitan.culturedapp.model.newyorktimes.FacetType.ORG;
import static com.androidtitan.culturedapp.model.newyorktimes.FacetType.PER;

/**
 * Created by amohnacs on 9/19/16.
 */

/**
    Because we do not need to tie our Loader to the lifecycle of activities and fragments , which is why we use
    LoaderManager, we can interact directly with the Loader in the Provider.
<p>
    The Provider will be created well before our activity before it is being manufactured by Dagger 2.
    We will store the list of Articles inside the provider and they are handed down to the presenter and activity
    when it is ready.

*/
public class TopArticleProvider implements TopArticleMvp.Provider, Loader.OnLoadCompleteListener<Cursor> {
    private final String TAG = getClass().getSimpleName();

    private static volatile TopArticleProvider instance;
    @NonNull
    private Context context;

    //todo: this needs to changed to a WeakReference at some point
    private CallbackListener reservedCallback;

    private ArrayList<Article> articles = new ArrayList<>();
    private ArrayList<Multimedium> media = new ArrayList<>();
    private HashMap<FacetType, HashMap<Integer, List<Facet>>> facetMap = new HashMap<>();

    private LoaderHelper loaderHelper;

    CursorLoader articleCursorLoader;
    CursorLoader mediaCursorLoader;
    CursorLoader facetCursorLoader;

    public static TopArticleProvider getInstance(Context context) {
        if(instance == null) {
            synchronized (TopArticleProvider.class) {
                if(instance == null) {
                    instance = new TopArticleProvider(context);
                }
            }
        }
        return instance;
    }

    private TopArticleProvider(Context context) {
        this.context = context;

        loaderHelper = new LoaderHelper();
    }

    @Override
    public void fetchArticles(CallbackListener listener) {

        articleCursorLoader =  loaderHelper.createBasicCursorLoader(ARTICLE_LOADER_ID);
        articleCursorLoader.registerListener(ARTICLE_LOADER_ID, this);
        reservedCallback = listener;
        articleCursorLoader.startLoading();
    }

    @Override
    public void fetchFacets(CallbackListener listener) {
        facetCursorLoader = loaderHelper.createBasicCursorLoader(TOP_ARTICLE_FACET_LOADER_ID);
        facetCursorLoader.registerListener(TOP_ARTICLE_FACET_LOADER_ID, this);
        reservedCallback = listener;
        facetCursorLoader.startLoading();
    }


    @Override
    public void onLoadComplete(Loader<Cursor> loader, Cursor cursor) {

        switch (loader.getId()) {

            case ARTICLE_LOADER_ID:
                // this cursor loads Articles minus media
                if(cursor != null && !cursor.isClosed()) {
                    if (cursor.getCount() > 0) {

                        loadArticlesMinusMedia(cursor, articles);
                    } else {
                        if(reservedCallback != null) {
                            reservedCallback.cursorDataEmpty();
                        }
                    }
                } else {
                    if(reservedCallback != null) {
                        reservedCallback.cursorDataNotAvailable();
                    }
                }

                facetCursorLoader = loaderHelper.createBasicCursorLoader(TOP_ARTICLE_FACET_LOADER_ID);
                facetCursorLoader.registerListener(TOP_ARTICLE_FACET_LOADER_ID, this);

                facetCursorLoader.startLoading();

                break;

            case TOP_ARTICLE_FACET_LOADER_ID:

                if(cursor != null && cursor.getCount() > 0) {

                    updateFacetMapDelegation(cursor, facetMap);

                    //we have already processed the articles and the facets will then be added to them
                    if(articles != null && !articles.isEmpty()) {
                        mediaCursorLoader = loaderHelper.createBasicCursorLoader(TOP_ARTICLE_MEDIA_LOADER_ID);
                        mediaCursorLoader.registerListener(TOP_ARTICLE_MEDIA_LOADER_ID, this);

                        mediaCursorLoader.startLoading();
                    } else {
                        reservedCallback.onFacetConstructionComplete(facetMap);
                    }
                }
                break;

            case TOP_ARTICLE_MEDIA_LOADER_ID:

                if (cursor != null && cursor.getCount() > 0) {
                    MultimediumCursorWrapper wrapper = new MultimediumCursorWrapper(cursor);
                    wrapper.moveToFirst();
                    while (!wrapper.isAfterLast()) {

                        Multimedium multimedium = wrapper.getMultimedium();
                        media.add(multimedium);
                        //Log.e(TAG, "media : " + multimedium.getStoryId() + " : " + multimedium.getCaption());
                        wrapper.moveToNext();
                    }
                    cursor.close();
                }


                articles = getMergedArticles(articles, media, facetMap);

                if(reservedCallback != null) {
                   reservedCallback.onArticleConstructionComplete(articles);
                }

                break;

            default:
                throw new IllegalArgumentException("You have passed in an illegal Loader ID : " + loader.getId());
        }
    }

    /**
     *
     * @param cursor
     * @param articles
     */
    private void loadArticlesMinusMedia(Cursor cursor, ArrayList<Article> articles) {

        ArticleCursorWrapper wrapper = new ArticleCursorWrapper(cursor);
        wrapper.moveToFirst();
        while (!wrapper.isAfterLast()) {

            Article article = wrapper.getArticle();
            articles.add(article);
            //Log.e(TAG, article.getId() + " : " + article.getTitle());
            wrapper.moveToNext();
        }
        cursor.close();
    }

    private void updateFacetMapDelegation(Cursor cursor, HashMap<FacetType, HashMap<Integer, List<Facet>>> facetMap) {

        FacetCursorWrapper wrapper = new FacetCursorWrapper(cursor);
        wrapper.moveToFirst();
        while(!wrapper.isAfterLast()) {
            Facet facet = wrapper.getFacet();

            switch (facet.getFacetType()) {
                case DES:
                    updateFacetMap(facetMap, null, DES, facet);
                    break;
                case ORG:
                    updateFacetMap(facetMap, null, ORG, facet);
                    break;
                case PER:
                    updateFacetMap(facetMap, null, PER, facet);
                    break;
                case GEO:
                    updateFacetMap(facetMap, null, GEO, facet);
                    break;


            }

            wrapper.moveToNext();
        }
        cursor.close();

    }


    private ArrayList<Article> getMergedArticles(ArrayList<Article> articles, ArrayList<Multimedium> media,
                                                 HashMap<FacetType, HashMap<Integer, List<Facet>>> facetMap) {

        for (Article article : articles) {
            ArrayList<Multimedium> tempMultimedium = new ArrayList<>();
            for (Multimedium multimedium : media) {
                if (multimedium.getStoryId() == article.getId()) {
                    tempMultimedium.add(multimedium);
                }
                article.setMultimedia(tempMultimedium);
            }

            //todo: there is a better way to do this than try/catch we're breaking best practices
            try {
                article.setDesFacet(facetMap.get(DES).get((int) article.getId()));
                article.setPerFacet(facetMap.get(PER).get((int) article.getId()));
                article.setOrgFacet(facetMap.get(ORG).get((int) article.getId()));
                article.setGeoFacet(facetMap.get(GEO).get((int) article.getId()));
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

        }
        return articles;
    }

    private void updateFacetMap(HashMap<FacetType, HashMap<Integer, List<Facet>>> facetMap,
                                HashMap<Integer, List<Facet>> storyIdMap,
                                FacetType facetType, Facet facet) {

        /*
        If our existing FacetMap does have a value associated with this key...update
         */
        if(facetMap.get(facet.getFacetType()) != null) {
            storyIdMap = facetMap.get(facetType);
            // If our StoryId EXISTS and
            if(facetMap.get(facetType).get(facet.getStoryId()) != null) {

                List<Facet> facetArrayList = storyIdMap.get(facet.getStoryId());

                facetArrayList.add(facet);
                storyIdMap.put(facet.getStoryId(), facetArrayList);
                facetMap.put(facetType, storyIdMap);


            } else {
                List<Facet> facetArrayList = new ArrayList<>();

                facetArrayList.add(facet);
                storyIdMap.put(facet.getStoryId(), facetArrayList);
                facetMap.put(facetType, storyIdMap);
            }

        /*
            If our existing FacetMap does NOT have a value associated with the key...
                we are going to create the value instead of pulling it down and updating it
             */
        } else {
            storyIdMap = new HashMap<>();
            ArrayList<Facet> emptyFacetList = new ArrayList<>();
            storyIdMap.put(facet.getStoryId(), emptyFacetList);
            facetMap.put(facetType, storyIdMap);
            updateFacetMap(facetMap, storyIdMap, facetType, facet);
        }
    }

    public void onDestroy() {
        // Stop the cursor loader
        if (articleCursorLoader != null) {
            articleCursorLoader.unregisterListener(this);
            articleCursorLoader.cancelLoad();
            articleCursorLoader.stopLoading();
        }
    }


}
