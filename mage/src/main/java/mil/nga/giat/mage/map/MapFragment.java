package mil.nga.giat.mage.map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.MAGE.OnCacheOverlayListener;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.event.EventBannerFragment;
import mil.nga.giat.mage.filter.DateTimeFilter;
import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.map.GoogleMapWrapper.OnMapPanListener;
import mil.nga.giat.mage.map.marker.LocationMarkerCollection;
import mil.nga.giat.mage.map.marker.MyHistoricalLocationMarkerCollection;
import mil.nga.giat.mage.map.marker.ObservationMarkerCollection;
import mil.nga.giat.mage.map.marker.PointCollection;
import mil.nga.giat.mage.map.marker.StaticGeometryCollection;
import mil.nga.giat.mage.map.preference.MapPreferencesActivity;
import mil.nga.giat.mage.observation.ObservationEditActivity;
import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.location.LocationProperty;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureHelper;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.event.ILocationEventListener;
import mil.nga.giat.mage.sdk.event.IObservationEventListener;
import mil.nga.giat.mage.sdk.event.IStaticFeatureEventListener;
import mil.nga.giat.mage.sdk.exceptions.LayerException;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.location.LocationService;

public class MapFragment extends Fragment implements OnMapClickListener, OnMapLongClickListener, OnMarkerClickListener, OnInfoWindowClickListener, OnMapPanListener, OnMyLocationButtonClickListener, OnClickListener, LocationSource, LocationListener, OnCacheOverlayListener, OnSharedPreferenceChangeListener,
		IObservationEventListener, ILocationEventListener, IStaticFeatureEventListener {

	private static final String LOG_NAME = MapFragment.class.getName();

	private MAGE mage;
	private MapView mapView;
	private GoogleMap map;
	private View searchLayout;
	private EditText edittextSearch;
	private Location location;
	private boolean followMe = false;
	private GoogleMapWrapper mapWrapper;
	protected User currentUser = null;
	private OnLocationChangedListener locationChangedListener;

	private static final int REFRESHMARKERINTERVALINSECONDS = 30;
	private RefreshMarkersTask refreshLocationsMarkersTask;
	private RefreshMarkersTask refreshMyHistoricLocationsMarkersTask;

	private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(64);
	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 10, TimeUnit.SECONDS, queue);

	private PointCollection<Observation> observations;
	private PointCollection<mil.nga.giat.mage.sdk.datastore.location.Location> locations;
	private PointCollection<mil.nga.giat.mage.sdk.datastore.location.Location> myHistoricLocations;
	private StaticGeometryCollection staticGeometryCollection;
	private List<Marker> searchMarkes = new ArrayList<Marker>();

	private Map<String, TileOverlay> tileOverlays = new HashMap<String, TileOverlay>();

	private LocationService locationService;

	public static String INITIAL_LOCATION = "INITIAL_LOCATION";

	SharedPreferences preferences;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_map, container, false);

		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.beginTransaction().add(R.id.map_event_holder, new EventBannerFragment()).commit();

		setHasOptionsMenu(true);

		mage = (MAGE) getActivity().getApplication();

		mapWrapper = new GoogleMapWrapper(getActivity().getApplicationContext());
		mapWrapper.addView(view);

		preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

		mapView = (MapView) view.findViewById(R.id.mapView);
		mapView.onCreate(savedInstanceState);

		searchLayout = view.findViewById(R.id.search_layout);
		edittextSearch = (EditText) view.findViewById(R.id.edittext_search);

		MapsInitializer.initialize(getActivity().getApplicationContext());

		ImageButton mapSettings = (ImageButton) view.findViewById(R.id.map_settings);
		mapSettings.setOnClickListener(this);

		locationService = mage.getLocationService();

		return mapWrapper;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ObservationHelper.getInstance(getActivity().getApplicationContext()).addListener(this);
		LocationHelper.getInstance(getActivity().getApplicationContext()).addListener(this);
	}

	@Override
	public void onDestroy() {
		ObservationHelper.getInstance(getActivity().getApplicationContext()).removeListener(this);
		LocationHelper.getInstance(getActivity().getApplicationContext()).removeListener(this);

		mapView.onDestroy();
		map = null;

		observations.clear();
		observations = null;

		locations.clear();
		locations = null;

		myHistoricLocations.clear();
		myHistoricLocations = null;

		if (searchMarkes != null) {
			for (Marker m : searchMarkes) {
				m.remove();
			}
			searchMarkes.clear();
		}
		
		staticGeometryCollection = null;
		currentUser = null;
		super.onDestroy();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mapView.onLowMemory();
	}

	@Override
	public void onResume() {
		super.onResume();

		try {
			currentUser = UserHelper.getInstance(getActivity().getApplicationContext()).readCurrentUser();
		} catch (UserException ue) {
			Log.e(LOG_NAME, "Could not find current user.", ue);
		}

		mapView.onResume();

		PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).registerOnSharedPreferenceChangeListener(this);

		if (map == null) {
			map = mapView.getMap();
		}
		map.setOnMapClickListener(this);
		map.setOnMarkerClickListener(this);
		map.setOnMapLongClickListener(this);
		map.setOnMyLocationButtonClickListener(this);
		map.setOnInfoWindowClickListener(this);

		// zoom to map location
		updateMapView();

		if (staticGeometryCollection == null) {
			staticGeometryCollection = new StaticGeometryCollection();
		}
		updateStaticFeatureLayers();

		if (observations == null) {
			observations = new ObservationMarkerCollection(getActivity(), map);
		}

		getActivity().getActionBar().setTitle(getTemporalFilterTitle());
		ObservationLoadTask observationLoad = new ObservationLoadTask(getActivity(), observations);
		observationLoad.setFilter(getTemporalFilter("last_modified"));
		observationLoad.executeOnExecutor(executor);

		boolean showObservations = preferences.getBoolean(getResources().getString(R.string.showObservationsKey), true);
		observations.setVisibility(showObservations);

		if (locations == null) {
			locations = new LocationMarkerCollection(getActivity(), map);
		}
		LocationLoadTask locationLoad = new LocationLoadTask(getActivity(), locations);
		locationLoad.setFilter(getTemporalFilter("timestamp"));
		locationLoad.executeOnExecutor(executor);

		boolean showLocations = preferences.getBoolean(getResources().getString(R.string.showLocationsKey), true);
		locations.setVisibility(showLocations);

		if (myHistoricLocations == null) {
			myHistoricLocations = new MyHistoricalLocationMarkerCollection(getActivity(), map);
		}

		HistoricLocationLoadTask myHistoricLocationLoad = new HistoricLocationLoadTask(getActivity(), myHistoricLocations);
		myHistoricLocationLoad.executeOnExecutor(executor);

		boolean showMyLocationHistory = preferences.getBoolean(getResources().getString(R.string.showMyLocationHistoryKey), false);
		myHistoricLocations.setVisibility(showMyLocationHistory);
		
		mage.registerCacheOverlayListener(this);
		StaticFeatureHelper.getInstance(getActivity().getApplicationContext()).addListener(this);

		// Check if any map preferences changed that I care about
		boolean locationServiceEnabled = preferences.getBoolean(getResources().getString(R.string.locationServiceEnabledKey), false);
		map.setMyLocationEnabled(locationServiceEnabled);

		if (locationServiceEnabled) {
			map.setLocationSource(this);
			locationService.registerOnLocationListener(this);
		}

		// task to refresh location markers every x seconds
		if (refreshLocationsMarkersTask == null) {
			refreshLocationsMarkersTask = new RefreshMarkersTask(locations);
		}
		if (!refreshLocationsMarkersTask.isCancelled()) {
			refreshLocationsMarkersTask.executeOnExecutor(executor, REFRESHMARKERINTERVALINSECONDS);
		}

		// task to refresh my historic location markers every x seconds
		if (refreshMyHistoricLocationsMarkersTask == null) {
			refreshMyHistoricLocationsMarkersTask = new RefreshMarkersTask(myHistoricLocations);
		}
		if (!refreshMyHistoricLocationsMarkersTask.isCancelled()) {
			refreshMyHistoricLocationsMarkersTask.executeOnExecutor(executor, REFRESHMARKERINTERVALINSECONDS);
		}

		edittextSearch.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					search(v);
					return true;
				} else {
					return false;
				}
			}
		});
		
		edittextSearch.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if(s == null || s.toString().trim().isEmpty()) {
					if (searchMarkes != null) {
						for (Marker m : searchMarkes) {
							m.remove();
						}
						searchMarkes.clear();
					}
				}
			}

		});
	}

	@Override
	public void onPause() {
		super.onPause();

		if (refreshLocationsMarkersTask != null) {
			refreshLocationsMarkersTask.cancel(true);
			refreshLocationsMarkersTask = null;
		}

		if (refreshMyHistoricLocationsMarkersTask != null) {
			refreshMyHistoricLocationsMarkersTask.cancel(true);
			refreshMyHistoricLocationsMarkersTask = null;
		}
		
		mapView.onPause();

		saveMapView();

		PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);

		mage.unregisterCacheOverlayListener(this);
		StaticFeatureHelper.getInstance(getActivity().getApplicationContext()).removeListener(this);

		boolean locationServiceEnabled = preferences.getInt(getString(R.string.userReportingFrequencyKey), getResources().getInteger(R.integer.userReportingFrequencyDefaultValue)) > 0;
		if (locationServiceEnabled) {
			map.setLocationSource(null);
			locationService.unregisterOnLocationListener(this);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.search, menu);
		inflater.inflate(R.menu.observation_new, menu);
		inflater.inflate(R.menu.filter, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.observation_new:
			Intent intent = new Intent(getActivity().getApplicationContext(), ObservationEditActivity.class);
			LocationService ls = ((MAGE) getActivity().getApplication()).getLocationService();
			Location l = ls.getLocation();
			// if there is not a location from the location service, then try to pull one from the database.
			if (l == null) {
				List<mil.nga.giat.mage.sdk.datastore.location.Location> tLocations = LocationHelper.getInstance(getActivity().getApplicationContext()).getCurrentUserLocations(getActivity().getApplicationContext(), 1, true);
				if (!tLocations.isEmpty()) {
					mil.nga.giat.mage.sdk.datastore.location.Location tLocation = tLocations.get(0);
					Geometry geo = tLocation.getGeometry();
					Map<String, LocationProperty> propertiesMap = tLocation.getPropertiesMap();
					if (geo instanceof Point) {
						Point point = (Point) geo;
						String provider = "manual";
						if (propertiesMap.get("provider").getValue() != null) {
							provider = propertiesMap.get("provider").getValue().toString();
						}
						l = new Location(provider);
						l.setTime(tLocation.getTimestamp().getTime());
						if (propertiesMap.get("accuracy").getValue() != null) {
							l.setAccuracy(Float.valueOf(propertiesMap.get("accuracy").getValue().toString()));
						}
						l.setLatitude(point.getY());
						l.setLongitude(point.getX());
					}
				}
			} else {
				l = new Location(l);
			}

			if(!UserHelper.getInstance(getActivity().getApplicationContext()).isCurrentUserPartOfCurrentEvent()) {
				new AlertDialog.Builder(getActivity()).setTitle("Not a member of this event").setMessage("You are an administrator and not a member of the current event.  You can not create an observation in this event.").setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
			} else if (l != null) {
				intent.putExtra(ObservationEditActivity.LOCATION, l);
				intent.putExtra(ObservationEditActivity.INITIAL_LOCATION, map.getCameraPosition().target);
				intent.putExtra(ObservationEditActivity.INITIAL_ZOOM, map.getCameraPosition().zoom);
				startActivity(intent);
			} else {
				new AlertDialog.Builder(getActivity()).setTitle("No Location Available").setMessage("The device has not received a location yet.  To make an observation manually, long press on the map.").setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).show();
			}
			break;
		case R.id.search:
			boolean isVisible = searchLayout.getVisibility() == View.VISIBLE;
			searchLayout.setVisibility(isVisible ? View.GONE : View.VISIBLE);

			if (isVisible) {
				hideKeyboard();
			} else {
				edittextSearch.requestFocus();
				InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
				inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
			}

			break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onObservationCreated(Collection<Observation> o) {
		if (observations != null) {
			ObservationTask task = new ObservationTask(ObservationTask.Type.ADD, observations);
			task.setFilter(getTemporalFilter("last_modified"));
			task.executeOnExecutor(executor, o.toArray(new Observation[o.size()]));
		}
	}

	@Override
	public void onObservationUpdated(Observation o) {
		if (observations != null) {
			ObservationTask task = new ObservationTask(ObservationTask.Type.UPDATE, observations);
			task.setFilter(getTemporalFilter("last_modified"));
			task.executeOnExecutor(executor, o);
		}
	}

	@Override
	public void onObservationDeleted(Observation o) {
		if (observations != null) {
			new ObservationTask(ObservationTask.Type.DELETE, observations).executeOnExecutor(executor, o);
		}
	}

	@Override
	public void onLocationCreated(Collection<mil.nga.giat.mage.sdk.datastore.location.Location> ls) {
		for (mil.nga.giat.mage.sdk.datastore.location.Location l : ls) {
			if (currentUser != null && !currentUser.getRemoteId().equals(l.getUser().getRemoteId())) {
				if (locations != null) {
					LocationTask task = new LocationTask(LocationTask.Type.ADD, locations);
					task.setFilter(getTemporalFilter("timestamp"));
					task.executeOnExecutor(executor, l);
				}
			} else {
				if (myHistoricLocations != null) {
					new LocationTask(LocationTask.Type.ADD, myHistoricLocations).executeOnExecutor(executor, l);
				}
			}
		}
	}

	@Override
	public void onLocationUpdated(mil.nga.giat.mage.sdk.datastore.location.Location l) {
		if (currentUser != null && !currentUser.getRemoteId().equals(l.getUser().getRemoteId())) {
			if (locations != null) {
				LocationTask task = new LocationTask(LocationTask.Type.UPDATE, locations);
				task.setFilter(getTemporalFilter("timestamp"));
				task.executeOnExecutor(executor, l);
			}
		} else {
			if (myHistoricLocations != null) {
				new LocationTask(LocationTask.Type.UPDATE, myHistoricLocations).executeOnExecutor(executor, l);
			}
		}
	}

	@Override
	public void onLocationDeleted(Collection<mil.nga.giat.mage.sdk.datastore.location.Location> l) {
		// this is slowing the app down a lot!  Moving the delete like code into the add methods of the collections
		/*
		if (currentUser != null && !currentUser.getRemoteId().equals(l.getUser().getRemoteId())) {
			if (locations != null) {
				new LocationTask(LocationTask.Type.DELETE, locations).execute(l);
			}
		} else {
			if (myHistoricLocations != null) {
				new LocationTask(LocationTask.Type.DELETE, myHistoricLocations).execute(l);
			}
		}
		*/
	}
	
	@Override
	public void onInfoWindowClick(Marker marker) {
		observations.onInfoWindowClick(marker);
		locations.onInfoWindowClick(marker);
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		hideKeyboard();
		// search marker
		if(searchMarkes != null) {
			for(Marker m :searchMarkes) {
				 if(marker.getId().equals(m.getId())) {
						m.showInfoWindow();
						return true;		 
				 }
			}
		}
		
		// You can only have one marker click listener per map.
		// Lets listen here and shell out the click event to all
		// my marker collections. Each one need to handle
		// gracefully if it does not actually contain the marker
		if (observations.onMarkerClick(marker)) {
			return true;
		}

		if (locations.onMarkerClick(marker)) {
			return true;
		}

		if (myHistoricLocations.onMarkerClick(marker)) {
			return true;
		}

		// static layer
		View markerInfoWindow = LayoutInflater.from(getActivity()).inflate(R.layout.static_feature_infowindow, null, false);
		WebView webView = ((WebView) markerInfoWindow.findViewById(R.id.static_feature_infowindow_content));
		webView.loadData(marker.getSnippet(), "text/html; charset=UTF-8", null);
		new AlertDialog.Builder(getActivity()).setView(markerInfoWindow).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
		}).show();
		return true;
	}

	@Override
	public void onMapClick(LatLng latLng) {
		hideKeyboard();
		// remove old accuracy circle
		((LocationMarkerCollection) locations).offMarkerClick();

		staticGeometryCollection.onMapClick(map, latLng, getActivity());
	}

	@Override
	public void onMapLongClick(LatLng point) {
		hideKeyboard();
		if(!UserHelper.getInstance(getActivity().getApplicationContext()).isCurrentUserPartOfCurrentEvent()) {
			new AlertDialog.Builder(getActivity()).setTitle("Not a member of this event").setMessage("You are an administrator and not a member of the current event.  You can not create an observation in this event.").setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
				}
			}).show();
		} else {
			Intent intent = new Intent(getActivity().getApplicationContext(), ObservationEditActivity.class);
			Location l = new Location("manual");
			l.setAccuracy(0.0f);
			l.setLatitude(point.latitude);
			l.setLongitude(point.longitude);
			l.setTime(new Date().getTime());
			intent.putExtra(ObservationEditActivity.LOCATION, l);
			startActivity(intent);
		}
	}

	@Override
	public void onClick(View view) {
		// close keyboard
		hideKeyboard();
		switch (view.getId()) {
		case R.id.map_settings: {
			Intent i = new Intent(getActivity().getApplicationContext(), MapPreferencesActivity.class);
			startActivity(i);
			break;
		}
		}
	}

	@Override
	public boolean onMyLocationButtonClick() {
		if (location != null) {
			LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
			float zoom = map.getCameraPosition().zoom < 15 ? 15 : map.getCameraPosition().zoom;
			map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom), new CancelableCallback() {

				@Override
				public void onCancel() {
					mapWrapper.setOnMapPanListener(MapFragment.this);
					followMe = true;
				}

				@Override
				public void onFinish() {
					mapWrapper.setOnMapPanListener(MapFragment.this);
					followMe = true;
				}
			});
		}
		return true;
	}

	@Override
	public void activate(OnLocationChangedListener listener) {
		Log.i(LOG_NAME, "map location, activate");
		locationChangedListener = listener;
		if (location != null) {
			Log.i(LOG_NAME, "map location, activate we have a location, let our listener know");
			locationChangedListener.onLocationChanged(location);
		}
	}

	@Override
	public void deactivate() {
		Log.i(LOG_NAME, "map location, deactivate");
		locationChangedListener = null;
	}

	@Override
	public void onMapPan() {
		mapWrapper.setOnMapPanListener(null);
		followMe = false;
	}

	@Override
	public void onLocationChanged(Location location) {
		this.location = location;
		Log.d(LOG_NAME, "Map location updated.");
		if (locationChangedListener != null) {
			locationChangedListener.onLocationChanged(location);
		}

		if (followMe) {
			LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
			LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
			if (!bounds.contains(latLng)) {
				// Move the camera to the user's location once it's available!
				map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
			}
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onCacheOverlay(List<CacheOverlay> cacheOverlays) {
		Set<String> overlays = preferences.getStringSet(getResources().getString(R.string.tileOverlaysKey), Collections.<String> emptySet());

		// Add all overlays that are in the preferences
		// For now there is no ordering in how tile overlays are stacked
		Set<String> removedOverlays = new HashSet<String>(tileOverlays.keySet());

		for (CacheOverlay cacheOverlay : cacheOverlays) {
			// The user has asked for this overlay
			if (overlays.contains(cacheOverlay.getName())) {
				if (!tileOverlays.keySet().contains(cacheOverlay.getName())) {
					TileProvider tileProvider = new FileSystemTileProvider(256, 256, cacheOverlay.getDirectory().getAbsolutePath());
					TileOverlay tileOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
					tileOverlays.put(cacheOverlay.getName(), tileOverlay);
				}

				removedOverlays.remove(cacheOverlay.getName());
			}
		}

		// Remove any overlays that are on the map but no longer selected in
		// preferences
		for (String overlay : removedOverlays) {
			tileOverlays.remove(overlay).remove();
		}
	}

	private void updateStaticFeatureLayers() {
		removeStaticFeatureLayers();

		try {
			for (Layer l : LayerHelper.getInstance(getActivity().getApplicationContext()).readByEvent(EventHelper.getInstance(getActivity().getApplicationContext()).getCurrentEvent())) {
				onStaticFeatureLayer(l);
			}
		} catch (LayerException e) {
			Log.e(LOG_NAME, "Problem updating static features.", e);
		}
	}

	private void removeStaticFeatureLayers() {
		Set<String> selectedLayerIds = preferences.getStringSet(getResources().getString(R.string.staticFeatureLayersKey), Collections.<String> emptySet());

		for (String currentLayerId : staticGeometryCollection.getLayers()) {
			if (!selectedLayerIds.contains(currentLayerId)) {
				staticGeometryCollection.removeLayer(currentLayerId);
			}
		}
	}

	@Override
	public void onStaticFeaturesCreated(final Layer layer) {
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				onStaticFeatureLayer(layer);
			}
		});
	}

	private void onStaticFeatureLayer(Layer layer) {
		Set<String> layers = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getStringSet(getString(R.string.staticFeatureLayersKey), Collections.<String> emptySet());

		// The user has asked for this feature layer
		String layerId = layer.getId().toString();
		if (layers.contains(layerId) && layer.isLoaded()) {
			new StaticFeatureLoadTask(getActivity().getApplicationContext(), staticGeometryCollection, map).executeOnExecutor(executor, layer);
		}
	}

	private void updateMapView() {
		// Check the map type
		map.setMapType(preferences.getInt(getString(R.string.baseLayerKey), getResources().getInteger(R.integer.baseLayerDefaultValue)));

		// Check the map location and zoom
		String xyz = preferences.getString(getString(R.string.recentMapXYZKey), getString(R.string.recentMapXYZDefaultValue));
		if (xyz != null) {
			String[] values = StringUtils.split(xyz, ",");
			LatLng latLng = new LatLng(0.0, 0.0);
			if(values.length > 1) {
				try {
					latLng = new LatLng(Double.valueOf(values[1]), Double.valueOf(values[0]));
				} catch (NumberFormatException nfe) {
					Log.e(LOG_NAME, "Could not parse lon,lat: " + String.valueOf(values[1]) + ", " + String.valueOf(values[0]));
				}
			}
			float zoom = 1.0f;
			if(values.length > 2) {
				try {
					zoom = Float.valueOf(values[2]);
				} catch (NumberFormatException nfe) {
					Log.e(LOG_NAME, "Could not parse zoom level: " + String.valueOf(values[2]));
				}
			}
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
		}
	}

	private void saveMapView() {
		CameraPosition position = map.getCameraPosition();

		String xyz = new StringBuilder().append(Double.valueOf(position.target.longitude).toString()).append(",").append(Double.valueOf(position.target.latitude).toString()).append(",").append(Float.valueOf(position.zoom).toString()).toString();
		preferences.edit().putString(getResources().getString(R.string.recentMapXYZKey), xyz).commit();
	}

	private void search(View v) {
		String searchString = edittextSearch.getText().toString();
		if (searchString != null && !searchString.equals("")) {

			InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
			if (getActivity().getCurrentFocus() != null) {
				inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
			}

			new GeocoderTask(getActivity(), map, searchMarkes).execute(searchString);
		}
	}

	@Override
	public void onError(Throwable error) {
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (getResources().getString(R.string.activeTimeFilterKey).equalsIgnoreCase(key)) {
			observations.clear();
			ObservationLoadTask observationLoad = new ObservationLoadTask(getActivity(), observations);
			observationLoad.setFilter(getTemporalFilter("last_modified"));
			observationLoad.executeOnExecutor(executor);

			locations.clear();
			LocationLoadTask locationLoad = new LocationLoadTask(getActivity(), locations);
			locationLoad.setFilter(getTemporalFilter("timestamp"));
			locationLoad.executeOnExecutor(executor);

			getActivity().getActionBar().setTitle(getTemporalFilterTitle());
		}
	}

	private Filter<Temporal> getTemporalFilter(String columnName) {
		int timeFilter = preferences.getInt(getResources().getString(R.string.activeTimeFilterKey), R.id.none_rb);

		Filter<Temporal> filter = null;

		Calendar c = Calendar.getInstance();
		switch (timeFilter) {
		case R.id.last_hour_rb:
			c.add(Calendar.HOUR, -1);
			break;
		case R.id.last_six_hours_rb:
			c.add(Calendar.HOUR, -6);
			break;
		case R.id.last_twelve_hours_rb:
			c.add(Calendar.HOUR, -12);
			break;
		case R.id.last_24_hours_rb:
			c.add(Calendar.HOUR, -24);
			break;
		case R.id.since_midnight_rb:
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			break;
		default:
			// no filter
			c = null;
		}

		if (c != null) {
			Date start = c.getTime();
			Date end = null;

			filter = new DateTimeFilter(start, end, columnName);
		}

		return filter;
	}

	private String getTemporalFilterTitle() {
		String title = "MAGE";
		int timeFilter = preferences.getInt(getResources().getString(R.string.activeTimeFilterKey), R.id.none_rb);
		switch (timeFilter) {
		case R.id.last_hour_rb:
			title = "Last Hour";
			break;
		case R.id.last_six_hours_rb:
			title = "Last 6 Hours";
			break;
		case R.id.last_twelve_hours_rb:
			title = "Last 12 Hours";
			break;
		case R.id.last_24_hours_rb:
			title = "Last 24 Hours";
			break;
		case R.id.since_midnight_rb:
			title = "Since Midnight";
			break;
		default:
			break;
		}

		return title;
	}

	private void hideKeyboard() {
		InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
		if (getActivity().getCurrentFocus() != null) {
			inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
		}
	}


}