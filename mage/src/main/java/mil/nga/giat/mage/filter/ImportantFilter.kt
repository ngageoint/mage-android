package mil.nga.giat.mage.filter

import android.content.Context
import com.j256.ormlite.stmt.QueryBuilder
import com.j256.ormlite.stmt.Where
import mil.nga.giat.mage.sdk.datastore.DaoStore
import mil.nga.giat.mage.sdk.datastore.observation.Observation
import mil.nga.giat.mage.sdk.datastore.observation.ObservationImportant
import java.sql.SQLException

class ImportantFilter(private val context: Context) : Filter<Observation> {

   @Throws(SQLException::class)
   override fun query(): QueryBuilder<ObservationImportant, Long>? {
      val observationImportantDao = DaoStore.getInstance(context).observationImportantDao
      val importantQb = observationImportantDao.queryBuilder()
      importantQb.where().eq("is_important", true)
      return importantQb
   }

   @Throws(SQLException::class)
   override fun and(where: Where<*, Long>) {}

   override fun passesFilter(observation: Observation): Boolean {
      return observation.important?.isImportant == true
   }
}