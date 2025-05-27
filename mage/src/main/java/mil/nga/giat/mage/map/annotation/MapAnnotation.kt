package mil.nga.giat.mage.map.annotation

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.load.Transformation
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.database.model.event.Form
import mil.nga.giat.mage.database.model.feed.ItemWithFeed
import mil.nga.giat.mage.database.model.location.Location
import mil.nga.giat.mage.database.model.observation.Observation
import mil.nga.giat.mage.database.model.geojson.StaticFeature
import mil.nga.giat.mage.database.model.observation.ObservationForm
import mil.nga.giat.mage.database.model.user.User
import mil.nga.sf.Geometry
import mil.nga.sf.GeometryType

data class MapAnnotation<T>(
   val id: T,
   val layer: String,
   val geometry: Geometry,
   val timestamp: Long? = null,
   val accuracy: Float? = null,
   val style: BaseObservationStyle? = null,
   val allowEmptyIcon: Boolean = false,
   val iconTransformations: List<Transformation<Bitmap>> = mutableListOf(),
) {
   companion object {

      fun getAnnotationWithStyleFromObservation(
         event: Event?,
         observation: Observation,
         formDefinition: Form?,
         observationForm: ObservationForm?,
         geometryType: GeometryType,
         context: Context
      ): MapAnnotation<Long> {
         val iconUri = ObservationIconHelper.getObservationIconUriFromObservation(event, formDefinition, observationForm, context)

         val style = if (geometryType == GeometryType.POINT) {
            BaseObservationStyle(iconUri)
         } else {
            ShapeObservationStyle.getStyleFromObservation(event, formDefinition, observationForm, iconUri, context)
         }

         return MapAnnotation(
            id = observation.id,
            layer = "observation",
            geometry = observation.geometry,
            timestamp = observation.timestamp.time,
            accuracy = observation.accuracy,
            style = style
         )
      }

      fun getAnnotationWithStyleFromStaticFeature(feature: StaticFeature, context: Context): MapAnnotation<Long> {
         val iconUri = ObservationIconHelper.getObservationIconUriFromStaticFeatures(feature)

         val style = if (feature.geometry.geometryType == GeometryType.POINT) {
            BaseObservationStyle(iconUri)
         } else {
            ShapeObservationStyle.getStyleFromStaticFeature(feature, iconUri, context)
         }

         return MapAnnotation(
            id = feature.id,
            layer = feature.layer.id.toString(),
            geometry = feature.geometry,
            style = style
         )
      }

      fun getAnnotationWithBaseStyleFromObservationProperties(
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
         val iconUri = ObservationIconHelper.getObservationIconUriFromProperties(eventId, formId, primary, secondary, context)
         val baseStyle = BaseObservationStyle(iconUri)

         return MapAnnotation(
            id = id,
            layer = "observation",
            geometry = geometry,
            timestamp = timestamp,
            accuracy = accuracy,
            style = baseStyle
         )
      }

      fun getAnnotationWithBaseStyleFromUser(user: User, location: Location): MapAnnotation<Long> {
         val iconUri = ObservationIconHelper.getObservationIconUriFromUser(user)
         val baseStyle = BaseObservationStyle(iconUri)

         val accuracy = location.propertiesMap["accuracy"]?.value?.toString()?.toFloatOrNull()

         return MapAnnotation(
            id = location.id,
            layer = "location",
            geometry = location.geometry,
            timestamp = location.timestamp.time,
            accuracy = accuracy,
            style = baseStyle,
            allowEmptyIcon = true
         )
      }

      fun getAnnotationWithBaseStyleFromFeedItem(itemWithFeed: ItemWithFeed, context: Context): MapAnnotation<String>? {
         val feed = itemWithFeed.feed
         val item = itemWithFeed.item
         val geometry = item.geometry ?: return null

         val iconUri = ObservationIconHelper.getObservationIconUriFromFeed(feed, context)
         val baseStyle = BaseObservationStyle(iconUri)

         return MapAnnotation(
            id = item.id,
            layer = feed.id,
            geometry = geometry,
            timestamp = item.timestamp,
            style = baseStyle
         )
      }
   }
}