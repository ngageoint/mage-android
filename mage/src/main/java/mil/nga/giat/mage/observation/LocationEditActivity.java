package mil.nga.giat.mage.observation;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
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
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.android.gms.maps.GoogleMap.OnCameraMoveListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Locale;

import mil.nga.geopackage.map.geom.GoogleMapShape;
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter;
import mil.nga.geopackage.map.geom.GoogleMapShapeMarkers;
import mil.nga.giat.mage.R;
import mil.nga.wkb.geom.Geometry;
import mil.nga.wkb.geom.GeometryType;
import mil.nga.wkb.geom.Point;

public class LocationEditActivity extends AppCompatActivity implements TextWatcher, View.OnFocusChangeListener, OnMapClickListener, OnMapReadyCallback, OnCameraMoveListener, OnCameraIdleListener, OnItemSelectedListener {

	public static String LOCATION = "LOCATION";
	public static String MARKER_BITMAP = "MARKER_BITMAP";

	private ObservationLocation location;
	private GoogleMap map;
	private Spinner shapeTypeSpinner;
	private EditText longitudeEdit;
	private EditText latitudeEdit;
	private MapFragment mapFragment;
	private GoogleMapShapeMarkers shapeMarkers;
	private final GoogleMapShapeConverter shapeConverter = new GoogleMapShapeConverter();
	private final MarkerOptions markerOptions = new MarkerOptions();

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
		Bitmap markerBitmap = intent.getParcelableExtra(MARKER_BITMAP);
		markerOptions.icon(BitmapDescriptorFactory.fromBitmap(markerBitmap));

		shapeTypeSpinner = (Spinner) findViewById(R.id.location_edit_shape_type);
		ArrayAdapter shapeTypeAdapter = ArrayAdapter.createFromResource(this, R.array.observationShapeType, R.layout.location_edit_spinner);
		shapeTypeAdapter.setDropDownViewResource(R.layout.location_edit_spinner_dropdown);
		shapeTypeSpinner.setAdapter(shapeTypeAdapter);
		longitudeEdit = (EditText) findViewById(R.id.location_edit_longitude);
		longitudeEdit.clearFocus();
		latitudeEdit = (EditText) findViewById(R.id.location_edit_latitude);
		latitudeEdit.clearFocus();

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

		// TODO
		//ImageView iv = (ImageView) findViewById(R.id.location_edit_marker);
		//iv.setImageBitmap(markerBitmap);

		map.setOnCameraMoveListener(this);
		map.setOnMapClickListener(this);

		shapeTypeSpinner.setOnItemSelectedListener(this);

		addMapShape(location.getGeometry());

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

	@Override
	public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
		switch (parent.getId()){
			case R.id.location_edit_shape_type:
				GeometryType newShapeType = null;
				switch(parent.getSelectedItemPosition()){
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
				changeShapeType(newShapeType);
				break;
			default:
				throw new IllegalArgumentException("Unsupported item selected adapter view: " + parent.getId());
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent){
		switch (parent.getId()){
			case R.id.location_edit_shape_type:
				changeShapeType(GeometryType.POINT);
				break;
			default:
				throw new IllegalArgumentException("Unsupported item selected adapter view: " + parent.getId());
		}
	}

	private void confirmAndChangeShapeType(GeometryType selectedType){
		if(shapeMarkers == null || selectedType != shapeMarkers.getShape().getGeometryType()){
			// TODO
			changeShapeType(selectedType);
		}
	}

	private void changeShapeType(GeometryType selectedType){
	// TODO
	}

	private void addMapShape(Geometry geometry){
		if(shapeMarkers != null){
			shapeMarkers.remove();
		}
		GoogleMapShape shape = shapeConverter.toShape(geometry);
		shapeMarkers = shapeConverter.addShapeToMapAsMarkers(map, shape, markerOptions, null, null, null, null, null);
	}

	@Override
	public void onCameraMove() {
		CameraPosition position = map.getCameraPosition();
		longitudeEdit.setText(String.format(Locale.getDefault(), "%.6f", position.target.longitude));
		latitudeEdit.setText(String.format(Locale.getDefault(), "%.6f", position.target.latitude));
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
}
