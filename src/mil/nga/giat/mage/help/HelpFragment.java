package mil.nga.giat.mage.help;

import mil.nga.giat.mage.R;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class HelpFragment extends Fragment {

	private View rootView;

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

	
		rootView = inflater.inflate(R.layout.fragment_help_acknowledgement, container, false);

		return rootView;

	}
	
	
    

	
}
