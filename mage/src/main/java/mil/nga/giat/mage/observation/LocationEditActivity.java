package mil.nga.giat.mage.observation;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.android.gms.maps.GoogleMap.OnCameraMoveListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

import mil.nga.geopackage.map.geom.GoogleMapShape;
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter;
import mil.nga.geopackage.map.geom.GoogleMapShapeMarkers;
import mil.nga.geopackage.map.geom.PolygonMarkers;
import mil.nga.geopackage.map.geom.PolylineMarkers;
import mil.nga.geopackage.map.geom.ShapeMarkers;
import mil.nga.giat.mage.R;
import mil.nga.wkb.geom.Geometry;
import mil.nga.wkb.geom.GeometryEnvelope;
import mil.nga.wkb.geom.GeometryType;
import mil.nga.wkb.geom.LineString;
import mil.nga.wkb.geom.Point;
import mil.nga.wkb.geom.Polygon;
import mil.nga.wkb.util.GeometryEnvelopeBuilder;

public class LocationEditActivity extends AppCompatActivity implements TextWatcher, View.OnFocusChangeListener,
        OnMapClickListener, OnMapReadyCallback, OnCameraMoveListener, OnCameraIdleListener, OnItemSelectedListener,
        OnMarkerDragListener, OnMapLongClickListener, OnMarkerClickListener {

    public static String LOCATION = "LOCATION";
    public static String MARKER_BITMAP = "MARKER_BITMAP";
    public static String NEW_OBSERVATION = "NEW_OBSERVATION";

    private ObservationLocation location;
    private GoogleMap map;
    private Spinner shapeTypeSpinner;
    private EditText longitudeEdit;
    private EditText latitudeEdit;
    private MapFragment mapFragment;
    private GoogleMapShapeMarkers shapeMarkers;
    private final GoogleMapShapeConverter shapeConverter = new GoogleMapShapeConverter();
    private Bitmap markerBitmap;
    private ImageView imageView = null;
    private MarkerOptions editMarkerOptions;
    private PolylineOptions editPolylineOptions;
    private PolygonOptions editPolygonOptions;
    private Vibrator vibrator;
    private MenuItem acceptMenuItem;
    private boolean newDrawing;
    private GeometryType shapeType = GeometryType.POINT;
    private boolean isRectangle = false;
    private Marker selectedMarker = null;
    private final DecimalFormat formatter = new DecimalFormat("0.00000");
    private Marker rectangleSameXMarker;
    private Marker rectangleSameYMarker;
    private boolean rectangleSameXSide1;

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

        Intent intent = getIntent();
        location = intent.getParcelableExtra(LOCATION);
        markerBitmap = intent.getParcelableExtra(MARKER_BITMAP);
        newDrawing = intent.getBooleanExtra(NEW_OBSERVATION, false);

        shapeTypeSpinner = (Spinner) findViewById(R.id.location_edit_shape_type);
        ArrayAdapter shapeTypeAdapter = ArrayAdapter.createFromResource(this, R.array.observationShapeType, R.layout.location_edit_spinner);
        shapeTypeAdapter.setDropDownViewResource(R.layout.location_edit_spinner_dropdown);
        shapeTypeSpinner.setAdapter(shapeTypeAdapter);
        longitudeEdit = (EditText) findViewById(R.id.location_edit_longitude);
        latitudeEdit = (EditText) findViewById(R.id.location_edit_latitude);
        clearLatitudeAndLongitudeFocus();

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        editMarkerOptions = getEditMarkerOptions();
        editPolylineOptions = getEditPolylineOptions();
        editPolygonOptions = getEditPolygonOptions();

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
     * Setup the map after it has been fully loaded
     */
    private void setupMap() {

        map.moveCamera(location.getCameraUpdate(mapFragment.getView()));

        imageView = (ImageView) findViewById(R.id.location_edit_marker);

        map.setOnCameraMoveListener(this);
        map.setOnMapClickListener(this);
        map.setOnMapLongClickListener(this);
        map.setOnMarkerClickListener(this);
        map.setOnMarkerDragListener(this);

        shapeTypeSpinner.setOnItemSelectedListener(this);
        Geometry geometry = location.getGeometry();
        setShapeType(geometry);
        addMapShape(geometry);

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
    }

    /**
     * Clear the focus from the latitude and longitude text entries
     */
    private void clearLatitudeAndLongitudeFocus() {
        longitudeEdit.clearFocus();
        latitudeEdit.clearFocus();
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
            checkIfRectangle(points);
        }
    }

    /**
     * Check if the points form a rectangle
     *
     * @param points points
     */
    private void checkIfRectangle(List<Point> points) {
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
        int index = -1;
        switch (shapeType) {
            case POINT:
                index = 0;
                break;
            case LINESTRING:
                index = 1;
                break;
            case POLYGON:
                index = 2;
                if (!isRectangle) {
                    index++;
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported Geometry Type: " + shapeType);
        }
        shapeTypeSpinner.setSelection(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        switch (parent.getId()) {
            case R.id.location_edit_shape_type:
                // User has changed the shape type
                GeometryType newShapeType = null;
                boolean newRectangle = false;
                switch (parent.getSelectedItemPosition()) {
                    case 0:
                        newShapeType = GeometryType.POINT;
                        break;
                    case 1:
                        newShapeType = GeometryType.LINESTRING;
                        break;
                    case 2:
                        newRectangle = true;
                    case 3:
                        newShapeType = GeometryType.POLYGON;
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported Shape Type item position: " + parent.getSelectedItemPosition());
                }
                confirmAndChangeShapeType(newShapeType, newRectangle);
                break;
            default:
                throw new IllegalArgumentException("Unsupported item selected adapter view: " + parent.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        switch (parent.getId()) {
            case R.id.location_edit_shape_type:
                // Default to point shape
                changeShapeType(GeometryType.POINT, false);
                break;
            default:
                throw new IllegalArgumentException("Unsupported item selected adapter view: " + parent.getId());
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

            // If changing to a point and there are multiple points in the current shape, confirm selection
            if (selectedType == GeometryType.POINT && shapeMarkers != null && shapeMarkers.getSize() > 1) {
                LatLng newPointPosition = getShapeToPointLocation();

                AlertDialog deleteDialog = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
                        .setTitle(getString(R.string.shape_edit_change_shape_title))
                        .setMessage(String.format(getString(R.string.shape_edit_change_shape_message),
                                formatter.format(newPointPosition.latitude), formatter.format(newPointPosition.longitude)))
                        .setPositiveButton(getString(R.string.yes),

                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        changeShapeType(selectedType, selectedRectangle);
                                    }
                                })
                        .setOnCancelListener(
                                new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        revertShapeType();
                                    }
                                })
                        .setNegativeButton(getString(R.string.no),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        revertShapeType();
                                        dialog.dismiss();
                                    }
                                }).create();
                deleteDialog.show();
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
                GoogleMapShape mapShape = shapeMarkers.getShape();
                List<LatLng> latLngPoints = null;
                switch (mapShape.getShapeType()) {
                    case POLYLINE_MARKERS:
                        PolylineMarkers polylineMarkers = (PolylineMarkers) mapShape.getShape();
                        latLngPoints = polylineMarkers.getPolyline().getPoints();
                        break;
                    case POLYGON_MARKERS:
                        PolygonMarkers polygonMarkers = (PolygonMarkers) mapShape.getShape();
                        latLngPoints = polygonMarkers.getPolygon().getPoints();
                        // Break the polygon closure when changing to a line
                        if (selectedType == GeometryType.LINESTRING && latLngPoints.size() > 1 && latLngPoints.get(0).equals(latLngPoints.get(latLngPoints.size() - 1))) {
                            latLngPoints.remove(latLngPoints.size() - 1);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported Shape Type: " + mapShape.getShapeType());
                }
                lineString = shapeConverter.toLineString(latLngPoints);
            }

            switch (selectedType) {

                case LINESTRING:
                    newDrawing = lineString.getPoints().size() <= 1;
                    geometry = lineString;
                    break;

                case POLYGON:

                    // If converting to a rectangle, use the current shape bounds
                    if (selectedRectangle) {
                        GeometryEnvelope envelope = GeometryEnvelopeBuilder.buildEnvelope(lineString);
                        lineString = new LineString();
                        lineString.addPoint(new Point(envelope.getMinX(), envelope.getMaxY()));
                        lineString.addPoint(new Point(envelope.getMinX(), envelope.getMinY()));
                        lineString.addPoint(new Point(envelope.getMaxX(), envelope.getMinY()));
                        lineString.addPoint(new Point(envelope.getMaxX(), envelope.getMaxY()));
                        lineString.addPoint(new Point(envelope.getMinX(), envelope.getMaxY()));
                        checkIfRectangle(lineString.getPoints());
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

        addMapShape(geometry);
        shapeType = selectedType;
        updateAcceptState();
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
            String latitudeString = latitudeEdit.getText().toString();
            String longitudeString = longitudeEdit.getText().toString();
            double latitude = 0;
            double longitude = 0;
            if (!latitudeString.isEmpty() && !longitudeString.isEmpty()) {
                latitude = Double.parseDouble(latitudeString);
                longitude = Double.parseDouble(longitudeString);
            } else {
                CameraPosition position = map.getCameraPosition();
                latitude = position.target.latitude;
                longitude = position.target.longitude;
            }
            newPointPosition = new LatLng(latitude, longitude);
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
        }
        if (geometry.getGeometryType() == GeometryType.POINT) {
            imageView.setImageBitmap(markerBitmap);
            Point point = (Point) geometry;
            updateLatitudeLongitudeText(point.getY(), point.getX());
        } else {
            imageView.setImageBitmap(null);
            GoogleMapShape shape = shapeConverter.toShape(geometry);
            shapeMarkers = shapeConverter.addShapeToMapAsMarkers(map, shape, null,
                    editMarkerOptions, editMarkerOptions, null, editPolylineOptions, editPolygonOptions);
            List<Marker> markers = shapeMarkers.getShapeMarkersMap().values().iterator().next().getMarkers();
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCameraMove() {
        // Points are represented by the camera position
        if (shapeType == GeometryType.POINT) {
            clearLatitudeAndLongitudeFocus();
            CameraPosition position = map.getCameraPosition();
            updateLatitudeLongitudeText(position.target.latitude, position.target.longitude);
        }
    }

    /**
     * Update the latitude and longitude text entries
     *
     * @param latLng lat lng point
     */
    private void updateLatitudeLongitudeText(LatLng latLng) {
        updateLatitudeLongitudeText(latLng.latitude, latLng.longitude);
    }

    /**
     * Update the latitude and longitude text entries
     *
     * @param latitude  latitude
     * @param longitude longitude
     */
    private void updateLatitudeLongitudeText(double latitude, double longitude) {
        latitudeEdit.setText(String.format(Locale.getDefault(), "%.5f", latitude));
        longitudeEdit.setText(String.format(Locale.getDefault(), "%.5f", longitude));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterTextChanged(Editable s) {
        // Only handle when the longitude or latitude entries have focus
        if (getCurrentFocus() != longitudeEdit && getCurrentFocus() != latitudeEdit) {
            return;
        }

        // Move the camera and update selected markers & shape
        String latitudeString = latitudeEdit.getText().toString();
        String longitudeString = longitudeEdit.getText().toString();
        if (!latitudeString.isEmpty() && !longitudeString.isEmpty()) {
            double latitude = Double.parseDouble(latitudeString);
            double longitude = Double.parseDouble(longitudeString);
            LatLng latLng = new LatLng(latitude, longitude);

            map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            if (selectedMarker != null) {
                selectedMarker.setPosition(latLng);
                updateRectangleCorners(selectedMarker);
                updateShape();
            }

        }
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.location_edit_menu, menu);
        acceptMenuItem = menu.findItem(R.id.apply);
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

        Geometry geometry = null;
        if (shapeType == GeometryType.POINT) {
            LatLng center = map.getCameraPosition().target;
            geometry = new Point(center.longitude, center.latitude);
        } else {
            geometry = shapeConverter.toGeometry(shapeMarkers.getShape());
        }

        location.setGeometry(geometry);
        location.setProvider(ObservationLocation.MANUAL_PROVIDER);
        location.setAccuracy(0.0f);
        location.setTime(System.currentTimeMillis());

        Intent data = new Intent();
        data.setData(getIntent().getData());
        data.putExtra(LOCATION, location);
        setResult(RESULT_OK, data);

        finish();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMapClick(LatLng latLng) {
        clearLatitudeAndLongitudeFocus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (v == longitudeEdit || v == latitudeEdit) {
            if (hasFocus) {
                // Move the camera to a selected line or polygon marker
                if (shapeType != GeometryType.POINT && selectedMarker != null) {
                    map.moveCamera(CameraUpdateFactory.newLatLng(selectedMarker.getPosition()));
                }
            } else {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
            }

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMarkerDrag(Marker marker) {
        updateLatitudeLongitudeText(marker.getPosition());
        updateRectangleCorners(marker);
        updateShape();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMarkerDragEnd(Marker marker) {
        updateLatitudeLongitudeText(marker.getPosition());
        updateRectangleCorners(marker);
        updateShape();
        if (isRectangle && shapeMarkers != null) {
            shapeMarkers.setVisibleMarkers(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMarkerDragStart(Marker marker) {
        clearLatitudeAndLongitudeFocus();
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
            List<Marker> markers = shapeMarkers.getShapeMarkersMap().values().iterator().next().getMarkers();
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
        if (shapeType != GeometryType.POINT && !isRectangle) {
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
                updateShape();
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

                } else if (!isRectangle) {

                    ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.select_dialog_item);
                    adapter.add(getString(R.string.shape_edit_delete_label));

                    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
                    LatLng position = marker.getPosition();
                    final String title = formatter.format(position.latitude) + ", " + formatter.format(position.longitude);
                    builder.setTitle(title);
                    builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {

                            if (item >= 0) {
                                switch (item) {
                                    case 0:
                                        shapeMarkers.delete(marker);
                                        selectedMarker = null;
                                        updateShape();
                                        break;
                                    default:
                                }
                            }
                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();
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
        if (selectedMarker != null && !selectedMarker.getId().equals(marker.getId())) {
            selectedMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_shape_edit));
        }
        selectedMarker = marker;
        updateLatitudeLongitudeText(marker.getPosition());
        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_shape_edit_selected));
        findRectangleCorners(marker);
    }

    /**
     * Update the shape with any modifications, adjust the accept menu button state
     */
    private void updateShape() {
        if (shapeMarkers != null) {
            shapeMarkers.update();
            if (shapeMarkers.isEmpty()) {
                shapeMarkers = null;
            }
        }
        updateAcceptState();
    }

    /**
     * Update the accept button state
     */
    private void updateAcceptState() {
        boolean acceptEnabled = true;
        if (shapeMarkers != null) {
            acceptEnabled = shapeMarkersValid();
        }
        if (acceptMenuItem != null) {
            acceptMenuItem.setEnabled(acceptEnabled);
        }
    }

    /**
     * Validate that the shape markers are a valid shape and contain multiple unique positions
     *
     * @return true if valid
     */
    private boolean shapeMarkersValid() {
        boolean valid = shapeMarkers != null && shapeMarkers.isValid();
        if (valid) {
            List<Marker> markers = shapeMarkers.getShapeMarkersMap().values().iterator().next().getMarkers();
            LatLng position = null;
            valid = false;
            for (Marker marker : markers) {
                if (position == null) {
                    position = marker.getPosition();
                } else if (!position.equals(marker.getPosition())) {
                    valid = true;
                    break;
                }
            }
        }
        return valid;
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
     * @return edit polyline options
     */
    private PolylineOptions getEditPolylineOptions() {
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.color(ContextCompat.getColor(this, R.color.polyline_edit_color));
        return polylineOptions;
    }

    /**
     * Get the edit polygon options
     *
     * @return edit polygon options
     */
    private PolygonOptions getEditPolygonOptions() {
        PolygonOptions polygonOptions = new PolygonOptions();
        polygonOptions.strokeColor(ContextCompat.getColor(this, R.color.polygon_edit_color));
        polygonOptions.fillColor(ContextCompat.getColor(this, R.color.polygon_edit_fill_color));
        return polygonOptions;
    }

}
