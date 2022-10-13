package mil.nga.giat.mage.coordinate

import com.google.android.gms.maps.model.LatLng
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.truncate

enum class CoordinateType {
   LATITUDE, LONGITUDE
}

data class MutableDMSLocation(
   var degrees: String? = null,
   var minutes: String? = null,
   var seconds: String? = null,
   var direction: String? = null
) {
   companion object {
      fun parse(location: String, type: CoordinateType, addDirection: Boolean = false): MutableDMSLocation {
         val charactersToKeep = if (type == CoordinateType.LATITUDE) "-.NS" else "-.EW"
         var parsable = location.filter {
            it.isDigit() || charactersToKeep.contains(it.uppercase())
         }

         if (parsable.isEmpty()) return MutableDMSLocation()

         val direction = if (addDirection) {
            when {
               parsable.first() == '-' -> {
                  if (type == CoordinateType.LATITUDE) "S" else "W"
               }
               else -> if (type == CoordinateType.LATITUDE) "N" else "E"
            }
         } else {
            when {
               parsable.last().isLetter() -> {
                  val direction = parsable.last().uppercase()
                  parsable = parsable.dropLast(1)
                  direction
               }
               parsable.first().isLetter() -> {
                  val direction = parsable.first().uppercase()
                  parsable = parsable.drop(1)
                  direction
               }
               else -> null
            }
         }

         parsable = parsable.filter { it.isDigit() || it == '.' }

         val split = parsable.split(".")
         parsable = split.first()
         val decimalSeconds = if (split.size == 2) split[1].toIntOrNull() else null

         var seconds: String? = parsable.takeLast(2)
         parsable = parsable.dropLast(2)

         var minutes = if (parsable.isNotEmpty()) parsable.takeLast(2) else null
         var degrees: String? = if (parsable.isNotEmpty()) parsable.dropLast(2) else null

         if (degrees == null || degrees.isEmpty()) {
            if (minutes == null || minutes.isEmpty()) {
               degrees = seconds
               seconds = null
            } else {
               degrees = minutes
               minutes = seconds
               seconds = null
            }
         }

         if (minutes == null && seconds == null && decimalSeconds != null) {
            // this would be the case if a decimal degrees was passed in ie 11.123
            val decimal = ".${decimalSeconds}".toDoubleOrNull() ?: 0.0
            minutes = abs((decimal % 1) * 60.0).toString()
            seconds = abs((((decimal % 1) * 60.0) % 1) * 60.0).roundToInt().toString()
         } else if (decimalSeconds != null) {
            // add the decimal seconds to seconds and round
            seconds = "${seconds ?: 0}.${decimalSeconds}".toDouble().roundToInt().toString()
         }

         return MutableDMSLocation(degrees, minutes, seconds, direction)
      }
   }
}

class DMSLocation(
   degrees: Int,
   minutes: Int,
   seconds: Int,
   direction: String
) {
   var degrees: Int = degrees
      private set

   var minutes: Int = minutes
      private set

   var seconds: Int = seconds
      private set

   var direction: String = direction
      private set


   fun toDecimalDegrees(): Double {
      var decimalDegrees =
         degrees.toDouble() +
         (minutes / 60.0) +
         (seconds / 3600.0)

      if (direction == "S" || direction == "W") {
         decimalDegrees = -decimalDegrees
      }

      return decimalDegrees
   }

   fun format(): String {
      val latitudeMinutes = minutes.toString().padStart(2, '0')
      val latitudeSeconds = seconds.toString().padStart(2, '0')
      return "${abs(degrees)}° $latitudeMinutes' $latitudeSeconds\" $direction"
   }

   companion object {
      fun parse(location: String, type: CoordinateType): DMSLocation? {
         val (degreesString, minutesString, secondsString, direction) = MutableDMSLocation.parse(location, type)

         val degrees = degreesString?.toIntOrNull()
         val minutes = minutesString?.toIntOrNull()
         val seconds = secondsString?.toIntOrNull()

         val validDegrees = degrees?.let {
            if (type == CoordinateType.LATITUDE) it in 0..90 else it in 0..180
         } ?: false

         val validMinutes = minutes?.let {
            !((it < 0 || it > 59) || (type == CoordinateType.LATITUDE && degrees == 90 && minutes != 0) || (type == CoordinateType.LONGITUDE && degrees == 180 && minutes != 0))
         } ?: false

         val validSeconds = seconds?.let {
            !((it < 0 || it > 59) || (type == CoordinateType.LATITUDE && degrees == 90 && seconds != 0) || (type == CoordinateType.LONGITUDE && degrees == 180 && seconds != 0))
         } ?: false

         if (!validDegrees || !validMinutes || !validSeconds) return null

         return if (degrees != null && minutes != null && seconds != null && direction != null) {
            DMSLocation(degrees, minutes, seconds, direction)
         } else null
      }
   }
}

class DMS(
   latitude: DMSLocation,
   longitude: DMSLocation
) {
   var latitude: DMSLocation = latitude
      private set

   var longitude: DMSLocation = longitude
      private set


   fun toLatLng(): LatLng {
      return LatLng(latitude.toDecimalDegrees(), longitude.toDecimalDegrees())
   }

   fun format(): String {
      return "${latitude.format()}, ${longitude.format()}"
   }

   companion object {
      fun from(latLng: LatLng): DMS {
         var latDegrees = truncate(latLng.latitude).toInt()
         var latMinutes = abs((latLng.latitude % 1) * 60.0).toInt()
         var latSeconds = (abs((((latLng.latitude % 1) * 60.0) % 1) * 60.0)).roundToInt()
         if (latSeconds == 60) {
            latSeconds = 0
            latMinutes += 1
         }
         if (latMinutes == 60) {
            latDegrees += 1
            latMinutes = 0
         }
         val latitudeDMS = DMSLocation(latDegrees, latMinutes, latSeconds, if (latDegrees >= 0) "N" else "S")

         var lonDegrees = truncate(latLng.longitude).toInt()
         var lonMinutes = abs((latLng.longitude % 1) * 60.0).toInt()
         var lonSeconds = (abs((((latLng.longitude % 1) * 60.0) % 1) * 60.0)).roundToInt()
         if (lonSeconds == 60) {
            lonSeconds = 0
            lonMinutes += 1
         }
         if (lonMinutes == 60) {
            lonDegrees += 1
            lonMinutes = 0
         }
         val longitudeDMS = DMSLocation(lonDegrees, lonMinutes, lonSeconds, if (lonDegrees >= 0) "E" else "W")

         return DMS(latitudeDMS, longitudeDMS)
      }

      fun from(latitude: String, longitude: String): DMS? {
         val dmsLatitude = DMSLocation.parse(latitude, CoordinateType.LATITUDE)
         val dmsLongitude = DMSLocation.parse(longitude, CoordinateType.LONGITUDE)

         return if (dmsLatitude != null && dmsLongitude != null) {
            DMS(dmsLatitude, dmsLongitude)
         } else null
      }
   }
}
