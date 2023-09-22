package mil.nga.giat.mage.map.annotation

import android.content.Context
import android.net.Uri
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.database.model.event.Form
import mil.nga.giat.mage.database.model.feature.StaticFeature
import mil.nga.giat.mage.database.model.observation.ObservationForm
import mil.nga.sf.GeometryType
import java.io.File

/**
 *  Map annotation style for points, lines and polygons
 **/
sealed class AnnotationStyle {
   companion object {
      fun fromObservation(
         event: Event?,
         formDefinition: Form?,
         observationForm: ObservationForm?,
         geometryType: GeometryType,
         context: Context
      ): AnnotationStyle {
         return when (geometryType) {
            GeometryType.POINT -> {
               ObservationIconStyle.fromObservation(event, formDefinition, observationForm, context)
            }
            else -> {
               ShapeStyle.fromObservation(event, formDefinition, observationForm, context)
            }
         }
      }

      fun fromStaticFeature(feature: StaticFeature, context: Context): AnnotationStyle {
         return when (feature.geometry.geometryType) {
            GeometryType.POINT -> {
               val iconUri = feature.localPath?.let { path ->
                  val file = File(path)
                  if (file.exists()) {
                     Uri.fromFile(file)
                  } else null
               }

               return IconStyle(iconUri)
            }
            else -> {
               ShapeStyle.fromStaticFeature(feature, context)
            }
         }
      }
   }
}