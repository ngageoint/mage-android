package mil.nga.giat.mage.map.annotation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.DisplayMetrics
import androidx.core.graphics.ColorUtils
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

         val style: BaseObservationStyle = when (geometry.geometryType) {
            GeometryType.POLYGON, GeometryType.MULTIPOLYGON,
            GeometryType.LINESTRING, GeometryType.MULTILINESTRING -> {
               val styleJson = item.properties?.asJsonObject?.getAsJsonObject("style")
               android.util.Log.d("MapAnnotation", "Feed item ${item.id} geometry=${geometry.geometryType} properties=${item.properties} styleJson=$styleJson")
               if (styleJson != null) {
                  val fillHex   = styleJson.get("fillColor")?.takeIf { !it.isJsonNull }?.asString
                  val strokeHex = styleJson.get("color")?.takeIf { !it.isJsonNull }?.asString
                  val fillOpacity   = styleJson.get("fillOpacity")?.takeIf { !it.isJsonNull }?.asFloat ?: 0.3f
                  val strokeOpacity = styleJson.get("opacity")?.takeIf { !it.isJsonNull }?.asFloat ?: 1.0f
                  val weight = styleJson.get("weight")?.takeIf { !it.isJsonNull }?.asFloat ?: 2.0f
                  val density = context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT

                  val fillColor = fillHex
                     ?.let { runCatching { Color.parseColor(it) }.getOrNull() }
                     ?.let { ColorUtils.setAlphaComponent(it, (fillOpacity * 255).toInt()) }
                     ?: 0
                  val strokeColor = strokeHex
                     ?.let { runCatching { Color.parseColor(it) }.getOrNull() }
                     ?.let { ColorUtils.setAlphaComponent(it, (strokeOpacity * 255).toInt()) }
                     ?: Color.BLACK

                  ShapeObservationStyle(iconUri, weight * density, strokeColor, fillColor)
               } else {
                  BaseObservationStyle(iconUri)
               }
            }
            else -> BaseObservationStyle(iconUri)
         }

         return MapAnnotation(
            id = item.id,
            layer = feed.id,
            geometry = geometry,
            timestamp = item.timestamp,
            style = style
         )
      }
   }
}