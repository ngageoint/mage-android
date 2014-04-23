package mil.nga.giat.mage.map;

import java.sql.SQLException;

import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.map.marker.ObservationCollection;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

public class ObservationLoadTask extends AsyncTask<Void, Observation, Void> {
    
    private Context context;
    private Filter<Observation> filter;
    private ObservationCollection observationCollection;

    public ObservationLoadTask(Context context, ObservationCollection observationCollection) {
        this.context = context.getApplicationContext();
        this.observationCollection = observationCollection;
    }
       
    public void setFilter(Filter<Observation> filter) {
        this.filter = filter;
    }
    
    @Override
    protected Void doInBackground(Void... params ) {
        CloseableIterator<Observation> iterator = null;
        try {
            iterator = iterator();

            int i = 0;
            while (iterator.hasNext()) {
                i++;
                publishProgress(iterator.next());
            }
            Log.i("observation query", "observation query get all observations: " + i);

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
    
    @Override
    protected void onPostExecute(Void result) {
        // TODO Auto-generated method stub
        super.onPostExecute(result);
    }
    
    private CloseableIterator<Observation> iterator() throws SQLException {
        Dao<Observation, Long> dao = DaoStore.getInstance(context).getObservationDao();
        QueryBuilder<Observation, Long> query = dao.queryBuilder();
        Where<Observation, Long> where = query
                .orderBy("last_modified", false)
                .where()
                .ge("last_modified", observationCollection.getLatestObservationDate());   

        if (filter != null) {
            where = filter.where(where.and());            
        }
                
        Log.i("observation query", "observation query get all observations gte: " + observationCollection.getLatestObservationDate());

        return dao.iterator(query.prepare());
    }
}