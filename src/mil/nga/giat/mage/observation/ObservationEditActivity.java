package mil.nga.giat.mage.observation;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.preferences.PublicPreferencesActivity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

public class ObservationEditActivity extends FragmentActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.observation_editor);
		this.setTitle("Create New Observation");
		
		GoogleMap map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
		
		// TODO debugging location
		LatLng sydney = new LatLng(-33.867, 151.206);
		
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 15));
		
		map.addMarker(new MarkerOptions()
        .title("Sydney")
        .snippet("The most populous city in Australia.")
        .position(sydney));
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
			break;
		}

		return super.onOptionsItemSelected(item);
	}
	
}
