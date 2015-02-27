package mil.nga.giat.mage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mil.nga.giat.mage.event.EventFragment;
import mil.nga.giat.mage.help.HelpFragment;
import mil.nga.giat.mage.login.AlertBannerFragment;
import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.map.MapFragment;
import mil.nga.giat.mage.navigation.DrawerItem;
import mil.nga.giat.mage.newsfeed.ObservationFeedFragment;
import mil.nga.giat.mage.newsfeed.PeopleFeedFragment;
import mil.nga.giat.mage.preferences.PublicPreferencesFragment;
import mil.nga.giat.mage.profile.MyProfileFragment;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.utils.MediaUtility;
import mil.nga.giat.mage.status.StatusFragment;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * This is the Activity that holds other fragments. Map, feeds, etc. It 
 * starts and stops much of the application. It also contains menus.
 * 
 */
public class LandingActivity extends Activity implements ListView.OnItemClickListener {

	private static final String LOG_NAME = LandingActivity.class.getName();

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerItem currentActivity;
    private int activeTimeFilter = 0;
    private String currentTitle = "";
    private DrawerItem mapItem;
	private int logoutId;
    private boolean switchFragment;
    private DrawerItem itemToSwitchTo;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
		int id = 0;
        mapItem = new DrawerItem.Builder("Map").id(id++).drawableId(R.drawable.ic_globe_white).fragment(new MapFragment()).build();
		DrawerItem logoutItem = new DrawerItem.Builder("Logout").id(id++).secondary(true).build();
		logoutId = logoutItem.getId();

