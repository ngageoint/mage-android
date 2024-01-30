package mil.nga.giat.mage.data.datasource.feature

import android.util.Log
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.misc.TransactionManager
import mil.nga.giat.mage.database.dao.MageSqliteOpenHelper
import mil.nga.giat.mage.database.model.geojson.StaticFeature
import mil.nga.giat.mage.database.model.geojson.StaticFeatureProperty
import mil.nga.giat.mage.database.model.layer.Layer
import mil.nga.giat.mage.sdk.event.IEventDispatcher
import mil.nga.giat.mage.sdk.event.IStaticFeatureEventListener
import mil.nga.giat.mage.sdk.exceptions.StaticFeatureException
import java.sql.SQLException
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureLocalDataSource @Inject constructor(
   private val daoStore: MageSqliteOpenHelper,
   private val featureDao: Dao<StaticFeature, Long>,
   private val featurePropertyDao: Dao<StaticFeatureProperty, Long>
): IEventDispatcher<IStaticFeatureEventListener> {
   private val listeners: MutableCollection<IStaticFeatureEventListener> = CopyOnWriteArrayList()

   @Throws(StaticFeatureException::class)
   fun create(pStaticFeature: StaticFeature): StaticFeature {
      val createdStaticFeature: StaticFeature
      try {
         createdStaticFeature = featureDao.createIfNotExists(pStaticFeature)
         val properties = pStaticFeature.properties
         if (properties != null) {
            for (property in properties) {
               property.staticFeature = createdStaticFeature
               featurePropertyDao.create(property)
            }
         }
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem creating the static feature: $pStaticFeature.", e)
         throw StaticFeatureException("There was a problem creating the static feature: $pStaticFeature.", e)
      }
      return createdStaticFeature
   }

   /**
    * Set of layers that features were added to, or already belonged to.
    *
    * @param staticFeatures
    * @return
    * @throws StaticFeatureException
    */
   @Throws(StaticFeatureException::class)
   fun createAll(staticFeatures: Collection<StaticFeature>, pLayer: Layer): Layer {
      try {
         TransactionManager.callInTransaction<Void>(daoStore.connectionSource) {
            for (staticFeature2 in staticFeatures) {
               try {
                  val properties = staticFeature2.properties
                  val newStaticFeature = featureDao.createIfNotExists(staticFeature2)

                  if (properties != null) {
                     for (property in properties) {
                        property.staticFeature = newStaticFeature
                        featurePropertyDao.create(property)
                     }
                  }
               } catch (e: SQLException) {
                  Log.e(LOG_NAME, "There was a problem creating the static feature: $staticFeature2.", e)
                  continue
               }
            }
            null
         }
         pLayer.isLoaded = true
         for (listener in listeners) {
            listener.onStaticFeaturesCreated(pLayer)
         }
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem creating static features.", e)
      }
      return pLayer
   }

   @Throws(StaticFeatureException::class)
   fun read(id: Long): StaticFeature {
      return try {
         featureDao.queryForId(id)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to query for existence for id = '$id'", e)
         throw StaticFeatureException("Unable to query for existence for id = '$id'", e)
      }
   }

   @Throws(StaticFeatureException::class)
   fun read(pRemoteId: String): StaticFeature {
      var staticFeature: StaticFeature? = null
      try {
         val results = featureDao.queryBuilder().where().eq("remote_id", pRemoteId).query()
         if (results != null && results.size > 0) {
            staticFeature = results[0]
         }
      } catch (sqle: SQLException) {
         Log.e(LOG_NAME, "Unable to query for existence for remote_id = '$pRemoteId'", sqle)
         throw StaticFeatureException(
            "Unable to query for existence for remote_id = '$pRemoteId'",
            sqle
         )
      }
      return staticFeature!!
   }

   @Throws(StaticFeatureException::class)
   fun readAll(pLayerId: Long): List<StaticFeature> {
      val staticFeatures: MutableList<StaticFeature> = ArrayList()
      try {
         val results = featureDao.queryBuilder().where().eq("layer_id", pLayerId).query()
         if (results != null) {
            staticFeatures.addAll(results)
         }
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to query for features with layer id = '$pLayerId'", e)
         throw StaticFeatureException("Unable to query for features with layer id = '$pLayerId'", e)
      }
      return staticFeatures
   }

   @Throws(StaticFeatureException::class)
   fun readFeature(layerId: Long, id: Long): StaticFeature? {
      return try {
         val results = featureDao.queryBuilder()
            .where()
            .eq(StaticFeature.STATIC_FEATURE_LAYER_ID, layerId)
            .and()
            .eq(StaticFeature.STATIC_FEATURE_ID, id)
            .query()
         results.firstOrNull()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to query for feature with layer id: $layerId and id: $id", e)
         throw StaticFeatureException("Unable to query for feature with layer id: $layerId and id: $id", e)
      }
   }

   @Throws(StaticFeatureException::class)
   fun deleteAll(layerId: Long) {
      val features = readAll(layerId)
      val ids: MutableCollection<Long?> = ArrayList(features.size)
      for (feature in features) {
         ids.add(feature.id)
      }
      try {
         // Delete the properties (children)
         val propertyDeleteBuilder = featurePropertyDao.deleteBuilder()
         propertyDeleteBuilder.where().`in`(StaticFeatureProperty.STATIC_FEATURE_ID, ids)
         val propertiesDeleted = featurePropertyDao.delete(propertyDeleteBuilder.prepare())
         Log.i(LOG_NAME, "$propertiesDeleted static feature properties deleted")

         // All children deleted, delete the static feature.
         val featureDeleteBuilder = featureDao.deleteBuilder()
         featureDeleteBuilder.where().eq(StaticFeature.STATIC_FEATURE_LAYER_ID, layerId)
         featureDao.delete(featureDeleteBuilder.prepare())
         Log.i(LOG_NAME, "$featureDeleteBuilder features deleted")
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to delete Static Feature: $ids", e)
         throw StaticFeatureException("Unable to delete Static Feature: $ids", e)
      }
   }

   override fun addListener(listener: IStaticFeatureEventListener): Boolean {
      return listeners.add(listener)
   }

   override fun removeListener(listener: IStaticFeatureEventListener): Boolean {
      return listeners.remove(listener)
   }

   companion object {
      private val LOG_NAME = FeatureLocalDataSource::class.java.name
   }
}