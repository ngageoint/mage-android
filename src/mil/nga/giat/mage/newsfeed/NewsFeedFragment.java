package mil.nga.giat.mage.newsfeed;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class NewsFeedFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_news_feed, container, false);
		
		ListView lv = (ListView)rootView.findViewById(R.id.news_feed_list);
		
		List<Observation> observations = new ArrayList<Observation>();
		ObservationHelper oh = ObservationHelper.getInstance(getActivity().getApplicationContext());
		try {
			for (long i = 1L; i <= 122L; i++) {
				Observation o = oh.readObservation(i);
				if (o != null) {
					observations.add(o);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		lv.setAdapter(new NewsFeedObservationAdapter(getActivity(), observations));

		return rootView;
	}
}
