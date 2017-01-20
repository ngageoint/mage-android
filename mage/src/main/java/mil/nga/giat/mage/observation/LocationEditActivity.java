package mil.nga.giat.mage.observation;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.Locale;

import mil.nga.giat.mage.R;

public class LocationEditActivity extends AppCompatActivity implements TextWatcher, View.OnFocusChangeListener, GoogleMap.OnMapClickListener, OnMapReadyCallback, GoogleMap.OnCameraMoveListener {

	public static String LOCATION = "LOCATION";
	public static String MARKER_BITMAP = "MARKER_BITMAP";

	private Location l;
	private Bitmap markerBitmap;
	private GoogleMap map;
	private EditText longitudeEdit;
	private EditText latitudeEdit;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.location_edit);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		l = intent.getParcelableExtra(LOCATION);
		markerBitmap = intent.getParcelableExtra(MARKER_BITMAP);

		longitudeEdit = (EditText) findViewById(R.id.location_edit_longitude);
		longitudeEdit.clearFocus();
		latitudeEdit = (EditText) findViewById(R.id.location_edit_latitude);
		latitudeEdit.clearFocus();

		((MapFragment) getFragmentManager().findFragmentById(R.id.location_edit_map)).getMapAsync(this);
	}

	public void cancel(View v) {
		onBackPressed();
	}

	@Override
	public void onMapReady(GoogleMap map) {
		this.map = map;

		LatLng location = new LatLng(l.getLatitude(), l.getLongitude());

		map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 18));

		ImageView iv = (ImageView) findViewById(R.id.location_edit_marker);
		iv.setImageBitmap(markerBitmap);

		map.setOnCameraMoveListener(this);
		map.setOnMapClickListener(this);

		longitudeEdit.setText(String.format(Locale.getDefault(), "%.6f", l.getLongitude()));
		latitudeEdit.setText(String.format(Locale.getDefault(), "%.6f", l.getLatitude()));

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
		l.setLatitude(center.latitude);
		l.setLongitude(center.longitude);
		l.setProvider("manual");
		l.setAccuracy(0.0f);
		l.setTime(System.currentTimeMillis());

		Intent data = new Intent();
		data.setData(getIntent().getData());
		data.putExtra(LOCATION, l);
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
