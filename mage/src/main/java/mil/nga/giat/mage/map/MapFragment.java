package mil.nga.giat.mage.map;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.qualifiers.ApplicationContext;
import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageCache;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.extension.link.FeatureTileTableLinker;
import mil.nga.geopackage.extension.scale.TileScaling;
import mil.nga.geopackage.extension.scale.TileTableScaling;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.features.user.FeatureCursor;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.map.geom.GoogleMapShape;
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter;
import mil.nga.geopackage.map.tiles.overlay.BoundedOverlay;
import mil.nga.geopackage.map.tiles.overlay.FeatureOverlay;
import mil.nga.geopackage.map.tiles.overlay.FeatureOverlayQuery;
import mil.nga.geopackage.map.tiles.overlay.GeoPackageOverlayFactory;
import mil.nga.geopackage.tiles.TileBoundingBoxUtils;
import mil.nga.geopackage.tiles.features.DefaultFeatureTiles;
import mil.nga.geopackage.tiles.features.FeatureTiles;
import mil.nga.geopackage.tiles.features.custom.NumberFeaturesTile;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.data.feed.FeedItem;
import mil.nga.giat.mage.data.feed.FeedWithItems;
import mil.nga.giat.mage.databinding.FragmentMapBinding;
import mil.nga.giat.mage.feed.item.FeedItemActivity;
import mil.nga.giat.mage.filter.DateTimeFilter;
import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.filter.FilterActivity;
import mil.nga.giat.mage.location.LocationPolicy;
import mil.nga.giat.mage.map.cache.CacheOverlay;
import mil.nga.giat.mage.map.cache.CacheOverlayFilter;
import mil.nga.giat.mage.map.cache.CacheOverlayType;
import mil.nga.giat.mage.map.cache.CacheProvider;
import mil.nga.giat.mage.map.cache.CacheProvider.OnCacheOverlayListener;
import mil.nga.giat.mage.map.cache.GeoPackageCacheOverlay;
import mil.nga.giat.mage.map.cache.GeoPackageFeatureTableCacheOverlay;
import mil.nga.giat.mage.map.cache.GeoPackageTileTableCacheOverlay;
import mil.nga.giat.mage.map.cache.StaticFeatureCacheOverlay;
import mil.nga.giat.mage.map.cache.URLCacheOverlay;
import mil.nga.giat.mage.map.cache.WMSCacheOverlay;
import mil.nga.giat.mage.map.cache.XYZDirectoryCacheOverlay;
import mil.nga.giat.mage.map.marker.FeedItemCollection;
import mil.nga.giat.mage.map.marker.LocationMarkerCollection;
import mil.nga.giat.mage.map.marker.MyHistoricalLocationMarkerCollection;
import mil.nga.giat.mage.map.marker.ObservationBitmapFactory;
import mil.nga.giat.mage.map.marker.ObservationMarkerCollection;
import mil.nga.giat.mage.map.marker.PointCollection;
import mil.nga.giat.mage.map.marker.StaticGeometryCollection;
import mil.nga.giat.mage.map.navigation.bearing.StraightLineNavigation;
import mil.nga.giat.mage.map.navigation.bearing.StraightLineNavigationDelegate;
import mil.nga.giat.mage.map.preference.MapPreferencesActivity;
import mil.nga.giat.mage.newsfeed.ObservationListAdapter;
import mil.nga.giat.mage.observation.ObservationLocation;
import mil.nga.giat.mage.observation.edit.ObservationEditActivity;
import mil.nga.giat.mage.observation.view.ObservationViewActivity;
import mil.nga.giat.mage.profile.ProfileActivity;
import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.location.LocationProperty;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.event.ILocationEventListener;
import mil.nga.giat.mage.sdk.event.IObservationEventListener;
import mil.nga.giat.mage.sdk.event.IUserEventListener;
import mil.nga.giat.mage.sdk.exceptions.LayerException;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.gzd.MGRSTileProvider;
import mil.nga.sf.Geometry;
import mil.nga.sf.GeometryType;
import mil.nga.sf.Point;
import mil.nga.sf.proj.Projection;
import mil.nga.sf.proj.ProjectionConstants;
import mil.nga.sf.proj.ProjectionFactory;

