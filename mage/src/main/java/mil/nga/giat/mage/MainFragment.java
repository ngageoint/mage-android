package mil.nga.giat.mage;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.map.MapFragment;
import mil.nga.giat.mage.newsfeed.ObservationFeedFragment;
import mil.nga.giat.mage.newsfeed.PeopleFeedFragment;

/**
 * Created by wnewman on 1/24/17.
 */

public class MainFragment extends Fragment {

    public enum NavigationItem {
        MAP(0),
        OBSERVATIONS(1),
        PEOPLE(2);

        private int index;

        NavigationItem(int index) {
            this.index = index;
        }
    }

    public interface OnNavigationItemSelectedListener {
        void onNavigationItemSelected(NavigationItem navigationItem);
    }

    public static final String SELECTED_NAVIGATION_ITEM = "SELECTED_NAVIGATION_ITEM";

    private List<Fragment> navigationFragments = new ArrayList<>();
    private int selectedNavigationItem;
    private BottomNavigationView navigationView;
    private NavigationItem navigationItem;
    private OnNavigationItemSelectedListener onNavigationItemSelectedListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        navigationFragments.add(new MapFragment());
        navigationFragments.add(new ObservationFeedFragment());
        navigationFragments.add(new PeopleFeedFragment());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        navigationView = (BottomNavigationView) view.findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.map_tab:
                        navigationItem = NavigationItem.MAP;
                        break;
                    case R.id.observations_tab:
                        navigationItem = NavigationItem.OBSERVATIONS;
                        break;
                    case R.id.people_tab:
                        navigationItem = NavigationItem.PEOPLE;
                        break;
                }
                onNavigationItemSelectedListener.onNavigationItemSelected(navigationItem);

                switchFragment(item);
                return true;
            }
        });

        if (navigationItem != null) {
            selectNavigationItem(navigationItem);
        } else {
            MenuItem selectedItem;
            if (savedInstanceState != null) {
                selectedNavigationItem = savedInstanceState.getInt(SELECTED_NAVIGATION_ITEM, R.id.map_tab);
                selectedItem = navigationView.getMenu().findItem(selectedNavigationItem);
            } else {
                selectedItem = navigationView.getMenu().getItem(NavigationItem.MAP.index);
            }

            switchFragment(selectedItem);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        MenuItem selectedItem = navigationView.getMenu().findItem(selectedNavigationItem);
        if (selectedItem != null) {
            switchFragment(selectedItem);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELECTED_NAVIGATION_ITEM, selectedNavigationItem);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            selectedNavigationItem = savedInstanceState.getInt(SELECTED_NAVIGATION_ITEM);
            MenuItem selectedItem = navigationView.getMenu().findItem(selectedNavigationItem);
            switchFragment(selectedItem);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            onNavigationItemSelectedListener = (OnNavigationItemSelectedListener) context;
        } catch (final ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnNavigationItemSelectedListener");
        }
    }

    public void setNavigationItem(NavigationItem navigationItem) {
        this.navigationItem = navigationItem;

        if (isAdded()) {
            selectNavigationItem(navigationItem);
        }
    }

    private void selectNavigationItem(NavigationItem item) {
        switch (item) {
            case MAP:
                navigationView.findViewById(R.id.map_tab).performClick();
                break;
            case OBSERVATIONS:
                navigationView.findViewById(R.id.observations_tab).performClick();
                break;
            case PEOPLE:
                navigationView.findViewById(R.id.people_tab).performClick();
                break;
        }

        selectedNavigationItem = item.index;
    }

    private void switchFragment(MenuItem item) {
        Fragment fragment = null;
        switch (item.getItemId()) {
            case R.id.map_tab:
                fragment = navigationFragments.get(NavigationItem.MAP.index);
                break;
            case R.id.observations_tab:
                fragment = navigationFragments.get(NavigationItem.OBSERVATIONS.index);
                break;
            case R.id.people_tab:
                fragment = navigationFragments.get(NavigationItem.PEOPLE.index);
                break;
        }

        item.setChecked(true);

        // update selected item
        selectedNavigationItem = item.getItemId();

        if (fragment != null) {
            FragmentManager fragmentManager = getChildFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.navigation_content, fragment).commit();
        }
    }
}
