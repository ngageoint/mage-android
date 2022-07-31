package mil.nga.giat.mage.map.annotation

import android.content.Context
import android.graphics.Color
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import mil.nga.giat.mage.R
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.field.FieldValue
import mil.nga.giat.mage.network.gson.asJsonObjectOrNull
import mil.nga.giat.mage.sdk.datastore.observation.Observation
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature
import mil.nga.giat.mage.sdk.datastore.user.EventHelper
import mil.nga.sf.LineString
import mil.nga.sf.Polygon

class ShapeStyle: AnnotationStyle {

   constructor(context: Context) {
      val defaultStrokeWidth = TypedValue()
      context.resources.getValue(R.dimen.fill_default_stroke_width, defaultStrokeWidth, true)
      strokeWidth = defaultStrokeWidth.float * (context.resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)

      this.strokeColor = Color.BLACK
      this.fillColor = 0
   }

   constructor(strokeWidth: Float, strokeColor: Int = 0, fillColor: Int = 0) {
      this.strokeWidth = strokeWidth
      this.strokeColor = strokeColor
      this.fillColor = fillColor
   }

   var strokeWidth: Float
   var strokeColor: Int
   var fillColor: Int

   companion object {
      private const val FILL_ELEMENT = "fill"
      private const val STROKE_ELEMENT = "stroke"
      private const val FILL_OPACITY_ELEMENT = "fillOpacity"
      private const val STROKE_OPACITY_ELEMENT = "strokeOpacity"
      private const val STROKE_WIDTH_ELEMENT = "strokeWidth"
      private const val MAX_ALPHA = 255.0f

      fun fromObservation(observation: Observation, context: Context): AnnotationStyle {
         var jsonStyle = observation.forms.firstOrNull()?.let { observationForm ->
            val form = EventHelper.getInstance(context).getForm(observationForm.formId)
            form.style?.let { formStyle ->
               var style = JsonParser.parseString(formStyle)?.asJsonObjectOrNull()
               if (style != null) {
                  if (form.primaryMapField != null) {
                     val primaryProperty = observationForm.properties.find { it.key == form.primaryMapField }
                     val primary = primaryProperty?.value?.toString()

                     // Check for primary within the style object
                     style.get(primary)?.asJsonObjectOrNull()?.let { primaryStyle ->
                        style = primaryStyle

                        if (form.secondaryMapField != null) {
                           val secondaryProperty = observationForm.properties.find { it.key == form.secondaryMapField }
                           val secondary = secondaryProperty?.value?.toString()

                           // Check for secondary within the style type object
                           primaryStyle.get(secondary)?.asJsonObjectOrNull()?.let { secondaryStyle ->
                              style = secondaryStyle
                           }
                        }
                     }
                  }
               }

               style
            }
         }

         if (jsonStyle == null) {
            jsonStyle = observation.event.style?.let { style ->
               JsonParser.parseString(style)?.asJsonObjectOrNull()
            }
         }

         return if (jsonStyle != null) {
            return fromJson(jsonStyle, context)
         } else ShapeStyle(context)
      }

      fun fromStaticFeature(feature: StaticFeature, context: Context): ShapeStyle {
         val defaultStyle = ShapeStyle(context)
         return when (feature.geometry) {
            is LineString -> {
               val properties = feature.propertiesMap
               val lineWidth = properties["stylelinestylewidth"]?.value?.toFloatOrNull()
               val strokeWidth = lineWidth?.let { width ->
                  width * (context.resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
               } ?: defaultStyle.strokeWidth

               val strokeColor = properties["stylelinestylecolorrgb"]?.let { property ->
                  Color.parseColor(property.value)
               } ?: 0

               ShapeStyle(strokeWidth, strokeColor)
            }
            is Polygon -> {
               val properties = feature.propertiesMap

               val lineWidth = properties["stylelinestylewidth"]?.value?.toFloatOrNull()
               val strokeWidth = lineWidth?.let { width ->
                  width * (context.resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
               } ?: defaultStyle.strokeWidth

               val polylineColor = properties["stylelinestylecolorrgb"]?.let { property ->
                  Color.parseColor(property.value)
               }
               val polylineOpacity = properties["stylelinestylecoloropacity"]?.value?.toIntOrNull()
               val polylineColorWithOpacity = polylineColor?.let { color ->
                  Color.argb(polylineOpacity ?: 255, color.red, color.green, color.blue)
               }

               val polygonColor = properties["stylepolystylecolorrgb"]?.let { property ->
                  Color.parseColor(property.value)
               }
               val polygonOpacity = properties["stylepolystylecoloropacity"]?.value?.toIntOrNull()
               val polygonColorWithOpacity = polygonColor?.let { color ->
                  Color.argb(polygonOpacity ?: 255, color.red, color.green, color.blue)
               }

               val strokeColor = polylineColorWithOpacity ?: polygonColorWithOpacity ?: defaultStyle.strokeColor
               val fillColor = polygonColorWithOpacity ?: defaultStyle.fillColor

               ShapeStyle(strokeWidth, strokeColor, fillColor)
            }
            else -> defaultStyle
         }
      }

      fun fromForm(formState: FormState?, context: Context): ShapeStyle {
         var style = ShapeStyle(context)

         // Check for a style
         if (formState != null) {
            // Found the top level style
            var jsonStyle = formState.definition.style
            var primaryValue: FieldValue.Text? = null
            for (fieldState in formState.fields) {
               if (fieldState.definition.name == formState.definition.primaryMapField &&
                  fieldState.answer is FieldValue.Text
               ) {
                  primaryValue = fieldState.answer as FieldValue.Text?
                  break
               }
            }
            var secondaryValue: FieldValue.Text? = null
            for (fieldState in formState.fields) {
               if (fieldState.definition.name == formState.definition.secondaryMapField &&
                  fieldState.answer is FieldValue.Text
               ) {
                  secondaryValue = fieldState.answer as FieldValue.Text?
                  break
               }
            }
            if (primaryValue != null) {
               // Check for a type within the style
               val primaryElement = jsonStyle?.get(primaryValue.text)
               if (primaryElement != null && !primaryElement.isJsonNull) {

                  // Found the type level style
                  jsonStyle = primaryElement.asJsonObject
                  if (secondaryValue != null) {
                     // Check for a variant within the style type
                     val secondaryElement = jsonStyle[secondaryValue.text]
                     if (secondaryElement != null && !secondaryElement.isJsonNull) {
                        // Found the variant level style
                        jsonStyle = secondaryElement.asJsonObject
                     }
                  }
               }
            }

            if (jsonStyle != null) {
               style = fromJson(jsonStyle, context)
            } else {
               EventHelper.getInstance(context).read(formState.eventId)?.style?.let { jsonStyle ->
                  JsonParser.parseString(jsonStyle)?.asJsonObjectOrNull()?.let { jsonObject ->
                     style = fromJson(jsonObject, context)
                  }
               }
            }
         }


         return style
      }

      private fun fromJson(json: JsonObject, context: Context): ShapeStyle {
         // Get the style properties
         val fill = json[FILL_ELEMENT].asString
         val stroke = json[STROKE_ELEMENT].asString
         val fillOpacity = json[FILL_OPACITY_ELEMENT].asFloat
         val strokeOpacity = json[STROKE_OPACITY_ELEMENT].asFloat
         val strokeWidth = json[STROKE_WIDTH_ELEMENT].asFloat

         // Set the stroke width
         val strokeWithDensity = strokeWidth * (context.resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)

         // Create and set the stroke color
         val strokeColor = Color.parseColor(stroke)
         val strokeColorWithAlpha = ColorUtils.setAlphaComponent(strokeColor, getAlpha(strokeOpacity))

         // Create and set the fill color
         val fillColor = Color.parseColor(fill)
         val fillColorWithAlpha = ColorUtils.setAlphaComponent(fillColor, getAlpha(fillOpacity))

         return ShapeStyle(strokeWithDensity, strokeColorWithAlpha, fillColorWithAlpha)
      }

      private fun getAlpha(opacity: Float): Int {
         return (opacity * MAX_ALPHA).toInt()
      }
   }
}