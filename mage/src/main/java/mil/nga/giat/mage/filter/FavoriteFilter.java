package mil.nga.giat.mage.filter;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;

import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationFavorite;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;

public class FavoriteFilter implements Filter<Observation> {
	private static final String LOG_NAME = FavoriteFilter.class.getName();

	private Context context;
	private User currentUser;

	public FavoriteFilter(Context context) {
		this.context = context;

		try {
			currentUser= UserHelper.getInstance(context).readCurrentUser();
		} catch (UserException e) {
			Log.e(LOG_NAME, "Error reading current user", e);
		}
	}

	@Override
	public QueryBuilder<ObservationFavorite, Long> query() throws SQLException {
		if (currentUser == null) {
			return null;
		}

		Dao<ObservationFavorite, Long> observationFavoriteDao = DaoStore.getInstance(context).getObservationFavoriteDao();
		QueryBuilder<ObservationFavorite, Long> favoriteQb = observationFavoriteDao.queryBuilder();
		favoriteQb.where()
				.eq("user_id", currentUser.getRemoteId())
				.and()
				.eq("is_favorite", true);

		return favoriteQb;
	}

	@Override
	public void and(Where<? extends Observation, Long> where) throws SQLException {

	}

	@Override
	public boolean passesFilter(Observation observation) {
		ObservationFavorite favorite = observation.getFavoritesMap().get(currentUser.getRemoteId());
		return favorite != null && favorite.isFavorite();
	}
}