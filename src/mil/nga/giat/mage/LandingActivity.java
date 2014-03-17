	package mil.nga.giat.mage;

import java.util.Locale;

import mil.nga.giat.mage.map.MapFragment;
import mil.nga.giat.mage.map.MapPreferencesActivity;
import mil.nga.giat.mage.newsfeed.NewsFeedFragment;
import mil.nga.giat.mage.observation.ObservationViewActivity;
import mil.nga.giat.mage.preferences.PublicPreferencesActivity;
import mil.nga.giat.mage.sdk.location.LocationService;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

/**
 * FIXME: Currently a mock of what a landing page might look like. Could be
 * replaced entirely if need be. Menu options do exist.
 * 
 * @author wiedemannse
 * 
 */
public class LandingActivity extends FragmentActivity implements ActionBar.TabListener {

	private static final int RESULT_PUBLIC_PREFERENCES = 1;
	private static final int RESULT_MAP_PREFERENCES = 2;


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

	protected LocationService locationService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_landing);

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
			}
		});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(actionBar.newTab().setText(mSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
		}

		// Start the location services!
		if (locationService == null) {
			locationService = new LocationService(getApplicationContext());
		}
		locationService.start();
		

		////////////// FIXME: TESTING //////////////
		
		//ObservationDatabase obsDatabase = new ObservationDatabase(getApplicationContext());
		//obsDatabase.onUpgrade(obsDatabase.getWritableDatabase(),1,1);
		//obsDatabase.onCreate(obsDatabase.getWritableDatabase());
		
		try {
			/*
			DBHelper helper = new DBHelper(getApplicationContext());
			Dao<Observation, ?> observationDao = helper.getObservationDao();
			Dao<State, ?> stateDao = helper.getStateDao();
			Dao<Geometry, ?> geometryDao = helper.getGeometryDao();
			Dao<GeometryType, ?> geometryTypeDao = helper.getGeometryTypeDao();
			Dao<Property, ?> propertyDao = helper.getPropertyDao();
			Dao<Attachment, ?> attachmentDao = helper.getAttachmentDao();
			
			State state = new State("active");
			stateDao.create(state);			
			
			GeometryType geometryType = new GeometryType("POINT");
			geometryTypeDao.create(geometryType);
			
			Geometry geometry = new Geometry("[33.33,44.44]", geometryType);
			geometryDao.create(geometry);
			
			Property prop1 = new Property("HOTDOG_DATE",String.valueOf(new Date().getTime()));
			Property prop2 = new Property("HAMBUR_DATE",String.valueOf(new Date().getTime()));
			Collection<Property> properties = new ArrayList<Property>();
			properties.add(prop1);
			properties.add(prop2);
			
			Attachment attachment1 = new Attachment("png", 12345L, "test.png", "/a/b/c", "d/e/f");
			Attachment attachment2 = new Attachment("jpg", 12345L, "fame.png", "/g/h/i", "j/k/l");
			Collection<Attachment> attachments = new ArrayList<Attachment>();
			attachments.add(attachment1);
			attachments.add(attachment2);
			
			Observation obs = new Observation("123LMNOP",state,geometry,properties,attachments);	
			
			Observation o = observationDao.createIfNotExists(obs);
			prop1.setObservation(o);
			prop2.setObservation(o);
			propertyDao.create(prop1);
			propertyDao.create(prop2);
			
			attachment1.setObservation(o);
			attachment2.setObservation(o);
			attachmentDao.create(attachment1);
			attachmentDao.create(attachment2);
			
			
			
			List<Observation> observations = observationDao.queryForAll();
			for(Observation observation : observations) {
				
				stateDao.refresh(observation.getState());
				geometryDao.refresh(observation.getGeometry());
				geometryTypeDao.refresh(observation.getGeometry().getGeometryType());
				
				System.out.println("Observation: " + observation.getPk_id() + " " + observation.getRemote_id()); 
				System.out.println("     State: " + observation.getState().getState());
				System.out.println("     Geometry: " + observation.getGeometry().getCoordinates());
				System.out.println("          GeometryType: " + observation.getGeometry().getGeometryType().getType());
				System.out.println("     Properties: " + observation.getProperties().toArray().length);				
				for(Property p : observation.getProperties()) {
					System.out.println("         " + p.getKey() + "," + p.getValue());
				}

				System.out.println("     Attachments: " + observation.getAttachments().toArray().length);				
				for(Attachment a : observation.getAttachments()) {
					System.out.println("         " + a.getName() + "," + a.getSize() + "," + a.getContent_type() + "," + 
				                       a.getLocal_path() + "," + a.getRemote_path());
				}								
				
			}
			*/
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		
		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (locationService != null) {
			locationService.stop();			
		}
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
			case R.id.menu_settings: {
				Intent i = new Intent(this, PublicPreferencesActivity.class);
				startActivityForResult(i, RESULT_PUBLIC_PREFERENCES);
				break;
			}
			case R.id.menu_logout: {
				// TODO : wipe user certs
				finish();
				break;
			}
			// TODO all of this is not to go here, just for debugging
			case R.id.observation_view: {
				Intent o = new Intent(this, ObservationViewActivity.class);
				startActivityForResult(o, 2);
				break;
			}
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
		case RESULT_MAP_PREFERENCES:
			System.out.println(RESULT_MAP_PREFERENCES);
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
			Fragment fragment = null;
			switch (position) {
				case 0: {
					fragment = new MapFragment();
					break;
				}
				case 1: {
					fragment = new NewsFeedFragment();
					break;
				}
				default: {
					// TODO not sure what to do here, if anything (fix your code)
				}
			}
			
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
}