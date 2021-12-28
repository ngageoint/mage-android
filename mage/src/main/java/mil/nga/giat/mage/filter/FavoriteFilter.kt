package mil.nga.giat.mage.filter

import android.content.Context
import android.util.Log
import com.j256.ormlite.stmt.QueryBuilder
import com.j256.ormlite.stmt.Where
import mil.nga.giat.mage.filter.FavoriteFilter
import mil.nga.giat.mage.sdk.datastore.DaoStore
import mil.nga.giat.mage.sdk.datastore.observation.Observation
import mil.nga.giat.mage.sdk.datastore.observation.ObservationFavorite
import mil.nga.giat.mage.sdk.datastore.user.User
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.giat.mage.sdk.exceptions.UserException
import java.sql.SQLException

class FavoriteFilter(private val context: Context) : Filter<Observation> {

   private var currentUser: User? = null

   init {
      try {
         currentUser = UserHelper.getInstance(context).readCurrentUser()
      } catch (e: UserException) {
         Log.e(LOG_NAME, "Error reading current user", e)
      }
   }

   @Throws(SQLException::class)
   override fun query(): QueryBuilder<ObservationFavorite, Long>? {
      val user = currentUser ?: return null

      val observationFavoriteDao = DaoStore.getInstance(context).observationFavoriteDao

      val favoriteQb = observationFavoriteDao.queryBuilder()
      favoriteQb.where()
         .eq("user_id", user.remoteId)
         .and()
         .eq("is_favorite", true)

      return favoriteQb
   }

   @Throws(SQLException::class)
   override fun and(where: Where<*, Long>) {}

   override fun passesFilter(observation: Observation): Boolean {
      return observation.favoritesMap[currentUser?.remoteId]?.isFavorite == true
   }

   companion object {
      private val LOG_NAME = FavoriteFilter::class.java.name
   }

}