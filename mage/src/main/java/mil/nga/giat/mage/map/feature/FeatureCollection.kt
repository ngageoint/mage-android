package mil.nga.giat.mage.map.feature

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.Pair
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter
import mil.nga.giat.mage.R
import mil.nga.giat.mage.glide.target.MarkerTarget
import mil.nga.giat.mage.map.annotation.MapAnnotation
import mil.nga.giat.mage.map.annotation.ShapeStyle
import mil.nga.giat.mage.map.center
import mil.nga.giat.mage.map.lineTolerance
import mil.nga.sf.GeometryType
import mil.nga.sf.util.GeometryUtils
import kotlin.math.roundToInt

class FeatureCollection<T>(
   private val context: Context,
   private val map: GoogleMap,
   private val iconDimension: Int = 32,
   private val iconTransformers: ((MapAnnotation<*>) -> MutableList<Transformation<Bitmap>>)? = null
) {
   private var accuracyCircle: Pair<T, Circle>? = null
   private var markerAnimator: ValueAnimator? = null
   private var mapFeatures = mutableMapOf<T, Mappable<*>>()

   var isVisible = true
      private set

   fun add(annotations: List<MapAnnotation<T>>) {
      val features = mutableMapOf<T, Mappable<*>>()
      annotations.forEach { annotation ->
         val existingAnnotation = mapFeatures.remove(annotation.id)
         val geometry = annotation.geometry

         val shape: Mappable<*>? = if (geometry.geometryType == GeometryType.POINT) {
            val centroid = GeometryUtils.getCentroid(geometry)

            val marker = if (existingAnnotation == null) {
               val markerOptions = MarkerOptions()
                  .visible(false)
                  .position(LatLng(centroid.y, centroid.x))

               map.addMarker(markerOptions)?.apply {
                  tag = annotation
               }
            } else {
               val marker = existingAnnotation.feature as Marker
               marker.position = LatLng(centroid.y, centroid.x)
               marker
            }

            marker?.let { loadIcon(it, annotation) }
            marker?.toMappable()
         } else {
            val shape: Mappable<*>? = when (annotation.geometry.geometryType) {
               GeometryType.LINESTRING -> {
                  val shape = GoogleMapShapeConverter().toShape(annotation.geometry)

                  val polyline: Polyline = if (existingAnnotation == null) {
                     val options = shape.shape as PolylineOptions
                     options.visible(isVisible)
                     val shapeStyle = annotation.style as? ShapeStyle
                     shapeStyle?.let { style ->
                        options.width(style.strokeWidth)
                           .color(style.strokeColor)
                     }

                     map.addPolyline(options).apply {
                        tag = annotation
                     }
                  } else {
                     val polyline = existingAnnotation.feature as Polyline
                     polyline.points = (shape.shape as PolylineOptions).points
                     val shapeStyle = annotation.style as? ShapeStyle

                     shapeStyle?.let { style ->
                        polyline.color = style.strokeColor
                        polyline.width = style.strokeWidth
                     }

                     polyline
                  }

                  polyline.toMappable()
               }
               GeometryType.POLYGON -> {
                  val shape = GoogleMapShapeConverter().toShape(annotation.geometry)

                  val polygon: Polygon = if (existingAnnotation == null) {
                     val options = shape.shape as PolygonOptions
                     options.visible(isVisible)

                     val shapeStyle = annotation.style as? ShapeStyle
                     shapeStyle?.let { style ->
                        options.strokeWidth(style.strokeWidth)
                           .strokeColor(style.strokeColor)
                           .fillColor(style.fillColor)
                     }

                     map.addPolygon(options).apply {
                        tag = annotation
                     }
                  } else {
                     val polygon = existingAnnotation.feature as Polygon
                     polygon.points = (shape.shape as PolygonOptions).points
                     polygon.holes = (shape.shape as PolygonOptions).holes

                     val shapeStyle = annotation.style as? ShapeStyle
                     shapeStyle?.let { style ->
                        polygon.strokeColor = style.strokeColor
                        polygon.strokeWidth = style.strokeWidth
                        polygon.fillColor = style.fillColor
                     }

                     polygon
                  }

                  polygon.toMappable()
               }
               else -> null
            }

            shape
         }

         shape?.let {
            features[annotation.id] = it
         }
      }

      mapFeatures.values.forEach { it.remove() }
      mapFeatures.clear()

      mapFeatures = features
   }

   fun setVisibility(visible: Boolean) {
      if (isVisible == visible) return

      isVisible = visible
      mapFeatures.values.forEach {
         it.visible = visible
      }
   }

   fun remove(annotation: MapAnnotation<T>) {
      mapFeatures.remove(annotation.id)

      if (accuracyCircle?.first == annotation.id) {
         accuracyCircle?.second?.remove()
         accuracyCircle = null
      }
   }

   fun mapAnnotation(marker: Marker, layer: String): MapAnnotation<T>? {
      val feature = marker.tag as? MapAnnotation<T> ?: return null
      if (!mapFeatures.containsKey(feature.id) || feature.layer != layer) {
         return null
      }

      map.center(feature.geometry)
      animateMarker(marker, feature)
      marker.zIndex = 1f

      val point = GeometryUtils.getCentroid(feature.geometry)
      val latLng = LatLng(point.y, point.x)
      val accuracy = feature.accuracy
      if (accuracy != null) {
         try {
            accuracyCircle?.second?.remove()

            val circle = map.addCircle(
               CircleOptions()
                  .center(latLng)
                  .radius(accuracy.toDouble())
                  .fillColor(ContextCompat.getColor(context, R.color.accuracy_circle_fill))
                  .strokeColor(ContextCompat.getColor(context, R.color.accuracy_circle_stroke))
                  .strokeWidth(2.0f)
            )
            accuracyCircle = Pair(feature.id, circle)
         } catch (e: NumberFormatException) {
            Log.e(LOG_NAME, "Problem adding accuracy circle to the map.", e)
         }
      }

      return feature
   }

   fun offMarkerClick() {
      markerAnimator?.reverse()
      markerAnimator = null

      accuracyCircle?.second?.remove()
      accuracyCircle = null
   }

   fun refreshMarkerIcons() {
      mapFeatures.values.forEach {
         val mapFeature = it.tag as? MapAnnotation<*>
         val feature = it.feature
         if (feature is Marker && mapFeature != null) {
            loadIcon(feature, mapFeature)
         }
      }
   }

   fun count(): Int {
      return mapFeatures.size
   }

   fun onMapClick(latLng: LatLng): MapAnnotation<T>? {
      val shape = shapeForLocation(latLng)
      val feature = shape?.tag as? MapAnnotation<T>

      feature?.let {
         map.center(it.geometry)
      }

      return feature
   }

   fun clear() {
      mapFeatures.values.forEach {
         it.remove()
      }
      mapFeatures.clear()

      accuracyCircle?.second?.remove()
      accuracyCircle = null
   }

   private fun loadIcon(marker: Marker, annotation: MapAnnotation<*>) {
      val target = MarkerTarget(
         context,
         marker,
         iconDimension,
         iconDimension,
         isVisible
      )

      val transformations: List<Transformation<Bitmap>> = iconTransformers?.invoke(annotation) ?: emptyList()

      Glide.with(context)
         .asBitmap()
         .load(annotation)
         .error(R.drawable.default_marker)
         .transform(*transformations.toTypedArray())
         .into<MarkerTarget>(target)
   }

   private fun animateMarker(marker: Marker, annotation: MapAnnotation<*>) {
      val transformations: List<Transformation<Bitmap>> = iconTransformers?.invoke(annotation) ?: emptyList()

      Glide.with(context)
         .asBitmap()
         .load(annotation)
         .transform(*transformations.toTypedArray())
         .error(R.drawable.default_marker)
         .listener(object : RequestListener<Bitmap> {
            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean = true

            override fun onResourceReady(bitmap: Bitmap, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
               val animator = ValueAnimator.ofFloat(1f, 2f)
               animator.duration = 500
               animator.addUpdateListener { animation ->
                  val scale = animation.animatedValue as Float
                  val sizeX = (bitmap.width * scale).roundToInt()
                  val sizeY = (bitmap.height * scale).roundToInt()
                  val scaled =  Bitmap.createScaledBitmap(bitmap, sizeX, sizeY, false)

                  if (marker.tag != null) {
                     marker.setIcon(BitmapDescriptorFactory.fromBitmap(scaled))
                  }
               }
               animator.start()
               markerAnimator = animator

               return true
            }
         })
         .into(MarkerTarget(context, marker, iconDimension, iconDimension))
   }

   private fun shapeForLocation(latLng: LatLng): Mappable<*>? {
      // how many meters away from the click can the geometry be?
      val tolerance = map.lineTolerance()

      // Find the first polyline with the point on it, else find the first polygon
      val shape = mapFeatures.values
         .asSequence()
         .filter { it.feature is Polyline || it.feature is Polygon }
         .find {
            when(val feature = it.feature) {
               is Polyline -> {
                  PolyUtil.isLocationOnPath(latLng, feature.points, feature.isGeodesic, tolerance)
               }
               is Polygon -> {
                  PolyUtil.containsLocation(latLng, feature.points, feature.isGeodesic)
               }
               else -> false
            }
         }

      return shape
   }

   companion object {
      private val LOG_NAME = FeatureCollection::class.java.name
   }
}