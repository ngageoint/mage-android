package mil.nga.giat.mage.data.layer

import android.app.Application
import android.util.Log
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mil.nga.giat.mage.R
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureHelper
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import javax.inject.Inject

class LayerRepository @Inject constructor(
   private val application: Application,
) {
   private val layerHelper = LayerHelper.getInstance(application)
   private val eventHelper = EventHelper.getInstance(application)
   private val staticFeatureHelper = StaticFeatureHelper.getInstance(application)

   suspend fun getStaticFeatureLayers(eventId: Long) = withContext(Dispatchers.IO) {
      val preferences = PreferenceManager.getDefaultSharedPreferences(application)
      val enabledLayers = preferences.getStringSet(application.getString(R.string.tileOverlaysKey), null) ?: emptySet()

      try {
         layerHelper.readByEvent(eventHelper.read(eventId), "Feature")
            .asSequence()
            .filter { it.isLoaded }
            .filter { enabledLayers.contains(it.name) }
            .toList()
      } catch (e: Exception) {
         Log.w(LOG_NAME, "Failed to load feature layers", e)
         emptyList()
      }
   }

   suspend fun getStaticFeature(layerId: Long, featureId: Long): StaticFeature = withContext(Dispatchers.IO) {
      staticFeatureHelper.readFeature(layerId, featureId)
   }

   suspend fun getStaticFeatures(layerId: Long): Collection<StaticFeature> = withContext(Dispatchers.IO) {
      layerHelper.read(layerId).staticFeatures
   }

   companion object {
      private val LOG_NAME = LayerRepository::class.java.simpleName
   }
}