package mil.nga.giat.mage.data.datasource.layer

import android.util.Log
import com.j256.ormlite.dao.Dao
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.data.datasource.feature.FeatureLocalDataSource
import mil.nga.giat.mage.database.model.layer.Layer
import mil.nga.giat.mage.sdk.exceptions.LayerException
import java.sql.SQLException
import javax.inject.Inject

class LayerLocalDataSource @Inject constructor(
   private val layerDao: Dao<Layer, Long>,
   private val featureLocalDataSource: FeatureLocalDataSource
)  {

   @Throws(LayerException::class)
   fun readAll(type: String): List<Layer> {
      return try {
         layerDao.queryBuilder().where().eq("type", type).query()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to read Layers", e)
         throw LayerException("Unable to read Layers.", e)
      }
   }

   fun readByEvent(event: Event?, type: String? = null): List<Layer> {
      event ?: return emptyList()

      return try {
         val where = layerDao.queryBuilder().where().eq("event_id", event.id)
         type?.let { where.and().eq("type", it) }
         where.query()
      } catch (e: Exception) {
         Log.e(LOG_NAME, "Unable to read Layers", e)
         emptyList()
      }
   }

   @Throws(LayerException::class)
   fun read(id: Long): Layer {
      return try {
         layerDao.queryForId(id)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to query for existence for id = '$id'", e)
         throw LayerException("Unable to query for existence for id = '$id'", e)
      }
   }

   @Throws(LayerException::class)
   fun read(pRemoteId: String): Layer? {
      return try {
         layerDao.queryBuilder()
            .where()
            .eq("remote_id", pRemoteId)
            .query()
            .firstOrNull()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to query for existence for remote_id = '$pRemoteId'", e)
         throw LayerException("Unable to query for existence for remote_id = '$pRemoteId'", e)
      }
   }

   @Throws(LayerException::class)
   fun create(pLayer: Layer): Layer {
      return try {
         layerDao.createIfNotExists(pLayer)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem creating the layer: $pLayer.", e)
         throw LayerException("There was a problem creating the layer: $pLayer.", e)
      }
   }

   @Throws(LayerException::class)
   fun update(layer: Layer): Layer {
      return try {
         layerDao.update(layer)
         layer
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "There was a problem updating layer: $layer")
         throw LayerException("There was a problem updating layer: $layer", e)
      }
   }

   @Throws(LayerException::class)
   fun getByRelativePath(relativePath: String): Layer? {
      return try {
        layerDao.queryBuilder()
           .where()
           .eq("relative_path", relativePath)
           .query()
           .firstOrNull()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to query for existence for relativePath = '$relativePath'", e)
         throw LayerException("Unable to query for existence for relativePath = '$relativePath'", e)
      }
   }

   @Throws(LayerException::class)
   fun getByDownloadId(downloadId: Long): Layer? {
      return try {
         layerDao.queryBuilder()
            .where()
            .eq("download_id", downloadId)
            .query()
            .firstOrNull()
      } catch (e: SQLException) {
         throw LayerException("Unable to query Layer by download id = '$downloadId'", e)
      }
   }

   @Throws(LayerException::class)
   fun delete(id: Long) {
      try {
         layerDao.queryForId(id)?.let { layer ->
            featureLocalDataSource.deleteAll(layer.id)
            layerDao.deleteById(id)
         }
      } catch (e: Exception) {
         Log.e(LOG_NAME, "Unable to delete layer: $id", e)
         throw LayerException("Unable to delete layer: $id", e)
      }
   }

   @Throws(LayerException::class)
   fun deleteAll(type: String) {
      try {
         layerDao.queryForAll()
            .filter { it.type == type}
            .forEach { delete(it.id) }
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Error deleting layers")
      }
   }

   companion object {
      private val LOG_NAME = LayerLocalDataSource::class.java.name
   }
}