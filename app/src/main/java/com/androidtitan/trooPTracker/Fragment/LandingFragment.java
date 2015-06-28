package com.androidtitan.trooptracker.Fragment;


import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.androidtitan.alphaarmyapp.R;
import com.androidtitan.trooptracker.Adapter.LandingAdapter;
import com.androidtitan.trooptracker.Adapter.LandingExpandableListAdapter;
import com.androidtitan.trooptracker.Data.DatabaseHelper;
import com.androidtitan.trooptracker.Data.Division;
import com.androidtitan.trooptracker.Interface.LandingInterface;

import java.util.List;


public class LandingFragment extends Fragment {

    DatabaseHelper databaseHelper;

    LandingInterface landingInterface;

    Toolbar toolbar;

    private ImageView edit;
    private ImageView add;

    private ListView simpleListView;
    private LandingAdapter landingAdapter;
    private ExpandableListView expandableListView;
    private LandingExpandableListAdapter expandableAdapter;

    private int selection = -1;
    private boolean expanded = false;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            landingInterface = (LandingInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement SecondF2AInterface");
        }
    }

    public static LandingFragment newInstance(String param1, String param2) {
        LandingFragment fragment = new LandingFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public LandingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        databaseHelper = databaseHelper.getInstance(getActivity());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v= inflater.inflate(R.layout.fragment_landing, container, false);

        toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);

        edit = (ImageView) getActivity().findViewById(R.id.editBtn);
        edit.setVisibility(View.GONE);

        add = (ImageView) getActivity().findViewById(R.id.addBtn);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            //listview when in LANDSCAPE

            simpleListView = (ListView) v.findViewById(R.id.divisionList);
            final List<Division> allDivisions = databaseHelper.getAllDivisions();

            landingAdapter = new LandingAdapter(getActivity(), databaseHelper.getAllDivisions());
            simpleListView.setAdapter(landingAdapter);


            simpleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                /*
                listView.getChildAt(i) works where 0 is the very first visible row and (n-1)
                is the last visible row (where n is the number of visible views you see).
                 */
                    for (int i = 0; i <= simpleListView.getLastVisiblePosition() - simpleListView.getFirstVisiblePosition(); i++) {
                        View item = simpleListView.getChildAt(i);
                        item.setBackgroundColor(0xFFFFFFFF);
                    }
                    if (selection != position) {
                        view.setBackgroundColor(0xCCFFCD38);
                        selection = position;

                    } else {
                        view.setBackgroundColor(0xFFFFFFFF);
                        selection = -1;
                    }
                }
            });


        }
        else {
            //expandable listview when in PORTRAIT
            expandableAdapter = new LandingExpandableListAdapter(getActivity(), databaseHelper.getAllDivisions());

            expandableListView = (ExpandableListView) v.findViewById(R.id.expDivisionList);
            expandableListView.setAdapter(expandableAdapter);
            expandableListView.invalidateViews();


            expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
                int previousItem = -1;

                @Override
                public void onGroupExpand(int groupPosition) {
                    if (groupPosition != previousItem) {
                        expandableListView.collapseGroup(previousItem);
                    }
                    previousItem = groupPosition;
                    expanded = true;
                }
            });

            expandableListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
                @Override
                public void onGroupCollapse(int groupPosition) {
                    expanded = false;
                }
            });


            expandableListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    int previousItem = -1;

                    Animation slideIn = AnimationUtils.loadAnimation(getActivity(), R.anim.edit_icon_slidein);
                    Animation slideOut = AnimationUtils.loadAnimation(getActivity(), R.anim.edit_icon_slideout);
                    Animation addSlideIn = AnimationUtils.loadAnimation(getActivity(), R.anim.add_icon_slidein);
                    Animation addSlideOut = AnimationUtils.loadAnimation(getActivity(), R.anim.add_icon_slideout);

                    final RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams
                            (RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    int afterSlideInValue = (int) (47 * getActivity().getResources().getDisplayMetrics().density);

                    lp.setMargins(afterSlideInValue, 0, 0, 0);

                    Handler handler = new Handler();

                    if (expanded == false) {
                        //highlight item
                        for (int i = 0; i < expandableAdapter.getGroupCount(); i++) {
                            View item = expandableListView.getChildAt(i);
                            item.setBackgroundColor(0xFFFFFFFF);
                        }
                        if (selection == position) {
                            view.setBackgroundColor(0xFFFFFFFF);
                            selection = -1;

                            edit.startAnimation(slideOut);
                            add.startAnimation(addSlideOut);
                            edit.setVisibility(View.GONE);

                        } else {
                            view.setBackgroundColor(0xCCFFCD38);
                            selection = position;

                            //slidein
                            edit.setVisibility(View.VISIBLE);
                            edit.startAnimation(slideIn);
                            add.startAnimation(addSlideIn);

                        }

                    }
                    return true;
                }
            });
        }



        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //this is a division, a new division,  nothing to edit
                landingInterface.divPasser(true, false, -1);
            }
        });

        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selection != -1) {
                    //this is a divison, we are editing, this is what we are editing.
                    landingInterface.divPasser(true, true, selection);
                } else {

                }

            }
        });


        return v;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        landingInterface = null;
    }


}
