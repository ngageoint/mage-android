package mil.nga.giat.mage.map;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.bricolsoftconsulting.geocoderplus.Address;
import com.bricolsoftconsulting.geocoderplus.Geocoder;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.mgrs.MGRS;

public class GeocoderTask extends AsyncTask<String, Void, MarkerOptions> {

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
	protected MarkerOptions doInBackground(String... params) {
		String searchString = params[0];
		MarkerOptions markerOptions = null;

		if (MGRS.isMGRS(searchString)) {
			try {
				mil.nga.mgrs.wgs84.LatLng latLng = mil.nga.mgrs.wgs84.LatLng.parse(searchString);
				markerOptions = new MarkerOptions()
						.position(new LatLng(latLng.latitude, latLng.longitude))
						.title("MGRS")
						.snippet(searchString);
			} catch (ParseException e) {
				Log.e(LOG_NAME, "Problem parsing mgrs string", e);
			}
		} else {
			// Creating an instance of Geocoder class
			Geocoder geocoder = new Geocoder();

			try {
				List<Address> addresses = geocoder.getFromLocationName(searchString, 1);
				if (addresses != null && addresses.size() > 0) {
					Address address = addresses.get(0);
					markerOptions = new MarkerOptions()
						.position(new LatLng(address.getLatitude(), address.getLongitude()))
						.title(searchString)
						.snippet(address.getFormattedAddress());
				}
			} catch (IOException e) {
				Log.e(LOG_NAME, "Problem executing search.");
			}
		}

		return markerOptions;
	}

	@Override
	protected void onPostExecute(MarkerOptions markerOptions) {
		if (markers != null) {
			for (Marker m : markers) {
				m.remove();
			}
			markers.clear();
		}

		if (markerOptions == null) {
			if(ConnectivityUtility.isOnline(context)) {
				Toast.makeText(context, "Unknown location", Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(context, "No connectivity, try again later.", Toast.LENGTH_LONG).show();
			}
		} else {
			markers.add(map.addMarker(markerOptions));
			for (Marker m : markers) {
				m.showInfoWindow();
			}

			map.animateCamera(CameraUpdateFactory.newLatLngZoom(markerOptions.getPosition(), 10));
		}
	}
}
