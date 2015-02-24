package mil.nga.giat.mage.status;

import mil.nga.giat.mage.R;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class StatusFragment extends Fragment {

	private static final String LOG_NAME = StatusFragment.class.getName();
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		getActivity().getActionBar().setTitle("Status");
		return inflater.inflate(R.layout.fragment_status, container, false);
	}

}
