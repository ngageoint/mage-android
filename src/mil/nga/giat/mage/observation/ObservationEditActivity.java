package mil.nga.giat.mage.observation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.common.Geometry;
import mil.nga.giat.mage.sdk.datastore.common.GeometryType;
import mil.nga.giat.mage.sdk.datastore.common.Property;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.State;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class ObservationEditActivity extends FragmentActivity {
	
	Observation obervation;
	Date date;
	DecimalFormat latLngFormat = new DecimalFormat("###.######");

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.observation_editor);
		this.setTitle("Create New Observation");
		
		GoogleMap map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
		
		Intent intent = getIntent();
		LatLng location = new LatLng(intent.getDoubleExtra("latitude", 0.0), intent.getDoubleExtra("longitude", 0.0));
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
		map.addMarker(new MarkerOptions().position(location));
		
		((TextView) findViewById(R.id.location)).setText(latLngFormat.format(location.latitude) + ", " + latLngFormat.format(location.longitude));
		date = new Date();
		((TextView) findViewById(R.id.date)).setText(date.toString());
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.observation_edit_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		System.out.println("STarting the observation view");
		switch (item.getItemId()) {
	
		case R.id.observation_save:
			System.out.println("SAVE");
			
			double lat = 39.7;
			double lon = -104.0;
			
			
			//o.setGeometry(new Geometry(lat, lon), new GeometryType("point"));
			obervation = new Observation();
			obervation.setState(new State("active"));
			obervation.setGeometry(new Geometry("[" + lat + "," + lon + "]", new GeometryType("point")));
			
			Collection<Property> properties = new ArrayList<Property>();
			properties.add(new Property("OBSERVATION_DATE", String.valueOf(date.getTime())));
			properties.add(new Property("TYPE", (String)((Spinner)findViewById(R.id.type_spinner)).getSelectedItem()));
			properties.add(new Property("LEVEL", ((EditText)findViewById(R.id.level)).getText().toString()));
			properties.add(new Property("TEAM", ((EditText)findViewById(R.id.team)).getText().toString()));
			properties.add(new Property("DESCRIPTION", ((EditText)findViewById(R.id.description)).getText().toString()));			
			obervation.setProperties(properties);
			
			
			ObservationHelper oh = ObservationHelper.getInstance(getApplicationContext());
			try {
				Observation newObs = oh.createObservation(obervation);
				System.out.println(newObs);
			} catch (Exception e) {
				
			}
			
			break;
		}

		return super.onOptionsItemSelected(item);
	}
	
}