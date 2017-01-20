package mil.nga.giat.mage.preferences;

import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

/**
 * Clear Data screen
 *
 * @author wiedemanns
 */
public class ClearDataPreferenceActivity extends AppCompatActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setTitle("MAGE Data");

		ClearDataFragment fragment = new ClearDataFragment();
		getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
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


	public static class ClearDataFragment extends ListFragment {

		private MenuItem clearDataButton;

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			setHasOptionsMenu(true);

			return inflater.inflate(R.layout.fragment_cleardata, container, false);
		}

		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);

			ListView listView = getListView();
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			List<String> clearDataEntries = Arrays.asList(getResources().getStringArray(R.array.clearDataEntries));

			ClearDataAdapter clearDataAdapter = new ClearDataAdapter(getActivity(), clearDataEntries);
			setListAdapter(clearDataAdapter);
		}

		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			super.onCreateOptionsMenu(menu, inflater);

			inflater.inflate(R.menu.cleardata_menu, menu);

			clearDataButton = menu.findItem(R.id.clear_data);
			clearDataButton.setEnabled(false);
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
				case R.id.clear_data:
					deleteDataDialog(getListView());
					return true;
				case android.R.id.home:
					getActivity().finish();
					return true;
				default:
					return super.onOptionsItemSelected(item);
			}
		}

		public void onListItemClick(ListView listView, View view, int position, long id) {
			SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();

			if (checkedItems.indexOfKey(position) >=0 && checkedItems.valueAt(checkedItems.indexOfKey(position))) {
				if (position == 0 || position == 2) {
					getListView().setItemChecked(1, true);
				}
			} else if (position == 1) {
				if ((checkedItems.indexOfKey(0) >=0 && checkedItems.valueAt(checkedItems.indexOfKey(0))) || (checkedItems.indexOfKey(2) >=0 && checkedItems.valueAt(checkedItems.indexOfKey(2)))) {
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

		public void deleteDataDialog(final View view) {

			new AlertDialog.Builder(getActivity())
					.setTitle("Delete All Data")
					.setMessage(R.string.clear_data_message)
					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// stop doing stuff
							((MAGE) getActivity().getApplication()).onLogout(false, null);

							SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();

							if (checkedItems.indexOfKey(0) >= 0 && checkedItems.valueAt(checkedItems.indexOfKey(0))) {
								// delete database
								DaoStore.getInstance(view.getContext()).resetDatabase();
							}

							if (checkedItems.indexOfKey(1) >= 0 && checkedItems.valueAt(checkedItems.indexOfKey(1))) {
								// clear preferences
								PreferenceManager.getDefaultSharedPreferences(view.getContext()).edit().clear().commit();
							}

							if (checkedItems.indexOfKey(2) >= 0 && checkedItems.valueAt(checkedItems.indexOfKey(2))) {
								// delete the application contents on the filesystem
								LandingActivity.clearApplicationData(getActivity());
							}

							// go to login activity
							startActivity(new Intent(getActivity(), LoginActivity.class));

							// finish the activity
							getActivity().finish();
						}
					})
					.setNegativeButton(android.R.string.no, null)
					.show();
		}
	}
}
