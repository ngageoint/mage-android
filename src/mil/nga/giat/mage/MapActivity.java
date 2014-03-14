package mil.nga.giat.mage;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import mil.nga.giat.mage.map.FileSystemTileProvider;
import mil.nga.giat.mage.observation.ObservationEditActivity;
import mil.nga.giat.mage.observation.ObservationViewActivity;
import mil.nga.giat.mage.preferences.PublicPreferencesActivity;
import mil.nga.giat.mage.sdk.location.LocationService;
import android.app.ActionBar;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

/**
 * FIXME: Currently a mock of what a landing page might look like.
 * 
 * @author wiedemannse
 * 
 */
public class MapActivity extends FragmentActivity implements ActionBar.TabListener {

	private static final int RESULT_PUBLIC_PREFERENCES = 1;
		
	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;
	
    private GoogleMap map;
    private int mapType = 1;
    private Map<String, TileOverlay> tileOverlays = new HashMap<String, TileOverlay>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_map);
        
        map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		mapType = Integer.parseInt(preferences.getString("baseLayer", "1"));
		map.setMapType(mapType);

        map.setOnMapLongClickListener(new OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng point) {
                // TODO Auto-generated method stub
                MarkerOptions marker = new MarkerOptions().position(
                        new LatLng(point.latitude, point.longitude)).title("New Marker");
                map.addMarker(marker);
            }
        });
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		// Check if any map preferences changed that I care about
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		updateMapType(preferences);
		updateMapOverlays(preferences);
	}
	
	private void updateMapType(SharedPreferences preferences) {
		int mapType = Integer.parseInt(preferences.getString("mapBaseLayer", "1"));
		if (mapType != this.mapType) {
			this.mapType = mapType;
			map.setMapType(this.mapType);
		}
	}
	
	private void updateMapOverlays(SharedPreferences preferences) {
		Set<String> overlays = preferences.getStringSet("mapTileOverlays", Collections.<String>emptySet());
			
		// Add all overlays that are in the preferences
		// For now there is no ordering in how tile overlays are stacked
		
		Set<String> removedOverlays = new HashSet<String>(tileOverlays.keySet());
		for (String overlay : overlays) {
			if (!tileOverlays.keySet().contains(overlay)) {
		        TileProvider tileProvider = new FileSystemTileProvider(256, 256, overlay);
		        TileOverlay tileOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
		        tileOverlays.put(overlay, tileOverlay);
			}
			
	        removedOverlays.remove(overlay);	
		}
		
		// Remove any overlays that are on the map but no longer in the preferences
		for (String overlay : removedOverlays) {
			tileOverlays.remove(overlay).remove();
		}
	}
	
	public void showLayersPopup(View view) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.map_preference);
        dialog.setCancelable(true);
        dialog.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.landing, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent i = new Intent(this, PublicPreferencesActivity.class);
			startActivityForResult(i, RESULT_PUBLIC_PREFERENCES);
			break;
		case R.id.menu_logout:
			// TODO : wipe user certs
			finish();
			break;
		case R.id.observation_new:
			Intent c = new Intent(this, ObservationEditActivity.class);
			startActivityForResult(c, 3);
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case RESULT_PUBLIC_PREFERENCES:
			System.out.println(RESULT_PUBLIC_PREFERENCES);
			break;
		}
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			Fragment fragment = new DummySectionFragment();
			Bundle args = new Bundle();
			args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_map).toUpperCase(l);
			case 1:
				return getString(R.string.title_newsfeed).toUpperCase(l);
			}
			return null;
		}
	}

	/**
	 * A dummy fragment representing a section of the app, but that simply
	 * displays dummy text.
	 */
	public static class DummySectionFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "section_number";

		public DummySectionFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main_dummy, container, false);

			LocationService locationService = new LocationService(container.getContext());
			TextView dummyTextView = (TextView) rootView.findViewById(R.id.section_label);
			dummyTextView.setText("Viewing " + Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)) + " " + locationService.getLocation().getLatitude() + ", " + locationService.getLocation().getLongitude());
			return rootView;
		}
	}
}
