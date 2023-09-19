package mil.nga.giat.mage.map.preference

import android.app.Activity
import android.content.SharedPreferences
import androidx.preference.SwitchPreferenceCompat
import mil.nga.giat.mage.database.model.feed.Feed
import javax.inject.Inject

class MapLayerPreferences @Inject constructor(
   val preferences: SharedPreferences
) {
   companion object {
      private const val ENABLED_FEED_LAYERS_KEY = "enabled_feed_layers_%s"
   }

   fun getEnabledFeeds(eventId: Long?): Set<String> {
      return eventId?.let {
         preferences.getStringSet(ENABLED_FEED_LAYERS_KEY.format(it), null) ?: emptySet()
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