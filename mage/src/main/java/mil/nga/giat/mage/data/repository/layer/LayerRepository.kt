package mil.nga.giat.mage.data.repository.layer

import android.app.Application
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.common.io.ByteStreams
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.data.datasource.feature.FeatureLocalDataSource
import mil.nga.giat.mage.data.datasource.layer.LayerLocalDataSource
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.database.model.geojson.StaticFeature
import mil.nga.giat.mage.database.model.geojson.StaticFeatureProperty
import mil.nga.giat.mage.database.model.layer.Layer
import mil.nga.giat.mage.map.cache.CacheOverlayFilter
import mil.nga.giat.mage.network.geojson.GeometryConverter
import mil.nga.giat.mage.network.layer.LayerService
import mil.nga.giat.mage.sdk.exceptions.StaticFeatureException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import javax.inject.Inject


class LayerRepository @Inject constructor(
   private val application: Application,
   private val layerService: LayerService,
   private val layerLocalDataSource: LayerLocalDataSource,
   private val eventLocalDataSource: EventLocalDataSource,
   private val featureLocalDataSource: FeatureLocalDataSource
) {

   @Throws(IOException::class)
   suspend fun fetchLayers(event: Event, type: String): List<Layer> {
      var layers = emptyList<Layer>()
      val response = layerService.getLayers(event.remoteId, type)
      if (response.isSuccessful) {
         layers = response.body() ?: emptyList()
      } else {
         Log.e(LOG_NAME, "Error fetching layers")
         response.errorBody()?.let { body ->
            Log.e(LOG_NAME, body.string())
         }
      }

      return layers
   }

   suspend fun getStaticFeatureLayers(eventId: Long) = withContext(Dispatchers.IO) {
      val preferences = PreferenceManager.getDefaultSharedPreferences(application)
      val enabledLayers = preferences.getStringSet(application.getString(R.string.tileOverlaysKey), null) ?: emptySet()

      layerLocalDataSource.readByEvent(eventLocalDataSource.read(eventId), "Feature")
         .asSequence()
         .filter { it.isLoaded }
         .filter { enabledLayers.contains(it.name) }
         .toList()
   }

   suspend fun getStaticFeature(layerId: Long, featureId: Long): StaticFeature? = withContext(Dispatchers.IO) {
      featureLocalDataSource.readFeature(layerId, featureId)
   }

   suspend fun getStaticFeatures(layerId: Long): Collection<StaticFeature> = withContext(Dispatchers.IO) {
      layerLocalDataSource.read(layerId).staticFeatures
   }

   @Throws(IOException::class)
   suspend fun fetchFeatureIcon(url: String): InputStream? {
      var inputStream: InputStream? = null

      val response = layerService.getFeatureIcon(url)
      if (response.isSuccessful) {
         inputStream = response.body()?.byteStream()
      } else {
         Log.e(LOG_NAME, "Error fetching feature icon")
         response.errorBody()?.let { body ->
            Log.e(LOG_NAME, body.string())
         }
      }

      return inputStream
   }

   suspend fun fetchImageryLayers() {
      val event = eventLocalDataSource.currentEvent ?: return

      try {
         val response = layerService.getLayers(event.remoteId, "Imagery")
         if (response.isSuccessful) {
            val remoteLayers = response.body() ?: emptyList()

            val localLayers: List<Layer> = layerLocalDataSource.readAll("Imagery")
            val remoteIdToLayer: MutableMap<String, Layer> = HashMap(localLayers.size)
            val it: Iterator<Layer> = localLayers.iterator()
            while (it.hasNext()) {
               val localLayer = it.next()

               // Delete layer not returned from server
               if (!remoteLayers.contains(localLayer)) {
//                  it.remove()
                  layerLocalDataSource.delete(localLayer.id)
               } else {
                  remoteIdToLayer[localLayer.remoteId] = localLayer
               }
            }

            remoteLayers.forEach { remoteLayer ->
               remoteLayer.event = event
               remoteLayer.isLoaded = true
               if (!localLayers.contains(remoteLayer)) {
                  layerLocalDataSource.create(remoteLayer)
               } else {
                  remoteIdToLayer[remoteLayer.remoteId]?.let { localLayer ->
                     layerLocalDataSource.delete(localLayer.id)
                     layerLocalDataSource.create(remoteLayer)
                  }
               }
            }
         } else {
            Log.w(LOG_NAME, "Error fetching imagery layers")
         }
      } catch (e: Exception) {
         Log.w(LOG_NAME, "Error performing imagery layer operations", e)
      }
   }

   suspend fun fetchFeatureLayers(event: Event, deleteLocal: Boolean): List<Layer> {
      val newLayers = mutableListOf<Layer>()

      Log.d(LOG_NAME, "Pulling static layers for event " + event.name)

      try {
         if (deleteLocal) {
            layerLocalDataSource.deleteAll("Feature")
         }

         val response = layerService.getLayers(event.remoteId, "Feature")
         if (response.isSuccessful) {
            val remoteLayers = response.body() ?: emptyList()

            // get local layers
            val localLayers = layerLocalDataSource.readAll("Feature")
            val remoteIdToLayer: MutableMap<String, Layer> = java.util.HashMap(localLayers.size)
            val it = localLayers.iterator()
            while (it.hasNext()) {
               val localLayer = it.next()

               // See if the layer has been deleted on the server
               if (!remoteLayers.contains(localLayer)) {
//                  it.remove()
                  layerLocalDataSource.delete(localLayer.id)
               } else {
                  remoteIdToLayer[localLayer.remoteId] = localLayer
               }
            }

            for (remoteLayer in remoteLayers) {
               remoteLayer.event = event
               if (!localLayers.contains(remoteLayer)) {
                  layerLocalDataSource.create(remoteLayer)
               } else {
                  val localLayer = remoteIdToLayer[remoteLayer.remoteId]
                  if (remoteLayer.event != localLayer!!.event) {
                     layerLocalDataSource.delete(localLayer.id)
                     layerLocalDataSource.create(remoteLayer)
                  }
               }
            }
            newLayers.addAll(layerLocalDataSource.readAll("Feature"))
         } else {
            Log.e(LOG_NAME, "Error fetching static layers")
         }
      } catch (e: java.lang.Exception) {
         Log.e(LOG_NAME, "Problem creating layers.", e)
      }

      return newLayers
   }

   suspend fun loadFeatures(layer: Layer) {

      try {
         if (!layer.isLoaded) {
               layer.downloadId = 1L
               layerLocalDataSource.update(layer)

               Log.i(LOG_NAME, "Loading static features for layer " + layer.name + ".")
               val features = fetchFeatures(layer)

               // Pull down the icons
               val failedIconUrls = mutableListOf<String>()
               features
                  .mapNotNull { feature ->
                     feature.propertiesMap["styleiconstyleiconhref"]?.let { url ->
                        feature to url.key
                     }
                  }
                  .forEach { (feature, url) ->
                     if (url != null) {
                        var iconFile: File? = null
                        try {
                           val iconUrl = URL(url)
                           var filename = iconUrl.file
                           // remove leading /
                           if (filename != null) {
                              filename = filename.trim { it <= ' ' }
                              while (filename!!.startsWith("/")) {
                                 filename = filename.substring(1)
                              }
                           }
                           iconFile = File(application.filesDir.toString() + "/icons/staticfeatures", filename)
                           if (!iconFile.exists()) {
                              iconFile.parentFile?.mkdirs()
                              iconFile.createNewFile()
                              val inputStream = fetchFeatureIcon(url)
                              if (inputStream != null) {
                                 ByteStreams.copy(inputStream, FileOutputStream(iconFile))
                                 feature.localPath = iconFile.absolutePath
                              }
                           } else {
                              feature.localPath = iconFile.absolutePath
                           }
                        } catch (e: java.lang.Exception) {
                           // this block should never flow exceptions up! Log for now.
                           Log.w(LOG_NAME, "Could not get icon.", e)
                           failedIconUrls.add(url)
                           if (iconFile != null && iconFile.exists()) {
                              iconFile.delete()
                           }
                        }
                     }
                  }

               val updatedLayer = featureLocalDataSource.createAll(features, layer)
               try {
                  updatedLayer.isLoaded = true
                  updatedLayer.downloadId = null
                  layerLocalDataSource.update(updatedLayer)
               } catch (e: java.lang.Exception) {
                  throw StaticFeatureException("Unable to update the layer to loaded: " + layer.name)
               }
               Log.i(LOG_NAME, "Loaded static features for layer " + layer.name)
         }
      } catch (e: Exception) {
         Log.e(LOG_NAME, "Problem loading layers.", e)
      }
   }

   private suspend fun fetchFeatures(layer: Layer): List<StaticFeature> {
      val response = layerService.getFeatures(layer.event.remoteId, layer.remoteId)
      return if (response.isSuccessful) {
         response.body()?.features()?.map { json ->
            val staticFeature = StaticFeature()
            staticFeature.remoteId = json.id()
            staticFeature.layer = layer
            staticFeature.geometry = json.geometry()?.let { GeometryConverter.convert(it) }
            staticFeature.properties = json.properties()?.let { fromJsonProperties(it) } ?: emptyList()
            staticFeature
         } ?: emptyList()
      } else {
         Log.e(LOG_NAME, "Error fetching static features")
         if (response.errorBody() != null) {
            response.errorBody()?.let { body ->
               Log.e(LOG_NAME, body.string())
            }
         }

         emptyList()
      }
   }

   private fun fromJsonProperties(json: JsonObject): Collection<StaticFeatureProperty> {
      return parseProperties(json, "")
   }

   private fun parseProperties(
      json: JsonObject,
      prefix: String
   ): Collection<StaticFeatureProperty> {
      return json.asMap().map { (key, value) ->
         val keyWithPrefix = "$prefix${key.lowercase()}"
         if (value.isJsonObject) {
            parseProperties(value.asJsonObject, keyWithPrefix)
         } else {
            if (!value.isJsonNull) {
               listOf(StaticFeatureProperty(keyWithPrefix, value.asString))
            } else emptyList()
         }
      }.flatten()
   }

   companion object {
      private val LOG_NAME = LayerRepository::class.java.simpleName
   }
}