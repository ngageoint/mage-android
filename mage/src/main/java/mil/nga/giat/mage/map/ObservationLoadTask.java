package mil.nga.giat.mage.map;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.vividsolutions.jts.geom.Point;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.filter.FavoriteFilter;
import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.filter.ImportantFilter;
import mil.nga.giat.mage.map.marker.ObservationBitmapFactory;
import mil.nga.giat.mage.map.marker.PointCollection;
import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;

public class ObservationLoadTask extends AsyncTask<Void, Pair<MarkerOptions, Observation>, Void> {
    
    private Context context;

    private Collection<Filter> filters = new ArrayList<>();

    private PointCollection<Observation> observationCollection;
	private Long currentEventId;

    public ObservationLoadTask(Context context, PointCollection<Observation> observationCollection) {
        this.context = context.getApplicationContext();
        this.observationCollection = observationCollection;
		this.currentEventId = EventHelper.getInstance(context).getCurrentEvent().getId();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean important = preferences.getBoolean(context.getResources().getString(R.string.activeImportantFilterKey), false);
        if (important) {
            filters.add(new ImportantFilter(context));
        }

        boolean favorites = preferences.getBoolean(context.getResources().getString(R.string.activeFavoritesFilterKey), false);
        if (favorites) {
            filters.add(new FavoriteFilter(context));
        }
    }
       
    public void addFilter(Filter<Temporal> filter) {
        if (filter == null) return;

        filters.add(filter);
    }
    
    @Override
    protected Void doInBackground(Void... params ) {
        CloseableIterator<Observation> iterator = null;
        try {
            iterator = iterator();
            while (iterator.hasNext()) {
                Observation o = iterator.current();
                Point point = (Point) o.getGeometry();
                MarkerOptions options = new MarkerOptions().position(new LatLng(point.getY(), point.getX())).icon(ObservationBitmapFactory.bitmapDescriptor(context, o));

                publishProgress(new Pair<>(options, o));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (iterator != null) {
                iterator.closeQuietly();
            }
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Pair<MarkerOptions, Observation>... pairs) {
        observationCollection.add(pairs[0].first, pairs[0].second);
    }

    private CloseableIterator<Observation> iterator() throws SQLException {
        Dao<Observation, Long> dao = DaoStore.getInstance(context).getObservationDao();
        QueryBuilder<Observation, Long> query = dao.queryBuilder();
        Where<Observation, Long> where = query
                .orderBy("timestamp", false)
                .where()
                .ge("last_modified", observationCollection.getLatestDate())
                .and()
                .eq("event_id", currentEventId);

        for (Filter filter : filters) {
            QueryBuilder<?, ?> filterQuery = filter.query();
            if (filterQuery != null) {
                query.join(filterQuery);
            }

            filter.and(where);
        }

        return dao.iterator(query.prepare());
    }
}