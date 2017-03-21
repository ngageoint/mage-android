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
import java.util.Locale;

import mil.nga.geopackage.map.geom.GoogleMapShape;
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter;
import mil.nga.geopackage.map.geom.GoogleMapShapeMarkers;
import mil.nga.geopackage.map.geom.PolygonMarkers;
import mil.nga.geopackage.map.geom.PolylineMarkers;
import mil.nga.geopackage.map.geom.ShapeMarkers;
import mil.nga.giat.mage.R;
import mil.nga.wkb.geom.Geometry;
import mil.nga.wkb.geom.GeometryType;
import mil.nga.wkb.geom.LineString;
import mil.nga.wkb.geom.Point;
import mil.nga.wkb.geom.Polygon;

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
    private boolean newObservation;
    private GeometryType shapeType = GeometryType.POINT;

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
        newObservation = intent.getBooleanExtra(NEW_OBSERVATION, false);

        shapeTypeSpinner = (Spinner) findViewById(R.id.location_edit_shape_type);
        ArrayAdapter shapeTypeAdapter = ArrayAdapter.createFromResource(this, R.array.observationShapeType, R.layout.location_edit_spinner);
        shapeTypeAdapter.setDropDownViewResource(R.layout.location_edit_spinner_dropdown);
        shapeTypeSpinner.setAdapter(shapeTypeAdapter);
        longitudeEdit = (EditText) findViewById(R.id.location_edit_longitude);
        longitudeEdit.clearFocus();
        latitudeEdit = (EditText) findViewById(R.id.location_edit_latitude);
        latitudeEdit.clearFocus();

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        editMarkerOptions = getEditMarkerOptions();
        editPolylineOptions = getEditPolylineOptions();
        editPolygonOptions = getEditPolygonOptions();

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.location_edit_map);
        mapFragment.getMapAsync(this);
    }

    public void cancel(View v) {
        onBackPressed();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        map.setOnCameraIdleListener(this);
    }

    @Override
    public void onCameraIdle() {
        map.setOnCameraIdleListener(null);
        setupMap();
    }

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

        Point point = location.getCentroid();
        longitudeEdit.setText(String.format(Locale.getDefault(), "%.6f", point.getX()));
        latitudeEdit.setText(String.format(Locale.getDefault(), "%.6f", point.getY()));

        longitudeEdit.addTextChangedListener(this);
        longitudeEdit.setOnFocusChangeListener(this);

        latitudeEdit.addTextChangedListener(this);
        latitudeEdit.setOnFocusChangeListener(this);

        latitudeEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    latitudeEdit.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(latitudeEdit.getApplicationWindowToken(), 0);
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
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(longitudeEdit.getApplicationWindowToken(), 0);
                    return true;
                }

                return false;
            }
        });
    }

    private void setShapeType(Geometry geometry){
        shapeType = geometry.getGeometryType();
        int index = -1;
        switch(geometry.getGeometryType()){
            case POINT:
                index = 0;
                break;
            case LINESTRING:
                index = 1;
                break;
            case POLYGON:
                index = 2;
                break;
            default:
                throw new IllegalArgumentException("Unsupported Geometry Type: " + geometry.getGeometryType());

        }
        shapeTypeSpinner.setSelection(index);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        switch (parent.getId()) {
            case R.id.location_edit_shape_type:
                GeometryType newShapeType = null;
                switch (parent.getSelectedItemPosition()) {
                    case 0:
                        newShapeType = GeometryType.POINT;
                        break;
                    case 1:
                        newShapeType = GeometryType.LINESTRING;
                        break;
                    case 2:
                        newShapeType = GeometryType.POLYGON;
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported Shape Type item position: " + parent.getSelectedItemPosition());
                }
                confirmAndChangeShapeType(newShapeType);
                break;
            default:
                throw new IllegalArgumentException("Unsupported item selected adapter view: " + parent.getId());
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        switch (parent.getId()) {
            case R.id.location_edit_shape_type:
                changeShapeType(GeometryType.POINT);
                break;
            default:
                throw new IllegalArgumentException("Unsupported item selected adapter view: " + parent.getId());
        }
    }

    private void confirmAndChangeShapeType(GeometryType selectedType) {
        if (selectedType != shapeType) {
            // TODO
            changeShapeType(selectedType);
        }
    }

    private void changeShapeType(GeometryType selectedType) {
        shapeType = selectedType;
        // TODO
    }

    private void addMapShape(Geometry geometry) {
        if (shapeMarkers != null) {
            shapeMarkers.remove();
        }
        if (geometry.getGeometryType() == GeometryType.POINT) {
            imageView.setImageBitmap(markerBitmap);
        } else {
            imageView.setImageBitmap(null);
            GoogleMapShape shape = shapeConverter.toShape(geometry);
            shapeMarkers = shapeConverter.addShapeToMapAsMarkers(map, shape, null,
                    editMarkerOptions, editMarkerOptions, null, editPolylineOptions, editPolygonOptions);
        }
    }

    @Override
    public void onCameraMove() {
        if(shapeType == GeometryType.POINT) {
            CameraPosition position = map.getCameraPosition();
            updateLatitudeLongitudeText(position.target.latitude, position.target.longitude);
        }
    }

    private void updateLatitudeLongitudeText(LatLng latLng){
        updateLatitudeLongitudeText(latLng.latitude, latLng.longitude);
    }

    private void updateLatitudeLongitudeText(double latitude, double longitude){
        latitudeEdit.setText(String.format(Locale.getDefault(), "%.6f", latitude));
        longitudeEdit.setText(String.format(Locale.getDefault(), "%.6f", longitude));
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (getCurrentFocus() != longitudeEdit && getCurrentFocus() != latitudeEdit) {
            return;
        }

        double latitude = Double.parseDouble(latitudeEdit.getText().toString());
        double longitude = Double.parseDouble(longitudeEdit.getText().toString());
        LatLng latLng = new LatLng(latitude, longitude);

        map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.location_edit_menu, menu);
        acceptMenuItem = menu.findItem(R.id.apply);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
            case R.id.apply:
                updateLocation();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateLocation() {
        // TODO
        LatLng center = map.getCameraPosition().target;
        location.setGeometry(new Point(center.longitude, center.latitude));
        location.setProvider(ObservationLocation.MANUAL_PROVIDER);
        location.setAccuracy(0.0f);
        location.setTime(System.currentTimeMillis());

        Intent data = new Intent();
        data.setData(getIntent().getData());
        data.putExtra(LOCATION, location);
        setResult(RESULT_OK, data);

        finish();
    }

    @Override
    public void onMapClick(LatLng latLng) {
        longitudeEdit.clearFocus();
        latitudeEdit.clearFocus();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus && (v == longitudeEdit || v == latitudeEdit)) {
            map.setOnCameraMoveListener(null);
        } else {
            map.setOnCameraMoveListener(this);
        }
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        updateLatitudeLongitudeText(marker.getPosition());
        updateShape();
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        updateLatitudeLongitudeText(marker.getPosition());
        updateShape();
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        vibrator.vibrate(getResources().getInteger(
                R.integer.shape_edit_drag_long_click_vibrate));
        updateLatitudeLongitudeText(marker.getPosition());
    }

    @Override
    public void onMapLongClick(LatLng point) {
        if(shapeType != GeometryType.POINT){
            vibrator.vibrate(getResources().getInteger(
                    R.integer.shape_edit_add_long_click_vibrate));
            updateLatitudeLongitudeText(point);

            if(shapeMarkers == null){
                Geometry geometry = null;
                Point firstPoint = new Point(point.longitude, point.latitude);
                switch(shapeType){
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
            }else {
                MarkerOptions markerOptions = getEditMarkerOptions();
                markerOptions.position(point);
                Marker marker = map.addMarker(markerOptions);
                ShapeMarkers shape = null;
                GoogleMapShape mapShape = shapeMarkers.getShape();
                switch (mapShape.getShapeType()) {
                    case POLYLINE_MARKERS:
                        PolylineMarkers polylineMarkers = (PolylineMarkers) mapShape.getShape();
                        shape = polylineMarkers;
                        if (newObservation) {
                            polylineMarkers.add(marker);
                        } else {
                            polylineMarkers.addNew(marker);
                        }
                        break;
                    case POLYGON_MARKERS:
                        PolygonMarkers polygonMarkers = (PolygonMarkers) shapeMarkers.getShape().getShape();
                        shape = polygonMarkers;
                        if (newObservation) {
                            polygonMarkers.add(marker);
                        } else {
                            polygonMarkers.addNew(marker);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported Shape Type: " + mapShape.getShapeType());
                }
                shapeMarkers.add(marker, shape);
                updateShape();
            }
        }
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        boolean handled = false;
        if(shapeType != GeometryType.POINT){
            final ShapeMarkers shape = shapeMarkers.getShapeMarkers(marker);
            if(shape != null){
                updateLatitudeLongitudeText(marker.getPosition());

                ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.select_dialog_item);
                adapter.add(getString(R.string.shape_edit_delete_label));

                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
                DecimalFormat formatter = new DecimalFormat("0.0###");
                LatLng position = marker.getPosition();
                final String title = "(lat=" + formatter.format(position.latitude)
                        + ", lon=" + formatter.format(position.longitude) + ")";
                builder.setTitle(title);
                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {

                        if (item >= 0) {
                            switch (item) {
                                case 0:
                                    shapeMarkers.delete(marker);
                                    updateShape();
                                    break;
                                default:
                            }
                        }
                    }
                });

                AlertDialog alert = builder.create();
                alert.show();

                handled = true;
            }
        }

        return handled;
    }

    private void updateShape(){
        if (shapeMarkers != null) {
            shapeMarkers.update();
            if(shapeMarkers.isEmpty()){
                shapeMarkers = null;
            }
            if(acceptMenuItem != null) {
                acceptMenuItem.setEnabled(shapeMarkers != null && shapeMarkers.isValid());
            }
        }
    }

    private MarkerOptions getEditMarkerOptions() {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_shape_edit));
        markerOptions.anchor(0.5f, 0.5f);
        markerOptions.draggable(true);
        return markerOptions;
    }

    private PolylineOptions getEditPolylineOptions() {
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.color(ContextCompat.getColor(this, R.color.polyline_edit_color));
        return polylineOptions;
    }

    private PolygonOptions getEditPolygonOptions() {
        PolygonOptions polygonOptions = new PolygonOptions();
        polygonOptions.strokeColor(ContextCompat.getColor(this, R.color.polygon_edit_color));
        polygonOptions.fillColor(ContextCompat.getColor(this, R.color.polygon_edit_fill_color));
        return polygonOptions;
    }

}
