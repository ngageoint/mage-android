package mil.nga.giat.mage.map.navigation.bearing
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import androidx.preference.PreferenceManager
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import mil.nga.giat.mage.R
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class LatLngEvaluator: TypeEvaluator<Pair<LatLng, LatLng>> {
   override fun evaluate(fraction: Float, startValue: Pair<LatLng, LatLng>?, endValue: Pair<LatLng, LatLng>?): Pair<LatLng, LatLng> {
      val x = (startValue?.first?.longitude ?: 0.0) + ((startValue?.second?.longitude ?: 0.0) - (startValue?.first?.longitude ?: 0.0)) * fraction
      val y = (startValue?.first?.latitude ?: 0.0) + ((startValue?.second?.latitude ?: 0.0) - (startValue?.first?.latitude ?: 0.0)) * fraction

      val x2 = (endValue?.first?.longitude ?: 0.0) + ((endValue?.second?.longitude ?: 0.0) - (endValue?.first?.longitude ?: 0.0)) * fraction
      val y2 = (endValue?.first?.latitude ?: 0.0) + ((endValue?.second?.latitude ?: 0.0) - (endValue?.first?.latitude ?: 0.0)) * fraction

      return Pair(LatLng(y, x), LatLng(y2, x2))
   }
}

class StraightLineNavigation(
   private val sensorManager: SensorManager?,
   private val mapView: GoogleMap,
   private val view: ViewGroup,
   private val context: Context,
   private val navigationStopped: (() -> Unit)? = null
) : SensorEventListener  {
   private var accelerometerReading: FloatArray? = null
   private var magnetometerReading: FloatArray? = null

   private var headingLine: Polyline? = null
   private var relativeBearingLine: Polyline? = null
   private var relativeBearingLineAnimator: ValueAnimator? = null
   private var navigationModeEnabled: Boolean = false
   private var headingModeEnabled: Boolean = false
   private var lastUserLocation: Location? = null
   private var lastHeadingLocation: LatLng? = null
   private var lastDestinationLocation: LatLng? = null
   private var lastAngle: Double = 0.0
   private var lastUpdateTime: Long = 0
   private var straightLineNav: StraightLineNavigationView? = null
   private var straightLineNavigationData: StraightLineNavigationData = StraightLineNavigationData()

   private val relativeBearingColor: Int
      get(): Int {
         val hexColor = PreferenceManager.getDefaultSharedPreferences(context).getString(context.resources.getString(R.string.relativeBearingColorKey), context.resources.getString(R.string.relativeBearingColorDefaultValue))
         return try {
            Color.parseColor(hexColor)
         } catch (ignored: IllegalArgumentException) {
            Color.GREEN
         }
      }

   private val headingColor: Int
      get(): Int {
         val hexColor = PreferenceManager.getDefaultSharedPreferences(context).getString(context.resources.getString(R.string.headingColorKey), context.resources.getString(R.string.headingColorDefaultValue))
         return try {
            Color.parseColor(hexColor)
         } catch (ignored: IllegalArgumentException) {
            Color.RED
         }
      }

   fun startNavigation(userLocation: Location, destinationCoordinate: LatLng, icon: Any? = null) {
      lastDestinationLocation = destinationCoordinate
      straightLineNavigationData.destinationCoordinate.set(destinationCoordinate)
      straightLineNavigationData.heading.set(0.0)
      straightLineNavigationData.mapFeature = icon
      straightLineNavigationData.headingColor.set(headingColor)
      straightLineNavigationData.bearingColor.set(relativeBearingColor)
      straightLineNavigationData.currentLocation.set(userLocation)
      straightLineNav = StraightLineNavigationView(context).also {
         val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
         it.layoutParams = layoutParams
         it.cancel = { stopNavigation() }
         view.addView(it)
         it.populate(straightLineNavigationData)
      }

      sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { accelerometer ->
         sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL,
            SensorManager.SENSOR_DELAY_NORMAL
         )
      }
      sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let { magneticField ->
         sensorManager.registerListener(
            this,
            magneticField,
            SensorManager.SENSOR_DELAY_NORMAL,
            SensorManager.SENSOR_DELAY_NORMAL
         )
      }

      lastUserLocation = userLocation
      navigationModeEnabled = true
      headingModeEnabled = true
      updateNavigationLines(userLocation, destinationCoordinate)
   }

   fun startHeading(userLocation: Location) {
      sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { accelerometer ->
         sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL,
            SensorManager.SENSOR_DELAY_NORMAL
         )
      }
      sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let { magneticField ->
         sensorManager.registerListener(
            this,
            magneticField,
            SensorManager.SENSOR_DELAY_NORMAL,
            SensorManager.SENSOR_DELAY_NORMAL
         )
      }

      headingModeEnabled = true
      updateHeadingLine(userLocation)
   }

   fun stopHeading() {
      headingModeEnabled = false
      headingLine?.remove()
      sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
         sensorManager.unregisterListener(this, accelerometer)
      }
      sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
         sensorManager.unregisterListener(this, magneticField)
      }
   }

   private fun stopNavigation() {
      navigationModeEnabled = false
      headingModeEnabled = false
      relativeBearingLine?.remove()
      headingLine?.remove()
      sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
         sensorManager.unregisterListener(this, accelerometer)
      }
      sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
         sensorManager.unregisterListener(this, magneticField)
      }
      view.removeView(straightLineNav)
      lastHeadingLocation = null

      navigationStopped?.invoke()
   }

   fun isNavigating(): Boolean {
      return navigationModeEnabled
   }

   fun updateDestination(destinationCoordinate: LatLng) {
      if (navigationModeEnabled) {
         lastDestinationLocation = destinationCoordinate
         lastUserLocation?.let { userLocation ->
            updateNavigationLines(userLocation, destinationCoordinate)
         }
      }
   }

   fun updateUserLocation(userLocation: Location) {
      if (navigationModeEnabled) {
         lastUserLocation = userLocation
         lastDestinationLocation?.let { destinationLocation ->
            updateNavigationLines(userLocation, destinationLocation)
         }
      }
   }

   private fun updateNavigationLines(userLocation: Location, destinationCoordinate: LatLng) {
      val previousBearing = straightLineNavigationData.relativeBearing.get()?.toFloat() ?: 0f
      val previousHeading = straightLineNavigationData.heading.get()?.toFloat() ?: 0f
      val previousDirection = previousBearing - previousHeading

      lastDestinationLocation = destinationCoordinate
      lastUserLocation = userLocation

      relativeBearingLine = relativeBearingLine?.apply {
         points = listOf(destinationCoordinate, LatLng(userLocation.latitude, userLocation.longitude))
      } ?: run {
         mapView.addPolyline(
            PolylineOptions()
               .add(destinationCoordinate)
               .add(LatLng(userLocation.latitude, userLocation.longitude))
               .color(relativeBearingColor)
               .zIndex(2.0f)
               .width(15.0f)
         )
      }

      val targetLocation = Location("")
      targetLocation.latitude = destinationCoordinate.latitude
      targetLocation.longitude = destinationCoordinate.longitude
      straightLineNavigationData.relativeBearing.set(userLocation.bearingTo(targetLocation).toDouble())
      straightLineNavigationData.currentLocation.set(lastUserLocation)
      straightLineNav?.populate(straightLineNavigationData)

      updateHeadingLine(lastUserLocation!!)

      val bearing = straightLineNavigationData.relativeBearing.get()?.toFloat() ?: 0f
      val heading = straightLineNavigationData.heading.get()?.toFloat() ?: 0f
      val direction = bearing - heading

      val valueAnimator = ValueAnimator.ofFloat(previousDirection, direction)
      valueAnimator.addUpdateListener {
         straightLineNav?.rotateDirectionIcon(it.animatedValue as Float)
      }
      valueAnimator.interpolator = AccelerateDecelerateInterpolator()
      valueAnimator.duration = 500
      valueAnimator.start()
   }

   private fun updateHeadingLine(userLocation: Location) {
      lastUserLocation = userLocation

      if (lastHeadingLocation == null) {
         headingLine = mapView.addPolyline(
            PolylineOptions()
               .add(calculateBearingPoint(userLocation, lastAngle))
               .add(LatLng(userLocation.latitude, userLocation.longitude))
               .color(headingColor)
               .zIndex(1.0f)
               .width(30.0f)
         )
      } else {
         relativeBearingLineAnimator?.cancel()
         relativeBearingLineAnimator?.removeAllUpdateListeners()

         val bearingLineAnimator = ValueAnimator.ofObject(
            LatLngEvaluator(),
            Pair(LatLng(userLocation.latitude, userLocation.longitude),
               LatLng(userLocation.latitude, userLocation.longitude)),
            Pair(lastHeadingLocation, calculateBearingPoint(userLocation, lastAngle)))

         bearingLineAnimator.addUpdateListener {
            val value = it.animatedValue as Pair<LatLng, LatLng>
            headingLine?.points = arrayListOf(value.second, value.first)
         }

         bearingLineAnimator.interpolator = AccelerateDecelerateInterpolator()
         bearingLineAnimator.duration = 500
         bearingLineAnimator.start()

         relativeBearingLineAnimator = bearingLineAnimator
      }

      lastHeadingLocation = calculateBearingPoint(userLocation, lastAngle)

      straightLineNavigationData.heading.set(lastAngle)
      straightLineNav?.populate(straightLineNavigationData)
   }

   private fun calculateBearingPoint(startLocation: Location, bearing: Double): LatLng {
      val projection = mapView.projection

      val span = projection.visibleRegion
      val center = span.latLngBounds.center

      val neCornerLocation = Location("")
      neCornerLocation.latitude = span.latLngBounds.northeast.latitude
      neCornerLocation.longitude = span.latLngBounds.northeast.longitude

      val centerLocation = Location("")
      centerLocation.latitude = center.latitude
      centerLocation.longitude = center.longitude

      val metersToDestination = centerLocation.distanceTo(neCornerLocation)
      val radianBearing = bearing * (Math.PI / 180.0)

      return locationWithBearing(radianBearing, metersToDestination, startLocation)
   }

   private fun locationWithBearing(radianBearing: Double, distanceMeters: Float, origin: Location): LatLng {
      val distanceRadians = distanceMeters / 6372797.6f
      val lat1 = origin.latitude * Math.PI / 180.0
      val lon1 = origin.longitude * Math.PI / 180.0

      val lat2 = asin(sin(lat1) * cos(distanceRadians.toDouble()) + cos(lat1) * sin(distanceRadians.toDouble()) * cos(radianBearing))
      val lon2 = lon1 + atan2(sin(radianBearing) * sin(distanceRadians.toDouble()) * cos(lat1), cos(distanceRadians.toDouble()) - sin(lat1) * sin(lat2))

      return LatLng(lat2 * 180.0 / Math.PI, lon2 * 180.0 / Math.PI)
   }

   override fun onSensorChanged(event: SensorEvent?) {
      val safeEvent: SensorEvent = event ?: return
      if (lastUserLocation == null) {
         return
      }

      if (safeEvent.sensor.type == Sensor.TYPE_ACCELEROMETER) {
         accelerometerReading = safeEvent.values
      } else if (safeEvent.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
         magnetometerReading = safeEvent.values
      }
      if (accelerometerReading != null && magnetometerReading != null && (safeEvent.timestamp - lastUpdateTime) > FIVE_SECONDS) {
         lastUpdateTime = safeEvent.timestamp
         val rotationMatrix = FloatArray(9)
         val inclinationMatrix = FloatArray(9)
         if (SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, accelerometerReading, magnetometerReading)) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            lastAngle = (Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0
            if (lastDestinationLocation != null) {
               this.updateNavigationLines(lastUserLocation!!, lastDestinationLocation!!)
            } else {
               this.updateHeadingLine(lastUserLocation!!)
            }
         }
      }
   }

   override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

   companion object {
      private const val FIVE_SECONDS = 500000000
   }
}