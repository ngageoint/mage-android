package mil.nga.giat.mage.form.edit.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.R
import mil.nga.giat.mage.databinding.DialogMapSearchBinding
import mil.nga.giat.mage.ui.map.MapSearch
import mil.nga.giat.mage.ui.theme.MageTheme3

@AndroidEntryPoint
class MapSearchDialog(
   private val listener: MapSearchDialogListener? = null
): DialogFragment() {

   interface MapSearchDialogListener {
      fun onComplete(dialog: DialogFragment, latLng: LatLng? = null)
   }

   private lateinit var binding: DialogMapSearchBinding

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      setStyle(STYLE_NORMAL, R.style.AppTheme_Dialog_Fullscreen)
   }

   override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
   ): View {
      binding = DialogMapSearchBinding.inflate(inflater, container, false)

      binding.composeView.setContent {
         MageTheme3 {
            MapSearch(
               onApply = { latLng ->
                  listener?.onComplete(this, latLng)
               },
               onDismiss = {
                  listener?.onComplete(this)
               }
            )
         }
      }

      return binding.root
   }
}