@AndroidEntryPoint
public class MapFragment extends Fragment implements
		OnMapReadyCallback,
		OnMapClickListener,
		OnMapLongClickListener,
		OnMarkerClickListener,
		OnInfoWindowClickListener,
		GoogleMap.OnInfoWindowCloseListener,
		GoogleMap.OnCameraIdleListener,
		GoogleMap.OnCameraMoveStartedListener,
		OnClickListener, LocationSource,
		OnCacheOverlayListener,
		IObservationEventListener,
		ILocationEventListener,
		IUserEventListener,
		StraightLineNavigationDelegate,
		ObservationListAdapter.ObservationActionListener,
		Observer<Location> {

	private static final String LOG_NAME = MapFragment.class.getName();

	private static final String MAP_VIEW_STATE = "MAP_VIEW_STATE";

	private static final String OBSERVATION_FILTER_TYPE = "Observation";
	private static final String LOCATION_FILTER_TYPE = "Location";

	private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

	private static final int MARKER_REFRESH_INTERVAL_SECONDS = 300;

	private enum LocateState {
		OFF,
		FOLLOW;

		public LocateState next() {
			return this.ordinal() < LocateState.values().length - 1 ?
					LocateState.values()[this.ordinal() + 1] :
					LocateState.OFF;
		}
	}

	@Inject
	protected @ApplicationContext Context context;

	@Inject
	protected SharedPreferences preferences;

	private MapViewModel viewModel;

	private MapView mapView;
	private GoogleMap map;
	private View searchLayout;
	private SearchView searchView;
	private StraightLineNavigation straightLineNavigation;
	private SensorManager sensorManager;
	private LocateState locateState = LocateState.OFF;
	protected User currentUser = null;
	private long currentEventId = -1;
	private OnLocationChangedListener locationChangedListener;

	@Inject
	protected LocationPolicy locationPolicy;
	private LiveData<Location> locationProvider;

	private RefreshMarkersRunnable refreshLocationsTask;
	private RefreshMarkersRunnable refreshHistoricLocationsTask;

	private PointCollection<Observation> observations;
	private PointCollection<Pair<mil.nga.giat.mage.sdk.datastore.location.Location, User>> locations;
	private PointCollection<Pair<mil.nga.giat.mage.sdk.datastore.location.Location, User>> historicLocations;
	private StaticGeometryCollection staticGeometryCollection;
	private List<Marker> searchMarkers = new ArrayList<>();

	private FeedItemCollection feedItems;
	private Map<String, LiveData<FeedWithItems>> feeds = Collections.emptyMap();

	private Map<String, CacheOverlay> cacheOverlays = new HashMap<>();

	// GeoPackage cache of open GeoPackage connections
	private GeoPackageCache geoPackageCache;
	private BoundingBox addedCacheBoundingBox;

	private FloatingActionButton reportLocationButton;
	private FloatingActionButton searchButton;
	private FloatingActionButton zoomToLocationButton;

	private boolean showMgrs;
	private TileOverlay mgrsTileOverlay;
	private TextView mgrsChip;

	private BottomSheetBehavior<View> featureBottomSheetBehavior;

	private View availableLayerDownloadsIcon;
	private FragmentMapBinding binding;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		viewModel = new ViewModelProvider(this).get(MapViewModel.class);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater  inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_map, container, false);
		binding = DataBindingUtil.bind(view);

		setHasOptionsMenu(true);

		staticGeometryCollection = new StaticGeometryCollection();

		availableLayerDownloadsIcon = view.findViewById(R.id.available_layer_downloads);
		zoomToLocationButton = view.findViewById(R.id.zoom_button);

		reportLocationButton = view.findViewById(R.id.report_location);
		reportLocationButton.setOnClickListener(v -> toggleReportLocation());

		searchButton = view.findViewById(R.id.map_search_button);
		if (Geocoder.isPresent()) {
			searchButton.setOnClickListener(v -> search());
		} else {
			searchButton.hide();
		}

		view.findViewById(R.id.new_observation_button).setOnClickListener(v -> onNewObservation());

		searchLayout = view.findViewById(R.id.search_layout);
		searchView = view.findViewById(R.id.search_view);
		searchView.setIconifiedByDefault(false);
		searchView.setIconified(false);
		searchView.clearFocus();

		MapsInitializer.initialize(context);

		ImageButton mapSettings = view.findViewById(R.id.map_settings);
		mapSettings.setOnClickListener(this);

		mapView = view.findViewById(R.id.mapView);
		Bundle mapState = (savedInstanceState != null) ? savedInstanceState.getBundle(MAP_VIEW_STATE): null;
		mapView.onCreate(mapState);

		mgrsChip = view.findViewById(R.id.mgrs_chip);

		View featureBottomSheet = view.findViewById(R.id.observation_bottom_sheet);
		featureBottomSheetBehavior = BottomSheetBehavior.from(featureBottomSheet);
		featureBottomSheetBehavior.setFitToContents(true);
		featureBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

		featureBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
			// this only is called when the user interacts with the sheet, not when we make it EXPANDED programmatically
			@Override
			public void onStateChanged(@NonNull View view, int newState) {
				if (newState == BottomSheetBehavior.STATE_HIDDEN) {
					unselectObservation();
				}
			}

			@Override
			public void onSlide(@NonNull View view, float v) { }
		});

		// Initialize the GeoPackage cache with a GeoPackage manager
		GeoPackageManager geoPackageManager = GeoPackageFactory.getManager(getActivity().getApplicationContext());
		geoPackageCache = new GeoPackageCache(geoPackageManager);

		locationProvider = locationPolicy.getBestLocationProvider();

		return view;
	}

	void unselectObservation() {
		featureBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
		binding.setChosenObservation(null);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		viewModel.getItems().observe(getViewLifecycleOwner(), this::onFeedItems);
	}

	@Override
	public void onObservationClick(Observation observation) {
		Intent intent = new Intent(context, ObservationViewActivity.class);
		intent.putExtra(ObservationViewActivity.OBSERVATION_ID, observation.getId());
		intent.putExtra(ObservationViewActivity.INITIAL_LOCATION, map.getCameraPosition().target);
		intent.putExtra(ObservationViewActivity.INITIAL_ZOOM, map.getCameraPosition().zoom);
		startActivity(intent);
	}

	@Override
	public void onObservationDirections(@NonNull Observation observation) {
		featureBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

		// present a dialog to pick between android system map and straight line
		new AlertDialog.Builder(getActivity())
				.setTitle(getActivity().getResources().getString(R.string.navigation_choice_title))
				.setItems(R.array.navigationOptions, (dialog, which) -> {
					switch (which) {
						case 0:
							Intent intent = new Intent(Intent.ACTION_VIEW, observation.getGoogleMapsUri());
							startActivity(intent);
							break;
						case 1:
							Location location;
							if (locationProvider.getValue() == null) {
								if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
									new AlertDialog.Builder(getActivity())
											.setTitle(getActivity().getResources().getString(R.string.location_missing_title))
											.setMessage(getActivity().getResources().getString(R.string.location_missing_message))
											.setPositiveButton(android.R.string.ok, null)
											.show();
								} else {
									new AlertDialog.Builder(getActivity())
											.setTitle(getActivity().getResources().getString(R.string.location_access_observation_title))
											.setMessage(getActivity().getResources().getString(R.string.location_access_observation_message))
											.setPositiveButton(android.R.string.ok, (dialog1, which1) -> requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION))
											.show();
								}
							} else {
								location = locationProvider.getValue();
								Point centroid = observation.getGeometry().getCentroid();
								LatLng latLng = new LatLng(centroid.getY(), centroid.getX());
								straightLineNavigation.startNavigation(sensorManager, location, latLng, ObservationBitmapFactory.bitmap(context, observation));
								featureBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
							}
							break;
					}

				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	@Override
	public void cancelStraightLineNavigation() {
		straightLineNavigation.stopNavigation();
	}

	@Override
	public void onObservationLocation(@NonNull Observation observation) {

	}

	@Override
	public void onChanged(@Nullable Location location) {
		if (locationChangedListener != null) {
			locationChangedListener.onLocationChanged(location);
		}

		if (locateState == LocateState.FOLLOW) {
			CameraPosition cameraPosition = new CameraPosition.Builder()
				.target(new LatLng(location.getLatitude(), location.getLongitude()))
				.zoom(17)
				.bearing(location.getBearing())
				.build();

			map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
		}
		if (preferences.getBoolean(context.getResources().getString(R.string.showHeadingKey), false)) {
			startHeading();
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		mapView.onDestroy();

		if (observations != null) {
			observations.clear();
			observations = null;
		}

		if (locations != null) {
			locations.clear();
			locations = null;
		}

		if (feedItems != null) {
			feedItems.clear();
		}

		if (historicLocations != null) {
			historicLocations.clear();
			historicLocations = null;
		}

		if (searchMarkers != null) {
			for (Marker m : searchMarkers) {
				m.remove();
			}
			searchMarkers.clear();
		}

		if (mgrsTileOverlay != null) {
			mgrsTileOverlay.remove();
			mgrsTileOverlay = null;
		}

		// Close all open GeoPackages
		geoPackageCache.closeAll();

		cacheOverlays.clear();

		staticGeometryCollection = null;
		currentUser = null;
		map = null;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.filter, menu);
		getFilterTitle();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.filter_button:
				Intent intent = new Intent(getActivity(), FilterActivity.class);
				startActivity(intent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		if (map == null) {
			map = googleMap;
			map.getUiSettings().setMyLocationButtonEnabled(false);
			map.setOnMapClickListener(this);
			map.setOnMarkerClickListener(this);
			map.setOnMapLongClickListener(this);
			map.setOnInfoWindowClickListener(this);
			map.setOnCameraIdleListener(this);
			map.setOnCameraMoveStartedListener(this);

			observations = new ObservationMarkerCollection(getActivity(), map, this);
			historicLocations = new MyHistoricalLocationMarkerCollection(context, map);
			locations = new LocationMarkerCollection(getActivity(), map);
			feedItems = new FeedItemCollection(getActivity(), map);

			straightLineNavigation = new StraightLineNavigation(this, map, getActivity().findViewById(R.id.straight_line_nav_container), this.getActivity().getBaseContext(), context);
			sensorManager = (SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);
		}

		int dayNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
		if (dayNightMode == Configuration.UI_MODE_NIGHT_NO) {
			googleMap.setMapStyle(null);
		} else {
			googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.map_theme_night));
		}

		boolean showTraffic = preferences.getBoolean(getResources().getString(R.string.showTrafficKey), getResources().getBoolean(R.bool.showTrafficDefaultValue));
		map.setTrafficEnabled(showTraffic);

		Event currentEvent = EventHelper.getInstance(getActivity()).getCurrentEvent();
		long currentEventId = this.currentEventId;
		if (currentEvent != null) {
			currentEventId = currentEvent.getId();
		}
		if (this.currentEventId != currentEventId) {
			this.currentEventId = currentEventId;
			observations.clear();
			locations.clear();
			historicLocations.clear();
		}

		ObservationHelper.getInstance(context).addListener(this);
		LocationHelper.getInstance(context).addListener(this);
		UserHelper.getInstance(context).addListener(this);
		CacheProvider.getInstance(context).registerCacheOverlayListener(this);

		zoomToLocationButton.setOnClickListener(v -> zoomToLocation());

		ObservationLoadTask observationLoad = new ObservationLoadTask(context, observations);
		observationLoad.addFilter(getTemporalFilter("timestamp", getTimeFilterId(), OBSERVATION_FILTER_TYPE));
		observationLoad.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

		HistoricLocationLoadTask myHistoricLocationLoad = new HistoricLocationLoadTask(context, historicLocations);
		myHistoricLocationLoad.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

		LocationLoadTask locationLoad = new LocationLoadTask(context, locations);
		locationLoad.setFilter(getTemporalFilter("timestamp", getLocationTimeFilterId(), LOCATION_FILTER_TYPE));
		locationLoad.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

		updateMapView();

		// Set visibility on map markers as preferences may have changed
		observations.setVisibility(preferences.getBoolean(getResources().getString(R.string.showObservationsKey), true));
		locations.setVisibility(preferences.getBoolean(getResources().getString(R.string.showLocationsKey), true));
		historicLocations.setVisibility(preferences.getBoolean(getResources().getString(R.string.showMyLocationHistoryKey), false));

		// maybe need to turn off heading
		if (!preferences.getBoolean(getResources().getString(R.string.showHeadingKey), false)) {
			straightLineNavigation.stopHeading();
		}

		// Check if any map preferences changed that I care about
		if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			map.setMyLocationEnabled(true);
			map.setLocationSource(this);
		} else {
			map.setMyLocationEnabled(false);
			map.setLocationSource(null);
		}

		initializePeriodicTasks();

		getActivity().findViewById(R.id.mgrs_chip_container).setVisibility(showMgrs ? View.VISIBLE : View.GONE);
		if (showMgrs) {
			mgrsTileOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(new MGRSTileProvider(getContext())));
		}

		((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(getFilterTitle());

		viewModel.setEvent(currentEvent.getRemoteId());
	}

	private void updateReportLocationButton() {
		boolean reportLocation = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
				preferences.getBoolean(getResources().getString(R.string.reportLocationKey), false) &&
				UserHelper.getInstance(getActivity().getApplicationContext()).isCurrentUserPartOfCurrentEvent();

		reportLocationButton.setSelected(reportLocation);
		if (reportLocation) {
			reportLocationButton.setImageResource(R.drawable.ic_my_location_white_24dp);
		} else {
			reportLocationButton.setImageResource(R.drawable.ic_outline_location_disabled_24);
		}
	}

	private void zoomToLocation() {
		if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			new AlertDialog.Builder(getActivity())
					.setTitle(getActivity().getResources().getString(R.string.location_access_observation_title))
					.setMessage(getActivity().getResources().getString(R.string.location_access_zoom_message))
					.setPositiveButton(R.string.settings, (dialog, which) -> {
						Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
						intent.setData(Uri.fromParts("package", getActivity().getApplicationContext().getPackageName(), null));
						startActivity(intent);
					})
					.setNegativeButton(android.R.string.cancel, null)
					.show();

			return;
		}

		locateState = locateState.next();

		switch (locateState) {
			case OFF:
				zoomToLocationButton.setSelected(false);
				break;
			case FOLLOW:
				zoomToLocationButton.setSelected(true);

				Location location = locationProvider.getValue();
				if (location != null) {
					CameraPosition cameraPosition = new CameraPosition.Builder()
						.target(new LatLng(location.getLatitude(), location.getLongitude()))
						.zoom(17)
						.bearing(45)
						.build();

					map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
				}

				break;
		}
	}

	private void toggleReportLocation() {
		if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			new AlertDialog.Builder(getActivity())
				.setTitle(getActivity().getResources().getString(R.string.location_access_observation_title))
				.setMessage(getActivity().getResources().getString(R.string.location_access_report_message))
				.setPositiveButton(R.string.settings, (dialog, which) -> {
					Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
					intent.setData(Uri.fromParts("package", getActivity().getApplicationContext().getPackageName(), null));
					startActivity(intent);
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();

			return;
		}

		if (!UserHelper.getInstance(getActivity().getApplicationContext()).isCurrentUserPartOfCurrentEvent()) {
			new AlertDialog.Builder(getActivity())
				.setTitle(getActivity().getResources().getString(R.string.no_event_title))
				.setMessage(getActivity().getResources().getString(R.string.location_no_event_message))
				.setPositiveButton(android.R.string.ok, null)
				.show();

			return;
		}

		String key = getResources().getString(R.string.reportLocationKey);
		boolean reportLocation = !preferences.getBoolean(key, false);
		preferences.edit().putBoolean(key, reportLocation).apply();
		updateReportLocationButton();

		String text = reportLocation ?
				getResources().getString(R.string.report_location_start) :
				getResources().getString(R.string.report_location_stop);

		Snackbar snackbar = Snackbar.make(getActivity().findViewById(R.id.coordinator_layout), text, Snackbar.LENGTH_SHORT);
		snackbar.setAnchorView(getActivity().findViewById(R.id.new_observation_button));

		final ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackbar.getView().getLayoutParams();
		params.setMargins(0, 100, 0, 100);
		snackbar.getView().setLayoutParams(params);

		snackbar.show();
	}

	private void startHeading() {
		if (preferences.contains(context.getResources().getString(R.string.showHeadingKey))) {
			Location location = locationProvider.getValue();
			if (location != null && straightLineNavigation != null && preferences.getBoolean(context.getResources().getString(R.string.showHeadingKey), false)) {
				straightLineNavigation.startHeading(sensorManager, location);
			}
		} else {
			// show a dialog asking them what they want to do and set the preference
			new AlertDialog.Builder(getActivity())
					.setTitle(getActivity().getResources().getString(R.string.always_show_heading_title))
					.setMessage(getActivity().getResources().getString(R.string.always_show_heading_message))
					.setPositiveButton(getActivity().getResources().getString(R.string.yes), (dialog, which) -> {
						SharedPreferences.Editor edit = preferences.edit();
						edit.putBoolean(context.getResources().getString(R.string.showHeadingKey), true);
						edit.apply();
						startHeading();
					})
					.setNegativeButton(getActivity().getResources().getString(R.string.no), (dialog, which) -> {
						SharedPreferences.Editor edit = preferences.edit();
						edit.putBoolean(context.getResources().getString(R.string.showHeadingKey), false);
						edit.apply();
					})
					.show();
		}
	}

	private void resetMapBearing() {
		if (map == null) return;

		CameraPosition cameraPosition = new CameraPosition.Builder(map.getCameraPosition())
				.bearing(0)
				.build();

		map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
	}

	private void initializePeriodicTasks() {
		if (refreshLocationsTask == null) {
			refreshLocationsTask = new RefreshMarkersRunnable(locations, "timestamp", LOCATION_FILTER_TYPE, getLocationTimeFilterId(), MARKER_REFRESH_INTERVAL_SECONDS);
			scheduleMarkerRefresh(refreshLocationsTask);
		}

		if (refreshHistoricLocationsTask == null) {
			refreshHistoricLocationsTask = new RefreshMarkersRunnable(historicLocations, "timestamp", LOCATION_FILTER_TYPE, getLocationTimeFilterId(), MARKER_REFRESH_INTERVAL_SECONDS);
			scheduleMarkerRefresh(refreshHistoricLocationsTask);
		}
	}

	private void stopPeriodicTasks() {
		getView().removeCallbacks(refreshLocationsTask);
		getView().removeCallbacks(refreshHistoricLocationsTask);

		refreshLocationsTask = null;
		refreshHistoricLocationsTask = null;
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mapView.onLowMemory();
	}

	@Override
	public void onResume() {
		super.onResume();

		updateReportLocationButton();

		try {
			currentUser = UserHelper.getInstance(getActivity().getApplicationContext()).readCurrentUser();
		} catch (UserException ue) {
			Log.e(LOG_NAME, "Could not find current user.", ue);
		}

		mapView.onResume();

        showMgrs = preferences.getBoolean(getResources().getString(R.string.showMGRSKey), false);

		if (map == null) {
			mapView.getMapAsync(this);
		} else {
			onMapReady(map);
		}

		try {
			Event event = EventHelper.getInstance(getContext()).getCurrentEvent();
			Collection<Layer> available = Collections2.filter(LayerHelper.getInstance(getContext()).readByEvent(event, "GeoPackage"), new Predicate<Layer>() {
				@Override
				public boolean apply(Layer layer) {
					return !layer.isLoaded();
				}
			});

			availableLayerDownloadsIcon.setVisibility(available.isEmpty() ? View.GONE : View.VISIBLE);
		} catch (LayerException e) {
			Log.e(LOG_NAME, "Error reading layers", e);
		}

		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				if (StringUtils.isNoneBlank(query)) {
					new GeocoderTask(getActivity(), map, searchMarkers).execute(query);
				}

				searchView.clearFocus();
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				if (StringUtils.isEmpty(newText)) {
					if (searchMarkers != null) {
						for (Marker m : searchMarkers) {
							m.remove();
						}
						searchMarkers.clear();
					}
				}

				return true;
			}
		});
	}

	@Override
	public void onPause() {
		super.onPause();

		stopPeriodicTasks();
		
		mapView.onPause();

		ObservationHelper.getInstance(context).removeListener(this);
		LocationHelper.getInstance(context).removeListener(this);
		UserHelper.getInstance(context).removeListener(this);

		if (observations != null) {
			observations.clear();
		}

		if (locations != null) {
			locations.clear();
		}

		if (feedItems != null) {
			feedItems.clear();
		}

		for (LiveData<FeedWithItems> liveData : feeds.values()) {
			liveData.removeObservers(getViewLifecycleOwner());
		}

		CacheProvider.getInstance(getActivity().getApplicationContext()).unregisterCacheOverlayListener(this);

		if (map != null) {
			saveMapView();

			map.setLocationSource(null);

			if (mgrsTileOverlay != null) {
				mgrsTileOverlay.remove();
			}
		}
	}

	private void onFeedItems(Map<String, LiveData<FeedWithItems>> feeds) {
		for (LiveData<FeedWithItems> liveData : this.feeds.values()) {
			liveData.removeObservers(getViewLifecycleOwner());
		}

		this.feeds = feeds;
		for (LiveData<FeedWithItems> liveData : feeds.values()) {
			liveData.observe(getViewLifecycleOwner(), this::onFeedItems);
		}
	}

	private void onFeedItems(FeedWithItems items) {
		if (items != null) {
			feedItems.setItems(items);
		}
	}

	private void search() {
		boolean isVisible = searchLayout.getVisibility() == View.VISIBLE;
		searchLayout.setVisibility(isVisible ? View.GONE : View.VISIBLE);
		searchButton.setSelected(!isVisible);

		if (isVisible) {
			searchView.clearFocus();
		} else {
			searchView.requestFocus();
			InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
			inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
		}
	}

	private void onNewObservation() {
		ObservationLocation location = null;

		// if there is not a location from the location service, then try to pull one from the database.
		if (locationProvider.getValue() == null) {
			List<mil.nga.giat.mage.sdk.datastore.location.Location> locations = LocationHelper.getInstance(getActivity().getApplicationContext()).getCurrentUserLocations(1, true);
			if (!locations.isEmpty()) {
				mil.nga.giat.mage.sdk.datastore.location.Location tLocation = locations.get(0);
				Geometry geo = tLocation.getGeometry();
				Map<String, LocationProperty> propertiesMap = tLocation.getPropertiesMap();
				String provider = ObservationLocation.MANUAL_PROVIDER;
				if (propertiesMap.get("provider").getValue() != null) {
					provider = propertiesMap.get("provider").getValue().toString();
				}
				location = new ObservationLocation(provider, geo);
				location.setTime(tLocation.getTimestamp().getTime());
				if (propertiesMap.get("accuracy").getValue() != null) {
					location.setAccuracy(Float.valueOf(propertiesMap.get("accuracy").getValue().toString()));
				}
			}
		} else {
			location = new ObservationLocation(locationProvider.getValue());
		}

		if (!UserHelper.getInstance(getActivity().getApplicationContext()).isCurrentUserPartOfCurrentEvent()) {
			new AlertDialog.Builder(getActivity())
				.setTitle(getActivity().getResources().getString(R.string.no_event_title))
				.setMessage(getActivity().getResources().getString(R.string.observation_no_event_message))
				.setPositiveButton(android.R.string.ok, null)
				.show();
		} else if (location != null) {
			newObservation(location);
		} else {
			if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				new AlertDialog.Builder(getActivity())
						.setTitle(getActivity().getResources().getString(R.string.location_missing_title))
						.setMessage(getActivity().getResources().getString(R.string.location_missing_message))
						.setPositiveButton(android.R.string.ok, null)
						.show();
			} else {
				new AlertDialog.Builder(getActivity())
						.setTitle(getActivity().getResources().getString(R.string.location_access_observation_title))
						.setMessage(getActivity().getResources().getString(R.string.location_access_observation_message))
						.setPositiveButton(android.R.string.ok, (dialog, which) -> {
							requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
						})
						.show();
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					onNewObservation();
				}
				break;
			}
		}
	}

	@Override
	public void onObservationCreated(Collection<Observation> o, Boolean sendUserNotifications) {
		if (observations != null) {
			ObservationTask task = new ObservationTask(getActivity(), ObservationTask.Type.ADD, observations);
			task.addFilter(getTemporalFilter("last_modified", getTimeFilterId(), OBSERVATION_FILTER_TYPE));
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, o.toArray(new Observation[o.size()]));
		}
	}

	@Override
	public void onObservationUpdated(Observation o) {
		if (observations != null) {
			ObservationTask task = new ObservationTask(getActivity(), ObservationTask.Type.UPDATE, observations);
			task.addFilter(getTemporalFilter("last_modified", getTimeFilterId(), OBSERVATION_FILTER_TYPE));
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, o);
		}
	}

	@Override
	public void onObservationDeleted(Observation o) {
		if (observations != null) {
			new ObservationTask(context, ObservationTask.Type.DELETE, observations).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, o);
		}
	}

	@Override
	public void onLocationCreated(Collection<mil.nga.giat.mage.sdk.datastore.location.Location> ls) {
		for (mil.nga.giat.mage.sdk.datastore.location.Location l : ls) {
			if (currentUser != null && !currentUser.getRemoteId().equals(l.getUser().getRemoteId())) {
				if (locations != null) {
					LocationTask task = new LocationTask(getActivity(), LocationTask.Type.ADD, locations);
					task.addFilter(getTemporalFilter("timestamp", getLocationTimeFilterId(), LOCATION_FILTER_TYPE));
					task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, l);
				}
			} else {
				if (historicLocations != null) {
					new LocationTask(context, LocationTask.Type.ADD, historicLocations).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, l);
				}
			}
		}
	}

	@Override
	public void onLocationUpdated(mil.nga.giat.mage.sdk.datastore.location.Location l) {
		if (currentUser != null && !currentUser.getRemoteId().equals(l.getUser().getRemoteId())) {
			if (locations != null) {
				LocationTask task = new LocationTask(getActivity(), LocationTask.Type.UPDATE, locations);
				task.addFilter(getTemporalFilter("timestamp", getLocationTimeFilterId(), LOCATION_FILTER_TYPE));
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, l);
			}
		} else {
			if (historicLocations != null) {
				new LocationTask(context, LocationTask.Type.UPDATE, historicLocations).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, l);
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
	public void onUserCreated(User user) {}

	@Override
	public void onUserUpdated(User user) {}

	@Override
	public void onUserIconUpdated(final User user) {
		new Handler(Looper.getMainLooper()).post(() -> {
			if (locations == null) {
				return;
			}

			locations.refresh(new Pair(new mil.nga.giat.mage.sdk.datastore.location.Location(), user));
		});
	}

	@Override
	public void onUserAvatarUpdated(User user) {
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		Pair<mil.nga.giat.mage.sdk.datastore.location.Location, User> pair = locations.pointForMarker(marker);
		if (pair != null) {
			Intent profileView = new Intent(context, ProfileActivity.class);
			profileView.putExtra(ProfileActivity.USER_ID, pair.second.getRemoteId());
			startActivity(profileView);
			return;
		}

		FeedItem feedItem = feedItems.itemForMarker(marker);
		if (feedItem != null) {
			Intent intent = FeedItemActivity.Companion.intent(context, feedItem);
			startActivity(intent);
			return;
		}
	}

	@Override
	public void onInfoWindowClose(Marker marker) {
		feedItems.onInfoWindowClose(marker);
	}

	public void showObservationBottomSheet(Observation observation) {
		binding.setChosenObservation(observation);
		binding.observationBottomSheet.requestLayout();
		featureBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
		binding.observationBottomSheet.setObservationActionListener(this);
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		hideKeyboard();

		observations.offMarkerClick();

		// search marker
		if(searchMarkers != null) {
			for(Marker m :searchMarkers) {
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

		if (feedItems.onMarkerClick(marker)) {
			return true;
		}

		if (historicLocations.onMarkerClick(marker)) {
			return true;
		}

		// static layer
		if(marker.getSnippet() != null) {
			View markerInfoWindow = LayoutInflater.from(getActivity()).inflate(R.layout.static_feature_infowindow, null, false);
			WebView webView = markerInfoWindow.findViewById(R.id.static_feature_infowindow_content);
			webView.loadData(marker.getSnippet(), "text/html; charset=UTF-8", null);
			new AlertDialog.Builder(getActivity())
				.setView(markerInfoWindow)
				.setPositiveButton(android.R.string.yes, null)
				.show();
		}
		return true;
	}

	@Override
	public void onMapClick(LatLng latLng) {
		hideKeyboard();
		// remove old accuracy circle
		locations.offMarkerClick();
		observations.offMarkerClick();
		unselectObservation();

		observations.onMapClick(latLng);

		staticGeometryCollection.onMapClick(map, latLng, getActivity());

		if (!cacheOverlays.isEmpty()) {
			StringBuilder clickMessage = new StringBuilder();
			for (CacheOverlay cacheOverlay : cacheOverlays.values()) {
				String message = cacheOverlay.onMapClick(latLng, mapView, map);
				if (message != null) {
					if (clickMessage.length() > 0) {
						clickMessage.append("\n\n");
					}
					clickMessage.append(message);
				}
			}

			if (clickMessage.length() > 0) {
				final SpannableString text = new SpannableString(clickMessage.toString());
				Linkify.addLinks(text, Linkify.WEB_URLS);

				View view = getLayoutInflater().inflate(R.layout.view_feature_details, null);
				TextView textView = view.findViewById(R.id.text);
				textView.setText(text);

				new AlertDialog.Builder(getActivity())
					.setView(view)
					.setPositiveButton(android.R.string.yes, null)
					.show();
			}
		}
	}

	@Override
	public void onMapLongClick(LatLng point) {
		hideKeyboard();
		if(!UserHelper.getInstance(getActivity().getApplicationContext()).isCurrentUserPartOfCurrentEvent()) {
			new AlertDialog.Builder(getActivity())
				.setTitle(getActivity().getResources().getString(R.string.no_event_title))
				.setMessage(getActivity().getResources().getString(R.string.observation_no_event_message))
				.setPositiveButton(android.R.string.ok, null)
				.show();
		} else {
			ObservationLocation location = new ObservationLocation(ObservationLocation.MANUAL_PROVIDER, point);
			location.setAccuracy(0.0f);
			location.setTime(new Date().getTime());
			newObservation(location);
		}
	}

	private void newObservation(ObservationLocation location) {
		Intent intent = new Intent(getActivity(), ObservationEditActivity.class);
		intent.putExtra(ObservationEditActivity.LOCATION, location);
		intent.putExtra(ObservationEditActivity.INITIAL_LOCATION, map.getCameraPosition().target);
		intent.putExtra(ObservationEditActivity.INITIAL_ZOOM, map.getCameraPosition().zoom);
		startActivity(intent);
	}

	@Override
	public void onClick(View view) {
		// close keyboard
		hideKeyboard();
		switch (view.getId()) {
			case R.id.map_settings: {
				Intent intent = new Intent(getActivity(), MapPreferencesActivity.class);
				startActivity(intent);
				break;
			}
			}
	}

	@Override
	public void activate(OnLocationChangedListener listener) {
		locationChangedListener = listener;
		Location location = locationProvider.getValue();
		if (location != null) {
			locationChangedListener.onLocationChanged(location);
		}

		locationProvider.observe(this, this);
	}

	@Override
	public void deactivate() {
		locationProvider.removeObserver(this);
		locationChangedListener = null;
	}

	@Override
	public void onCameraIdle() {
		setMgrsCode();
	}

	@Override
	public void onCameraMoveStarted(int reason) {
		if (reason == REASON_GESTURE) {
			locateState = LocateState.OFF;
			zoomToLocationButton.setSelected(false);
		}
	}

	private void setMgrsCode() {
		if (mgrsTileOverlay != null) {
			float zoom = map.getCameraPosition().zoom;
			LatLng center = map.getCameraPosition().target;
			MGRS mgrs = MGRS.from(new mil.nga.mgrs.wgs84.LatLng(center.latitude, center.longitude));

			if (zoom > 9) {
				int accuracy = 5;
				if (zoom < 12) {
					accuracy = 2;
				} else if (zoom < 15) {
					accuracy = 3;
				} else if (zoom < 17) {
					accuracy = 4;
				}

				mgrsChip.setText(mgrs.format(accuracy));
			} else if (zoom > 5) {
				String text = String.format(Locale.getDefault(),"%s%c%c%c", mgrs.getZone(), mgrs.getBand(), mgrs.getE100k(), mgrs.getN100k());
				mgrsChip.setText(text);
			} else {
				String text = String.format(Locale.getDefault(),"%s%c", mgrs.getZone(), mgrs.getBand());
				mgrsChip.setText(text);
			}
		}
	}

	@Override
	public void onCacheOverlay(List<CacheOverlay> cacheOverlays) {
		// Add all overlays that are in the preferences
		Event currentEvent = EventHelper.getInstance(getActivity()).getCurrentEvent();
		cacheOverlays = new CacheOverlayFilter(getContext(), currentEvent).filter(cacheOverlays);

		// Track enabled cache overlays
		Map<String, CacheOverlay> enabledCacheOverlays = new HashMap<>();

		// Track enabled GeoPackages
		Set<String> enabledGeoPackages = new HashSet<>();

		// Reset the bounding box for newly added caches
		addedCacheBoundingBox = null;

		for (CacheOverlay cacheOverlay : cacheOverlays) {
			if(cacheOverlay instanceof StaticFeatureCacheOverlay){
				staticGeometryCollection.removeLayer(((StaticFeatureCacheOverlay)cacheOverlay).getId().toString());
			}

			// If this cache overlay potentially replaced by a new version
			if(cacheOverlay.isAdded()){
				if(cacheOverlay.getType() == CacheOverlayType.GEOPACKAGE){
					geoPackageCache.close(cacheOverlay.getName());
				}
			}

			// The user has asked for this overlay
			if (cacheOverlay.isEnabled()) {

				// Handle each type of cache overlay
				switch(cacheOverlay.getType()) {

					case XYZ_DIRECTORY:
						addXYZDirectoryCacheOverlay(enabledCacheOverlays, (XYZDirectoryCacheOverlay) cacheOverlay);
						break;

					case GEOPACKAGE:
						addGeoPackageCacheOverlay(enabledCacheOverlays, enabledGeoPackages, (GeoPackageCacheOverlay)cacheOverlay);
						break;

					case URL:
						addURLCacheOverlay(enabledCacheOverlays, (URLCacheOverlay)cacheOverlay);
						break;

					case STATIC_FEATURE:
						addStaticFeatureOverlay(enabledCacheOverlays, (StaticFeatureCacheOverlay)cacheOverlay);
						break;
				}
			}

			cacheOverlay.setAdded(false);
		}

		// Remove any overlays that are on the map but no longer selected in
		// preferences, update the tile overlays to the enabled tile overlays
		for (CacheOverlay cacheOverlay : this.cacheOverlays.values()) {
			cacheOverlay.removeFromMap();
		}
		this.cacheOverlays = enabledCacheOverlays;

		// Close GeoPackages no longer enabled
		geoPackageCache.closeRetain(enabledGeoPackages);

		// If a new cache was added, zoom to the bounding box area
		if(addedCacheBoundingBox != null){

			final LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
			boundsBuilder.include(new LatLng(addedCacheBoundingBox.getMinLatitude(), addedCacheBoundingBox
					.getMinLongitude()));
			boundsBuilder.include(new LatLng(addedCacheBoundingBox.getMinLatitude(), addedCacheBoundingBox
					.getMaxLongitude()));
			boundsBuilder.include(new LatLng(addedCacheBoundingBox.getMaxLatitude(), addedCacheBoundingBox
					.getMinLongitude()));
			boundsBuilder.include(new LatLng(addedCacheBoundingBox.getMaxLatitude(), addedCacheBoundingBox
					.getMaxLongitude()));

			try {
				map.animateCamera(CameraUpdateFactory.newLatLngBounds(
						boundsBuilder.build(), 0));
			} catch (Exception e) {
				Log.e(LOG_NAME, "Unable to move camera to newly added cache location", e);
			}
		}
	}

	private void addURLCacheOverlay(Map<String, CacheOverlay> enabledCacheOverlays, URLCacheOverlay urlCacheOverlay){
        // Retrieve the cache overlay if it already exists (and remove from cache overlays)
		CacheOverlay cacheOverlay = cacheOverlays.remove(urlCacheOverlay.getCacheName());
		if(cacheOverlay == null){
			// Create a new tile provider and add to the map
			TileProvider tileProvider = null;
			boolean isTransparent = false;
			if(urlCacheOverlay.getFormat().equalsIgnoreCase("xyz")) {
				tileProvider = new XYZTileProvider(256, 256, urlCacheOverlay);
			}else if(urlCacheOverlay.getFormat().equalsIgnoreCase("tms")){
				tileProvider = new TMSTileProvider(256, 256, urlCacheOverlay);
			}else {
				tileProvider = new WMSTileProvider(256, 256, urlCacheOverlay);
				WMSCacheOverlay wms = (WMSCacheOverlay)urlCacheOverlay;
				isTransparent =  Boolean.parseBoolean(wms.getWmsTransparent());
			}
			TileOverlayOptions overlayOptions = createTileOverlayOptions(tileProvider);

			if(urlCacheOverlay.isBase()) {
				overlayOptions.zIndex(-4);
			} else if(!isTransparent) {
				overlayOptions.zIndex(-3);
			} else{
				overlayOptions.zIndex(-2);
			}
			// Set the tile overlay in the cache overlay
			TileOverlay tileOverlay = map.addTileOverlay(overlayOptions);
			urlCacheOverlay.setTileOverlay(tileOverlay);
			cacheOverlay = urlCacheOverlay;
		}
		// Add the cache overlay to the enabled cache overlays
		enabledCacheOverlays.put(cacheOverlay.getCacheName(), cacheOverlay);
	}

	private void addStaticFeatureOverlay(Map<String, CacheOverlay> enabledCacheOverlays, final StaticFeatureCacheOverlay cacheOverlay) {
		try {
			final Layer layer = LayerHelper.getInstance(getActivity().getApplicationContext()).read(cacheOverlay.getId());
			new Handler(Looper.getMainLooper()).post(() -> new StaticFeatureLoadTask(getActivity().getApplicationContext(), staticGeometryCollection, map).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, layer));
		} catch (LayerException e) {
			Log.e(LOG_NAME, "Problem updating static features.", e);
		}
	}

	/**
	 * Add XYZ Directory tile cache overlay
	 * @param enabledCacheOverlays
	 * @param xyzDirectoryCacheOverlay
	 */
	private void addXYZDirectoryCacheOverlay(Map<String, CacheOverlay> enabledCacheOverlays, XYZDirectoryCacheOverlay xyzDirectoryCacheOverlay){
		// Retrieve the cache overlay if it already exists (and remove from cache overlays)
		CacheOverlay cacheOverlay = cacheOverlays.remove(xyzDirectoryCacheOverlay.getCacheName());
		if(cacheOverlay == null){
			// Create a new tile provider and add to the map
			TileProvider tileProvider = new FileSystemTileProvider(256, 256, xyzDirectoryCacheOverlay.getDirectory().getAbsolutePath());
			TileOverlayOptions overlayOptions = createTileOverlayOptions(tileProvider);
			// Set the tile overlay in the cache overlay
			TileOverlay tileOverlay = map.addTileOverlay(overlayOptions);
			xyzDirectoryCacheOverlay.setTileOverlay(tileOverlay);
			cacheOverlay = xyzDirectoryCacheOverlay;
		}
		// Add the cache overlay to the enabled cache overlays
		enabledCacheOverlays.put(cacheOverlay.getCacheName(), cacheOverlay);
	}

	/**
	 * Add a GeoPackage cache overlay, which contains tile and feature tables
	 * @param enabledCacheOverlays
	 * @param enabledGeoPackages
	 * @param geoPackageCacheOverlay
	 */
	private void addGeoPackageCacheOverlay(Map<String, CacheOverlay> enabledCacheOverlays, Set<String> enabledGeoPackages, GeoPackageCacheOverlay geoPackageCacheOverlay){

		// Check each GeoPackage table
		for(CacheOverlay tableCacheOverlay: geoPackageCacheOverlay.getChildren()){
			// Check if the table is enabled
			if(tableCacheOverlay.isEnabled()){

				// Get and open if needed the GeoPackage
				GeoPackage geoPackage = geoPackageCache.getOrOpen(geoPackageCacheOverlay.getName());
				enabledGeoPackages.add(geoPackage.getName());

				// Handle tile and feature tables
				try {
					switch (tableCacheOverlay.getType()) {
						case GEOPACKAGE_TILE_TABLE:
							addGeoPackageTileCacheOverlay(enabledCacheOverlays, (GeoPackageTileTableCacheOverlay) tableCacheOverlay, geoPackage, false);
							break;
						case GEOPACKAGE_FEATURE_TABLE:
							addGeoPackageFeatureCacheOverlay(enabledCacheOverlays, (GeoPackageFeatureTableCacheOverlay) tableCacheOverlay, geoPackage);
							break;
						default:
							throw new UnsupportedOperationException("Unsupported GeoPackage type: " + tableCacheOverlay.getType());
					}
				}catch(Exception e){
					Log.e(LOG_NAME, "Failed to add GeoPackage overlay. GeoPackage: " + geoPackage.getName() + ", Name: " + tableCacheOverlay.getName(), e);
				}

				// If a newly added cache, update the bounding box for zooming
				if(geoPackageCacheOverlay.isAdded()){

					try {
						BoundingBox boundingBox = geoPackage.getBoundingBox(
								ProjectionFactory.getProjection(ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM),
								tableCacheOverlay.getName());
						if(boundingBox != null) {
							if (addedCacheBoundingBox == null) {
								addedCacheBoundingBox = boundingBox;
							} else {
								addedCacheBoundingBox = TileBoundingBoxUtils.union(addedCacheBoundingBox, boundingBox);
							}
						}
					}catch(Exception e){
						Log.e(LOG_NAME, "Failed to retrieve GeoPackage Table bounding box. GeoPackage: "
								+ geoPackage.getName() + ", Table: " + tableCacheOverlay.getName(), e);
					}
				}
			}
		}
	}

	/**
	 * Add the GeoPackage Tile Table Cache Overlay
	 * @param enabledCacheOverlays
	 * @param tileTableCacheOverlay
	 * @param geoPackage
	 * @param linkedToFeatures
	 */
	private void addGeoPackageTileCacheOverlay(Map<String, CacheOverlay> enabledCacheOverlays, GeoPackageTileTableCacheOverlay tileTableCacheOverlay, GeoPackage geoPackage, boolean linkedToFeatures){
		// Retrieve the cache overlay if it already exists (and remove from cache overlays)
		CacheOverlay cacheOverlay = cacheOverlays.remove(tileTableCacheOverlay.getCacheName());
		if(cacheOverlay != null){
			// If the existing cache overlay is being replaced, create a new cache overlay
			if(tileTableCacheOverlay.getParent().isAdded()){
				cacheOverlay = null;
			}
		}
		if(cacheOverlay == null){
			// Create a new GeoPackage tile provider and add to the map
			TileDao tileDao = geoPackage.getTileDao(tileTableCacheOverlay.getName());

			TileTableScaling tileTableScaling = new TileTableScaling(geoPackage, tileDao);
			TileScaling tileScaling = tileTableScaling.get();

			BoundedOverlay overlay = GeoPackageOverlayFactory
					.getBoundedOverlay(tileDao, getResources().getDisplayMetrics().density, tileScaling);

			TileOverlayOptions overlayOptions = null;
			if(linkedToFeatures){
				overlayOptions = createFeatureTileOverlayOptions(overlay);
			}else {
				overlayOptions = createTileOverlayOptions(overlay);
			}
			TileOverlay tileOverlay = map.addTileOverlay(overlayOptions);
			tileTableCacheOverlay.setTileOverlay(tileOverlay);

			// Check for linked feature tables
			tileTableCacheOverlay.clearFeatureOverlayQueries();
			FeatureTileTableLinker linker = new FeatureTileTableLinker(geoPackage);
			List<FeatureDao> featureDaos = linker.getFeatureDaosForTileTable(tileDao.getTableName());
			for(FeatureDao featureDao: featureDaos){

				// Create the feature tiles
				FeatureTiles featureTiles = new DefaultFeatureTiles(getActivity(), geoPackage, featureDao,
						getResources().getDisplayMetrics().density);

				// Add the feature overlay query
				FeatureOverlayQuery featureOverlayQuery = new FeatureOverlayQuery(getActivity(), overlay, featureTiles);
				tileTableCacheOverlay.addFeatureOverlayQuery(featureOverlayQuery);
			}

			cacheOverlay = tileTableCacheOverlay;
		}
		// Add the cache overlay to the enabled cache overlays
		enabledCacheOverlays.put(cacheOverlay.getCacheName(), cacheOverlay);
	}

	/**
	 * Add the GeoPackage Feature Table Cache Overlay
	 * @param enabledCacheOverlays
	 * @param featureTableCacheOverlay
	 * @param geoPackage
	 */
	private void addGeoPackageFeatureCacheOverlay(Map<String, CacheOverlay> enabledCacheOverlays, GeoPackageFeatureTableCacheOverlay featureTableCacheOverlay, GeoPackage geoPackage){
		// Retrieve the cache overlay if it already exists (and remove from cache overlays)
		CacheOverlay cacheOverlay = cacheOverlays.remove(featureTableCacheOverlay.getCacheName());
		if(cacheOverlay != null){
			// If the existing cache overlay is being replaced, create a new cache overlay
			if(featureTableCacheOverlay.getParent().isAdded()){
				cacheOverlay = null;
			}
			for(GeoPackageTileTableCacheOverlay linkedTileTable: featureTableCacheOverlay.getLinkedTileTables()){
				cacheOverlays.remove(linkedTileTable.getCacheName());
			}
		}
		if(cacheOverlay == null) {
			// Add the features to the map
			FeatureDao featureDao = geoPackage.getFeatureDao(featureTableCacheOverlay.getName());

			// If indexed, add as a tile overlay
			if(featureTableCacheOverlay.isIndexed()){
				FeatureTiles featureTiles = new DefaultFeatureTiles(getActivity(), geoPackage, featureDao,
						getResources().getDisplayMetrics().density);
				Integer maxFeaturesPerTile = null;
				if(featureDao.getGeometryType() == GeometryType.POINT){
					maxFeaturesPerTile = getResources().getInteger(R.integer.geopackage_feature_tiles_max_points_per_tile);
				}else{
					maxFeaturesPerTile = getResources().getInteger(R.integer.geopackage_feature_tiles_max_features_per_tile);
				}
				featureTiles.setMaxFeaturesPerTile(maxFeaturesPerTile);
				NumberFeaturesTile numberFeaturesTile = new NumberFeaturesTile(getActivity());
				// Adjust the max features number tile draw paint attributes here as needed to
				// change how tiles are drawn when more than the max features exist in a tile
				featureTiles.setMaxFeaturesTileDraw(numberFeaturesTile);
				// Adjust the feature tiles draw paint attributes here as needed to change how
				// features are drawn on tiles
				FeatureOverlay featureOverlay = new FeatureOverlay(featureTiles);
				featureOverlay.setMinZoom(featureTableCacheOverlay.getMinZoom());

				// Get the tile linked overlay
				BoundedOverlay overlay = GeoPackageOverlayFactory.getLinkedFeatureOverlay(featureOverlay, geoPackage);

				FeatureOverlayQuery featureOverlayQuery = new FeatureOverlayQuery(getActivity(), overlay, featureTiles);
				featureTableCacheOverlay.setFeatureOverlayQuery(featureOverlayQuery);
				TileOverlayOptions overlayOptions = createFeatureTileOverlayOptions(overlay);
				TileOverlay tileOverlay = map.addTileOverlay(overlayOptions);
				featureTableCacheOverlay.setTileOverlay(tileOverlay);
			}
			// Not indexed, add the features to the map
			else {
				int maxFeaturesPerTable = 0;
				if(featureDao.getGeometryType() == GeometryType.POINT){
					maxFeaturesPerTable = getResources().getInteger(R.integer.geopackage_features_max_points_per_table);
				}else{
					maxFeaturesPerTable = getResources().getInteger(R.integer.geopackage_features_max_features_per_table);
				}
				Projection projection = featureDao.getProjection();
				GoogleMapShapeConverter shapeConverter = new GoogleMapShapeConverter(projection);
				FeatureCursor featureCursor = featureDao.queryForAll();
				try {
					final int totalCount = featureCursor.getCount();
					int count = 0;
					while (featureCursor.moveToNext()) {
						try {
							FeatureRow featureRow = featureCursor.getRow();
							GeoPackageGeometryData geometryData = featureRow.getGeometry();
							if (geometryData != null && !geometryData.isEmpty()) {
								Geometry geometry = geometryData.getGeometry();
								if (geometry != null) {
									GoogleMapShape shape = shapeConverter.toShape(geometry);
									// Set the Shape Marker, PolylineOptions, and PolygonOptions here if needed to change color and style
									featureTableCacheOverlay.addShapeToMap(featureRow.getId(), shape, map);

									if (++count >= maxFeaturesPerTable) {
										if (count < totalCount) {
											Toast.makeText(getActivity().getApplicationContext(), featureTableCacheOverlay.getCacheName()
													+ "- added " + count + " of " + totalCount, Toast.LENGTH_LONG).show();
										}
										break;
									}
								}
							}
						}catch(Exception e){
							Log.e(LOG_NAME, "Failed to display feature. GeoPackage: " + geoPackage.getName()
									+ ", Table: " + featureDao.getTableName() + ", Row: " + featureCursor.getPosition(), e);
						}
					}
				} finally {
					featureCursor.close();
				}
			}

			cacheOverlay = featureTableCacheOverlay;
		}

		// Add the cache overlay to the enabled cache overlays
		enabledCacheOverlays.put(cacheOverlay.getCacheName(), cacheOverlay);
	}

	/**
	 * Create Feature Tile Overlay Options with the default z index for tile layers drawn from features
	 * @param tileProvider
	 * @return
	 */
	private TileOverlayOptions createFeatureTileOverlayOptions(TileProvider tileProvider){
		return createTileOverlayOptions(tileProvider, -1);
	}

	/**
	 * Create Tile Overlay Options with the default z index for tile layers
	 * @param tileProvider
	 * @return
	 */
	private TileOverlayOptions createTileOverlayOptions(TileProvider tileProvider){
		return createTileOverlayOptions(tileProvider, -2);
	}

	/**
	 * Create Tile Overlay Options for the Tile Provider using the z index
	 * @param tileProvider
	 * @param zIndex
	 * @return
	 */
	private TileOverlayOptions createTileOverlayOptions(TileProvider tileProvider, int zIndex){
		TileOverlayOptions overlayOptions = new TileOverlayOptions();
		overlayOptions.tileProvider(tileProvider);
		overlayOptions.zIndex(zIndex);
		return overlayOptions;
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

	@Override
	public void onError(Throwable error) {
	}

	private int getTimeFilterId() {
		return preferences.getInt(getResources().getString(R.string.activeTimeFilterKey), getResources().getInteger(R.integer.time_filter_last_month));
	}

	private int getLocationTimeFilterId() {
		return preferences.getInt(getResources().getString(R.string.activeLocationTimeFilterKey), getResources().getInteger(R.integer.time_filter_last_month));
	}

	private int getCustomTimeNumber(String filterType) {
		if (filterType.equalsIgnoreCase(OBSERVATION_FILTER_TYPE)) {
			return preferences.getInt(getResources().getString(R.string.customObservationTimeNumberFilterKey), 0);
		} else {
			return preferences.getInt(getResources().getString(R.string.customLocationTimeNumberFilterKey), 0);
		}
	}

	private String getCustomTimeUnit(String filterType) {
		if (filterType.equalsIgnoreCase(OBSERVATION_FILTER_TYPE)) {
			return preferences.getString(getResources().getString(R.string.customObservationTimeUnitFilterKey), getResources().getStringArray(R.array.timeUnitEntries)[0]);
		} else {
			return preferences.getString(getResources().getString(R.string.customLocationTimeUnitFilterKey), getResources().getStringArray(R.array.timeUnitEntries)[0]);
		}
	}

	private Filter<Temporal> getTemporalFilter(String columnName, int filterId, String filterType) {
		Filter<Temporal> filter = null;
		Calendar c = Calendar.getInstance();

		if (filterId == getResources().getInteger(R.integer.time_filter_last_month)) {
			c.add(Calendar.MONTH, -1);
		} else if (filterId == getResources().getInteger(R.integer.time_filter_last_week)) {
			c.add(Calendar.DAY_OF_MONTH, -7);
		} else if (filterId == getResources().getInteger(R.integer.time_filter_last_24_hours)) {
			c.add(Calendar.HOUR, -24);
		} else if (filterId == getResources().getInteger(R.integer.time_filter_today)) {
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
		} else if (filterId == getResources().getInteger(R.integer.time_filter_custom)) {
			String customFilterTimeUnit = getCustomTimeUnit(filterType);
			int customTimeNumber = getCustomTimeNumber(filterType);
			switch (customFilterTimeUnit) {
				case "Hours":
					c.add(Calendar.HOUR_OF_DAY, -1 * customTimeNumber);
					break;
				case "Days":
					c.add(Calendar.DAY_OF_MONTH, -1 * customTimeNumber);
					break;
				case "Months":
					c.add(Calendar.MONTH, -1 * customTimeNumber);
					break;
				default:
					c.add(Calendar.MINUTE, -1 * customTimeNumber);
					break;
			}

		} else {
			// no filter
			c = null;
		}

		if (c != null) {
			filter = new DateTimeFilter(c.getTime(), null, columnName);
		}

		return filter;
	}

	private String getFilterTitle() {
		if (getTimeFilterId() != getResources().getInteger(R.integer.time_filter_none)
				|| getLocationTimeFilterId() != getResources().getInteger(R.integer.time_filter_none)
				|| preferences.getBoolean(getResources().getString(R.string.activeImportantFilterKey), false)
				|| preferences.getBoolean(getResources().getString(R.string.activeFavoritesFilterKey), false)) {
			return "Showing filtered results.";
		} else {
			return "";
		}
	}

	private void hideKeyboard() {
		InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
		if (getActivity().getCurrentFocus() != null) {
			inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
		}
	}

	private void scheduleMarkerRefresh(RefreshMarkersRunnable task) {
		getView().postDelayed(task, task.intervalSeconds * 1000);
	}

	private class RefreshMarkersRunnable implements Runnable {
		private final PointCollection<?> points;
		private final String filterColumnName;
		private final String filterType;
		private final int timePeriodFilterId;
		private final int intervalSeconds;

		private RefreshMarkersRunnable(PointCollection<?> points, String filterColumnName, String filterType, int timePeriodFilterId, int intervalSeconds) {
			this.points = points;
			this.filterColumnName = filterColumnName;
			this.filterType = filterType;
			this.timePeriodFilterId = timePeriodFilterId;
			this.intervalSeconds = intervalSeconds;
		}

		public void run() {
			if (MapFragment.this.isDetached()) return;

			if (points.isVisible()) {
				points.refreshMarkerIcons(getTemporalFilter(filterColumnName, timePeriodFilterId, filterType));
			}
			scheduleMarkerRefresh(this);
		}
	}
}