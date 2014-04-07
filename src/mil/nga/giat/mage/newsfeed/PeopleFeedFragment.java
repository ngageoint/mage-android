package mil.nga.giat.mage.newsfeed;

import java.sql.SQLException;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;

public class PeopleFeedFragment extends Fragment {
//	private NewsFeedCursorAdapter adapter;
//	private PreparedQuery<Observation> query;
//	private Dao<Observation, Long> oDao;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_feed_people, container, false);

//		ListView lv = (ListView) rootView.findViewById(R.id.news_feed_list);
//		try {
//			oDao = DaoStore.getInstance(getActivity().getApplicationContext()).getObservationDao();
//			query = buildQuery(oDao);
//			Cursor c = obtainCursor(query, oDao);
//			adapter = new NewsFeedCursorAdapter(getActivity().getApplicationContext(), c, query, getActivity());
//			lv.setAdapter(adapter);
//			try {
//				ObservationHelper.getInstance(getActivity().getApplicationContext()).addListener(this);
//			} catch (ObservationException oe) {
//				oe.printStackTrace();
//			}
//			// iterator.closeQuietly();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		return rootView;
	}

	private PreparedQuery<Observation> buildQuery(Dao<Observation, Long> oDao) throws SQLException {
		QueryBuilder<Observation, Long> qb = oDao.queryBuilder();
		qb.where().gt("id", 0);
		qb.orderBy("last_modified", false);

		return qb.prepare();
	}

	private Cursor obtainCursor(PreparedQuery<Observation> query, Dao<Observation, Long> oDao) throws SQLException {
		// build your query
		QueryBuilder<Observation, Long> qb = oDao.queryBuilder();
		qb.where().gt("id", 0);
		// this is wrong.  need to figure out how to order on nested table or move the correct field up
		qb.orderBy("last_modified", false);

		Cursor c = null;
		CloseableIterator<Observation> iterator = oDao.iterator(query);

		// get the raw results which can be cast under Android
		AndroidDatabaseResults results = (AndroidDatabaseResults) iterator.getRawResults();
		c = results.getRawCursor();
		return c;
	}

//	@Override
//	public void onObservationCreated(final Collection<Observation> observations) {
//		 getActivity().runOnUiThread(new Runnable() {
//		 @Override
//		 public void run() {
//			 try {
//			 adapter.changeCursor(obtainCursor(query, oDao));
//			 } catch (Exception e) {
//				 Log.e("NewsFeedFragment", "Unable to change cursor", e);
//			 }
//		 }
//		 });
//
//	}
//
//	@Override
//	public void onObservationDeleted(final Observation observation) {
//		getActivity().runOnUiThread(new Runnable() {
//		@Override
//		 public void run() {
//			 try {
//			 adapter.changeCursor(obtainCursor(query, oDao));
//			 } catch (Exception e) {
//				 Log.e("NewsFeedFragment", "Unable to change cursor", e);
//			 }
//		 }
//		 });
//	}
//
//	@Override
//	public void onObservationUpdated(final Observation observation) {
//		getActivity().runOnUiThread(new Runnable() {
//		@Override
//		 public void run() {
//			 try {
//			 adapter.changeCursor(obtainCursor(query, oDao));
//			 } catch (Exception e) {
//				 Log.e("NewsFeedFragment", "Unable to change cursor", e);
//			 }
//		 }
//		 });
//
//	}
}