		List<DrawerItem> drawerItems = new ArrayList<DrawerItem>();
		drawerItems.add(mapItem);
		drawerItems.add(new DrawerItem.Builder("Observations").id(id++).drawableId(R.drawable.ic_map_marker_white).fragment(new ObservationFeedFragment()).build());
		drawerItems.add(new DrawerItem.Builder("People").id(id++).drawableId(R.drawable.ic_users_white).fragment(new PeopleFeedFragment()).build());
		if(EventHelper.getInstance(getApplicationContext()).getEventsForCurrentUser().size() > 1) {
			drawerItems.add(new DrawerItem.Builder("Events").id(id++).drawableId(R.drawable.ic_events_white).fragment(new EventFragment()).build());
		}
		drawerItems.add(new DrawerItem.Builder("My Profile").id(id++).drawableId(R.drawable.ic_fa_user).fragment(new MyProfileFragment()).build());
		drawerItems.add(new DrawerItem.Builder("Settings").id(id++).secondary(true).fragment(new PublicPreferencesFragment()).build());
		drawerItems.add(new DrawerItem.Builder("Status").id(id++).secondary(true).fragment(new StatusFragment()).build());
		drawerItems.add(new DrawerItem.Builder("Help").id(id++).secondary(true).fragment(new HelpFragment()).build());
		drawerItems.add(logoutItem);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        drawerList.setAdapter(new ArrayAdapter<DrawerItem>(this, R.layout.drawer_list_item, drawerItems) {
            @Override
            public View getView(int position, View view, ViewGroup parent) {
                DrawerItem item = getItem(position);

                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    if (item.isHeader()) {
                        view = inflater.inflate(R.layout.drawer_list_header_item, null);
                        view.setEnabled(false);
                        view.setOnClickListener(null);
                    } else if (item.isSecondary()) {
                        view = inflater.inflate(R.layout.drawer_list_secondary_item, null);
                    } else {
                        view = inflater.inflate(R.layout.drawer_list_item, null);

                        if (item.getDrawableId() != null) {
                            ImageView iv = (ImageView) view.findViewById(R.id.drawer_item_icon);
                            iv.setImageResource(item.getDrawableId());
                        }
                    }
                }

                TextView text = (TextView) view.findViewById(R.id.text);
                text.setText(item.getText());

                return view;
            }
        });

        // Set the list's click listener
        drawerList.setOnItemClickListener(this);

        actionbarToggleHandler();

		if (savedInstanceState == null) {
			Fragment alertBannerFragment = new AlertBannerFragment();
			getFragmentManager().beginTransaction().add(android.R.id.content, alertBannerFragment).commit();
		}

        goToMap();
    }

    private void goToMap() {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, mapItem.getFragment()).commit();
        getActionBar().setTitle("MAGE");
        currentActivity = mapItem;
    }

    private void actionbarToggleHandler() {
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                getActionBar().setTitle(currentTitle);

                if (drawerView.getId() == R.id.filter_drawer) {
                    setFilter();
                } else if (drawerView.getId() == R.id.left_drawer && switchFragment) {
                    switchFragment = false;

                    // Insert the fragment by replacing any existing fragment
                    FragmentManager fragmentManager = getFragmentManager();
                    fragmentManager.beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).add(R.id.content_frame, itemToSwitchTo.getFragment()).commit();
                    currentActivity = itemToSwitchTo;
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                // hide keyboard
				InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
				if (getCurrentFocus() != null) {
					inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
				}
                currentTitle = (String) getActionBar().getTitle();
                if (drawerView.getId() == R.id.left_drawer) {
                    // anything to do here?
                } else if (drawerView.getId() == R.id.filter_drawer) {
                    getActionBar().setTitle("Filter");
                    RadioGroup rg = (RadioGroup) findViewById(R.id.time_filter_radio_gorup);
                    int checkedFilter = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(getResources().getString(R.string.activeTimeFilterKey), R.id.none_rb);
                    rg.check(checkedFilter);
                }
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                invalidateOptionsMenu();
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);
    }

    public void filterOkClick(View v) {
        // setFilter();
        drawerLayout.closeDrawer(findViewById(R.id.filter_drawer));
    }

    public void setFilter() {
        RadioGroup rg = (RadioGroup) findViewById(R.id.time_filter_radio_gorup);
        if (activeTimeFilter != rg.getCheckedRadioButtonId()) {
            activeTimeFilter = rg.getCheckedRadioButtonId();
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putInt(getResources().getString(R.string.activeTimeFilterKey), rg.getCheckedRadioButtonId()).commit();
        }
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (currentActivity != mapItem) {
                goToMap();
                return true;
            }
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
        case R.id.filter_button:
            DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            View filterDrawer = findViewById(R.id.filter_drawer);
            if (!drawerLayout.isDrawerOpen(filterDrawer)) {
                drawerLayout.openDrawer(filterDrawer);
            } else {
                drawerLayout.closeDrawer(filterDrawer);
            }
            break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = drawerLayout.isDrawerOpen(drawerList);
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setVisible(!drawerOpen);
        }
        
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
        itemToSwitchTo = adapter.getItem(position);
        if (itemToSwitchTo.getFragment() == null) {
            if(itemToSwitchTo.getId() == logoutId) {
                ((MAGE)getApplication()).onLogout(true);
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                finish();
                return;
            } else {
				Log.e(LOG_NAME, "Your fragment was null. Fix the code.");
            }
        }
        if (currentActivity != itemToSwitchTo && itemToSwitchTo.getFragment() != null) {
            switchFragment = true;

            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().remove(currentActivity.getFragment()).commit();
        }

        // Highlight the selected item, update the title, and close the drawer
        drawerList.setItemChecked(position, true);
        drawerLayout.closeDrawer(drawerList);
    }

	public void deleteDataDialog(final View view) {

		final String[] items = { "Database", "Preferences", "Attachments", "App Filesystem" };
		final boolean[] defaultItems = new boolean[items.length];
		Arrays.fill(defaultItems, true);
		// arraylist to keep the selected items
		final Set<Integer> seletedItems = new HashSet<Integer>();
		for (int i = 0; i < items.length; i++) {
			seletedItems.add(i);
		}

		new AlertDialog.Builder(view.getContext()).setTitle("Delete All Data").setMultiChoiceItems(items, defaultItems, new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
				if (isChecked) {
					seletedItems.add(indexSelected);
					if (indexSelected == 0 || indexSelected == 3) {
						((AlertDialog) dialog).getListView().setItemChecked(1, true);
						seletedItems.add(1);
					}
				} else if (seletedItems.contains(indexSelected)) {
					if (indexSelected == 1) {
						if (seletedItems.contains(0) || seletedItems.contains(3)) {
							((AlertDialog) dialog).getListView().setItemChecked(1, true);
							return;
						}
					}
					seletedItems.remove(Integer.valueOf(indexSelected));
				}
			}
		}).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// stop doing stuff
				((MAGE) getApplication()).onLogout(false);

				if (seletedItems.contains(0)) {
					// delete database
					DaoStore.getInstance(view.getContext()).resetDatabase();
				}

				if (seletedItems.contains(1)) {
					// clear preferences
					PreferenceManager.getDefaultSharedPreferences(view.getContext()).edit().clear().commit();
				}

				if (seletedItems.contains(2)) {
					// delete attachments
					deleteDir(MediaUtility.getMediaStageDirectory());
				}

				if (seletedItems.contains(3)) {
					// delete the application contents on the filesystem
					clearApplicationData(getApplicationContext());
				}

				// go to login activity
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                
				// finish the activity
				finish();
			}
		}).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		}).show();
	}
	
	public static void deleteAllData(Context context) {
		DaoStore.getInstance(context).resetDatabase();
		PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
		deleteDir(MediaUtility.getMediaStageDirectory());
		clearApplicationData(context);
	}

	private static void clearApplicationData(Context context) {
		File cache = context.getCacheDir();
		File appDir = new File(cache.getParent());
		if (appDir.exists()) {
			String[] children = appDir.list();
			for (String s : children) {
				if (!s.equals("lib") && !s.equals("databases")) {
					File f = new File(appDir, s);
					Log.d(LOG_NAME, "Deleting " + f.getAbsolutePath());
					deleteDir(f);
				}
			}
		}
	}

	private static boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (String kid : children) {
				boolean success = deleteDir(new File(dir, kid));
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}
}