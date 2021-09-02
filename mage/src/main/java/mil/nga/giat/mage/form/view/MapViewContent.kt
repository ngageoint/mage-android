package mil.nga.giat.mage.form.view

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.preference.PreferenceManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.google.maps.android.ktx.*
import kotlinx.coroutines.launch
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter
import mil.nga.giat.mage.R
import mil.nga.giat.mage.map.marker.ObservationBitmapFactory
import mil.nga.giat.mage.observation.MapShapeObservation
import mil.nga.giat.mage.observation.ObservationLocation
import mil.nga.giat.mage.observation.ObservationShapeStyleParser
import mil.nga.giat.mage.form.FormState
import mil.nga.giat.mage.form.field.FieldValue
import mil.nga.sf.GeometryType
import mil.nga.sf.util.GeometryUtils

data class MapState(val center: LatLng?, val zoom: Float?)

@Composable
fun MapViewContent(
  map: MapView,
  formState: FormState?,
  location: ObservationLocation,
  mapState: MapState
) {
  val context = LocalContext.current
  var mapInitialized by remember(map) { mutableStateOf(false) }

  val primaryFieldState = formState?.fields?.find { it.definition.name == formState.definition.primaryMapField }
  val secondaryFieldState = formState?.fields?.find { it.definition.name == formState.definition.secondaryMapField }

  LaunchedEffect(map, mapInitialized) {
    if (!mapInitialized) {
      val googleMap = map.awaitMap()
      googleMap.uiSettings.isMapToolbarEnabled = false

      if (mapState.center != null && mapState.zoom != null) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mapState.center, mapState.zoom))
      }

      val preferences = PreferenceManager.getDefaultSharedPreferences(context)
      googleMap.mapType = preferences.getInt(context.getString(R.string.baseLayerKey), context.resources.getInteger(R.integer.baseLayerDefaultValue))

      val dayNightMode: Int = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
      if (dayNightMode == Configuration.UI_MODE_NIGHT_NO) {
        googleMap.setMapStyle(null)
      } else {
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_theme_night))
      }

      mapInitialized = true
    }
  }

  val scope = rememberCoroutineScope()
  AndroidView({ map }) { mapView ->
    val primary = primaryFieldState?.answer as? FieldValue.Text
    val secondary = secondaryFieldState?.answer as? FieldValue.Text

    scope.launch {
      val googleMap = mapView.awaitMap()
      googleMap.clear()
      if (location.geometry.geometryType == GeometryType.POINT) {
        val centroid = GeometryUtils.getCentroid(location.geometry)
        val point = LatLng(centroid.y, centroid.x)
        googleMap.addMarker {
          position(point)
          icon(ObservationBitmapFactory.bitmapDescriptor(context, formState?.eventId, formState?.definition?.id, primary, secondary))
        }

        if (!location.provider.equals(ObservationLocation.MANUAL_PROVIDER, true) && location.accuracy != null) {
          googleMap.addCircle {
            fillColor(context.resources.getColor(R.color.accuracy_circle_fill))
            strokeColor(context.resources.getColor(R.color.accuracy_circle_stroke))
            strokeWidth(2f)
            center(point)
            radius(location.accuracy.toDouble())
          }
        }
      } else {
        val shape = GoogleMapShapeConverter().toShape(location.geometry).shape
        val style = ObservationShapeStyleParser.getStyle(context, formState)

        if (shape is PolylineOptions) {
          googleMap.addPolyline {
            addAll(shape.points)
            width(style.strokeWidth)
            color(style.strokeColor)
            geodesic(MapShapeObservation.GEODESIC)
          }
        } else if (shape is PolygonOptions) {
          googleMap.addPolygon {
            addAll(shape.points)
            for (hole in shape.holes) {
              addHole(hole)
            }

            strokeWidth(style.strokeWidth)
            strokeColor(style.strokeColor)
            fillColor(style.fillColor)
            geodesic(MapShapeObservation.GEODESIC)
          }
        }
      }

      googleMap.animateCamera(location.getCameraUpdate(mapView, true, 1.0f / 6))
    }
  }
}

@Composable
fun MapViewContent(
  map: MapView,
  location: ObservationLocation
) {
  val context = LocalContext.current
  var mapInitialized by remember(map) { mutableStateOf(false) }

  LaunchedEffect(map, mapInitialized) {
    if (!mapInitialized) {
      val googleMap = map.awaitMap()
      googleMap.uiSettings.isMapToolbarEnabled = false

      val preferences = PreferenceManager.getDefaultSharedPreferences(context)
      googleMap.mapType = preferences.getInt(context.getString(R.string.baseLayerKey), context.resources.getInteger(R.integer.baseLayerDefaultValue))

      val dayNightMode: Int = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
      if (dayNightMode == Configuration.UI_MODE_NIGHT_NO) {
        googleMap.setMapStyle(null)
      } else {
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_theme_night))
      }

      mapInitialized = true
    }
  }

  val scope = rememberCoroutineScope()
  AndroidView({ map }) { mapView ->
    scope.launch {
      val googleMap = mapView.awaitMap()
      googleMap.clear()
      if (location.geometry.geometryType == GeometryType.POINT) {
        val centroid = GeometryUtils.getCentroid(location.geometry)
        val point = LatLng(centroid.y, centroid.x)
        googleMap.addMarker {
          position(point)

          val color: Int = Color.parseColor("#1E88E5")
          val hsv = FloatArray(3)
          Color.colorToHSV(color, hsv)
          icon(BitmapDescriptorFactory.defaultMarker(hsv[0]))
        }
      } else {
        val shape = GoogleMapShapeConverter().toShape(location.geometry).shape
        if (shape is PolylineOptions) {
          googleMap.addPolyline {
            addAll(shape.points)
            geodesic(MapShapeObservation.GEODESIC)
          }
        } else if (shape is PolygonOptions) {
          googleMap.addPolygon {
            addAll(shape.points)
            for (hole in shape.holes) {
              addHole(hole)
            }

            geodesic(MapShapeObservation.GEODESIC)
          }
        }
      }

      googleMap.moveCamera(location.getCameraUpdate(mapView))
    }
  }
}


@Composable
fun rememberMapViewWithLifecycle(): MapView {
  val context = LocalContext.current
  val mapView = remember {
    MapView(context).apply {
      id = R.id.map
    }
  }

  // Makes MapView follow the lifecycle of this composable
  val lifecycleObserver = rememberMapLifecycleObserver(mapView)
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  DisposableEffect(lifecycle) {
    lifecycle.addObserver(lifecycleObserver)
    onDispose {
      lifecycle.removeObserver(lifecycleObserver)
    }
  }

  return mapView
}

@Composable
private fun rememberMapLifecycleObserver(mapView: MapView): LifecycleEventObserver =
  remember(mapView) {
    LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
        Lifecycle.Event.ON_START -> mapView.onStart()
        Lifecycle.Event.ON_RESUME -> mapView.onResume()
        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
        Lifecycle.Event.ON_STOP -> mapView.onStop()
        Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
        else -> throw IllegalStateException()
      }
    }
  }