package mil.nga.giat.mage.observation;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mil.nga.geopackage.map.geom.GoogleMapShape;
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter;
import mil.nga.geopackage.map.geom.GoogleMapShapeMarkers;
import mil.nga.geopackage.map.geom.PolygonMarkers;
import mil.nga.geopackage.map.geom.PolylineMarkers;
import mil.nga.geopackage.map.geom.ShapeMarkers;
import mil.nga.geopackage.projection.ProjectionConstants;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.coordinate.CoordinateSystem;
import mil.nga.giat.mage.map.MapUtils;
import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.gzd.MGRSTileProvider;
import mil.nga.wkb.geom.Geometry;
import mil.nga.wkb.geom.GeometryEnvelope;
import mil.nga.wkb.geom.GeometryType;
import mil.nga.wkb.geom.LineString;
import mil.nga.wkb.geom.Point;
import mil.nga.wkb.geom.Polygon;
import mil.nga.wkb.util.GeometryEnvelopeBuilder;
import mil.nga.wkb.util.GeometryUtils;

public class LocationEditActivity extends AppCompatActivity implements OnMapClickListener, OnMapReadyCallback,
        GoogleMap.OnCameraMoveStartedListener, GoogleMap.OnCameraMoveListener, OnCameraIdleListener, OnMarkerDragListener,
        OnMapLongClickListener, OnMarkerClickListener, View.OnClickListener, CoordinateChangeListener, TabLayout.OnTabSelectedListener {

    public static String LOCATION = "LOCATION";
    public static String MARKER_BITMAP = "MARKER_BITMAP";
    public static String NEW_OBSERVATION = "NEW_OBSERVATION";

    private static final String LOCATION_PRECISION = "%.6f";
    private final DecimalFormat locationFormatter = new DecimalFormat("0.000000");

    private static final int WGS84_COORDINATE_TAB_POSITION = 0;
    private static final int MGRS_COORDINATE_TAB_POSITION = 1;

    private ObservationLocation location;
    private GoogleMap map;
    private int onCameraMoveReason;
    private TileOverlay mgrsTileOverlay;
    private TabLayout tabLayout;
    private WGS84CoordinateFragment wgs84CoordinateFragment;
    private MGRSCoordinateFragment mgrsCoordinateFragment;
    private TextView hintText;
    private MapFragment mapFragment;
    private GoogleMapShapeMarkers shapeMarkers;
    private final GoogleMapShapeConverter shapeConverter = new GoogleMapShapeConverter();
    private Bitmap markerBitmap;
    private ImageView imageView = null;
    private MarkerOptions editMarkerOptions;
    private PolylineOptions editPolylineOptions;
    private PolygonOptions editPolygonOptions;
    private Vibrator vibrator;
    private boolean newDrawing;
    private GeometryType shapeType = GeometryType.POINT;
    private boolean isRectangle = false;
    private Marker selectedMarker = null;
    private Marker rectangleSameXMarker;
    private Marker rectangleSameYMarker;
    private boolean rectangleSameXSide1;
    private FloatingActionButton pointButton;
    private FloatingActionButton lineButton;
    private FloatingActionButton rectangleButton;
    private FloatingActionButton polygonButton;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.location_edit);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        wgs84CoordinateFragment = (WGS84CoordinateFragment) getFragmentManager().findFragmentById(R.id.wgs84CoordinateFragment);
        mgrsCoordinateFragment = (MGRSCoordinateFragment) getFragmentManager().findFragmentById(R.id.mgrsCoordinateFragment);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.addTab(tabLayout.newTab().setText("Lat/Lng"), WGS84_COORDINATE_TAB_POSITION);
        tabLayout.addTab(tabLayout.newTab().setText("MGRS"), MGRS_COORDINATE_TAB_POSITION);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int defaultCoordinateSystem = preferences.getInt(getResources().getString(R.string.coordinateSystemViewKey), CoordinateSystem.WGS84.getPreferenceValue());
        CoordinateSystem coordinateSystem = CoordinateSystem.get(preferences.getInt(getResources().getString(R.string.coordinateSystemEditKey), defaultCoordinateSystem));
        switch (coordinateSystem) {
            case MGRS:
                getFragmentManager().beginTransaction()
                        .show(mgrsCoordinateFragment)
                        .hide(wgs84CoordinateFragment)
                        .commit();

                tabLayout.getTabAt(MGRS_COORDINATE_TAB_POSITION).select();
                break;
            default:
                getFragmentManager().beginTransaction()
                        .show(wgs84CoordinateFragment)
                        .hide(mgrsCoordinateFragment)
                        .commit();

                tabLayout.getTabAt(WGS84_COORDINATE_TAB_POSITION).select();
        }

        tabLayout.addOnTabSelectedListener(this);

        Intent intent = getIntent();
        location = intent.getParcelableExtra(LOCATION);
        markerBitmap = intent.getParcelableExtra(MARKER_BITMAP);
        newDrawing = intent.getBooleanExtra(NEW_OBSERVATION, false);

        hintText = (TextView) findViewById(R.id.location_edit_hint);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        editMarkerOptions = getEditMarkerOptions();
        ObservationShapeStyle style = new ObservationShapeStyle(this);
        editPolylineOptions = getEditPolylineOptions(style);
        editPolygonOptions = getEditPolygonOptions(style);

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.location_edit_map);
        mapFragment.getMapAsync(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        map.setOnCameraIdleListener(this);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        map.setMapType(preferences.getInt(getString(R.string.baseLayerKey), getResources().getInteger(R.integer.baseLayerDefaultValue)));

        int dayNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (dayNightMode == Configuration.UI_MODE_NIGHT_NO) {
            map.setMapStyle(null);
        } else {
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.map_theme_night));
        }
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        int position = tab.getPosition();

        if (position == WGS84_COORDINATE_TAB_POSITION) {
            getFragmentManager().beginTransaction()
                    .show(wgs84CoordinateFragment)
                    .hide(mgrsCoordinateFragment)
                    .commit();

            if (!wgs84CoordinateFragment.setLatLng(mgrsCoordinateFragment.getLatLng())) {
                map.moveCamera(CameraUpdateFactory.newLatLng(wgs84CoordinateFragment.getLatLng()));
            }

            mgrsTileOverlay.remove();
        } else if (position == MGRS_COORDINATE_TAB_POSITION) {
            getFragmentManager().beginTransaction()
                    .show(mgrsCoordinateFragment)
                    .hide(wgs84CoordinateFragment)
                    .commit();

            if (!mgrsCoordinateFragment.setLatLng(wgs84CoordinateFragment.getLatLng())) {
                map.moveCamera(CameraUpdateFactory.newLatLng(mgrsCoordinateFragment.getLatLng()));
            }

            mgrsTileOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(new MGRSTileProvider(getApplicationContext())));
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    @Override
    public void onCoordinateChanged(LatLng coordinate) {
        map.moveCamera(CameraUpdateFactory.newLatLng(coordinate));
        if (selectedMarker != null) {
            selectedMarker.setPosition(coordinate);
            updateShape(selectedMarker);
        }
    }

    @Override
    public void onCoordinateChangeStart(LatLng coordinate) {
        // Move the camera to a selected line or polygon marker
        if (shapeType != GeometryType.POINT && selectedMarker != null) {
            map.moveCamera(CameraUpdateFactory.newLatLng(selectedMarker.getPosition()));
        }

        updateHint();
    }

    @Override
    public void onCoordinateChangeEnd(LatLng coordinate) {
        updateHint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCameraIdle() {
        map.setOnCameraIdleListener(null);
        setupMap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCameraMoveStarted(int reason) {
        onCameraMoveReason = reason;
    }

    @Override
    public void onCameraMove() {
        if (onCameraMoveReason != REASON_DEVELOPER_ANIMATION) {
            // Points are represented by the camera position
            if (shapeType == GeometryType.POINT) {
                clearCoordinateFocus();
                CameraPosition position = map.getCameraPosition();
                updateLatitudeLongitudeText(position.target);
            }
        }
    }

    /**
     * Setup the map after it has been fully loaded
     */
    private void setupMap() {

        map.moveCamera(location.getCameraUpdate(mapFragment.getView()));

        if (tabLayout.getSelectedTabPosition() == MGRS_COORDINATE_TAB_POSITION) {
            mgrsTileOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(new MGRSTileProvider(getApplicationContext())));
        }

        imageView = (ImageView) findViewById(R.id.location_edit_marker);

        map.getUiSettings().setCompassEnabled(false);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.setOnCameraMoveListener(this);
        map.setOnCameraMoveStartedListener(this);
        map.setOnMapClickListener(this);
        map.setOnMapLongClickListener(this);
        map.setOnMarkerClickListener(this);
        map.setOnMarkerDragListener(this);

        pointButton = (FloatingActionButton) findViewById(R.id.location_edit_point_button);
        setupMapButton(pointButton);
        lineButton = (FloatingActionButton) findViewById(R.id.location_edit_line_button);
        setupMapButton(lineButton);
        rectangleButton = (FloatingActionButton) findViewById(R.id.location_edit_rectangle_button);
        setupMapButton(rectangleButton);
        polygonButton = (FloatingActionButton) findViewById(R.id.location_edit_polygon_button);
        setupMapButton(polygonButton);

        Geometry geometry = location.getGeometry();
        setShapeType(geometry);
        addMapShape(geometry);
    }

    /**
     * Setup the map button
     *
     * @param button button
     */
    private void setupMapButton(FloatingActionButton button) {
        Drawable drawable = DrawableCompat.wrap(button.getDrawable());
        button.setImageDrawable(drawable);
        DrawableCompat.setTintList(drawable, AppCompatResources.getColorStateList(this, R.color.toggle_button_selected));
        button.setOnClickListener(this);
    }

    /**
     * Clear the focus from the coordinate text entries
     */
    private void clearCoordinateFocus() {
        int tabPosition = tabLayout.getSelectedTabPosition();
        if (tabPosition == WGS84_COORDINATE_TAB_POSITION) {
            wgs84CoordinateFragment.clearFocus();
        } else if (tabPosition == MGRS_COORDINATE_TAB_POSITION) {
            mgrsCoordinateFragment.clearFocus();
        }
    }

    /**
     * Update the current shape type
     *
     * @param geometry geometry
     */
    private void setShapeType(Geometry geometry) {
        shapeType = geometry.getGeometryType();
        checkIfRectangle(geometry);
        setShapeTypeSelection();
    }

    /**
     * Check if the geometry is a rectangle polygon
     *
     * @param geometry geometry
     */
    private void checkIfRectangle(Geometry geometry) {
        isRectangle = false;
        if (geometry.getGeometryType() == GeometryType.POLYGON) {
            Polygon polygon = (Polygon) geometry;
            LineString ring = polygon.getRings().get(0);
            List<Point> points = ring.getPoints();
            updateIfRectangle(points);
        }
    }

    /**
     * Check if the points form a rectangle and update
     *
     * @param points points
     */
    private void updateIfRectangle(List<Point> points) {
        int size = points.size();
        if (size == 4 || size == 5) {
            Point point1 = points.get(0);
            Point lastPoint = points.get(points.size() - 1);
            boolean closed = point1.getX() == lastPoint.getX() && point1.getY() == lastPoint.getY();
            if ((closed && size == 5) || (!closed && size == 4)) {
                Point point2 = points.get(1);
                Point point3 = points.get(2);
                Point point4 = points.get(3);
                if (point1.getX() == point2.getX() && point2.getY() == point3.getY()) {
                    if (point1.getY() == point4.getY() && point3.getX() == point4.getX()) {
                        isRectangle = true;
                        rectangleSameXSide1 = true;
                    }
                } else if (point1.getY() == point2.getY() && point2.getX() == point3.getX()) {
                    if (point1.getX() == point4.getX() && point3.getY() == point4.getY()) {
                        isRectangle = true;
                        rectangleSameXSide1 = false;
                    }
                }
            }
        }
    }

    /**
     * Revert the selected shape type to the current shape type
     */
    private void revertShapeType() {
        setShapeTypeSelection();
    }

    /**
     * Set the shape type selection to match the current shape type
     */
    private void setShapeTypeSelection() {
        pointButton.setSelected(shapeType == GeometryType.POINT);
        lineButton.setSelected(shapeType == GeometryType.LINESTRING);
        rectangleButton.setSelected(shapeType == GeometryType.POLYGON && isRectangle);
        polygonButton.setSelected(shapeType == GeometryType.POLYGON && !isRectangle);
    }

    /**
     * {@inheritDoc}
     */
    public void onClick(View v) {
        GeometryType newShapeType = null;
        boolean newRectangle = false;
        switch (v.getId()) {
            case R.id.location_edit_point_button:
                newShapeType = GeometryType.POINT;
                break;
            case R.id.location_edit_line_button:
                newShapeType = GeometryType.LINESTRING;
                break;
            case R.id.location_edit_rectangle_button:
                newRectangle = true;
            case R.id.location_edit_polygon_button:
                newShapeType = GeometryType.POLYGON;
                break;
        }

        if (newShapeType != null) {
            confirmAndChangeShapeType(newShapeType, newRectangle);
        }
    }

    /**
     * If a new shape type was selected, confirm data loss changes and change the shape
     *
     * @param selectedType      newly selected shape type
     * @param selectedRectangle true if a rectangle polygon
     */
    private void confirmAndChangeShapeType(final GeometryType selectedType, final boolean selectedRectangle) {

        // Only care if not the current shape type
        if (selectedType != shapeType || selectedRectangle != isRectangle) {

            clearCoordinateFocus();

            String title = null;
            String message = null;

            // Changing to a point or rectangle, and there are multiple unique positions in the shape
            if ((selectedType == GeometryType.POINT || selectedRectangle) && multipleShapeMarkerPositions()) {

                if (selectedRectangle) {
                    // Changing to a rectangle
                    List<Marker> markers = getShapeMarkers();
                    boolean formRectangle = false;
                    if (markers.size() == 4 || markers.size() == 5) {
                        List<Point> points = new ArrayList<>();
                        for (Marker marker : markers) {
                            points.add(shapeConverter.toPoint(marker.getPosition()));
                        }
                        formRectangle = ObservationLocation.checkIfRectangle(points);
                    }
                    if (!formRectangle) {
                        // Points currently do not form a rectangle
                        title = getString(R.string.location_edit_to_rectangle_title);
                        message = getString(R.string.location_edit_to_rectangle_message);
                    }

                } else {
                    // Changing to a point
                    LatLng newPointPosition = getShapeToPointLocation();
                    title = getString(R.string.location_edit_to_point_title);
                    message = String.format(getString(R.string.location_edit_to_point_message),
                            locationFormatter.format(newPointPosition.latitude), locationFormatter.format(newPointPosition.longitude));
                }

            }

            // If changing to a point and there are multiple points in the current shape, confirm selection
            if (message != null) {

                AlertDialog.Builder changeDialog = new AlertDialog.Builder(this);
                changeDialog.setTitle(title);
                changeDialog.setMessage(message);
                changeDialog.setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                revertShapeType();
                            }
                        });
                changeDialog.setOnCancelListener(
                        new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                revertShapeType();
                            }
                        });
                changeDialog.setPositiveButton(R.string.change,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                changeShapeType(selectedType, selectedRectangle);
                            }
                        });
                changeDialog.show();

            } else {
                changeShapeType(selectedType, selectedRectangle);
            }
        }
    }

    /**
     * Change the current shape type
     *
     * @param selectedType      newly selected shape type
     * @param selectedRectangle true if a rectangle polygon
     */
    private void changeShapeType(GeometryType selectedType, boolean selectedRectangle) {

        isRectangle = selectedRectangle;

        Geometry geometry = null;

        // Changing from point to a shape
        if (shapeType == GeometryType.POINT) {
            LatLng center = map.getCameraPosition().target;
            Point firstPoint = new Point(center.longitude, center.latitude);
            LineString lineString = new LineString();
            lineString.addPoint(firstPoint);
            // Changing to a rectangle
            if (selectedRectangle) {
                // Closed rectangle polygon all at the same point
                lineString.addPoint(firstPoint);
                lineString.addPoint(firstPoint);
                lineString.addPoint(firstPoint);
                lineString.addPoint(firstPoint);
            }
            // Changing to a line or polygon
            else {
                newDrawing = true;
            }
            switch (selectedType) {
                case LINESTRING:
                    geometry = lineString;
                    break;
                case POLYGON:
                    Polygon polygon = new Polygon();
                    polygon.addRing(lineString);
                    geometry = polygon;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported Geometry Type: " + selectedType);
            }
        }
        // Changing from line or polygon to a point
        else if (selectedType == GeometryType.POINT) {
            LatLng newPointPosition = getShapeToPointLocation();
            geometry = new Point(newPointPosition.longitude, newPointPosition.latitude);
            map.moveCamera(CameraUpdateFactory.newLatLng(newPointPosition));
            newDrawing = false;
        }
        // Changing from between a line, polygon, and rectangle
        else {

            LineString lineString = null;
            if (shapeMarkers != null) {

                List<Marker> markers = getShapeMarkers();

                // If all markers are in the same spot only keep one
                if (!markers.isEmpty() && !multipleMarkerPositions(markers)) {
                    markers = markers.subList(0, 1);
                }

                // Add each marker location and find the selected marker index
                List<LatLng> latLngPoints = new ArrayList<>();
                Integer startLocation = null;
                for (Marker marker : markers) {
                    if (startLocation == null && selectedMarker != null && selectedMarker.equals(marker)) {
                        startLocation = latLngPoints.size();
                    }
                    latLngPoints.add(marker.getPosition());
                }

                // When going from the polygon or rectangle to a line
                if (selectedType == GeometryType.LINESTRING) {
                    // Break the polygon closure when changing to a line
                    if (latLngPoints.size() > 1 && latLngPoints.get(0).equals(latLngPoints.get(latLngPoints.size() - 1))) {
                        latLngPoints.remove(latLngPoints.size() - 1);
                    }
                    // Break the line apart at the selected location
                    if (startLocation != null && startLocation < latLngPoints.size()) {
                        List<LatLng> latLngPointsTemp = new ArrayList<>();
                        latLngPointsTemp.addAll(latLngPoints.subList(startLocation, latLngPoints.size()));
                        latLngPointsTemp.addAll(latLngPoints.subList(0, startLocation));
                        latLngPoints = latLngPointsTemp;
                    }
                }

                lineString = shapeConverter.toLineString(latLngPoints);
            } else {
                LatLng newPointPosition = getShapeToPointLocation();
                lineString = new LineString();
                lineString.addPoint(shapeConverter.toPoint(newPointPosition));
            }

            switch (selectedType) {

                case LINESTRING:
                    newDrawing = lineString.getPoints().size() <= 1;
                    geometry = lineString;
                    break;

                case POLYGON:

                    // If converting to a rectangle, use the current shape bounds
                    if (selectedRectangle) {
                        LineString lineStringCopy = (LineString) lineString.copy();
                        GeometryUtils.minimizeGeometry(lineStringCopy, ProjectionConstants.WGS84_HALF_WORLD_LON_WIDTH);
                        GeometryEnvelope envelope = GeometryEnvelopeBuilder.buildEnvelope(lineStringCopy);
                        lineString = new LineString();
                        lineString.addPoint(new Point(envelope.getMinX(), envelope.getMaxY()));
                        lineString.addPoint(new Point(envelope.getMinX(), envelope.getMinY()));
                        lineString.addPoint(new Point(envelope.getMaxX(), envelope.getMinY()));
                        lineString.addPoint(new Point(envelope.getMaxX(), envelope.getMaxY()));
                        lineString.addPoint(new Point(envelope.getMinX(), envelope.getMaxY()));
                        updateIfRectangle(lineString.getPoints());
                    }

                    Polygon polygon = new Polygon();
                    polygon.addRing(lineString);
                    newDrawing = lineString.getPoints().size() <= 2;
                    geometry = polygon;
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported Geometry Type: " + selectedType);
            }
        }

        shapeType = selectedType;
        addMapShape(geometry);
        setShapeTypeSelection();
    }

    /**
     * Get the best single point when converting from a line or polygon to a point.
     * This is either the current selected marker, the current lat & lon values, or map position
     *
     * @return single point location
     */
    private LatLng getShapeToPointLocation() {
        LatLng newPointPosition = null;

        if (selectedMarker != null) {
            newPointPosition = selectedMarker.getPosition();
        } else {
            LatLng latLng = null;
            int tabPosition = tabLayout.getSelectedTabPosition();
            if (tabPosition == WGS84_COORDINATE_TAB_POSITION) {
                wgs84CoordinateFragment.clearFocus();
                newPointPosition = wgs84CoordinateFragment.getLatLng();
            } else if (tabPosition == MGRS_COORDINATE_TAB_POSITION) {
                mgrsCoordinateFragment.clearFocus();
                newPointPosition = mgrsCoordinateFragment.getLatLng();
            }

            if (newPointPosition == null) {
                CameraPosition position = map.getCameraPosition();
                newPointPosition = new LatLng(position.target.latitude, position.target.longitude);
            }
        }

        return newPointPosition;
    }

    /**
     * Add the geometry to the map as the current editing observation location, cleaning up existing shape
     *
     * @param geometry new geometry
     */
    private void addMapShape(Geometry geometry) {

        LatLng previousSelectedMarkerLocation = null;
        if (selectedMarker != null) {
            previousSelectedMarkerLocation = selectedMarker.getPosition();
            selectedMarker = null;
            clearRectangleCorners();
        }
        if (shapeMarkers != null) {
            shapeMarkers.remove();
            shapeMarkers = null;
        }
        if (geometry.getGeometryType() == GeometryType.POINT) {
            imageView.setImageBitmap(markerBitmap);
            Point point = (Point) geometry;
            updateLatitudeLongitudeText(new LatLng(point.getY(), point.getX()));
        } else {
            imageView.setImageBitmap(null);
            GoogleMapShape shape = shapeConverter.toShape(geometry);
            shapeMarkers = shapeConverter.addShapeToMapAsMarkers(map, shape, null,
                    editMarkerOptions, editMarkerOptions, null, editPolylineOptions, editPolygonOptions);
            List<Marker> markers = getShapeMarkers();
            Marker selectMarker = markers.get(0);
            if (previousSelectedMarkerLocation != null) {
                for (Marker marker : markers) {
                    if (marker.getPosition().equals(previousSelectedMarkerLocation)) {
                        selectMarker = marker;
                        break;
                    }
                }
            }
            selectShapeMarker(selectMarker);
        }

        updateHint();
    }

    /**
     * Update the hint text
     */
    private void updateHint() {
        updateHint(false);
    }

    /**
     * Update the hint text
     *
     * @param dragging true if a point is currently being dragged
     */
    private void updateHint(boolean dragging) {

        boolean locationEditHasFocus = false;
        int tabPosition = tabLayout.getSelectedTabPosition();
        if (tabPosition == WGS84_COORDINATE_TAB_POSITION) {
            locationEditHasFocus = wgs84CoordinateFragment.hasFocus();
        } else if (tabPosition == MGRS_COORDINATE_TAB_POSITION) {
            locationEditHasFocus = mgrsCoordinateFragment.hasFocus();
        }

        String hint = "";

        switch (shapeType) {
            case POINT:
                if (locationEditHasFocus) {
                    hint = getString(R.string.location_edit_hint_point_edit);
                } else {
                    hint = getString(R.string.location_edit_hint_point);
                }
                break;
            case POLYGON:
                if (isRectangle) {
                    if (locationEditHasFocus) {
                        hint = getString(R.string.location_edit_hint_rectangle_edit);
                    } else if (dragging) {
                        hint = getString(R.string.location_edit_hint_rectangle_drag);
                    } else if (!multipleShapeMarkerPositions()) {
                        hint = getString(R.string.location_edit_hint_rectangle_new);
                    } else {
                        hint = getString(R.string.location_edit_hint_rectangle);
                    }
                    break;
                }
            case LINESTRING:
                if (locationEditHasFocus) {
                    hint = getString(R.string.location_edit_hint_shape_edit);
                } else if (dragging) {
                    hint = getString(R.string.location_edit_hint_shape_drag);
                } else if (newDrawing) {
                    hint = getString(R.string.location_edit_hint_shape_new);
                } else {
                    hint = getString(R.string.location_edit_hint_shape);
                }
                break;
        }

        hintText.setText(hint);
    }

    /**
     * Update the latitude and longitude text entries
     *
     * @param latLng lat lng point
     */
    private void updateLatitudeLongitudeText(LatLng latLng) {
        wgs84CoordinateFragment.setLatLng(latLng);
        mgrsCoordinateFragment.setLatLng(latLng);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.location_edit_menu, menu);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.apply:
                updateLocation();
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Update the location into the response
     */
    private void updateLocation() {
        Geometry geometry = convertToGeometry();
        if (geometry == null) {
            return;
        }

        location.setGeometry(geometry);
        location.setProvider(ObservationLocation.MANUAL_PROVIDER);
        location.setAccuracy(0.0f);
        location.setTime(System.currentTimeMillis());

        Intent data = new Intent();
        data.setData(getIntent().getData());
        data.putExtra(LOCATION, location);
        setResult(RESULT_OK, data);

        // Save coordinate system used for edit
        CoordinateSystem coordinateSystem = tabLayout.getSelectedTabPosition() == 0 ? CoordinateSystem.WGS84 : CoordinateSystem.MGRS;
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        editor.putInt(getResources().getString(R.string.coordinateSystemEditKey), coordinateSystem.getPreferenceValue()).commit();

        finish();
    }

    private Geometry convertToGeometry() {

        // validate shape has minimum number of points
        if (shapeType == GeometryType.POLYGON && (!multipleShapeMarkerPositions() || getShapeMarkers().size() < 3)) {
            String errorMessage = isRectangle ? getString(R.string.location_edit_error_rectangle_min_points) : getString(R.string.location_edit_error_polygon_min_points);
            Snackbar.make(findViewById(R.id.coordinator_layout), errorMessage, Snackbar.LENGTH_SHORT).show();
            return null;
        } else if (shapeType == GeometryType.LINESTRING && !multipleShapeMarkerPositions()) {
            Snackbar.make(findViewById(R.id.coordinator_layout), getString(R.string.location_edit_error_linestring_min_points), Snackbar.LENGTH_SHORT).show();
            return null;
        }

        Geometry geometry;
        if (shapeType == GeometryType.POINT) {
            LatLng center = map.getCameraPosition().target;
            geometry = new Point(center.longitude, center.latitude);
        } else {
            // general shape validity test
            if (!shapeMarkers.isValid()) {
                Snackbar.make(findViewById(R.id.coordinator_layout), getString(R.string.location_edit_error_shape), Snackbar.LENGTH_SHORT).show();
                return null;
            }

            geometry = shapeConverter.toGeometry(shapeMarkers.getShape());

            // validate polygon does not intersect itself
            if (shapeType == GeometryType.POLYGON && MapUtils.polygonHasKinks((Polygon) geometry)) {
                Snackbar.make(findViewById(R.id.coordinator_layout), getString(R.string.location_edit_error_polygon_kinks), Snackbar.LENGTH_SHORT).show();
                return null;
            }
        }

        return geometry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMapClick(LatLng latLng) {
        clearCoordinateFocus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMarkerDrag(Marker marker) {
        updateLatitudeLongitudeText(marker.getPosition());
        updateShape(marker);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMarkerDragEnd(Marker marker) {
        updateLatitudeLongitudeText(marker.getPosition());
        updateShape(marker);
        if (isRectangle && shapeMarkers != null) {
            shapeMarkers.setVisibleMarkers(true);
        }
        updateHint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMarkerDragStart(Marker marker) {
        clearCoordinateFocus();
        updateHint(true);
        vibrator.vibrate(getResources().getInteger(
                R.integer.shape_edit_drag_long_click_vibrate));
        selectShapeMarker(marker);
        if (isRectangle && shapeMarkers != null) {
            shapeMarkers.setVisibleMarkers(false);
        }
    }

    /**
     * Find the neighboring rectangle corners
     *
     * @param marker selected marker
     */
    private void findRectangleCorners(Marker marker) {
        clearRectangleCorners();
        if (shapeMarkers != null && isRectangle) {
            List<Marker> markers = getShapeMarkers();
            boolean afterMatchesX = rectangleSameXSide1;
            for (int i = 0; i < markers.size(); i++) {
                Marker shapeMarker = markers.get(i);
                if (shapeMarker.getId().equals(marker.getId())) {
                    int beforeIndex = i > 0 ? i - 1 : markers.size() - 1;
                    int afterIndex = i < markers.size() - 1 ? i + 1 : 0;
                    Marker before = markers.get(beforeIndex);
                    Marker after = markers.get(afterIndex);
                    if (afterMatchesX) {
                        rectangleSameXMarker = after;
                        rectangleSameYMarker = before;
                    } else {
                        rectangleSameXMarker = before;
                        rectangleSameYMarker = after;
                    }
                }
                afterMatchesX = !afterMatchesX;
            }
        }
    }

    /**
     * Update the neighboring rectangle corners from the modified marker
     *
     * @param marker modified marker
     */
    private void updateRectangleCorners(Marker marker) {
        if (rectangleSameXMarker != null) {
            rectangleSameXMarker.setPosition(
                    new LatLng(rectangleSameXMarker.getPosition().latitude, marker.getPosition().longitude));
        }
        if (rectangleSameYMarker != null) {
            rectangleSameYMarker.setPosition(
                    new LatLng(marker.getPosition().latitude, rectangleSameYMarker.getPosition().longitude));
        }
    }

    /**
     * Clear the rectangle corners
     */
    private void clearRectangleCorners() {
        rectangleSameXMarker = null;
        rectangleSameYMarker = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMapLongClick(LatLng point) {

        // Add a new point to a line or polygon
        if (shapeType != GeometryType.POINT) {

            if (!isRectangle) {
                vibrator.vibrate(getResources().getInteger(
                        R.integer.shape_edit_add_long_click_vibrate));

                if (shapeMarkers == null) {
                    Geometry geometry = null;
                    Point firstPoint = new Point(point.longitude, point.latitude);
                    switch (shapeType) {
                        case LINESTRING:
                            LineString lineString = new LineString();
                            lineString.addPoint(firstPoint);
                            geometry = lineString;
                            break;
                        case POLYGON:
                            Polygon polygon = new Polygon();
                            LineString ring = new LineString();
                            ring.addPoint(firstPoint);
                            polygon.addRing(ring);
                            geometry = polygon;
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported Geometry Type: " + shapeType);
                    }
                    addMapShape(geometry);
                } else {
                    MarkerOptions markerOptions = getEditMarkerOptions();
                    markerOptions.position(point);
                    Marker marker = map.addMarker(markerOptions);
                    ShapeMarkers shape = null;
                    GoogleMapShape mapShape = shapeMarkers.getShape();
                    switch (mapShape.getShapeType()) {
                        case POLYLINE_MARKERS:
                            PolylineMarkers polylineMarkers = (PolylineMarkers) mapShape.getShape();
                            shape = polylineMarkers;
                            if (newDrawing) {
                                polylineMarkers.add(marker);
                            } else {
                                polylineMarkers.addNew(marker);
                            }
                            break;
                        case POLYGON_MARKERS:
                            PolygonMarkers polygonMarkers = (PolygonMarkers) shapeMarkers.getShape().getShape();
                            shape = polygonMarkers;
                            if (newDrawing) {
                                polygonMarkers.add(marker);
                            } else {
                                polygonMarkers.addNew(marker);
                            }
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported Shape Type: " + mapShape.getShapeType());
                    }
                    shapeMarkers.add(marker, shape);
                    selectShapeMarker(marker);
                    updateShape(marker);
                }
            } else if (!shapeMarkersValid() && selectedMarker != null) {
                // Allow long click to expand a zero area rectangle
                vibrator.vibrate(getResources().getInteger(
                        R.integer.shape_edit_add_long_click_vibrate));
                selectedMarker.setPosition(point);
                updateShape(selectedMarker);
                updateHint();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onMarkerClick(final Marker marker) {
        boolean handled = false;

        // Selecting and selected options for line and polygon markers
        if (shapeType != GeometryType.POINT) {
            final ShapeMarkers shape = shapeMarkers.getShapeMarkers(marker);
            if (shape != null) {

                if (selectedMarker == null || !selectedMarker.getId().equals(marker.getId())) {

                    selectShapeMarker(marker);

                } else if (!isRectangle && shapeMarkers.size() > 1) {

                    AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
                    LatLng position = marker.getPosition();
                    deleteDialog.setTitle(R.string.location_edit_delete_point_title);
                    deleteDialog.setMessage(String.format(getString(R.string.location_edit_delete_point_message),
                            locationFormatter.format(position.latitude), locationFormatter.format(position.longitude)));
                    deleteDialog.setNegativeButton(R.string.cancel, null);
                    deleteDialog.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            List<Marker> markers = getShapeMarkers();

                            // Find the index of the marker being deleted
                            int index = 1;
                            for (int i = 0; i < markers.size(); i++) {
                                if (markers.get(i).equals(marker)) {
                                    index = i;
                                    break;
                                }
                            }
                            // Get the previous marker index
                            if (index > 0) {
                                index--;
                            } else if (shapeType == GeometryType.LINESTRING) {
                                // Select next marker in the line
                                index++;
                            } else {
                                // Select previous polygon marker
                                index = markers.size() - 1;
                            }
                            // Get the new marker to select
                            Marker selectMarker = markers.get(index);

                            // Delete the marker, select the new, and update the shape
                            shapeMarkers.delete(marker);
                            selectedMarker = null;
                            selectShapeMarker(selectMarker);
                            updateShape(selectMarker);
                            updateHint();

                        }
                    });
                    deleteDialog.show();

                }

                handled = true;
            }
        }

        return handled;
    }

    /**
     * Select the provided shape marker
     *
     * @param marker marker to select
     */
    private void selectShapeMarker(Marker marker) {
        clearRectangleCorners();
        if (selectedMarker != null && !selectedMarker.getId().equals(marker.getId())) {
            selectedMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_shape_edit));
            selectedMarker.setZIndex(0.0f);
        }
        selectedMarker = marker;
        updateLatitudeLongitudeText(marker.getPosition());
        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_shape_edit_selected));
        marker.setZIndex(1.0f);
        findRectangleCorners(marker);
    }

    /**
     * Update the shape with any modifications, adjust the accept menu button state
     *
     * @param selectedMarker selected marker
     */
    private void updateShape(Marker selectedMarker) {
        updateRectangleCorners(selectedMarker);
        if (shapeMarkers != null) {
            shapeMarkers.update();
            if (shapeMarkers.isEmpty()) {
                shapeMarkers = null;
            }
        }
    }

    /**
     * Validate that the shape markers are a valid shape and contain multiple unique positions
     *
     * @return true if valid
     */
    private boolean shapeMarkersValid() {
        return multipleShapeMarkerPositions() && shapeMarkers.isValid();
    }

    /**
     * Determine if there are multiple unique locational positions in the shape markers
     *
     * @return true if multiple positions
     */
    private boolean multipleShapeMarkerPositions() {
        boolean multiple = false;
        if (shapeMarkers != null) {
            multiple = multipleMarkerPositions(getShapeMarkers());
        }
        return multiple;
    }

    /**
     * Determine if the are multiple unique locational positions in the markers
     *
     * @param markers markers
     * @return true if multiple positions
     */
    private boolean multipleMarkerPositions(List<Marker> markers) {
        boolean multiple = false;
        LatLng position = null;
        for (Marker marker : markers) {
            if (position == null) {
                position = marker.getPosition();
            } else if (!position.equals(marker.getPosition())) {
                multiple = true;
                break;
            }
        }
        return multiple;
    }

    /**
     * Get the shape markers
     *
     * @return shape markers
     */
    private List<Marker> getShapeMarkers() {
        return shapeMarkers.getShapeMarkersMap().values().iterator().next().getMarkers();
    }

    /**
     * Get the marker options for an edit point in a shape
     *
     * @return edit marker options
     */
    private MarkerOptions getEditMarkerOptions() {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_shape_edit));
        markerOptions.anchor(0.5f, 0.5f);
        markerOptions.draggable(true);
        return markerOptions;
    }

    /**
     * Get the edit polyline options
     *
     * @param style observation shape style
     * @return edit polyline options
     */
    private PolylineOptions getEditPolylineOptions(ObservationShapeStyle style) {
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.width(style.getStrokeWidth());
        polylineOptions.color(style.getStrokeColor());
        polylineOptions.geodesic(MapShapeObservation.GEODESIC);
        return polylineOptions;
    }

    /**
     * Get the edit polygon options
     *
     * @param style observation shape style
     * @return edit polygon options
     */
    private PolygonOptions getEditPolygonOptions(ObservationShapeStyle style) {
        PolygonOptions polygonOptions = new PolygonOptions();
        polygonOptions.strokeWidth(style.getStrokeWidth());
        polygonOptions.strokeColor(style.getStrokeColor());
        polygonOptions.fillColor(style.getFillColor());
        polygonOptions.geodesic(MapShapeObservation.GEODESIC);
        return polygonOptions;
    }

    public static class WGS84CoordinateFragment extends Fragment implements TextWatcher, View.OnFocusChangeListener {
        private CoordinateChangeListener coordinateChangeListener;

        private EditText latitudeEdit;
        private EditText longitudeEdit;

        private final int LOCATION_MAX_PRECISION = 6;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.wgs84_location_edit, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            latitudeEdit = (EditText) view.findViewById(R.id.location_edit_latitude);
            latitudeEdit.setFilters(new InputFilter[]{new InputFilterDecimal(-90.0, 90.0, LOCATION_MAX_PRECISION)});

            longitudeEdit = (EditText) view.findViewById(R.id.location_edit_longitude);
            longitudeEdit.setFilters(new InputFilter[]{new InputFilterDecimal(-180.0, 180.0, LOCATION_MAX_PRECISION)});

            longitudeEdit.addTextChangedListener(this);
            longitudeEdit.setOnFocusChangeListener(this);

            latitudeEdit.addTextChangedListener(this);
            latitudeEdit.setOnFocusChangeListener(this);

            latitudeEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        latitudeEdit.clearFocus();
                        return true;
                    }
                    return false;
                }
            });

            longitudeEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        longitudeEdit.clearFocus();
                        return true;
                    }
                    return false;
                }
            });

            clearFocus();
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);

            try {
                coordinateChangeListener = (CoordinateChangeListener) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString() + " must implement CoordinateChangeListener");
            }
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (v == longitudeEdit || v == latitudeEdit) {
                LatLng latLng = getLatLng();

                if (longitudeEdit.hasFocus() || latitudeEdit.hasFocus()) {
                    coordinateChangeListener.onCoordinateChangeStart(latLng);
                } else {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
                    coordinateChangeListener.onCoordinateChangeEnd(latLng);
                }
            }

        }

        @Override
        public void afterTextChanged(Editable s) {
            // Only handle when the longitude or latitude entries have focus
            if (!longitudeEdit.hasFocus() && !latitudeEdit.hasFocus()) {
                return;
            }

            LatLng latLng = getLatLng();

            if (latLng == null) {
                // TODO might want to show error here...
                latLng = new LatLng(0, 0);
            }

            coordinateChangeListener.onCoordinateChanged(latLng);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        public boolean setLatLng(LatLng latLng) {
            if (latLng == null) {
                return false;
            }

            latitudeEdit.setText(String.format(Locale.getDefault(), LOCATION_PRECISION, latLng.latitude));
            longitudeEdit.setText(String.format(Locale.getDefault(), LOCATION_PRECISION, latLng.longitude));

            return true;
        }

        private LatLng getLatLng() {
            String latitudeString = latitudeEdit.getText().toString();
            String longitudeString = longitudeEdit.getText().toString();

            Double latitude = null;
            if (!latitudeString.isEmpty()) {
                try {
                    latitude = Double.parseDouble(latitudeString);
                } catch (NumberFormatException e) {
                }
            }

            Double longitude = null;
            if (!longitudeString.isEmpty()) {
                try {
                    longitude = Double.parseDouble(longitudeString);
                } catch (NumberFormatException e) {
                }
            }

            if (latitude == null || longitude == null) {
                return null;
            }

            return new LatLng(latitude, longitude);
        }

        public boolean hasFocus() {
            return longitudeEdit.hasFocus() || latitudeEdit.hasFocus();
        }

        public void clearFocus() {
            longitudeEdit.clearFocus();
            latitudeEdit.clearFocus();
        }
    }

    public static class MGRSCoordinateFragment extends Fragment implements TextWatcher, View.OnFocusChangeListener {
        private  CoordinateChangeListener coordinateChangeListener;
        private EditText mgrsEdit;
        private TextInputLayout mgrsLayout;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.mgrs_location_edit, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            mgrsEdit = (EditText) view.findViewById(R.id.mgrs);
            mgrsLayout = (TextInputLayout) view.findViewById(R.id.mgrs_layout);

            mgrsEdit.addTextChangedListener(this);
            mgrsEdit.setOnFocusChangeListener(this);
            mgrsEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        mgrsEdit.clearFocus();
                        return true;
                    }
                    return false;
                }
            });
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);

            try {
                coordinateChangeListener = (CoordinateChangeListener) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString() + " must implement CoordinateChangeListener");
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Only handle when the mgrs has focus
            if (!mgrsEdit.hasFocus()) {
                return;
            }

            LatLng latLng = getLatLng();

            if (latLng == null) {
                mgrsLayout.setError("Invaild MGRS Code");
            } else {
                mgrsLayout.setError(null);
                mgrsLayout.setErrorEnabled(false);
                coordinateChangeListener.onCoordinateChanged(latLng);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            LatLng latLng = getLatLng();

            if (mgrsEdit.hasFocus()) {
                coordinateChangeListener.onCoordinateChangeStart(latLng);
            } else {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
                coordinateChangeListener.onCoordinateChangeEnd(latLng);
            }
        }

        public boolean setLatLng(LatLng latLng) {
            if (latLng == null) {
                return false;
            }

            MGRS mgrs = MGRS.from(new mil.nga.mgrs.wgs84.LatLng(latLng.latitude, latLng.longitude));
            mgrsEdit.setText(mgrs.format(5));
            mgrsLayout.setError(null);
            mgrsLayout.setErrorEnabled(false);

            return true;
        }

        public LatLng getLatLng() {
            try {
                mil.nga.mgrs.wgs84.LatLng latLng = mil.nga.mgrs.wgs84.LatLng.parse(mgrsEdit.getText().toString());
                return  new LatLng(latLng.latitude, latLng.longitude);
            } catch (ParseException e) {

            }

            return null;
        }

        public boolean hasFocus() {
            return mgrsEdit.hasFocus();
        }

        public void clearFocus() {
            mgrsEdit.clearFocus();
        }
    }

}
