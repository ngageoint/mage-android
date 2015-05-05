package mil.nga.giat.mage.help;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import mil.nga.giat.mage.R;

public class HelpFragment extends Fragment {

	private View rootView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_help_about, container, false);
		String title = "About";
		getActivity().getActionBar().setTitle(title);
		setHasOptionsMenu(Boolean.TRUE);
		return rootView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.help, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Context context = getActivity();
		ViewGroup container = (ViewGroup) rootView;
		String title = "About";

		switch (item.getItemId()) {
		default:
		case R.id.about_button:
			container.removeAllViews();
			View.inflate(context, R.layout.fragment_help_about, container);
			break;

		case R.id.acknowledgements_button:
			container.removeAllViews();
			View.inflate(context, R.layout.fragment_help_acknowledgement, container);
			title = "Open Source Licenses";
			break;
		case R.id.disclaimer_button:
			container.removeAllViews();
			View.inflate(context, R.layout.fragment_help_disclaimer, container);
			title = "Disclaimer";
			break;
		}
		getActivity().getActionBar().setTitle(title);

		return super.onOptionsItemSelected(item);
	}

}
