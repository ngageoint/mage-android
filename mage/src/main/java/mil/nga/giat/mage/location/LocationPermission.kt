package mil.nga.giat.mage.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

data class LocationContractResult(
   val coarseGranted: Boolean,
   val preciseGranted: Boolean
)

class LocationPermission : ActivityResultContract<Void?, LocationContractResult>() {
   private val permissions = arrayOf(
      Manifest.permission.ACCESS_FINE_LOCATION,
      Manifest.permission.ACCESS_COARSE_LOCATION
   )

   override fun createIntent(context: Context, input: Void?): Intent {
      return Intent(ActivityResultContracts.RequestMultiplePermissions.ACTION_REQUEST_PERMISSIONS)
         .putExtra(ActivityResultContracts.RequestMultiplePermissions.EXTRA_PERMISSIONS, permissions)
   }

   override fun parseResult(resultCode: Int, intent: Intent?): LocationContractResult {
      if (resultCode != Activity.RESULT_OK) return LocationContractResult(
         preciseGranted = false,
         coarseGranted = false
      )

      if (intent == null) return LocationContractResult(
         preciseGranted = false,
         coarseGranted = false
      )

      val permissions = intent.getStringArrayExtra(ActivityResultContracts.RequestMultiplePermissions.EXTRA_PERMISSIONS)
      val grantResults = intent.getIntArrayExtra(ActivityResultContracts.RequestMultiplePermissions.EXTRA_PERMISSION_GRANT_RESULTS)
      if (grantResults == null || permissions == null) return LocationContractResult(
         preciseGranted = false,
         coarseGranted = false
      )

      val coarseIndex = permissions.indexOf(Manifest.permission.ACCESS_COARSE_LOCATION)
      val coarseGranted = grantResults[coarseIndex] == PackageManager.PERMISSION_GRANTED

      val preciseIndex = permissions.indexOf(Manifest.permission.ACCESS_FINE_LOCATION)
      val preciseGranted = grantResults[preciseIndex] == PackageManager.PERMISSION_GRANTED

      return LocationContractResult(
         preciseGranted = preciseGranted,
         coarseGranted = coarseGranted
      )
   }
}