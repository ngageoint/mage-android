package mil.nga.giat.mage.newsfeed;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.event.observation.IObservationEventListener;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class NewsFeedFragment extends Fragment implements IObservationEventListener {
	private List<Observation> observations = new ArrayList<Observation>();
	private NewsFeedObservationAdapter adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_news_feed, container, false);
		
		ListView lv = (ListView)rootView.findViewById(R.id.news_feed_list);
		adapter = new NewsFeedObservationAdapter(getActivity(), observations);
		lv.setAdapter(adapter);
		try {
			observations.addAll(ObservationHelper.getInstance(getActivity().getApplicationContext()).addListener(this));
		} catch (ObservationException oe) {
			oe.printStackTrace();
		}
//		ObservationHelper oh = ObservationHelper.getInstance(getActivity().getApplicationContext());
//		try {
//			for (long i = 1L; i <= 122L; i++) {
//				Observation o = oh.read(i);
//				if (o != null) {
//					observations.add(o);
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		

		return rootView;
	}

	@Override
	public void onComplete(Observation item) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onError(Throwable error) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onObservationCreated(Observation observation) {
		Log.i("test", "got an observation with id: " + observation.getId());
		observations.add(observation);
		getActivity().runOnUiThread(

		new Runnable() {

			@Override
			public void run() {
				adapter.notifyDataSetChanged();
			}
		});

	}

	@Override
	public void onObservationDeleted(Observation observation) {
		observations.remove(observation);
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onObservationUpdated(Observation observation) {
		// TODO Auto-generated method stub

	}
}
