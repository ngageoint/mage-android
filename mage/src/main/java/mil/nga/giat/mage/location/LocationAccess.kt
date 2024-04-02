package mil.nga.giat.mage.location

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationAccess @Inject constructor(
   val application: Application
) {
   fun isPreciseLocationGranted() =
     ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

   fun isApproximateLocationGranted() =
      ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

   fun isLocationGranted() = isPreciseLocationGranted() || isApproximateLocationGranted()

   fun isLocationDenied() = !isLocationGranted()
}