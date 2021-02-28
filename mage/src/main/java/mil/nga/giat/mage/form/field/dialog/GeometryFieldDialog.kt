package mil.nga.giat.mage.form.field.dialog

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.preference.PreferenceManager
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.dialog_geometry_field.*
import mil.nga.geopackage.map.geom.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.coordinate.CoordinateSystem
import mil.nga.giat.mage.form.FormField
import mil.nga.giat.mage.form.FormViewModel
import mil.nga.giat.mage.map.MapUtils
import mil.nga.giat.mage.observation.InputFilterDecimal
import mil.nga.giat.mage.observation.MapShapeObservation
import mil.nga.giat.mage.observation.ObservationLocation
import mil.nga.giat.mage.observation.ObservationShapeStyle
import mil.nga.mgrs.MGRS
import mil.nga.mgrs.gzd.MGRSTileProvider
import mil.nga.sf.*
import mil.nga.sf.Polygon
import mil.nga.sf.proj.ProjectionConstants
import mil.nga.sf.util.GeometryEnvelopeBuilder
import mil.nga.sf.util.GeometryUtils
import java.text.DecimalFormat
import java.text.ParseException
import java.util.*

/**
 * Created by wnewman on 2/9/17.
 */

class GeometryFieldDialog : DialogFragment(),
        GoogleMap.OnMapClickListener,
        GoogleMap.OnCameraMoveStartedListener,
        GoogleMap.OnCameraMoveListener,
        GoogleMap.OnCameraIdleListener,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerClickListener,
        OnMapReadyCallback,
        View.OnClickListener,
        CoordinateChangeListener,
        TabLayout.OnTabSelectedListener
{
    private lateinit var model: FormViewModel

    private var location: ObservationLocation = ObservationLocation(ObservationLocation.MANUAL_PROVIDER, LatLng(0.0, 0.0))
    private var newDrawing: Boolean = false
    private var markerBitmap: Bitmap? = null

    private lateinit var map: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private var onCameraMoveReason: Int = 0
    private lateinit var wgs84CoordinateFragment: WGS84CoordinateFragment
    private lateinit var mgrsCoordinateFragment: MGRSCoordinateFragment
    private var mgrsTileOverlay: TileOverlay? = null

    private var shapeType = GeometryType.POINT
    private var selectedMarker: Marker? = null
    private var shapeMarkers: GoogleMapShapeMarkers? = null
    private lateinit var editMarkerOptions: MarkerOptions
    private lateinit var editPolylineOptions: PolylineOptions
    private lateinit var editPolygonOptions: PolygonOptions
    private var isRectangle = false
    private var rectangleSameXMarker: Marker? = null
    private var rectangleSameYMarker: Marker? = null
    private var rectangleSameXSide1: Boolean = false
    private val shapeConverter = GoogleMapShapeConverter()

    private var fieldKey: String? = null
    private var field: FormField<ObservationLocation>? = null

    private val locationFormatter = DecimalFormat("0.000000")

    companion object {
        private val FORM_FIELD_KEY_EXTRA = "FORM_FIELD_KEY_EXTRA"
        private var LOCATION_EXTRA = "LOCATION"
        private var MARKER_BITMAP_EXTRA = "MARKER_BITMAP"
        private var NEW_OBSERVATION_EXTRA = "NEW_OBSERVATION"
        private val DEFAULT_MARKER_ASSET = "markers/default.png"

        private val WGS84_COORDINATE_TAB_POSITION = 0
        private val MGRS_COORDINATE_TAB_POSITION = 1

        @JvmOverloads
        fun newInstance(fieldKey: String? = null): GeometryFieldDialog {
            val fragment = GeometryFieldDialog()
            fragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0)

            if (fieldKey != null) {
                val bundle = Bundle()
                bundle.putString(FORM_FIELD_KEY_EXTRA, fieldKey)
                fragment.arguments = bundle
            }

            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, R.style.AppTheme_Dialog_Fullscreen)

        model = activity?.run {
            ViewModelProvider(this).get(FormViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        val fieldKey = arguments?.getString(FORM_FIELD_KEY_EXTRA, null)
        field = if (fieldKey == null) model.getLocation().value else model.getField(fieldKey) as FormField<ObservationLocation>?

        // TODO remove this once we only get location from field/model
        arguments?.getParcelable<ObservationLocation>(LOCATION_EXTRA)?.let {
            location = it
        }

        field?.value?.let {
            location = it
        }

        markerBitmap = arguments?.getParcelable(MARKER_BITMAP_EXTRA)
        if (markerBitmap == null) {
            val stream = requireContext().assets.open(DEFAULT_MARKER_ASSET)
            markerBitmap = BitmapFactory.decodeStream(stream)
        }

        newDrawing = arguments?.getBoolean(NEW_OBSERVATION_EXTRA, false) ?: false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_geometry_field, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val wgs84CoordinateFragment = childFragmentManager.findFragmentById(R.id.wgs84CoordinateFragment) as WGS84CoordinateFragment
        this.wgs84CoordinateFragment = wgs84CoordinateFragment

        val mgrsCoordinateFragment = childFragmentManager.findFragmentById(R.id.mgrsCoordinateFragment) as MGRSCoordinateFragment
        this.mgrsCoordinateFragment = mgrsCoordinateFragment

        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp)
        toolbar.setNavigationOnClickListener { dismiss() }
        toolbar.inflateMenu(R.menu.location_edit_menu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.apply -> {
                    updateLocation()
                    true
                }
                else -> super.onOptionsItemSelected(it)
            }
        }

        tabs.addTab(tabs.newTab().setText("Lat/Lng"), WGS84_COORDINATE_TAB_POSITION)
        tabs.addTab(tabs.newTab().setText("MGRS"), MGRS_COORDINATE_TAB_POSITION)

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val defaultCoordinateSystem = preferences.getInt(resources.getString(R.string.coordinateSystemViewKey), CoordinateSystem.WGS84.preferenceValue)
        when (CoordinateSystem.get(preferences.getInt(resources.getString(R.string.coordinateSystemEditKey), defaultCoordinateSystem))) {
            CoordinateSystem.MGRS -> {
                childFragmentManager.beginTransaction()
                    .show(mgrsCoordinateFragment)
                    .hide(wgs84CoordinateFragment)
                    .commit()

                tabs.getTabAt(MGRS_COORDINATE_TAB_POSITION)?.select()
            }
            else -> {
                childFragmentManager.beginTransaction()
                    .show(wgs84CoordinateFragment)
                    .hide(mgrsCoordinateFragment)
                    .commit()

                tabs.getTabAt(WGS84_COORDINATE_TAB_POSITION)?.select()
            }
        }

        tabs.addOnTabSelectedListener(this)

        val style = ObservationShapeStyle(context)
        editMarkerOptions = getEditMarkerOptions()
        editPolylineOptions = getEditPolylineOptions(style)
        editPolygonOptions = getEditPolygonOptions(style)

        mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onAttachFragment(fragment: Fragment) {
        if (fragment is WGS84CoordinateFragment) {
            fragment.coordinateChangeListener = this
        } else if (fragment is MGRSCoordinateFragment) {
            fragment.coordinateChangeListener = this
        }
    }

    override fun onMapReady(map: GoogleMap) {
        this.map = map
        map.setOnCameraIdleListener(this)

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        map.mapType = preferences.getInt(getString(R.string.baseLayerKey), resources.getInteger(R.integer.baseLayerDefaultValue))

        val dayNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (dayNightMode == Configuration.UI_MODE_NIGHT_NO) {
            map.setMapStyle(null)
        } else {
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_theme_night))
        }

        // Don't wait for map to show up to init these values, otherwise bottomsheet will jitter
        val showMgrs = preferences.getBoolean(resources.getString(R.string.showMGRSKey), false)
        if (showMgrs) {
            mgrsTileOverlay = map.addTileOverlay(TileOverlayOptions().tileProvider(MGRSTileProvider(context)))
        }
    }

    override fun onCameraMoveStarted(reason: Int) {
        onCameraMoveReason = reason
    }

    override fun onCameraMove() {
        if (onCameraMoveReason != GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION) {
            // Points are represented by the camera position
            if (shapeType == GeometryType.POINT) {
                clearCoordinateFocus()
                val position = map.cameraPosition
                updateLatitudeLongitudeText(position.target)
            }
        }
    }

    override fun onCameraIdle() {
        map.setOnCameraIdleListener(null)
        setupMap()
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        val position = tab.position

        if (position == WGS84_COORDINATE_TAB_POSITION) {
            childFragmentManager.beginTransaction()
                .show(wgs84CoordinateFragment as Fragment)
                .hide(mgrsCoordinateFragment as Fragment)
                .commit()

            if (!wgs84CoordinateFragment.setLatLng(mgrsCoordinateFragment.getLatLng())) {
                map.moveCamera(CameraUpdateFactory.newLatLng(wgs84CoordinateFragment.getLatLng()))
            }

            mgrsTileOverlay?.remove()
        } else if (position == MGRS_COORDINATE_TAB_POSITION) {
            childFragmentManager.beginTransaction()
                .show(mgrsCoordinateFragment)
                .hide(wgs84CoordinateFragment)
                .commit()

            if (!mgrsCoordinateFragment.setLatLng(wgs84CoordinateFragment.getLatLng())) {
                map.moveCamera(CameraUpdateFactory.newLatLng(mgrsCoordinateFragment.getLatLng()))
            }

            mgrsTileOverlay = map.addTileOverlay(TileOverlayOptions().tileProvider(MGRSTileProvider(context)))
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {}

    override fun onTabReselected(tab: TabLayout.Tab) {}

    override fun onMapClick(latLng: LatLng) {
        clearCoordinateFocus()
    }

    override fun onMarkerDrag(marker: Marker) {
        updateLatitudeLongitudeText(marker.position)
        updateShape(marker)
    }

    override fun onMarkerDragEnd(marker: Marker) {
        updateLatitudeLongitudeText(marker.position)
        updateShape(marker)
        if (isRectangle) {
            shapeMarkers?.setVisibleMarkers(true)
        }
        updateHint()
    }

    override fun onMarkerDragStart(marker: Marker) {
        clearCoordinateFocus()
        updateHint(true)
        vibrate(resources.getInteger(R.integer.shape_edit_drag_long_click_vibrate).toLong())
        selectShapeMarker(marker)
        if (isRectangle) {
            shapeMarkers?.setVisibleMarkers(false)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onMapLongClick(point: LatLng) {

        // Add a new point to a line or polygon
        if (shapeType != GeometryType.POINT) {

            if (!isRectangle) {
                vibrate(resources.getInteger(R.integer.shape_edit_add_long_click_vibrate).toLong())

                if (shapeMarkers == null) {
                    var geometry: Geometry? = null
                    val firstPoint = Point(point.longitude, point.latitude)
                    when (shapeType) {
                        GeometryType.LINESTRING -> {
                            val lineString = LineString()
                            lineString.addPoint(firstPoint)
                            geometry = lineString
                        }
                        GeometryType.POLYGON -> {
                            val polygon = Polygon()
                            val ring = LineString()
                            ring.addPoint(firstPoint)
                            polygon.addRing(ring)
                            geometry = polygon
                        }
                        else -> throw IllegalArgumentException("Unsupported Geometry Type: $shapeType")
                    }
                    addMapShape(geometry)
                } else {
                    val markerOptions = getEditMarkerOptions()
                    markerOptions.position(point)
                    val marker = map.addMarker(markerOptions)
                    var shape: ShapeMarkers? = null
                    val mapShape = shapeMarkers?.getShape()
                    when (mapShape?.getShapeType()) {
                        GoogleMapShapeType.POLYLINE_MARKERS -> {
                            val polylineMarkers = mapShape.getShape() as PolylineMarkers
                            shape = polylineMarkers
                            if (newDrawing) {
                                polylineMarkers.add(marker)
                            } else {
                                polylineMarkers.addNew(marker)
                            }
                        }
                        GoogleMapShapeType.POLYGON_MARKERS -> {
                            val polygonMarkers = shapeMarkers?.getShape()?.getShape() as PolygonMarkers
                            shape = polygonMarkers
                            if (newDrawing) {
                                polygonMarkers.add(marker)
                            } else {
                                polygonMarkers.addNew(marker)
                            }
                        }
                        else -> throw IllegalArgumentException("Unsupported Shape Type: " + mapShape?.getShapeType())
                    }
                    shapeMarkers?.add(marker, shape)
                    selectShapeMarker(marker)
                    updateShape(marker)
                }
            } else if (!shapeMarkersValid() && selectedMarker != null) {
                // Allow long click to expand a zero area rectangle
                vibrate(resources.getInteger(R.integer.shape_edit_add_long_click_vibrate).toLong())
                selectedMarker!!.setPosition(point)
                updateShape(selectedMarker!!)
                updateHint()
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onMarkerClick(marker: Marker): Boolean {
        var handled = false

        // Selecting and selected options for line and polygon markers
        if (shapeType != GeometryType.POINT) {
            val shape = shapeMarkers?.getShapeMarkers(marker)
            if (shape != null) {

                if (selectedMarker?.getId() != marker.id) {
                    selectShapeMarker(marker)
                } else if (!isRectangle && shapeMarkers != null && shapeMarkers!!.size() > 1) {

                    val deleteDialog = AlertDialog.Builder(requireActivity())
                    val position = marker.position
                    deleteDialog.setTitle(R.string.location_edit_delete_point_title)
                    deleteDialog.setMessage(String.format(getString(R.string.location_edit_delete_point_message),
                            locationFormatter.format(position.latitude), locationFormatter.format(position.longitude)))
                    deleteDialog.setNegativeButton(R.string.cancel, null)
                    deleteDialog.setPositiveButton(R.string.delete) { dialog, which ->
                        val markers = getShapeMarkers()

                        // Find the index of the marker being deleted
                        var index = 1
                        for (i in markers.indices) {
                            if (markers.get(i) == marker) {
                                index = i
                                break
                            }
                        }
                        // Get the previous marker index
                        if (index > 0) {
                            index--
                        } else if (shapeType == GeometryType.LINESTRING) {
                            // Select next marker in the line
                            index++
                        } else {
                            // Select previous polygon marker
                            index = markers.size - 1
                        }
                        // Get the new marker to select
                        val selectMarker = markers.get(index)

                        // Delete the marker, select the new, and update the shape
                        shapeMarkers?.delete(marker)
                        selectedMarker = null
                        selectShapeMarker(selectMarker)
                        updateShape(selectMarker)
                        updateHint()
                    }
                    deleteDialog.show()

                }

                handled = true
            }
        }

        return handled
    }

    override fun onCoordinateChanged(coordinate: LatLng) {
        map.moveCamera(CameraUpdateFactory.newLatLng(coordinate))
        if (selectedMarker != null) {
            selectedMarker!!.setPosition(coordinate)
            updateShape(selectedMarker!!)
        }
    }

    override fun onCoordinateChangeStart(coordinate: LatLng) {
        // Move the camera to a selected line or polygon marker
        if (shapeType != GeometryType.POINT && selectedMarker != null) {
            map.moveCamera(CameraUpdateFactory.newLatLng(selectedMarker!!.getPosition()))
        }

        updateHint()
    }

    override fun onCoordinateChangeEnd(coordinate: LatLng) {
        updateHint()
    }

    private fun setupMap() {
        map.moveCamera(location.getCameraUpdate(mapFragment.getView()))

        if (tabs.getSelectedTabPosition() == MGRS_COORDINATE_TAB_POSITION) {
            mgrsTileOverlay = map.addTileOverlay(TileOverlayOptions().tileProvider(MGRSTileProvider(context)))
        }

        map.getUiSettings().isCompassEnabled = false
        map.getUiSettings().isRotateGesturesEnabled = false
        map.setOnCameraMoveListener(this)
        map.setOnCameraMoveStartedListener(this)
        map.setOnMapClickListener(this)
        map.setOnMapLongClickListener(this)
        map.setOnMarkerClickListener(this)
        map.setOnMarkerDragListener(this)

        setupMapButton(editPointButton)
        setupMapButton(editLineButton)
        setupMapButton(editRectangleButton)
        setupMapButton(editPolygonButton)

        val geometry = location.getGeometry()
        setShapeType(geometry)
        addMapShape(geometry)
    }

    private fun setupMapButton(button: FloatingActionButton) {
        val drawable = DrawableCompat.wrap(button.drawable)
        button.setImageDrawable(drawable)
        DrawableCompat.setTintList(drawable, AppCompatResources.getColorStateList(requireContext(), R.color.toggle_button_selected))
        button.setOnClickListener(this)
    }

    /**
     * Clear the focus from the coordinate text entries
     */
    private fun clearCoordinateFocus() {
        val tabPosition = tabs.getSelectedTabPosition()
        if (tabPosition == WGS84_COORDINATE_TAB_POSITION) {
            wgs84CoordinateFragment.clearFocus()
        } else if (tabPosition == MGRS_COORDINATE_TAB_POSITION) {
            mgrsCoordinateFragment.clearFocus()
        }
    }

    /**
     * Update the current shape type
     *
     * @param geometry geometry
     */
    private fun setShapeType(geometry: Geometry) {
        shapeType = geometry.geometryType
        checkIfRectangle(geometry)
        setShapeTypeSelection()
    }

    /**
     * Check if the geometry is a rectangle polygon
     *
     * @param geometry geometry
     */
    private fun checkIfRectangle(geometry: Geometry) {
        isRectangle = false
        if (geometry.geometryType == GeometryType.POLYGON) {
            val polygon = geometry as Polygon
            val ring = polygon.rings[0]
            val points = ring.points
            updateIfRectangle(points)
        }
    }

    /**
     * Check if the points form a rectangle and update
     *
     * @param points points
     */
    private fun updateIfRectangle(points: List<Point>) {
        val size = points.size
        if (size == 4 || size == 5) {
            val point1 = points[0]
            val lastPoint = points[points.size - 1]
            val closed = point1.x == lastPoint.x && point1.y == lastPoint.y
            if (closed && size == 5 || !closed && size == 4) {
                val point2 = points[1]
                val point3 = points[2]
                val point4 = points[3]
                if (point1.x == point2.x && point2.y == point3.y) {
                    if (point1.y == point4.y && point3.x == point4.x) {
                        isRectangle = true
                        rectangleSameXSide1 = true
                    }
                } else if (point1.y == point2.y && point2.x == point3.x) {
                    if (point1.x == point4.x && point3.y == point4.y) {
                        isRectangle = true
                        rectangleSameXSide1 = false
                    }
                }
            }
        }
    }

    /**
     * Revert the selected shape type to the current shape type
     */
    private fun revertShapeType() {
        setShapeTypeSelection()
    }

    /**
     * Set the shape type selection to match the current shape type
     */
    private fun setShapeTypeSelection() {
        editPointButton.setSelected(shapeType == GeometryType.POINT)
        editLineButton.setSelected(shapeType == GeometryType.LINESTRING)
        editRectangleButton.setSelected(shapeType == GeometryType.POLYGON && isRectangle)
        editPolygonButton.setSelected(shapeType == GeometryType.POLYGON && !isRectangle)
    }

    override fun onClick(v: View) {
        var newShapeType: GeometryType? = null
        var newRectangle = false
        when (v.id) {
            R.id.editPointButton -> newShapeType = GeometryType.POINT
            R.id.editLineButton -> newShapeType = GeometryType.LINESTRING
            R.id.editPolygonButton -> newShapeType = GeometryType.POLYGON
            R.id.editRectangleButton -> {
                newRectangle = true
                newShapeType = GeometryType.POLYGON
            }
        }

        if (newShapeType != null) {
            confirmAndChangeShapeType(newShapeType, newRectangle)
        }
    }

    /**
     * If a new shape type was selected, confirm data loss changes and change the shape
     *
     * @param selectedType      newly selected shape type
     * @param selectedRectangle true if a rectangle polygon
     */
    private fun confirmAndChangeShapeType(selectedType: GeometryType, selectedRectangle: Boolean) {

        // Only care if not the current shape type
        if (selectedType != shapeType || selectedRectangle != isRectangle) {

            clearCoordinateFocus()

            var title: String? = null
            var message: String? = null

            // Changing to a point or rectangle, and there are multiple unique positions in the shape
            if ((selectedType == GeometryType.POINT || selectedRectangle) && multipleShapeMarkerPositions()) {

                if (selectedRectangle) {
                    // Changing to a rectangle
                    val markers = getShapeMarkers()
                    var formRectangle = false
                    if (markers.size == 4 || markers.size == 5) {
                        val points = ArrayList<Point>()
                        for (marker in markers) {
                            points.add(shapeConverter.toPoint(marker.getPosition()))
                        }
                        formRectangle = ObservationLocation.checkIfRectangle(points)
                    }
                    if (!formRectangle) {
                        // Points currently do not form a rectangle
                        title = getString(R.string.location_edit_to_rectangle_title)
                        message = getString(R.string.location_edit_to_rectangle_message)
                    }

                } else {
                    // Changing to a point
                    val newPointPosition = getShapeToPointLocation()
                    title = getString(R.string.location_edit_to_point_title)
                    message = String.format(getString(R.string.location_edit_to_point_message),
                            locationFormatter.format(newPointPosition.latitude), locationFormatter.format(newPointPosition.longitude))
                }

            }

            // If changing to a point and there are multiple points in the current shape, confirm selection
            if (message != null) {

                val changeDialog = AlertDialog.Builder(requireActivity())
                changeDialog.setTitle(title)
                changeDialog.setMessage(message)
                changeDialog.setNegativeButton(R.string.cancel
                ) { dialog, which -> revertShapeType() }
                changeDialog.setOnCancelListener { revertShapeType() }
                changeDialog.setPositiveButton(R.string.change
                ) { dialog, which -> changeShapeType(selectedType, selectedRectangle) }
                changeDialog.show()

            } else {
                changeShapeType(selectedType, selectedRectangle)
            }
        }
    }

    /**
     * Change the current shape type
     *
     * @param selectedType      newly selected shape type
     * @param selectedRectangle true if a rectangle polygon
     */
    private fun changeShapeType(selectedType: GeometryType, selectedRectangle: Boolean) {

        isRectangle = selectedRectangle

        var geometry: Geometry? = null

        // Changing from point to a shape
        if (shapeType == GeometryType.POINT) {
            val center = map.getCameraPosition().target
            val firstPoint = Point(center.longitude, center.latitude)
            val lineString = LineString()
            lineString.addPoint(firstPoint)
            // Changing to a rectangle
            if (selectedRectangle) {
                // Closed rectangle polygon all at the same point
                lineString.addPoint(firstPoint)
                lineString.addPoint(firstPoint)
                lineString.addPoint(firstPoint)
                lineString.addPoint(firstPoint)
            } else {
                newDrawing = true
            }// Changing to a line or polygon
            when (selectedType) {
                GeometryType.LINESTRING -> geometry = lineString
                GeometryType.POLYGON -> {
                    val polygon = Polygon()
                    polygon.addRing(lineString)
                    geometry = polygon
                }
                else -> throw IllegalArgumentException("Unsupported Geometry Type: $selectedType")
            }
        } else if (selectedType == GeometryType.POINT) {
            val newPointPosition = getShapeToPointLocation()
            geometry = Point(newPointPosition.longitude, newPointPosition.latitude)
            map.moveCamera(CameraUpdateFactory.newLatLng(newPointPosition))
            newDrawing = false
        } else {

            var lineString: LineString? = null
            if (shapeMarkers != null) {

                var markers = getShapeMarkers()

                // If all markers are in the same spot only keep one
                if (!markers.isEmpty() && !multipleMarkerPositions(markers)) {
                    markers = markers.subList(0, 1)
                }

                // Add each marker location and find the selected marker index
                var latLngPoints: MutableList<LatLng> = ArrayList()
                var startLocation: Int? = null
                for (marker in markers) {
                    if (startLocation == null && selectedMarker != null && selectedMarker == marker) {
                        startLocation = latLngPoints.size
                    }
                    latLngPoints.add(marker.getPosition())
                }

                // When going from the polygon or rectangle to a line
                if (selectedType == GeometryType.LINESTRING) {
                    // Break the polygon closure when changing to a line
                    if (latLngPoints.size > 1 && latLngPoints[0] == latLngPoints[latLngPoints.size - 1]) {
                        latLngPoints.removeAt(latLngPoints.size - 1)
                    }
                    // Break the line apart at the selected location
                    if (startLocation != null && startLocation < latLngPoints.size) {
                        val latLngPointsTemp = ArrayList<LatLng>()
                        latLngPointsTemp.addAll(latLngPoints.subList(startLocation, latLngPoints.size))
                        latLngPointsTemp.addAll(latLngPoints.subList(0, startLocation))
                        latLngPoints = latLngPointsTemp
                    }
                }

                lineString = shapeConverter.toLineString(latLngPoints)
            } else {
                val newPointPosition = getShapeToPointLocation()
                lineString = LineString()
                lineString.addPoint(shapeConverter.toPoint(newPointPosition))
            }

            when (selectedType) {

                GeometryType.LINESTRING -> {
                    newDrawing = lineString!!.points.size <= 1
                    geometry = lineString
                }

                GeometryType.POLYGON -> {

                    // If converting to a rectangle, use the current shape bounds
                    if (selectedRectangle) {
                        val lineStringCopy = lineString!!.copy() as LineString
                        GeometryUtils.minimizeGeometry(lineStringCopy, ProjectionConstants.WGS84_HALF_WORLD_LON_WIDTH)
                        val envelope = GeometryEnvelopeBuilder.buildEnvelope(lineStringCopy)
                        lineString = LineString()
                        lineString.addPoint(Point(envelope.minX, envelope.maxY))
                        lineString.addPoint(Point(envelope.minX, envelope.minY))
                        lineString.addPoint(Point(envelope.maxX, envelope.minY))
                        lineString.addPoint(Point(envelope.maxX, envelope.maxY))
                        lineString.addPoint(Point(envelope.minX, envelope.maxY))
                        updateIfRectangle(lineString.points)
                    }

                    val polygon = Polygon()
                    polygon.addRing(lineString)
                    newDrawing = lineString!!.points.size <= 2
                    geometry = polygon
                }

                else -> throw IllegalArgumentException("Unsupported Geometry Type: $selectedType")
            }
        }// Changing from between a line, polygon, and rectangle
        // Changing from line or polygon to a point

        shapeType = selectedType
        addMapShape(geometry)
        setShapeTypeSelection()
    }

    /**
     * Get the best single point when converting from a line or polygon to a point.
     * This is either the current selected marker, the current lat & lon values, or map position
     *
     * @return single point location
     */
    private fun getShapeToPointLocation(): LatLng {
        var newPointPosition: LatLng? = null

        return if (selectedMarker != null) {
             selectedMarker!!.getPosition()
        } else {
            val latLng: LatLng? = null
            val tabPosition = tabs.getSelectedTabPosition()
            if (tabPosition == WGS84_COORDINATE_TAB_POSITION) {
                wgs84CoordinateFragment.clearFocus()
                newPointPosition = wgs84CoordinateFragment.getLatLng()
            } else if (tabPosition == MGRS_COORDINATE_TAB_POSITION) {
                mgrsCoordinateFragment.clearFocus()
                newPointPosition = mgrsCoordinateFragment.getLatLng()
            }

            if (newPointPosition == null) {
                val position = map.getCameraPosition()
                LatLng(position.target.latitude, position.target.longitude)
            } else {
                newPointPosition
            }
        }
    }

    /**
     * Add the geometry to the map as the current editing observation location, cleaning up existing shape
     *
     * @param geometry new geometry
     */
    private fun addMapShape(geometry: Geometry) {

        var previousSelectedMarkerLocation: LatLng? = null
        if (selectedMarker != null) {
            previousSelectedMarkerLocation = selectedMarker?.getPosition()
            selectedMarker = null
            clearRectangleCorners()
        }
        if (shapeMarkers != null) {
            shapeMarkers?.remove()
            shapeMarkers = null
        }
        if (geometry.geometryType == GeometryType.POINT) {
            locationEditMarker.setImageBitmap(markerBitmap)
            val point = geometry as Point
            updateLatitudeLongitudeText(LatLng(point.y, point.x))
        } else {
            locationEditMarker.setImageBitmap(null)
            val shape = shapeConverter.toShape(geometry)
            shapeMarkers = shapeConverter.addShapeToMapAsMarkers(map, shape, null,
                    editMarkerOptions, editMarkerOptions, null, editPolylineOptions, editPolygonOptions)
            val markers = getShapeMarkers()
            var selectMarker = markers.get(0)
            if (previousSelectedMarkerLocation != null) {
                for (marker in markers) {
                    if (marker.getPosition() == previousSelectedMarkerLocation) {
                        selectMarker = marker
                        break
                    }
                }
            }
            selectShapeMarker(selectMarker)
        }

        updateHint()
    }

    /**
     * Update the hint text
     */
    private fun updateHint() {
        updateHint(false)
    }

    /**
     * Update the hint text
     *
     * @param dragging true if a point is currently being dragged
     */
    private fun updateHint(dragging: Boolean) {

        var locationEditHasFocus = false
        val tabPosition = tabs.getSelectedTabPosition()
        if (tabPosition == WGS84_COORDINATE_TAB_POSITION) {
            locationEditHasFocus = wgs84CoordinateFragment.hasFocus()
        } else if (tabPosition == MGRS_COORDINATE_TAB_POSITION) {
            locationEditHasFocus = mgrsCoordinateFragment.hasFocus()
        }

        var hint = ""

        when (shapeType) {
            GeometryType.POINT -> if (locationEditHasFocus) {
                hint = getString(R.string.location_edit_hint_point_edit)
            } else {
                hint = getString(R.string.location_edit_hint_point)
            }
            GeometryType.POLYGON -> {
                if (isRectangle) {
                    if (locationEditHasFocus) {
                        hint = getString(R.string.location_edit_hint_rectangle_edit)
                    } else if (dragging) {
                        hint = getString(R.string.location_edit_hint_rectangle_drag)
                    } else if (!multipleShapeMarkerPositions()) {
                        hint = getString(R.string.location_edit_hint_rectangle_new)
                    } else {
                        hint = getString(R.string.location_edit_hint_rectangle)
                    }
                } else {
                    if (locationEditHasFocus) {
                        hint = getString(R.string.location_edit_hint_shape_edit)
                    } else if (dragging) {
                        hint = getString(R.string.location_edit_hint_shape_drag)
                    } else if (newDrawing) {
                        hint = getString(R.string.location_edit_hint_shape_new)
                    } else {
                        hint = getString(R.string.location_edit_hint_shape)
                    }
                }
            }
            GeometryType.LINESTRING -> if (locationEditHasFocus) {
                hint = getString(R.string.location_edit_hint_shape_edit)
            } else if (dragging) {
                hint = getString(R.string.location_edit_hint_shape_drag)
            } else if (newDrawing) {
                hint = getString(R.string.location_edit_hint_shape_new)
            } else {
                hint = getString(R.string.location_edit_hint_shape)
            }
        }

        hintText.setText(hint)
    }

    /**
     * Update the latitude and longitude text entries
     *
     * @param latLng lat lng point
     */
    private fun updateLatitudeLongitudeText(latLng: LatLng) {
        wgs84CoordinateFragment.setLatLng(latLng)
        mgrsCoordinateFragment.setLatLng(latLng)
    }

    /**
     * Update the location into the response
     */
    private fun updateLocation() {
        // Save coordinate system used for edit
        val coordinateSystem = if (tabs.getSelectedTabPosition() == 0) CoordinateSystem.WGS84 else CoordinateSystem.MGRS
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putInt(resources.getString(R.string.coordinateSystemEditKey), coordinateSystem.preferenceValue).apply()

        val geometry = convertToGeometry() ?: return
        location.setGeometry(geometry)
        location.setProvider(ObservationLocation.MANUAL_PROVIDER)
        location.setAccuracy(0.0f)
        location.setTime(System.currentTimeMillis())

        field?.value = location

        dismiss()
    }

    private fun convertToGeometry(): Geometry? {

        // validate shape has minimum number of points
        if (shapeType == GeometryType.POLYGON && (!multipleShapeMarkerPositions() || getShapeMarkers().size < 3)) {
            val errorMessage = if (isRectangle) getString(R.string.location_edit_error_rectangle_min_points) else getString(R.string.location_edit_error_polygon_min_points)
            Snackbar.make(coordinatorLayout, errorMessage, Snackbar.LENGTH_SHORT).show()
            return null
        } else if (shapeType == GeometryType.LINESTRING && !multipleShapeMarkerPositions()) {
            Snackbar.make(coordinatorLayout, getString(R.string.location_edit_error_linestring_min_points), Snackbar.LENGTH_SHORT).show()
            return null
        }

        val geometry: Geometry
        if (shapeType == GeometryType.POINT) {
            val center = map.getCameraPosition().target
            geometry = Point(center.longitude, center.latitude)
        } else {
            // general shape validity test
            if (!(shapeMarkers?.isValid() ?: false)) {
                Snackbar.make(coordinatorLayout, getString(R.string.location_edit_error_shape), Snackbar.LENGTH_SHORT).show()
                return null
            }

            geometry = shapeConverter.toGeometry(shapeMarkers?.getShape())

            // validate polygon does not intersect itself
            if (shapeType == GeometryType.POLYGON && MapUtils.polygonHasKinks(geometry as Polygon)) {
                Snackbar.make(coordinatorLayout, getString(R.string.location_edit_error_polygon_kinks), Snackbar.LENGTH_SHORT).show()
                return null
            }
        }

        return geometry
    }


    /**
     * Find the neighboring rectangle corners
     *
     * @param marker selected marker
     */
    private fun findRectangleCorners(marker: Marker) {
        clearRectangleCorners()
        if (shapeMarkers != null && isRectangle) {
            val markers = getShapeMarkers()
            var afterMatchesX = rectangleSameXSide1
            for (i in markers.indices) {
                val shapeMarker = markers.get(i)
                if (shapeMarker.getId() == marker.id) {
                    val beforeIndex = if (i > 0) i - 1 else markers.size - 1
                    val afterIndex = if (i < markers.size - 1) i + 1 else 0
                    val before = markers.get(beforeIndex)
                    val after = markers.get(afterIndex)
                    if (afterMatchesX) {
                        rectangleSameXMarker = after
                        rectangleSameYMarker = before
                    } else {
                        rectangleSameXMarker = before
                        rectangleSameYMarker = after
                    }
                }
                afterMatchesX = !afterMatchesX
            }
        }
    }

    /**
     * Update the neighboring rectangle corners from the modified marker
     *
     * @param marker modified marker
     */
    private fun updateRectangleCorners(marker: Marker) {
        if (rectangleSameXMarker != null) {
            rectangleSameXMarker!!.setPosition(
                    LatLng(rectangleSameXMarker!!.getPosition().latitude, marker.position.longitude))
        }
        if (rectangleSameYMarker != null) {
            rectangleSameYMarker!!.setPosition(
                    LatLng(marker.position.latitude, rectangleSameYMarker!!.getPosition().longitude))
        }
    }

    /**
     * Clear the rectangle corners
     */
    private fun clearRectangleCorners() {
        rectangleSameXMarker = null
        rectangleSameYMarker = null
    }

    /**
     * Select the provided shape marker
     *
     * @param marker marker to select
     */
    private fun selectShapeMarker(marker: Marker) {
        clearRectangleCorners()
        if (selectedMarker != null && selectedMarker!!.getId() != marker.id) {
            selectedMarker!!.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_shape_edit))
            selectedMarker!!.setZIndex(0.0f)
        }
        selectedMarker = marker
        updateLatitudeLongitudeText(marker.position)
        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_shape_edit_selected))
        marker.zIndex = 1.0f
        findRectangleCorners(marker)
    }

    /**
     * Update the shape with any modifications, adjust the accept menu button state
     *
     * @param selectedMarker selected marker
     */
    private fun updateShape(selectedMarker: Marker) {
        updateRectangleCorners(selectedMarker)
        if (shapeMarkers != null) {
            shapeMarkers!!.update()
            if (shapeMarkers!!.isEmpty()) {
                shapeMarkers = null
            }
        }
    }

    /**
     * Validate that the shape markers are a valid shape and contain multiple unique positions
     *
     * @return true if valid
     */
    private fun shapeMarkersValid(): Boolean {
        return multipleShapeMarkerPositions() && shapeMarkers?.isValid() ?: false
    }

    /**
     * Determine if there are multiple unique locational positions in the shape markers
     *
     * @return true if multiple positions
     */
    private fun multipleShapeMarkerPositions(): Boolean {
        var multiple = false
        if (shapeMarkers != null) {
            multiple = multipleMarkerPositions(getShapeMarkers())
        }
        return multiple
    }

    /**
     * Determine if the are multiple unique locational positions in the markers
     *
     * @param markers markers
     * @return true if multiple positions
     */
    private fun multipleMarkerPositions(markers: List<Marker>): Boolean {
        var multiple = false
        var position: LatLng? = null
        for (marker in markers) {
            if (position == null) {
                position = marker.position
            } else if (position != marker.position) {
                multiple = true
                break
            }
        }
        return multiple
    }

    /**
     * Get the shape markers
     *
     * @return shape markers
     */
    private fun getShapeMarkers(): List<Marker> {
        return if (shapeMarkers != null) {
            shapeMarkers!!.getShapeMarkersMap().values.iterator().next().getMarkers()
        } else {
            listOf()
        }
    }

    /**
     * Get the marker options for an edit point in a shape
     *
     * @return edit marker options
     */
    private fun getEditMarkerOptions(): MarkerOptions {
        val markerOptions = MarkerOptions()
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_shape_edit))
        markerOptions.anchor(0.5f, 0.5f)
        markerOptions.draggable(true)
        return markerOptions
    }

    /**
     * Get the edit polyline options
     *
     * @param style observation shape style
     * @return edit polyline options
     */
    private fun getEditPolylineOptions(style: ObservationShapeStyle): PolylineOptions {
        val polylineOptions = PolylineOptions()
        polylineOptions.width(style.strokeWidth)
        polylineOptions.color(style.strokeColor)
        polylineOptions.geodesic(MapShapeObservation.GEODESIC)
        return polylineOptions
    }

    /**
     * Get the edit polygon options
     *
     * @param style observation shape style
     * @return edit polygon options
     */
    private fun getEditPolygonOptions(style: ObservationShapeStyle): PolygonOptions {
        val polygonOptions = PolygonOptions()
        polygonOptions.strokeWidth(style.strokeWidth)
        polygonOptions.strokeColor(style.strokeColor)
        polygonOptions.fillColor(style.fillColor)
        polygonOptions.geodesic(MapShapeObservation.GEODESIC)
        return polygonOptions
    }

    private fun vibrate(duration: Long) {
        val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    class WGS84CoordinateFragment : Fragment(), TextWatcher, View.OnFocusChangeListener {
        private val LOCATION_PRECISION = "%.6f"

        var coordinateChangeListener: CoordinateChangeListener? = null

        private lateinit var latitudeEdit: EditText
        private lateinit var longitudeEdit: EditText

        private val LOCATION_MAX_PRECISION = 6

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.wgs84_location_edit, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            latitudeEdit = view.findViewById<View>(R.id.location_edit_latitude) as EditText
            latitudeEdit.filters = arrayOf<InputFilter>(InputFilterDecimal(-90.0, 90.0, LOCATION_MAX_PRECISION))

            longitudeEdit = view.findViewById<View>(R.id.location_edit_longitude) as EditText
            longitudeEdit.filters = arrayOf<InputFilter>(InputFilterDecimal(-180.0, 180.0, LOCATION_MAX_PRECISION))

            longitudeEdit.addTextChangedListener(this)
            longitudeEdit.onFocusChangeListener = this

            latitudeEdit.addTextChangedListener(this)
            latitudeEdit.onFocusChangeListener = this

            latitudeEdit.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    latitudeEdit.clearFocus()
                    return@OnEditorActionListener true
                }
                false
            })

            longitudeEdit.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    longitudeEdit.clearFocus()
                    return@OnEditorActionListener true
                }
                false
            })

            clearFocus()
        }

        override fun onFocusChange(v: View, hasFocus: Boolean) {
            if (v === longitudeEdit || v === latitudeEdit) {
                val latLng = getLatLng()

                if (longitudeEdit.hasFocus() || latitudeEdit.hasFocus()) {
                    coordinateChangeListener!!.onCoordinateChangeStart(latLng)
                } else {
                    val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0)
                    coordinateChangeListener!!.onCoordinateChangeEnd(latLng)
                }
            }
        }

        override fun afterTextChanged(s: Editable) {
            // Only handle when the longitude or latitude entries have focus
            if (!longitudeEdit.hasFocus() && !latitudeEdit.hasFocus()) {
                return
            }

            var latLng = getLatLng()

            if (latLng == null) {
                // TODO might want to show error here...
                latLng = LatLng(0.0, 0.0)
            }

            coordinateChangeListener!!.onCoordinateChanged(latLng)
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        fun setLatLng(latLng: LatLng?): Boolean {
            if (latLng == null) {
                return false
            }

            latitudeEdit.setText(String.format(Locale.getDefault(), LOCATION_PRECISION, latLng.latitude))
            longitudeEdit.setText(String.format(Locale.getDefault(), LOCATION_PRECISION, latLng.longitude))

            return true
        }

        fun getLatLng(): LatLng? {
            val latitudeString = latitudeEdit.text.toString()
            val longitudeString = longitudeEdit.text.toString()

            var latitude: Double? = null
            if (!latitudeString.isEmpty()) {
                try {
                    latitude = java.lang.Double.parseDouble(latitudeString)
                } catch (e: NumberFormatException) {
                }

            }

            var longitude: Double? = null
            if (!longitudeString.isEmpty()) {
                try {
                    longitude = java.lang.Double.parseDouble(longitudeString)
                } catch (e: NumberFormatException) {
                }

            }

            return if (latitude == null || longitude == null) {
                null
            } else LatLng(latitude, longitude)
        }

        fun hasFocus(): Boolean {
            return longitudeEdit.hasFocus() || latitudeEdit.hasFocus()
        }

        fun clearFocus() {
            longitudeEdit.clearFocus()
            latitudeEdit.clearFocus()
        }
    }

    class MGRSCoordinateFragment : Fragment(), TextWatcher, View.OnFocusChangeListener {
        var coordinateChangeListener: CoordinateChangeListener? = null
        private lateinit var mgrsEdit: EditText
        private lateinit var mgrsLayout: TextInputLayout

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.mgrs_location_edit, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            mgrsEdit = view.findViewById<View>(R.id.mgrs) as EditText
            mgrsLayout = view.findViewById<View>(R.id.mgrs_layout) as TextInputLayout

            mgrsEdit.addTextChangedListener(this)
            mgrsEdit.onFocusChangeListener = this
            mgrsEdit.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mgrsEdit.clearFocus()
                    return@OnEditorActionListener true
                }
                false
            })
        }

        override fun afterTextChanged(s: Editable) {
            // Only handle when the mgrs has focus
            if (!mgrsEdit.hasFocus()) {
                return
            }

            val latLng = getLatLng()

            if (latLng == null) {
                mgrsLayout.error = "Invaild MGRS Code"
            } else {
                mgrsLayout.error = null
                mgrsLayout.isErrorEnabled = false
                coordinateChangeListener!!.onCoordinateChanged(latLng)
            }
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun onFocusChange(v: View, hasFocus: Boolean) {
            val latLng = getLatLng()

            if (mgrsEdit.hasFocus()) {
                coordinateChangeListener!!.onCoordinateChangeStart(latLng)
            } else {
                val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.applicationWindowToken, 0)
                coordinateChangeListener!!.onCoordinateChangeEnd(latLng)
            }
        }

        fun setLatLng(latLng: LatLng?): Boolean {
            if (latLng == null) {
                return false
            }

            val mgrs = MGRS.from(mil.nga.mgrs.wgs84.LatLng(latLng.latitude, latLng.longitude))
            mgrsEdit.setText(mgrs.format(5))
            mgrsLayout.error = null
            mgrsLayout.isErrorEnabled = false

            return true
        }

        fun getLatLng(): LatLng? {
            try {
                val latLng = mil.nga.mgrs.wgs84.LatLng.parse(mgrsEdit.text.toString())
                return LatLng(latLng.latitude, latLng.longitude)
            } catch (e: ParseException) {

            }

            return null
        }

        fun hasFocus(): Boolean {
            return mgrsEdit.hasFocus()
        }

        fun clearFocus() {
            mgrsEdit.clearFocus()
        }
    }
}
