package mil.nga.giat.mage.map;

import java.io.IOException;
import java.util.List;

import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.bricolsoftconsulting.geocoderplus.Address;
import com.bricolsoftconsulting.geocoderplus.Geocoder;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class GeocoderTask extends AsyncTask<String, Void, Address> {

	private static final String LOG_NAME = GeocoderTask.class.getName();

	private Context context;
	// reference to map
	private GoogleMap map;
	// reference to search marker
	private List<Marker> markers;

	public GeocoderTask(Context context, GoogleMap googleMap, List<Marker> markers) {
		this.context = context.getApplicationContext();
		this.map = googleMap;
		this.markers = markers;
	}

	@Override
	protected Address doInBackground(String... params) {
		Address address = null;
		String searchString = params[0];
		// Creating an instance of Geocoder class
		Geocoder geocoder = new Geocoder();

		try {
			List<Address> addresses = geocoder.getFromLocationName(searchString, 1);
			if (addresses != null && addresses.size() > 0) {
				address = addresses.get(0);
			}
		} catch (IOException e) {
			Log.e(LOG_NAME, "Problem executing search.");
		}

		return address;
	}

	@Override
	protected void onPostExecute(Address result) {
		if (markers != null) {
			for (Marker m : markers) {
				m.remove();
			}
			markers.clear();
		}

		if (result == null) {
			if(ConnectivityUtility.isOnline(context)) {
				Toast.makeText(context, "Unknown location", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(context, "No connectivity, try again later.", Toast.LENGTH_LONG).show();
			}
		} else {
			LatLng latLng = new LatLng(result.getLatitude(), result.getLongitude());
			String addressText = result.getFormattedAddress();

			if(addressText == null) {
				addressText = "";
			}
			
			MarkerOptions markerOptions = new MarkerOptions();
			markerOptions.position(latLng);
			markerOptions.title(addressText);
			markers.add(map.addMarker(markerOptions));
			for (Marker m : markers) {
				m.showInfoWindow();
			}

			Log.d(LOG_NAME, "Search found: " + addressText);
			
			LatLng southWestlatLng = new LatLng(result.getViewPort().getSouthWest().getLatitude(), result.getViewPort().getSouthWest().getLongitude());
			LatLng northEastlatLng = new LatLng(result.getViewPort().getNorthEast().getLatitude(), result.getViewPort().getNorthEast().getLongitude());
			
			map.animateCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(southWestlatLng, northEastlatLng), 10));
		}

		super.onPostExecute(result);
	}
}
