package mil.nga.giat.mage;

import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.map.MapFragment;
import mil.nga.giat.mage.navigation.DrawerItem;
import mil.nga.giat.mage.newsfeed.NewsFeedCursorAdapter;
import mil.nga.giat.mage.newsfeed.NewsFeedFragment;
import mil.nga.giat.mage.newsfeed.PeopleFeedFragment;
import mil.nga.giat.mage.observation.ObservationEditActivity;
import mil.nga.giat.mage.preferences.PublicPreferencesFragment;
import mil.nga.giat.mage.sdk.location.LocationService;
import mil.nga.giat.mage.sdk.utils.UserUtility;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * FIXME: Currently a mock of what a landing page might look like. Could be
 * replaced entirely if need be. Menu options do exist.
 * 
 * @author wiedemannse
 * 
 */
public class LandingActivity extends FragmentActivity implements ListView.OnItemClickListener {	
	
	private DrawerItem[] drawerItems;
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
        
        MAGE mage = (MAGE) getApplication();
        
        // FIXME : need to consider connectivity before talking to the server!!!
 		mage.startFetching();
 		mage.startPushing();
 		
 		// Start location services
 		mage.initLocationService();

 		drawerItems = new DrawerItem[] {
	        new DrawerItem("Map", R.drawable.ic_globe_white, new MapFragment()),
	        new DrawerItem("Observations", R.drawable.ic_map_marker_white, new NewsFeedFragment()),
	        new DrawerItem("People", R.drawable.ic_users_white, new PeopleFeedFragment()),
	        new DrawerItem("Settings", R.drawable.ic_settings_white, new PublicPreferencesFragment()),
	        new DrawerItem("Logout", R.drawable.ic_settings_white)
 		};

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        drawerList.setAdapter(new ArrayAdapter<DrawerItem>(this, R.layout.drawer_list_item, drawerItems) {
        	@Override
        	public View getView (int position, View view, ViewGroup parent) {
        		if (view == null) {
        			LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        			view = inflater.inflate(R.layout.drawer_list_item, null);
        		}
        		DrawerItem item = getItem(position);
        		TextView text = (TextView)view.findViewById(R.id.drawer_item_text);
        		text.setText(item.getItemText());
        		ImageView iv = (ImageView)view.findViewById(R.id.drawer_item_icon);
        		iv.setImageResource(item.getDrawableId());
        		
        		return view;
        	}
        });
        
        // Set the list's click listener
        drawerList.setOnItemClickListener(this);

        actionbarToggleHandler();
        
        // initialize with map
        MapFragment mf = new MapFragment();
        FragmentManager fragmentManager = getFragmentManager();
	    fragmentManager.beginTransaction()
	                   .replace(R.id.content_frame, mf)
	                   .commit();
    }
    
    private void actionbarToggleHandler() {  
        getActionBar().setHomeButtonEnabled(true);  
        getActionBar().setDisplayHomeAsUpEnabled(true);  
//        getActionBar().setHomeAsUpIndicator(R.drawable.ic_drawer);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,  
                  R.drawable.ic_drawer, R.string.drawer_open,  
                  R.string.drawer_close) {  
             @Override  
             public void onDrawerClosed(View drawerView) {
            	 super.onDrawerClosed(drawerView);
            	 invalidateOptionsMenu();
             }  
             @Override  
             public void onDrawerOpened(View drawerView) { 
            	 super.onDrawerOpened(drawerView);
                  invalidateOptionsMenu();
             }  
        };
        drawerLayout.setDrawerListener(drawerToggle);  
   }
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
    	drawerToggle.syncState();
    	super.onPostCreate(savedInstanceState);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		MAGE mage = (MAGE) getApplication();
		mage.destroyFetching();
		mage.destroyPushing();
		mage.destroyLocationService();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.landing, menu);
		return true;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ( keyCode == KeyEvent.KEYCODE_MENU ) {
	    	boolean drawerOpen = drawerLayout.isDrawerOpen(drawerList);
	    	if (!drawerOpen) {
	    		drawerLayout.openDrawer(drawerList);
	    	} else {
	    		drawerLayout.closeDrawer(drawerList);
	    	}
	        //Put the code for an action menu from the top here
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (drawerToggle.onOptionsItemSelected(item)) {
			// drawer handled the event
			return true;
		}
		switch (item.getItemId()) {
    		case R.id.observation_new:
    			 Intent intent = new Intent(this, ObservationEditActivity.class);
    			 LocationService ls = ((MAGE) getApplication()).getLocationService();
    			 Location l = ls.getLocation();
    			 intent.putExtra(ObservationEditActivity.LOCATION, l);
    	       	 startActivity(intent);
		}

		return super.onOptionsItemSelected(item);
	}
	
	 @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = drawerLayout.isDrawerOpen(drawerList);
        menu.findItem(R.id.observation_new).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

	/**
	 * Takes you to the home screen
	 */
	@Override
	public void onBackPressed() {
		Intent startMain = new Intent(Intent.ACTION_MAIN);
		startMain.addCategory(Intent.CATEGORY_HOME);
		startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(startMain);
	}

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
    	ArrayAdapter<DrawerItem> adapter = (ArrayAdapter<DrawerItem>) adapterView.getAdapter();
    	DrawerItem item = adapter.getItem(position);
        Fragment fragment = item.getFragment();
        String title = item.getItemText();
        if (fragment == null) {
	        switch (position) {
	            case 4: {
	                // TODO : wipe user certs, really just wipe out the token from shared preferences
	                UserUtility.getInstance(getApplicationContext()).clearTokenInformation();
	                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
	                finish();
	                return;
	            }
	            default: {
	                // TODO not sure what to do here, if anything (fix your code)
	            }
	        }
        }
        
        Bundle args = new Bundle();
        args.putInt("POSITION", position);
        fragment.setArguments(args);

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                       .replace(R.id.content_frame, fragment)
                       .commit();

        // Highlight the selected item, update the title, and close the drawer
        drawerList.setItemChecked(position, true);
        getActionBar().setTitle(title);
        drawerLayout.closeDrawer(drawerList);        
    }
}