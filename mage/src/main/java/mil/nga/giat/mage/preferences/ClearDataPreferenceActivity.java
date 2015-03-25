package mil.nga.giat.mage.preferences;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import java.util.Arrays;
import java.util.List;

import mil.nga.giat.mage.LandingActivity;
import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

/**
 * Clear Data screen
 *
 * @author wiedemanns
 */
public class ClearDataPreferenceActivity extends ListActivity {

	private MenuItem clearDataButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_cleardata);

		ListView listView = getListView();
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();


				if (checkedItems.indexOfKey(position) >=0 && checkedItems.valueAt(checkedItems.indexOfKey(position))) {
					if (position == 0 || position == 3) {
						getListView().setItemChecked(1, true);
					}
				} else if (position == 1) {
					if ((checkedItems.indexOfKey(0) >=0 && checkedItems.valueAt(checkedItems.indexOfKey(0))) || (checkedItems.indexOfKey(3) >=0 && checkedItems.valueAt(checkedItems.indexOfKey(3)))) {
						getListView().setItemChecked(position, true);
					}
				}

				for(int i = 0, size = getListView().getCheckedItemPositions().size(); i < size; i++) {
					Boolean b = getListView().getCheckedItemPositions().valueAt(i);
					if(b) {
						clearDataButton.setEnabled(true);
						return;
					}
				}
				clearDataButton.setEnabled(false);
			}
		});

		List<String> clearDataEntries = Arrays.asList(getResources().getStringArray(R.array.clearDataEntries));

		ClearDataAdapter clearDataAdapter = new ClearDataAdapter(ClearDataPreferenceActivity.this, clearDataEntries);
		setListAdapter(clearDataAdapter);
	}

	public static class ClearDataAdapter extends ArrayAdapter<String> {

		List<String> values;

		public ClearDataAdapter(Context context, List<String> values) {
			super(context, R.layout.cleardata_list_item, R.id.checkedTextView, values);
			this.values = values;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);

			String name = values.get(position);
			CheckedTextView checkedView = (CheckedTextView) view.findViewById(R.id.checkedTextView);
			checkedView.setText(name);

			return view;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		clearDataButton = menu.findItem(R.id.clear_data);
		clearDataButton.setEnabled(false);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.cleardata_menu, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.clear_data:
				deleteDataDialog(getListView());
				return true;
			case android.R.id.home:
				onBackPressed();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void deleteDataDialog(final View view) {

		new AlertDialog.Builder(view.getContext()).setTitle("Delete All Data").setMessage(R.string.clear_data_message).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// stop doing stuff
				((MAGE) getApplication()).onLogout(false);

				SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();

				if (checkedItems.indexOfKey(0) >=0 && checkedItems.valueAt(checkedItems.indexOfKey(0))) {
					// delete database
					DaoStore.getInstance(view.getContext()).resetDatabase();
				}

				if (checkedItems.indexOfKey(1) >=0 && checkedItems.valueAt(checkedItems.indexOfKey(1))) {
					// clear preferences
					PreferenceManager.getDefaultSharedPreferences(view.getContext()).edit().clear().commit();
				}

				if (checkedItems.indexOfKey(2) >=0 && checkedItems.valueAt(checkedItems.indexOfKey(2))) {
					// delete attachments
					LandingActivity.deleteDir(MediaUtility.getMediaStageDirectory());
				}

				if (checkedItems.indexOfKey(3) >=0 && checkedItems.valueAt(checkedItems.indexOfKey(3))) {
					// delete the application contents on the filesystem
					LandingActivity.clearApplicationData(getApplicationContext());
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

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}
}
