package mil.nga.giat.mage.newsfeed;

import java.sql.SQLException;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import android.app.Fragment;
import android.database.Cursor;
import android.os.Bundle;
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
	private PeopleCursorAdapter adapter;
	private PreparedQuery<Location> query;
	private Dao<Location, Long> lDao;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_feed_people, container, false);
		

		ListView lv = (ListView) rootView.findViewById(R.id.people_feed_list);
		try {
			lDao = DaoStore.getInstance(getActivity().getApplicationContext()).getLocationDao();
			query = buildQuery(lDao);
			Cursor c = obtainCursor(query, lDao);
			adapter = new PeopleCursorAdapter(getActivity().getApplicationContext(), c, query);
			lv.setAdapter(adapter);
//			adapter = new NewsFeedCursorAdapter(getActivity().getApplicationContext(), c, query, getActivity());
//			lv.setAdapter(adapter);
//			try {
//				ObservationHelper.getInstance(getActivity().getApplicationContext()).addListener(this);
//			} catch (ObservationException oe) {
//				oe.printStackTrace();
//			}
//			// iterator.closeQuietly();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rootView;
	}

	private PreparedQuery<Location> buildQuery(Dao<Location, Long> oDao) throws SQLException {
		QueryBuilder<Location, Long> qb = lDao.queryBuilder();
		qb.where().gt("_id", 0);
		qb.where().isNotNull("remote_id");
		qb.orderBy("lastModified", false);

		return qb.prepare();
	}

	private Cursor obtainCursor(PreparedQuery<Location> query, Dao<Location, Long> oDao) throws SQLException {

		Cursor c = null;
		CloseableIterator<Location> iterator = lDao.iterator(query);

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
