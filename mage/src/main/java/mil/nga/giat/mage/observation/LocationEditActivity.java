package mil.nga.giat.mage.observation;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import mil.nga.giat.mage.R;

public class LocationEditActivity extends Activity implements OnCameraChangeListener, OnFocusChangeListener, OnMapReadyCallback, OnMapClickListener {

	public static String LOCATION = "LOCATION";
	public static String MARKER_BITMAP = "MARKER_BITMAP";

	private Location l;
	private Bitmap markerBitmap;
	private GoogleMap map;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		l = intent.getParcelableExtra(LOCATION);
		markerBitmap = intent.getParcelableExtra(MARKER_BITMAP);
		setContentView(R.layout.location_edit);

		((MapFragment) getFragmentManager().findFragmentById(R.id.location_edit_map)).getMapAsync(this);
	}

	public void cancel(View v) {
		onBackPressed();
	}

	public void updateLocation(View v) {
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
	public void onMapReady(GoogleMap map) {
		this.map = map;

		LatLng location = new LatLng(l.getLatitude(), l.getLongitude());

		map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 18));

		ImageView iv = (ImageView) findViewById(R.id.location_edit_marker);
		iv.setImageBitmap(markerBitmap);
		map.setOnCameraChangeListener(this);
		map.setOnMapClickListener(this);

		EditText longitudeEdit = (EditText) findViewById(R.id.location_edit_longitude);
		longitudeEdit.setText(Double.toString(l.getLongitude()));
		longitudeEdit.setOnFocusChangeListener(this);

		EditText latitudeEdit = (EditText) findViewById(R.id.location_edit_latitude);
		latitudeEdit.setText(Double.toString(l.getLatitude()));
		latitudeEdit.setOnFocusChangeListener(this);
	}

	@Override
	public void onMapClick(LatLng location) {
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
		if (getCurrentFocus() != null) {
			inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
		}
		map.setOnCameraChangeListener(LocationEditActivity.this);
	}

	@Override
	public void onCameraChange(CameraPosition position) {
		EditText longitudeEdit = (EditText) findViewById(R.id.location_edit_longitude);
		EditText latitudeEdit = (EditText) findViewById(R.id.location_edit_latitude);
		longitudeEdit.setText(Double.toString(position.target.longitude));
		latitudeEdit.setText(Double.toString(position.target.latitude));
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		if (!hasFocus) {
			EditText longitudeEdit = (EditText) findViewById(R.id.location_edit_longitude);
			EditText latitudeEdit = (EditText) findViewById(R.id.location_edit_latitude);
			try {
				l.setLatitude(Double.parseDouble(latitudeEdit.getText().toString()));
				l.setLongitude(Double.parseDouble(longitudeEdit.getText().toString()));
				LatLng location = new LatLng(l.getLatitude(), l.getLongitude());
				map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, map.getCameraPosition().zoom));
				map.setOnCameraChangeListener(LocationEditActivity.this);
			} catch (NumberFormatException pe) {
				// TODO: warn the user
			}
		} else {
			map.setOnCameraChangeListener(null);
		}
	}
}
