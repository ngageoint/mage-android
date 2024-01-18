package mil.nga.giat.mage.map.preference

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.datasource.layer.LayerLocalDataSource
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.database.model.feed.Feed
import mil.nga.giat.mage.database.model.layer.Layer
import javax.inject.Inject

@AndroidEntryPoint
class MapPreferencesActivity : AppCompatActivity() {
   private val preference = MapPreferenceFragment()

   @AndroidEntryPoint
   class MapPreferenceFragment : PreferenceFragmentCompat() {

		@Inject lateinit var preferences: SharedPreferences
		@Inject lateinit var mapLayerPreferences: MapLayerPreferences
      @Inject lateinit var layerLocalDataSource: LayerLocalDataSource
		@Inject lateinit var eventLocalDataSource: EventLocalDataSource

      private var event: Event? = null
      private lateinit var viewModel: MapPreferencesViewModel

      override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         viewModel = ViewModelProvider(this).get(MapPreferencesViewModel::class.java)
         viewModel.feeds.observe(this) { feeds: List<Feed> -> onFeeds(feeds) }
      }

      override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
         addPreferencesFromResource(R.xml.mappreferences)
      }

      override fun onResume() {
         super.onResume()
         event = eventLocalDataSource.currentEvent
         viewModel.setEvent(event?.remoteId)

         findPreference<Preference>(getString(R.string.tileOverlaysKey))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
               val intent = Intent(activity, TileOverlayPreferenceActivity::class.java)
               requireActivity().startActivityForResult(intent, TILE_OVERLAY_ACTIVITY)
               true
            }

         findPreference<Preference>(getString(R.string.onlineLayersKey))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
               val intent = Intent(activity, OnlineLayersPreferenceActivity::class.java)
               requireActivity().startActivityForResult(intent, ONLINE_LAYERS_OVERLAY_ACTIVITY)
               true
            }
         val showMGRS = findPreference<SwitchPreferenceCompat>(getString(R.string.showMGRSKey))
         val showGARS = findPreference<SwitchPreferenceCompat>(getString(R.string.showGARSKey))
         showMGRS?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
               if (newValue as Boolean) {
                  showGARS?.isChecked = false
               }
               true
            }
         showGARS?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any ->
               if (newValue as Boolean) {
                  showMGRS?.isChecked = false
               }
               true
            }

         // TODO : Remove the below and rework OverlayPreference to have a 'entities' similar to a list preference, these would be the 'display values'
         val availableDownLoads = mutableListOf<Layer>().apply {
            addAll(layerLocalDataSource.readByEvent(event, "GeoPackage"))
            addAll(layerLocalDataSource.readByEvent(event, "Feature"))
            addAll(layerLocalDataSource.readByEvent(event, "Imagery"))
         }.any { !it.isLoaded }

         val overlayPreference = findPreference<Preference>(resources.getString(R.string.tileOverlaysKey)) as OverlayPreference?
         overlayPreference?.setAvailableDownloads(availableDownLoads)
      }

      override fun onPause() {
         super.onPause()
         findPreference<Preference>(getString(R.string.tileOverlaysKey))?.onPreferenceClickListener = null
         findPreference<Preference>(getString(R.string.onlineLayersKey))?.onPreferenceClickListener = null
      }

      private fun onFeeds(feeds: List<Feed>) {
         val screen = preferenceScreen
         val enabledFeeds = mapLayerPreferences.getEnabledFeeds(event?.id)
         for (feed in feeds) {
            val category = screen.getPreference(screen.preferenceCount - 1) as PreferenceCategory
            val feedPreference = mapLayerPreferences.mapFeedPreference(
               feed,
               requireActivity(),
               enabledFeeds.contains(feed.id)
            )
            feedPreference.onPreferenceClickListener =
               Preference.OnPreferenceClickListener {
                  onFeedClick(feed, feedPreference.isChecked)
                  true
               }
            category.addPreference(feedPreference)
         }
      }

      private fun onFeedClick(feed: Feed, on: Boolean) {
         val feeds = mapLayerPreferences.getEnabledFeeds(event?.id).toMutableSet()
         if (on) {
            feeds.add(feed.id)
         } else {
            feeds.remove(feed.id)
         }
         mapLayerPreferences.setEnabledFeeds(event?.id, feeds)
      }
   }

   public override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      supportFragmentManager.beginTransaction().replace(android.R.id.content, preference).commit()
   }

   companion object {
      const val TILE_OVERLAY_ACTIVITY = 100
      const val ONLINE_LAYERS_OVERLAY_ACTIVITY = 200
   }
}