package mil.nga.giat.mage.form.edit.dialog

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import mil.nga.geopackage.map.geom.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.coordinate.*
import mil.nga.giat.mage.databinding.DialogGeometryFieldBinding
import mil.nga.giat.mage.map.annotation.ShapeStyle
import mil.nga.giat.mage.map.hasKinks
import mil.nga.giat.mage.observation.InputFilterDecimal
import mil.nga.giat.mage.observation.ObservationLocation
import mil.nga.mgrs.MGRS
import mil.nga.mgrs.tile.MGRSTileProvider
import mil.nga.proj.ProjectionConstants
import mil.nga.sf.*
import mil.nga.sf.Polygon
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
    interface GeometryFieldDialogListener {
        fun onLocation(location: ObservationLocation?)
    }

    private lateinit var binding: DialogGeometryFieldBinding
    var listener: GeometryFieldDialogListener? = null

    private var title = "Location"
    private var clearable = true
    private var location: ObservationLocation = ObservationLocation(ObservationLocation.MANUAL_PROVIDER, LatLng(0.0, 0.0))
    private var newDrawing: Boolean = false
    private var markerBitmap: Bitmap? = null

    private lateinit var map: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private var onCameraMoveReason: Int = 0
    private lateinit var wgs84CoordinateFragment: WGS84CoordinateFragment
    private lateinit var mgrsCoordinateFragment: MGRSCoordinateFragment
    private lateinit var dmsCoordinateFragment: DMSCoordinateFragment
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

    private val locationFormatter = DecimalFormat("0.000000")

    companion object {
        private const val TITLE_KEY = "TITLE_KEY"
        private const val LOCATION_KEY = "LOCATION_KEY"
        private const val INITIAL_LOCATION = "INITIAL_LOCATION"
        private const val CLEARABLE_KEY = "CLEARABLE_KEY"
        private const val MARKER_BITMAP_EXTRA = "MARKER_BITMAP"
        private const val NEW_OBSERVATION_EXTRA = "NEW_OBSERVATION"
        private const val DEFAULT_MARKER_ASSET = "markers/default.png"

        private const val WGS84_COORDINATE_TAB_POSITION = 0
        private const val MGRS_COORDINATE_TAB_POSITION = 1
        private const val DMS_COORDINATE_TAB_POSITION = 2

        @JvmOverloads
        fun newInstance(title: String, location: ObservationLocation?, mapCenter: LatLng? = null, clearable: Boolean = false): GeometryFieldDialog {
            val fragment = GeometryFieldDialog()
            val bundle = Bundle()
            bundle.putString(TITLE_KEY, title)
            bundle.putParcelable(LOCATION_KEY, location)
            bundle.putBoolean(CLEARABLE_KEY, clearable)
            bundle.putParcelable(INITIAL_LOCATION, mapCenter)

            fragment.arguments = bundle

            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, R.style.AppTheme_Dialog_Fullscreen)

        title = requireArguments().getString(TITLE_KEY, "Location")
        clearable = requireArguments().getBoolean(CLEARABLE_KEY)

        val initialLocation: LatLng? = arguments?.getParcelable(INITIAL_LOCATION)
        if (initialLocation != null) {
            this.location = ObservationLocation(ObservationLocation.MANUAL_PROVIDER, initialLocation)
        }

        val location = arguments?.getParcelable(LOCATION_KEY) as? ObservationLocation
        if (location != null) {
            this.location = location
        }

        markerBitmap = arguments?.getParcelable(MARKER_BITMAP_EXTRA)
        if (markerBitmap == null) {
            val stream = requireContext().assets.open(DEFAULT_MARKER_ASSET)
            markerBitmap = BitmapFactory.decodeStream(stream)
        }

        newDrawing = arguments?.getBoolean(NEW_OBSERVATION_EXTRA, false) ?: false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogGeometryFieldBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val wgs84CoordinateFragment = childFragmentManager.findFragmentById(R.id.wgs84CoordinateFragment) as WGS84CoordinateFragment
        this.wgs84CoordinateFragment = wgs84CoordinateFragment

        val mgrsCoordinateFragment = childFragmentManager.findFragmentById(R.id.mgrsCoordinateFragment) as MGRSCoordinateFragment
        this.mgrsCoordinateFragment = mgrsCoordinateFragment

        val dmsCoordinateFragment = childFragmentManager.findFragmentById(R.id.dmsCoordinateFragment) as DMSCoordinateFragment
        this.dmsCoordinateFragment = dmsCoordinateFragment

        binding.toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp)
        binding.toolbar.setNavigationOnClickListener { dismiss() }
        binding.toolbar.title = title
        binding.toolbar.inflateMenu(R.menu.location_edit_menu)
        if (!clearable) {
            binding.toolbar.menu.removeItem(R.id.clear)
        }

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.clear -> {
                    listener?.onLocation(null)
                    dismiss()
                    true
                }
                R.id.apply -> {
                    updateLocation()
                    true
                }
                else -> super.onOptionsItemSelected(it)
            }
        }

        binding.tabs.addTab(binding.tabs.newTab().setText("Lat/Lng"), WGS84_COORDINATE_TAB_POSITION)
        binding.tabs.addTab(binding.tabs.newTab().setText("MGRS"), MGRS_COORDINATE_TAB_POSITION)
        binding.tabs.addTab(binding.tabs.newTab().setText("DMS"), DMS_COORDINATE_TAB_POSITION)

        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val defaultCoordinateSystem = preferences.getInt(resources.getString(R.string.coordinateSystemViewKey), CoordinateSystem.WGS84.preferenceValue)
        when (CoordinateSystem.fromPreference(preferences.getInt(resources.getString(R.string.coordinateSystemEditKey), defaultCoordinateSystem))) {
            CoordinateSystem.MGRS -> {
                childFragmentManager.beginTransaction()
                    .show(mgrsCoordinateFragment)
                    .hide(wgs84CoordinateFragment)
                    .hide(dmsCoordinateFragment)
                    .commit()

                binding.tabs.getTabAt(MGRS_COORDINATE_TAB_POSITION)?.select()
            }
            CoordinateSystem.WGS84 -> {
                childFragmentManager.beginTransaction()
                    .show(wgs84CoordinateFragment)
                    .hide(mgrsCoordinateFragment)
                    .hide(dmsCoordinateFragment)
                    .commit()

                binding.tabs.getTabAt(WGS84_COORDINATE_TAB_POSITION)?.select()
            }
            CoordinateSystem.DMS -> {
                childFragmentManager.beginTransaction()
                    .show(dmsCoordinateFragment)
                    .hide(wgs84CoordinateFragment)
                    .hide(mgrsCoordinateFragment)
                    .commit()

                binding.tabs.getTabAt(DMS_COORDINATE_TAB_POSITION)?.select()
            }
        }

        binding.tabs.addOnTabSelectedListener(this)

        val style = ShapeStyle(requireContext())
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
        } else if (fragment is DMSCoordinateFragment) {
            fragment.coordinateChangeListener = this
        }
    }

    override fun onMapReady(map: GoogleMap) {
        this.map = map
        map.setOnCameraIdleListener(this)

        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        map.mapType = preferences.getInt(getString(R.string.baseLayerKey), resources.getInteger(R.integer.baseLayerDefaultValue))

        val dayNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (dayNightMode == Configuration.UI_MODE_NIGHT_NO) {
            map.setMapStyle(null)
        } else {
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_theme_night))
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
                .hide(dmsCoordinateFragment)
                .commit()

            if (!wgs84CoordinateFragment.setLatLng(mgrsCoordinateFragment.getLatLng())) {
                wgs84CoordinateFragment.getLatLng()?.let {
                    map.moveCamera(CameraUpdateFactory.newLatLng(it))
                }
            }

            mgrsTileOverlay?.remove()
        } else if (position == MGRS_COORDINATE_TAB_POSITION) {
            childFragmentManager.beginTransaction()
                .show(mgrsCoordinateFragment)
                .hide(wgs84CoordinateFragment)
                .hide(dmsCoordinateFragment)
                .commit()

            if (!mgrsCoordinateFragment.setLatLng(wgs84CoordinateFragment.getLatLng())) {
                mgrsCoordinateFragment.getLatLng()?.let {
                    map.moveCamera(CameraUpdateFactory.newLatLng(it))
                }
            }

            mgrsTileOverlay = map.addTileOverlay(TileOverlayOptions().tileProvider(MGRSTileProvider(context)))
        } else if (position == DMS_COORDINATE_TAB_POSITION) {
            childFragmentManager.beginTransaction()
                .show(dmsCoordinateFragment as Fragment)
                .hide(wgs84CoordinateFragment as Fragment)
                .hide(mgrsCoordinateFragment as Fragment)
                .commit()

            if (!dmsCoordinateFragment.setLatLng(dmsCoordinateFragment.getLatLng())) {
                dmsCoordinateFragment.getLatLng()?.let {
                    map.moveCamera(CameraUpdateFactory.newLatLng(it))
                }
            }

            mgrsTileOverlay?.remove()
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
                    val firstPoint = Point(point.longitude, point.latitude)
                    val geometry = when (shapeType) {
                        GeometryType.LINESTRING -> {
                            val lineString = LineString()
                            lineString.addPoint(firstPoint)
                            lineString
                        }
                        GeometryType.POLYGON -> {
                            val polygon = Polygon()
                            val ring = LineString()
                            ring.addPoint(firstPoint)
                            polygon.addRing(ring)
                            polygon
                        }
                        else -> throw IllegalArgumentException("Unsupported Geometry Type: $shapeType")
                    }
                    addMapShape(geometry)
                } else {
                    val markerOptions = getEditMarkerOptions()
                    markerOptions.position(point)
                    val marker = map.addMarker(markerOptions)
                    val shape: ShapeMarkers?
                    val mapShape = shapeMarkers?.shape
                    when (mapShape?.shapeType) {
                        GoogleMapShapeType.POLYLINE_MARKERS -> {
                            val polylineMarkers = mapShape.shape as PolylineMarkers
                            shape = polylineMarkers
                            if (newDrawing) {
                                polylineMarkers.add(marker)
                            } else {
                                polylineMarkers.addNew(marker)
                            }
                        }
                        GoogleMapShapeType.POLYGON_MARKERS -> {
                            val polygonMarkers = shapeMarkers?.shape?.shape as PolygonMarkers
                            shape = polygonMarkers
                            if (newDrawing) {
                                polygonMarkers.add(marker)
                            } else {
                                polygonMarkers.addNew(marker)
                            }
                        }
                        else -> throw IllegalArgumentException("Unsupported Shape Type: " + mapShape?.shapeType)
                    }
                    shapeMarkers?.add(marker, shape)
                    selectShapeMarker(marker)
                    updateShape(marker)
                }
            } else if (!shapeMarkersValid() && selectedMarker != null) {
                // Allow long click to expand a zero area rectangle
                vibrate(resources.getInteger(R.integer.shape_edit_add_long_click_vibrate).toLong())
                selectedMarker!!.position = point
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

                if (selectedMarker?.id != marker.id) {
                    selectShapeMarker(marker)
                } else if (!isRectangle && shapeMarkers != null && shapeMarkers!!.size() > 1) {

                    val deleteDialog = AlertDialog.Builder(requireActivity())
                    val position = marker.position
                    deleteDialog.setTitle(R.string.location_edit_delete_point_title)
                    deleteDialog.setMessage(String.format(getString(R.string.location_edit_delete_point_message),
                            locationFormatter.format(position.latitude), locationFormatter.format(position.longitude)))
                    deleteDialog.setNegativeButton(R.string.cancel, null)
                    deleteDialog.setPositiveButton(R.string.delete) { _, _ ->
                        val markers = getShapeMarkers()

                        // Find the index of the marker being deleted
                        var index = 1
                        for (i in markers.indices) {
                            if (markers[i] == marker) {
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
                        val selectMarker = markers[index]

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
            selectedMarker!!.position = coordinate
            updateShape(selectedMarker!!)
        }
    }

    override fun onCoordinateChangeStart(coordinate: LatLng) {
        // Move the camera to a selected line or polygon marker
        if (shapeType != GeometryType.POINT && selectedMarker != null) {
            map.moveCamera(CameraUpdateFactory.newLatLng(selectedMarker!!.position))
        }

        updateHint()
    }

    override fun onCoordinateChangeEnd(coordinate: LatLng?) {
        updateHint()
    }

    private fun setupMap() {
        map.moveCamera(location.getCameraUpdate(mapFragment.view))

        if (binding.tabs.selectedTabPosition == MGRS_COORDINATE_TAB_POSITION) {
            mgrsTileOverlay = map.addTileOverlay(TileOverlayOptions().tileProvider(MGRSTileProvider(context)))
        }

        map.uiSettings.isCompassEnabled = false
        map.uiSettings.isRotateGesturesEnabled = false
        map.setOnCameraMoveListener(this)
        map.setOnCameraMoveStartedListener(this)
        map.setOnMapClickListener(this)
        map.setOnMapLongClickListener(this)
        map.setOnMarkerClickListener(this)
        map.setOnMarkerDragListener(this)

        setupMapButton(binding.editPointButton)
        setupMapButton(binding.editLineButton)
        setupMapButton(binding.editRectangleButton)
        setupMapButton(binding.editPolygonButton)

        val geometry = location.geometry
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
        when (binding.tabs.selectedTabPosition) {
            WGS84_COORDINATE_TAB_POSITION -> wgs84CoordinateFragment.clearFocus()
            MGRS_COORDINATE_TAB_POSITION -> mgrsCoordinateFragment.clearFocus()
            DMS_COORDINATE_TAB_POSITION -> dmsCoordinateFragment.clearFocus()
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
        binding.editPointButton.isSelected = shapeType == GeometryType.POINT
        binding.editLineButton.isSelected = shapeType == GeometryType.LINESTRING
        binding.editRectangleButton.isSelected = shapeType == GeometryType.POLYGON && isRectangle
        binding.editPolygonButton.isSelected = shapeType == GeometryType.POLYGON && !isRectangle
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
                            points.add(shapeConverter.toPoint(marker.position))
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
                ) { _, _ -> revertShapeType() }
                changeDialog.setOnCancelListener { revertShapeType() }
                changeDialog.setPositiveButton(R.string.change
                ) { _, _ -> changeShapeType(selectedType, selectedRectangle) }
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

        val geometry: Geometry?

        // Changing from point to a shape
        if (shapeType == GeometryType.POINT) {
            val center = map.cameraPosition.target
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
            } // Changing to a line or polygon
            geometry = when (selectedType) {
                GeometryType.LINESTRING -> lineString
                GeometryType.POLYGON -> {
                    val polygon = Polygon()
                    polygon.addRing(lineString)
                    polygon
                }
                else -> throw IllegalArgumentException("Unsupported Geometry Type: $selectedType")
            }
        } else if (selectedType == GeometryType.POINT) {
            val newPointPosition = getShapeToPointLocation()
            geometry = Point(newPointPosition.longitude, newPointPosition.latitude)
            map.moveCamera(CameraUpdateFactory.newLatLng(newPointPosition))
            newDrawing = false
        } else {

            var lineString: LineString
            if (shapeMarkers != null) {

                var markers = getShapeMarkers()

                // If all markers are in the same spot only keep one
                if (markers.isNotEmpty() && !multipleMarkerPositions(markers)) {
                    markers = markers.subList(0, 1)
                }

                // Add each marker location and find the selected marker index
                var latLngPoints: MutableList<LatLng> = ArrayList()
                var startLocation: Int? = null
                for (marker in markers) {
                    if (startLocation == null && selectedMarker != null && selectedMarker == marker) {
                        startLocation = latLngPoints.size
                    }
                    latLngPoints.add(marker.position)
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
                    newDrawing = lineString.points.size <= 1
                    geometry = lineString
                }

                GeometryType.POLYGON -> {

                    // If converting to a rectangle, use the current shape bounds
                    if (selectedRectangle) {
                        val lineStringCopy = lineString.copy() as LineString
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
                    newDrawing = lineString.points.size <= 2
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
             selectedMarker!!.position
        } else {
            val tabPosition = binding.tabs.selectedTabPosition
            if (tabPosition == WGS84_COORDINATE_TAB_POSITION) {
                wgs84CoordinateFragment.clearFocus()
                newPointPosition = wgs84CoordinateFragment.getLatLng()
            } else if (tabPosition == MGRS_COORDINATE_TAB_POSITION) {
                mgrsCoordinateFragment.clearFocus()
                newPointPosition = mgrsCoordinateFragment.getLatLng()
            } else if (tabPosition == DMS_COORDINATE_TAB_POSITION) {
                dmsCoordinateFragment.clearFocus()
                newPointPosition = dmsCoordinateFragment.getLatLng()
            }

            if (newPointPosition == null) {
                val position = map.cameraPosition
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
            previousSelectedMarkerLocation = selectedMarker?.position
            selectedMarker = null
            clearRectangleCorners()
        }
        if (shapeMarkers != null) {
            shapeMarkers?.remove()
            shapeMarkers = null
        }
        if (geometry.geometryType == GeometryType.POINT) {
            binding.locationEditMarker.setImageBitmap(markerBitmap)
            val point = geometry as Point
            updateLatitudeLongitudeText(LatLng(point.y, point.x))
        } else {
            binding.locationEditMarker.setImageBitmap(null)
            val shape = shapeConverter.toShape(geometry)
            shapeMarkers = shapeConverter.addShapeToMapAsMarkers(map, shape, null,
                    editMarkerOptions, editMarkerOptions, null, editPolylineOptions, editPolygonOptions)
            val markers = getShapeMarkers()
            var selectMarker = markers[0]
            if (previousSelectedMarkerLocation != null) {
                for (marker in markers) {
                    if (marker.position == previousSelectedMarkerLocation) {
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
        val tabPosition = binding.tabs.selectedTabPosition
        if (tabPosition == WGS84_COORDINATE_TAB_POSITION) {
            locationEditHasFocus = wgs84CoordinateFragment.hasFocus()
        } else if (tabPosition == MGRS_COORDINATE_TAB_POSITION) {
            locationEditHasFocus = mgrsCoordinateFragment.hasFocus()
        } else if (tabPosition == DMS_COORDINATE_TAB_POSITION) {
            locationEditHasFocus = dmsCoordinateFragment.hasFocus()
        }

        var hint = ""

        when (shapeType) {
            GeometryType.POINT -> hint = if (locationEditHasFocus) {
                getString(R.string.location_edit_hint_point_edit)
            } else {
                getString(R.string.location_edit_hint_point)
            }
            GeometryType.POLYGON -> {
                if (isRectangle) {
                    hint = if (locationEditHasFocus) {
                        getString(R.string.location_edit_hint_rectangle_edit)
                    } else if (dragging) {
                        getString(R.string.location_edit_hint_rectangle_drag)
                    } else if (!multipleShapeMarkerPositions()) {
                        getString(R.string.location_edit_hint_rectangle_new)
                    } else {
                        getString(R.string.location_edit_hint_rectangle)
                    }
                } else {
                    hint = if (locationEditHasFocus) {
                        getString(R.string.location_edit_hint_shape_edit)
                    } else if (dragging) {
                        getString(R.string.location_edit_hint_shape_drag)
                    } else if (newDrawing) {
                        getString(R.string.location_edit_hint_shape_new)
                    } else {
                        getString(R.string.location_edit_hint_shape)
                    }
                }
            }
            GeometryType.LINESTRING -> {
                hint = if (locationEditHasFocus) {
                    getString(R.string.location_edit_hint_shape_edit)
                } else if (dragging) {
                    getString(R.string.location_edit_hint_shape_drag)
                } else if (newDrawing) {
                    getString(R.string.location_edit_hint_shape_new)
                } else {
                    getString(R.string.location_edit_hint_shape)
                }
            }
        }

        binding.hintText.text = hint
    }

    /**
     * Update the latitude and longitude text entries
     *
     * @param latLng lat lng point
     */
    private fun updateLatitudeLongitudeText(latLng: LatLng?) {
        wgs84CoordinateFragment.setLatLng(latLng)
        mgrsCoordinateFragment.setLatLng(latLng)
        dmsCoordinateFragment.setLatLng(latLng)
    }

    /**
     * Update the location into the response
     */
    private fun updateLocation() {
        // Save coordinate system used for edit
        val coordinateSystem = CoordinateSystem.fromPreference(binding.tabs.selectedTabPosition)
        val editor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
        editor.putInt(resources.getString(R.string.coordinateSystemEditKey), coordinateSystem.preferenceValue).apply()

        val geometry = convertToGeometry() ?: return
        location.geometry = geometry
        location.provider = ObservationLocation.MANUAL_PROVIDER
        location.accuracy = 0.0f
        location.time = System.currentTimeMillis()

        listener?.onLocation(location)
        dismiss()
    }

    private fun convertToGeometry(): Geometry? {

        // validate shape has minimum number of points
        if (shapeType == GeometryType.POLYGON && (!multipleShapeMarkerPositions() || getShapeMarkers().size < 3)) {
            val errorMessage = if (isRectangle) getString(R.string.location_edit_error_rectangle_min_points) else getString(R.string.location_edit_error_polygon_min_points)
            Snackbar.make(binding.coordinatorLayout, errorMessage, Snackbar.LENGTH_SHORT).show()
            return null
        } else if (shapeType == GeometryType.LINESTRING && !multipleShapeMarkerPositions()) {
            Snackbar.make(binding.coordinatorLayout, getString(R.string.location_edit_error_linestring_min_points), Snackbar.LENGTH_SHORT).show()
            return null
        }

        val geometry: Geometry
        if (shapeType == GeometryType.POINT) {
            val center = map.cameraPosition.target
            geometry = Point(center.longitude, center.latitude)
        } else {
            // general shape validity test
            if (shapeMarkers?.isValid != true) {
                Snackbar.make(binding.coordinatorLayout, getString(R.string.location_edit_error_shape), Snackbar.LENGTH_SHORT).show()
                return null
            }

            geometry = shapeConverter.toGeometry(shapeMarkers?.shape)

            // validate polygon does not intersect itself
            if ((geometry as? Polygon)?.hasKinks() == true) {
                Snackbar.make(binding.coordinatorLayout, getString(R.string.location_edit_error_polygon_kinks), Snackbar.LENGTH_SHORT).show()
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
    private fun findRectangleCorners(marker: Marker?) {
        clearRectangleCorners()
        if (shapeMarkers != null && isRectangle) {
            val markers = getShapeMarkers()
            var afterMatchesX = rectangleSameXSide1
            for (i in markers.indices) {
                val shapeMarker = markers[i]
                if (shapeMarker.id == marker?.id) {
                    val beforeIndex = if (i > 0) i - 1 else markers.size - 1
                    val afterIndex = if (i < markers.size - 1) i + 1 else 0
                    val before = markers[beforeIndex]
                    val after = markers[afterIndex]
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
    private fun updateRectangleCorners(marker: Marker?) {
        if (marker != null) {
            rectangleSameXMarker?.apply {
                position = LatLng(position.latitude, marker.position.longitude)
            }

            rectangleSameYMarker?.apply {
                position = LatLng(marker.position.latitude, position.longitude)
            }
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
    private fun selectShapeMarker(marker: Marker?) {
        clearRectangleCorners()
        if (selectedMarker != null && selectedMarker?.id != marker?.id) {
            selectedMarker?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_shape_edit))
            selectedMarker?.zIndex = 0.0f
        }
        selectedMarker = marker
        updateLatitudeLongitudeText(marker?.position)
        marker?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_shape_edit_selected))
        marker?.zIndex = 1.0f
        findRectangleCorners(marker)
    }

    /**
     * Update the shape with any modifications, adjust the accept menu button state
     *
     * @param selectedMarker selected marker
     */
    private fun updateShape(selectedMarker: Marker?) {
        updateRectangleCorners(selectedMarker)
        shapeMarkers?.update()
        if (shapeMarkers?.isEmpty == true) {
            shapeMarkers = null
        }
    }

    /**
     * Validate that the shape markers are a valid shape and contain multiple unique positions
     *
     * @return true if valid
     */
    private fun shapeMarkersValid(): Boolean {
        return multipleShapeMarkerPositions() && shapeMarkers?.isValid ?: false
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
            shapeMarkers!!.shapeMarkersMap.values.iterator().next().markers
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
    private fun getEditPolylineOptions(style: ShapeStyle): PolylineOptions {
        val polylineOptions = PolylineOptions()
        polylineOptions.width(style.strokeWidth)
        polylineOptions.color(style.strokeColor)
        return polylineOptions
    }

    /**
     * Get the edit polygon options
     *
     * @param style observation shape style
     * @return edit polygon options
     */
    private fun getEditPolygonOptions(style: ShapeStyle): PolygonOptions {
        val polygonOptions = PolygonOptions()
        polygonOptions.strokeWidth(style.strokeWidth)
        polygonOptions.strokeColor(style.strokeColor)
        polygonOptions.fillColor(style.fillColor)
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
        companion object {
            private const val LOCATION_PRECISION = "%.6f"
            private const val LOCATION_MAX_PRECISION = 6
        }

        private lateinit var latitudeEdit: EditText
        private lateinit var longitudeEdit: EditText
        lateinit var coordinateChangeListener: CoordinateChangeListener

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

            latitudeEdit.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    latitudeEdit.clearFocus()
                    return@OnEditorActionListener true
                }
                false
            })

            longitudeEdit.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
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
                    coordinateChangeListener.onCoordinateChangeStart(latLng)
                } else {
                    val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0)
                    coordinateChangeListener.onCoordinateChangeEnd(latLng)
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

            coordinateChangeListener.onCoordinateChanged(latLng)
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
            if (latitudeString.isNotEmpty()) {
                try {
                    latitude = java.lang.Double.parseDouble(latitudeString)
                } catch (e: NumberFormatException) {}
            }

            var longitude: Double? = null
            if (longitudeString.isNotEmpty()) {
                try {
                    longitude = java.lang.Double.parseDouble(longitudeString)
                } catch (e: NumberFormatException) {}
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
        private lateinit var mgrsEdit: EditText
        private lateinit var mgrsLayout: TextInputLayout
        lateinit var coordinateChangeListener: CoordinateChangeListener

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.mgrs_location_edit, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            mgrsEdit = view.findViewById<View>(R.id.mgrs) as EditText
            mgrsLayout = view.findViewById<View>(R.id.mgrs_layout) as TextInputLayout

            mgrsEdit.addTextChangedListener(this)
            mgrsEdit.onFocusChangeListener = this
            mgrsEdit.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
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
                mgrsLayout.error = "Invalid MGRS Code"
            } else {
                mgrsLayout.error = null
                mgrsLayout.isErrorEnabled = false
                coordinateChangeListener.onCoordinateChanged(latLng)
            }
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun onFocusChange(v: View, hasFocus: Boolean) {
            val latLng = getLatLng()

            if (mgrsEdit.hasFocus()) {
                coordinateChangeListener.onCoordinateChangeStart(latLng)
            } else {
                val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.applicationWindowToken, 0)
                coordinateChangeListener.onCoordinateChangeEnd(latLng)
            }
        }

        fun setLatLng(latLng: LatLng?): Boolean {
            if (latLng == null) {
                return false
            }

            val mgrs = MGRS.from(mil.nga.grid.features.Point.point(latLng.longitude, latLng.latitude))
            mgrsEdit.setText(mgrs.coordinate())
            mgrsLayout.error = null
            mgrsLayout.isErrorEnabled = false

            return true
        }

        fun getLatLng(): LatLng? {
            return try {
                val point = MGRS.parse(mgrsEdit.text.toString()).toPoint()
                LatLng(point.latitude, point.longitude)
            } catch (e: ParseException) {
                null
            }
        }

        fun hasFocus(): Boolean {
            return mgrsEdit.hasFocus()
        }

        fun clearFocus() {
            mgrsEdit.clearFocus()
        }
    }

    class DMSCoordinateFragment : Fragment(), TextWatcher, View.OnFocusChangeListener {
        private lateinit var latitudeDMSEdit: EditText
        private lateinit var latitudeDMSLayout: TextInputLayout

        private lateinit var longitudeDMSEdit: EditText
        private lateinit var longitudeDMSLayout: TextInputLayout

        lateinit var coordinateChangeListener: CoordinateChangeListener

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater.inflate(R.layout.dms_location_edit, container, false)

            val splitCoordinates : (Pair<String, String>) -> Unit = { coordinates ->
                if (latitudeDMSEdit.text.isEmpty()) {
                    latitudeDMSEdit.setText(coordinates.first)
                }

                if (longitudeDMSEdit.text.isEmpty()) {
                    longitudeDMSEdit.setText(coordinates.second)
                }
            }

            latitudeDMSEdit = view.findViewById(R.id.location_edit_dms_latitude)
            latitudeDMSLayout = view.findViewById(R.id.location_edit_dms_latitude_layout)
            latitudeDMSEdit.addTextChangedListener(this)
            latitudeDMSEdit.addTextChangedListener(DMSCoordinateTextWatcher(latitudeDMSEdit, CoordinateType.LATITUDE, splitCoordinates))

            longitudeDMSEdit = view.findViewById(R.id.location_edit_dms_longitude)
            longitudeDMSLayout = view.findViewById(R.id.location_edit_dms_longitude_layout)
            longitudeDMSEdit.addTextChangedListener(this)
            longitudeDMSEdit.addTextChangedListener(DMSCoordinateTextWatcher(longitudeDMSEdit, CoordinateType.LONGITUDE, splitCoordinates))

            latitudeDMSEdit.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    latitudeDMSEdit.clearFocus()
                    return@OnEditorActionListener true
                }
                false
            })

            longitudeDMSEdit.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    longitudeDMSEdit.clearFocus()
                    return@OnEditorActionListener true
                }
                false
            })

            return view
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            clearFocus()
        }

        override fun onFocusChange(v: View, hasFocus: Boolean) {
            if (v === longitudeDMSEdit || v === latitudeDMSEdit) {
                val latLng = getLatLng()

                if (longitudeDMSEdit.hasFocus() || latitudeDMSEdit.hasFocus()) {
                    coordinateChangeListener.onCoordinateChangeStart(latLng)
                } else {
                    val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0)
                    coordinateChangeListener.onCoordinateChangeEnd(latLng)
                }
            }
        }

        override fun afterTextChanged(s: Editable) {
            // Only handle when the longitude or latitude entries have focus
            if (!longitudeDMSEdit.hasFocus() && !latitudeDMSEdit.hasFocus()) {
                return
            }

            var latLng = getLatLng()

            if (latLng == null) {
                // TODO might want to show error here...
                latLng = LatLng(0.0, 0.0)
            }

            coordinateChangeListener.onCoordinateChanged(latLng)
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        fun setLatLng(latLng: LatLng?): Boolean {
            if (latLng == null) {
                return false
            }

            val dms = DMS.from(latLng)
            latitudeDMSEdit.setText(dms.latitude.format())
            longitudeDMSEdit.setText(dms.latitude.format())

            return true
        }

        fun getLatLng(): LatLng? {
            val latitudeDMSString = latitudeDMSEdit.text.toString()
            val longitudeDMSString = longitudeDMSEdit.text.toString()

            val latitudeDMS = DMSLocation.parse(latitudeDMSString, CoordinateType.LATITUDE)
            latitudeDMSLayout.error = if (latitudeDMS == null) {
                " "
            } else null

            val longitudeDMS = DMSLocation.parse(longitudeDMSString, CoordinateType.LONGITUDE)
            longitudeDMSLayout.error = if (longitudeDMS == null) {
                " "
            } else null

            return if (latitudeDMS != null && longitudeDMS != null) {
                DMS(latitudeDMS, longitudeDMS).toLatLng()
            } else null
        }

        fun hasFocus(): Boolean {
            return longitudeDMSEdit.hasFocus() || latitudeDMSEdit.hasFocus()
        }

        fun clearFocus() {
            longitudeDMSEdit.clearFocus()
            latitudeDMSEdit.clearFocus()
        }
    }
}