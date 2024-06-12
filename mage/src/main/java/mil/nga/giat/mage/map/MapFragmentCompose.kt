package mil.nga.giat.mage.map

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import mil.nga.giat.mage.R
import mil.nga.giat.mage.filter.FilterActivity
import mil.nga.giat.mage.map.preference.MapPreferencesActivity
import mil.nga.giat.mage.observation.edit.ObservationEditActivity
import mil.nga.giat.mage.ui.map.MapScreen
import mil.nga.giat.mage.ui.theme.MageTheme3

class MapFragmentCompose: Fragment() {
   override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
   ): View {
      (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
         override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.filter, menu)
         }

         override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when(menuItem.itemId){
               R.id.filter_button -> {
                  val intent = Intent(activity, FilterActivity::class.java)
                  startActivity(intent)
                  true
               }
               else -> false
            }
         }
      }, viewLifecycleOwner, Lifecycle.State.RESUMED)

      return ComposeView(requireContext()).apply {
         setContent {
            MageTheme3 {
               MapScreen(
                  onSettings = { onSettingsTap() },
                  onAddObservation = { location -> onAddObservation(location) }
               )
            }
         }
      }
   }

   private fun onAddObservation(location: Location?) {
      val intent = Intent(activity, ObservationEditActivity::class.java)
      intent.putExtra(ObservationEditActivity.LOCATION, location)
      startActivity(intent)
   }

   private fun onSettingsTap() {
      val intent = Intent(activity, MapPreferencesActivity::class.java)
      startActivity(intent)
   }
}