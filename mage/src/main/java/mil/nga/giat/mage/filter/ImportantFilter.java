package mil.nga.giat.mage.filter;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;

import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationImportant;

public class ImportantFilter implements Filter<Observation> {
	private Context context;

	public ImportantFilter(Context context) {
		this.context = context;
	}

	@Override
	public QueryBuilder<ObservationImportant, Long> query() throws SQLException {

		Dao<ObservationImportant, Long> observationImportantDao = DaoStore.getInstance(context).getObservationImportantDao();
		QueryBuilder<ObservationImportant, Long> importantQb = observationImportantDao.queryBuilder();
		importantQb.where().eq("is_important", true);

		return importantQb;
	}

	@Override
	public void and(Where<? extends Observation, Long> where) throws SQLException {
	}

	@Override
	public boolean passesFilter(Observation o) {
		ObservationImportant important = o.getImportant();
		return (important != null && important.isImportant());
	}
}