package mil.nga.giat.mage.map.annotation

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.bumptech.glide.load.Transformation
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.database.model.event.Form
import mil.nga.giat.mage.database.model.feed.ItemWithFeed
import mil.nga.giat.mage.network.Server
import mil.nga.giat.mage.database.model.location.Location
import mil.nga.giat.mage.database.model.observation.Observation
import mil.nga.giat.mage.database.model.geojson.StaticFeature
import mil.nga.giat.mage.database.model.observation.ObservationForm
import mil.nga.giat.mage.database.model.user.User
import mil.nga.sf.Geometry
import mil.nga.sf.GeometryType
import java.io.File

data class MapAnnotation<T>(
   val id: T,
   val layer: String,
   val geometry: Geometry,
   val timestamp: Long? = null,
   val accuracy: Float? = null,
   val style: AnnotationStyle? = null,
   val allowEmptyIcon: Boolean = false,
   val iconTransformations: List<Transformation<Bitmap>> = mutableListOf(),
) {
   companion object {
      fun fromObservationProperties(
         id: Long,
         geometry: Geometry,
         timestamp: Long,
         accuracy: Float?,
         eventId: String,
         formId: Long?,
         primary: String?,
         secondary: String?,
         context: Context
      ): MapAnnotation<Long> {
         return MapAnnotation(
            id = id,
            layer = "observation",
            geometry = geometry,
            timestamp = timestamp,
            accuracy = accuracy,
            style = ObservationIconStyle.fromObservationProperties(eventId, formId, primary, secondary, context)
         )
      }

      fun fromObservation(
         event: Event?,
         observation: Observation,
         formDefinition: Form?,
         observationForm: ObservationForm?,
         geometryType: GeometryType,
         context: Context
      ): MapAnnotation<Long> {
         val style = AnnotationStyle.fromObservation(
            event = event,
            formDefinition = formDefinition,
            observationForm = observationForm,
            geometryType = geometryType,
            context = context)

         return MapAnnotation(
            id = observation.id,
            layer = "observation",
            geometry = observation.geometry,
            timestamp = observation.timestamp.time,
            accuracy = observation.accuracy,
            style = style
         )
      }

      fun fromUser(user: User, location: Location): MapAnnotation<Long> {
         val iconPath = if (user.iconPath != null) {
            File(user.iconPath)
         } else null

         val iconUri = iconPath?.let { Uri.fromFile(it) }
         val accuracy = location.propertiesMap["accuracy"]?.value?.toString()?.toFloatOrNull()

         return MapAnnotation(
            id = location.id,
            layer = "location",
            geometry = location.geometry,
            timestamp = location.timestamp.time,
            accuracy = accuracy,
            style = IconStyle(iconUri),
            allowEmptyIcon = true
         )
      }

      fun fromFeedItem(itemWithFeed: ItemWithFeed, context: Context): MapAnnotation<String>? {
         val feed = itemWithFeed.feed
         val item = itemWithFeed.item
         val geometry = item.geometry ?: return null
         val iconUri = feed.mapStyle?.iconStyle?.id?.let {
            Uri.parse("${Server(context).baseUrl}/api/icons/${it}/content")
         }

         return MapAnnotation(
            id = item.id,
            layer = feed.id,
            geometry = geometry,
            timestamp = item.timestamp,
            style = IconStyle(iconUri)
         )
      }

      fun fromStaticFeature(feature: StaticFeature, context: Context): MapAnnotation<Long> {
         return MapAnnotation(
            id = feature.id,
            layer = feature.layer.id.toString(),
            geometry = feature.geometry,
            style = AnnotationStyle.fromStaticFeature(feature, context)
         )
      }
   }
}