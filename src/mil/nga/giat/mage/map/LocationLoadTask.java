package mil.nga.giat.mage.map;

import java.sql.SQLException;

import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.map.marker.PointCollection;
import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import android.content.Context;
import android.os.AsyncTask;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

public class LocationLoadTask extends AsyncTask<Void, Location, Void> {
    
    private Context context;
    private Filter<Temporal> filter;
    private PointCollection<Location> locationCollection;
	    
    public LocationLoadTask(Context context, PointCollection<Location> locationCollection) {
        this.context = context.getApplicationContext();
        this.locationCollection = locationCollection;        
    }
       
    public void setFilter(Filter<Temporal> filter) {
        this.filter = filter;
    }
    
    @Override
    protected Void doInBackground(Void... params ) {
        CloseableIterator<Location> iterator = null;
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
    protected void onProgressUpdate(Location... locations) {
        locationCollection.add(locations[0]);
    }
    
	private CloseableIterator<Location> iterator() throws SQLException {
		Dao<Location, Long> dao = DaoStore.getInstance(context).getLocationDao();
		QueryBuilder<Location, Long> query = dao.queryBuilder();
		Where<? extends Temporal, Long> where = query.where().ge("timestamp", locationCollection.getLatestDate());
		User currentUser = null;
		try {
			currentUser = UserHelper.getInstance(context.getApplicationContext()).readCurrentUser();
		} catch (UserException e) {
			e.printStackTrace();
		}
		if (currentUser != null) {
			where.and().ne("user_id", currentUser.getId());
		}
		if (filter != null) {
			where = filter.where(where.and());
		}
		query.orderBy("timestamp", false);

		return dao.iterator(query.prepare());
	}
}