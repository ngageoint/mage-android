package mil.nga.giat.mage.map;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.mgrs.MGRS;

public class GeocoderTask extends AsyncTask<String, Void, GeocoderTask.SearchResults> {

	private static final String LOG_NAME = GeocoderTask.class.getName();

	private static final int MAX_ADDRESS_LINES = 3;
	private static final int MAX_ADDRESS_ZOOM = 18;

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
	protected SearchResults doInBackground(String... params) {
		String searchString = params[0];
		SearchResults results = new SearchResults();

		if (MGRS.isMGRS(searchString)) {
			try {
				mil.nga.mgrs.wgs84.LatLng latLng = mil.nga.mgrs.wgs84.LatLng.parse(searchString);
				LatLng position = new LatLng(latLng.latitude, latLng.longitude);
				results.markerOptions = new MarkerOptions()
						.position(position)
						.title("MGRS")
						.snippet(searchString);

				results.zoom = 18;
			} catch (ParseException e) {
				Log.e(LOG_NAME, "Problem parsing mgrs string", e);
			}
		} else {
			// Creating an instance of Geocoder class
			Geocoder geocoder = new Geocoder(context);

			try {
				List<Address> addresses = geocoder.getFromLocationName(searchString, 1);
				if (addresses != null && addresses.size() > 0) {
					Address address = addresses.get(0);

					int addressLines = address.getMaxAddressLineIndex() + 1;
					results.zoom = MAX_ADDRESS_ZOOM - ((MAX_ADDRESS_LINES - addressLines) * 2);

					results.markerOptions = new MarkerOptions()
						.position(new LatLng(address.getLatitude(), address.getLongitude()))
						.title(searchString)
						.snippet(address.getAddressLine(0));

				}
			} catch (IOException e) {
				Log.e(LOG_NAME, "Problem executing search.", e);
			}
		}

		return results;
	}

	@Override
	protected void onPostExecute(SearchResults results) {
		if (markers != null) {
			for (Marker m : markers) {
				m.remove();
			}
			markers.clear();
		}

		if (results.markerOptions == null) {
			if(ConnectivityUtility.isOnline(context)) {
				Toast.makeText(context, "Could not find address.", Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(context, "No connectivity, try again later.", Toast.LENGTH_LONG).show();
			}
		} else {
			markers.add(map.addMarker(results.markerOptions));
			for (Marker m : markers) {
				m.showInfoWindow();
			}

			CameraPosition position = CameraPosition.builder()
					.target(results.markerOptions.getPosition())
					.zoom(results.zoom).build();

			map.animateCamera(CameraUpdateFactory.newCameraPosition(position));
		}
	}

	public class SearchResults {
		public MarkerOptions markerOptions;
		public int zoom;
	}
}
