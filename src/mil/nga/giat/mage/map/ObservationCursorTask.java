package mil.nga.giat.mage.map;

import java.sql.SQLException;

import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.map.marker.ObservationCollection;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;

public class ObservationCursorTask extends AsyncTask<Observation, Observation, Void> {
    
    private Context context;
    private Filter<Observation> filter;
    private ObservationCollection observationCollection;

    public ObservationCursorTask(Context context, ObservationCollection observationCollection) {
        this.context = context.getApplicationContext();
        this.observationCollection = observationCollection;
    }
       
    public void setFilter(Filter<Observation> filter) {
        this.filter = filter;
    }
    
    @Override
    protected Void doInBackground(Observation... observations) {
        // Create the cursor
        
        try {
            Dao<Observation, Long> dao = DaoStore.getInstance(context).getObservationDao();
            PreparedQuery<Observation> query = prepareQuery(dao);
            Cursor cursor = obtainCursor(dao, query);
            Log.i("observation query", "observation query got: " + cursor.getCount() + " observations");

            while (cursor.moveToNext()) {
                Observation o = query.mapRow(new AndroidDatabaseResults(cursor, null));
                publishProgress(o);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Observation... observations) {
        observationCollection.add(observations[0]);
    }
    
    @Override
    protected void onPostExecute(Void result) {
        // TODO Auto-generated method stub
        super.onPostExecute(result);
    }
    
    private PreparedQuery<Observation> prepareQuery(Dao<Observation, Long> dao) throws SQLException {
        QueryBuilder<Observation, Long> query = dao.queryBuilder();

        if (filter != null) {
            //TODO filter the results
        }
                
        Log.i("observation query", "observation query get all observations gte: " + observationCollection.getLatestObservationDate());
        query.where().ge("last_modified", observationCollection.getLatestObservationDate());
        query.orderBy("last_modified", false);

        return query.prepare();
    }
    
    private Cursor obtainCursor(Dao<Observation, Long> dao, PreparedQuery<Observation> preparedQuery) throws SQLException {
        CloseableIterator<Observation> iterator = dao.iterator(preparedQuery);

        // get the raw results which can be cast under Android
        AndroidDatabaseResults results = (AndroidDatabaseResults) iterator.getRawResults();
        return results.getRawCursor();
    }
}