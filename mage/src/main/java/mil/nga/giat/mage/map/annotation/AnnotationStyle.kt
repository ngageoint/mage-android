package mil.nga.giat.mage.map.annotation

import android.content.Context
import mil.nga.giat.mage.sdk.datastore.observation.Observation
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature
import mil.nga.sf.GeometryType

/**
 *  Map annotation style for points, lines and polygons
 **/
sealed class AnnotationStyle {
   companion object {
      fun fromObservation(observation: Observation, context: Context): AnnotationStyle {
         return when (observation.geometry.geometryType) {
            GeometryType.POINT -> {
               IconStyle.fromObservation(observation, context)
            }
            else -> {
               ShapeStyle.fromObservation(observation, context)
            }
         }
      }

      fun fromStaticFeature(feature: StaticFeature, context: Context): AnnotationStyle {
         return when (feature.geometry.geometryType) {
            GeometryType.POINT -> {
               IconStyle.fromStaticFeature(feature)
            }
            else -> {
               ShapeStyle.fromStaticFeature(feature, context)
            }
         }
      }
   }
}