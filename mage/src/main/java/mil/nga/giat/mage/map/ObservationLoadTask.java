package mil.nga.giat.mage.map;

import java.sql.SQLException;

import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.map.marker.PointCollection;
import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;

import android.content.Context;
import android.os.AsyncTask;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

public class ObservationLoadTask extends AsyncTask<Void, Observation, Void> {
    
    private Context context;
    private Filter<Temporal> filter;
    private PointCollection<Observation> observationCollection;

    public ObservationLoadTask(Context context, PointCollection<Observation> observationCollection) {
        this.context = context.getApplicationContext();
        this.observationCollection = observationCollection;
    }
       
    public void setFilter(Filter<Temporal> filter) {
        this.filter = filter;
    }
    
    @Override
    protected Void doInBackground(Void... params ) {
        CloseableIterator<Observation> iterator = null;
        try {
            iterator = iterator();
            while (iterator.hasNext()) {
                publishProgress(iterator.next());
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
    protected void onProgressUpdate(Observation... observations) {
        observationCollection.add(observations[0]);
    }
    
    private CloseableIterator<Observation> iterator() throws SQLException {
        Dao<Observation, Long> dao = DaoStore.getInstance(context).getObservationDao();
        QueryBuilder<Observation, Long> query = dao.queryBuilder();
        Where<? extends Temporal, Long> where = query
                .orderBy("timestamp", false)
                .where()
                .ge("last_modified", observationCollection.getLatestDate())
                .and()
                .eq("event_id", EventHelper.getInstance(context).getCurrentEvent().getId());

        if (filter != null) {
            filter.where(where.and());            
        }
                
        return dao.iterator(query.prepare());
    }
}