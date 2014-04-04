package mil.nga.giat.mage.newsfeed;

import java.util.Collection;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.event.observation.IObservationEventListener;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class NewsFeedFragment extends Fragment implements IObservationEventListener {
	private NewsFeedObservationAdapter adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_news_feed, container, false);

		ListView lv = (ListView) rootView.findViewById(R.id.news_feed_list);
		adapter = new NewsFeedObservationAdapter(getActivity());
		lv.setAdapter(adapter);
		try {
			ObservationHelper.getInstance(getActivity().getApplicationContext()).addListener(this);
		} catch (ObservationException oe) {
			oe.printStackTrace();
		}

		return rootView;
	}

	@Override
	public void onError(Throwable error) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onObservationCreated(final Collection<Observation> observations) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				adapter.addAll(observations);
			}
		});

	}

	@Override
	public void onObservationDeleted(final Observation observation) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				adapter.remove(observation);
			}
		});
	}

	@Override
	public void onObservationUpdated(final Observation observation) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				adapter.remove(observation);
				adapter.add(observation);
			}
		});

	}
}
