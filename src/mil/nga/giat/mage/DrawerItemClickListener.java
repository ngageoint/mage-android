package mil.nga.giat.mage;

import mil.nga.giat.mage.map.MapFragment;
import mil.nga.giat.mage.newsfeed.NewsFeedFragment;
import mil.nga.giat.mage.newsfeed.PeopleFeedFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class DrawerItemClickListener implements ListView.OnItemClickListener {
	
	private FragmentActivity a;
	private ListView l;
	private DrawerLayout drawer;
	
	public DrawerItemClickListener(FragmentActivity a, ListView l, DrawerLayout drawer) {
		this.a = a;
		this.l = l;
		this.drawer = drawer;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		
		
		Fragment fragment = null;
		String title = null;
		switch (position) {
			case 0: {
				fragment = new MapFragment();
				title = "MAGE";
				break;
			}
			case 1: {
				fragment = new NewsFeedFragment();
				title = "Observations";
				break;
			}
			case 2: {
				fragment = new PeopleFeedFragment();
				title = "People";
				break;
			}
			default: {
				// TODO not sure what to do here, if anything (fix your code)
			}
		}
		
	    Bundle args = new Bundle();
	    args.putInt("POSITION", position);
	    fragment.setArguments(args);

	    // Insert the fragment by replacing any existing fragment
	    FragmentManager fragmentManager = a.getSupportFragmentManager();
	    fragmentManager.beginTransaction()
	                   .replace(R.id.content_frame, fragment)
	                   .commit();

	    // Highlight the selected item, update the title, and close the drawer
	    l.setItemChecked(position, true);
	    a.getActionBar().setTitle(title);
	    drawer.closeDrawer(l);
	}

}
