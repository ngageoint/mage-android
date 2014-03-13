package mil.nga.giat.mage.observation;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import mil.nga.giat.mage.R;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

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
	
}
