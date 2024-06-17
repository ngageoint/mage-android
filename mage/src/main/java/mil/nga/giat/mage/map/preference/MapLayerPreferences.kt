package mil.nga.giat.mage.map.preference

import android.app.Activity
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.preference.SwitchPreferenceCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import mil.nga.giat.mage.data.repository.event.EventRepository
import mil.nga.giat.mage.database.model.feed.Feed
import mil.nga.giat.mage.map.MapViewModel
import mil.nga.giat.mage.sdk.preferences.getStringSetFlowForKey
import javax.inject.Inject

class MapLayerPreferences @Inject constructor(
   val preferences: SharedPreferences,
) {
   companion object {
      private const val ENABLED_FEED_LAYERS_KEY = "enabled_feed_layers_%s"
   }

   var feedIds: Flow<Set<String>?> = emptyFlow()
   fun getEnabledFeeds(eventId: Long?): Set<String> {
      return eventId?.let {
         val feedIds = preferences.getStringSet(ENABLED_FEED_LAYERS_KEY.format(it), null) ?: emptySet()
         this.feedIds = preferences.getStringSetFlowForKey(ENABLED_FEED_LAYERS_KEY.format(it))
         feedIds
      } ?: emptySet()
   }

   fun setEnabledFeeds(eventId: Long?, feedIds: Set<String>) {
      eventId?.let {
         preferences.edit()
            .putStringSet(ENABLED_FEED_LAYERS_KEY.format(it), feedIds)
            .apply()
      }
   }

   fun mapFeedPreference(feed: Feed, activity: Activity, checked: Boolean): SwitchPreferenceCompat {
      val preference = SwitchPreferenceCompat(activity)

      preference.title = feed.title
      preference.summary = feed.summary
      preference.isChecked = checked

      return preference
   }
}