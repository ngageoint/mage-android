package mil.nga.giat.mage.newsfeed;

import java.util.Collection;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.event.observation.IObservationEventListener;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;

public class NewsFeedFragment extends Fragment implements IObservationEventListener {
	private NewsFeedCursorAdapter adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_news_feed, container, false);

		ListView lv = (ListView) rootView.findViewById(R.id.news_feed_list);
		try {
		Dao<Observation, Long> oDao = DaoStore.getInstance(getActivity().getApplicationContext()).getObservationDao();
		
		// build your query
		QueryBuilder<Observation, Long> qb = oDao.queryBuilder();
		qb.where().gt("id", 0);
		qb.orderBy("last_modified", false);
		
		Cursor c = null;
		// when you are done, prepare your query and build an iterator
		PreparedQuery<Observation> query = qb.prepare();
		CloseableIterator<Observation> iterator = oDao.iterator(query);
		
		   // get the raw results which can be cast under Android
		   AndroidDatabaseResults results =
		       (AndroidDatabaseResults)iterator.getRawResults();
		   	c = results.getRawCursor();
		   	adapter = new NewsFeedCursorAdapter(getActivity().getApplicationContext(), c, query, getActivity());
			lv.setAdapter(adapter);
			try {
				ObservationHelper.getInstance(getActivity().getApplicationContext()).addListener(this);
			} catch (ObservationException oe) {
				oe.printStackTrace();
			}
			//iterator.closeQuietly();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rootView;
	}

	@Override
	public void onError(Throwable error) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onObservationCreated(final Collection<Observation> observations) {
//		getActivity().runOnUiThread(new Runnable() {
//			@Override
//			public void run() {
//				adapter.addAll(observations);
//			}
//		});

	}

	@Override
	public void onObservationDeleted(final Observation observation) {
//		getActivity().runOnUiThread(new Runnable() {
//			@Override
//			public void run() {
//				adapter.remove(observation);
//			}
//		});
	}

	@Override
	public void onObservationUpdated(final Observation observation) {
//		getActivity().runOnUiThread(new Runnable() {
//			@Override
//			public void run() {
//				adapter.remove(observation);
//				adapter.add(observation);
//			}
//		});

	}
}
