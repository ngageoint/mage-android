package mil.nga.giat.mage.observation;

import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.form.LayoutBaker;
import mil.nga.giat.mage.people.PeopleActivity;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationFavorite;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationForm;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationImportant;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.Permission;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.event.IObservationEventListener;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.utils.DateFormatFactory;
import mil.nga.wkb.geom.Geometry;

public class ObservationViewActivity extends AppCompatActivity implements OnMapReadyCallback, OnCameraIdleListener {

	private static final String LOG_NAME = ObservationViewActivity.class.getName();

	public static String OBSERVATION_ID = "OBSERVATION_ID";
	public static String INITIAL_LOCATION = "INITIAL_LOCATION";
	public static String INITIAL_ZOOM = "INITIAL_ZOOM";

	private static int FORM_ID_PREFIX = 100;

	private DateFormat dateFormat;
	private GoogleMap map;
    private AttachmentGallery attachmentGallery;
	private IObservationEventListener observationEventListener;
	private Observation o;
	private User currentUser;
	private boolean hasEventUpdatePermission;
	private boolean canEditObservation;
	private MapObservation mapObservation;
	private DecimalFormat latLngFormat = new DecimalFormat("###.#####");
	private MapFragment mapFragment;
	private MapObservationManager mapObservationManager;
	private BottomSheetBehavior bottomSheetBehavior;

	ImageView favoriteIcon;

	private Map<Long, Collection<View>> controls = new HashMap<>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), getApplicationContext());
		try {
			currentUser = UserHelper.getInstance(this).readCurrentUser();
			hasEventUpdatePermission = currentUser.getRole().getPermissions().getPermissions().contains(Permission.UPDATE_EVENT);
		} catch (Exception e) {
			Log.e(LOG_NAME, "Cannot read current user");
		}

		setContentView(R.layout.observation_viewer);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		Event event = EventHelper.getInstance(getApplicationContext()).getCurrentEvent();
		if (event != null) {
			getSupportActionBar().setSubtitle(event.getName());
		}

		try {
			o = ObservationHelper.getInstance(getApplicationContext()).read(getIntent().getLongExtra(OBSERVATION_ID, 0L));
		} catch(Exception e) {
			Log.e(LOG_NAME, "Problem getting observation.", e);
		}

		try {
			Collection<Permission> permissions = currentUser.getRole().getPermissions().getPermissions();
			canEditObservation = permissions.contains(Permission.UPDATE_OBSERVATION_ALL) || permissions.contains(Permission.UPDATE_OBSERVATION_EVENT);
		} catch (Exception e) {
			Log.e(LOG_NAME, "Cannot read current user", e);
		}

		favoriteIcon = (ImageView) findViewById(R.id.favoriteIcon);
		favoriteIcon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleFavorite(o);
			}
		});
		findViewById(R.id.directions).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getDirections();
			}
		});

		findViewById(R.id.favorites).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onFavoritesClick(o.getFavorites());
			}
		});

		final FloatingActionButton editButton = (FloatingActionButton) findViewById(R.id.edit_button);
		editButton.setVisibility(canEditObservation ? View.VISIBLE : View.GONE);
		editButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				editObservation();
			}
		});

		View bottomSheet = findViewById(R.id.bottom_sheet);
		bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
		bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
		bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View bottomSheet, int newState) {
			}

			@Override
			public void onSlide(@NonNull View bottomSheet, float slideOffset) {
				float scale = slideOffset < 0 ? Math.abs(slideOffset) : 1 - slideOffset;
				if (!Float.isNaN(scale)) {
					editButton.animate().scaleX(scale).scaleY(scale).setDuration(0).start();
				}
			}
		});
		findViewById(R.id.important_actions_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
					bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
				} else {
					bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
				}
			}
		});

		observationEventListener = new IObservationEventListener() {
			@Override
			public void onObservationUpdated(Observation observation) {
				updateObservation(Collections.singletonList(observation));
			}

			@Override
			public void onObservationCreated(Collection<Observation> observations, Boolean sendUserNotifications) {
				updateObservation(observations);
			}

			private void updateObservation(Collection<Observation> observations) {
				if (map == null) return;

				for (final Observation observation : observations) {
					if (o == null || (observation.getId().equals(o.getId()) && !observation.getLastModified().equals(o.getLastModified()))) {
						ObservationViewActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								o = observation;
								setupObservation();
								setupMap();
							}
						});
					}
				}
			}

			@Override
			public void onObservationDeleted(Observation observation) {
			}

			@Override
			public void onError(Throwable error) {
			}
		};

		ObservationHelper.getInstance(getApplicationContext()).addListener(observationEventListener);

		mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mini_map);
		mapFragment.getMapAsync(this);
		setupObservation();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onDestroy() {
		if (observationEventListener != null) {
			ObservationHelper.getInstance(getApplicationContext()).removeListener(observationEventListener);
		}
		super.onDestroy();
	}

	@Override
	public void onMapReady(GoogleMap map) {
		this.map = map;
		mapObservationManager = new MapObservationManager(this, map);
		map.setOnCameraIdleListener(this);
	}

	@Override
	public void onCameraIdle() {
		map.setOnCameraIdleListener(null);
		setupMap();
	}

	private void setupObservation() {
		try {
			// Grab form definitions
			Map<Long, JsonObject> formMap = EventHelper.getInstance(getApplicationContext()).getCurrentEvent().getFormMap();
			Collection<JsonObject> formDefinitions = new ArrayList<>();
			for (ObservationForm observationForm : o.getForms()) {
				JsonObject form = formMap.get(observationForm.getFormId());

				if (form != null) {
					// TODO pull the form if we don't have it
					formDefinitions.add(formMap.get(observationForm.getFormId()));
				}
			}

			controls = LayoutBaker.createControls(this, LayoutBaker.ControlGenerationType.VIEW, formDefinitions);

			LayoutInflater inflater = getLayoutInflater();
			LinearLayout forms = (LinearLayout) findViewById(R.id.forms);
			forms.removeAllViews();
			for (Map.Entry<Long, Collection<View>> entry : controls.entrySet()) {
				LinearLayout form = (LinearLayout) inflater.inflate(R.layout.observation_editor_form, null);
				form.setId(FORM_ID_PREFIX + entry.getKey().intValue());

				JsonObject definition = formMap.get(entry.getKey());
				TextView formName = (TextView) form.findViewById(R.id.form_name);
				formName.setText(definition.get("name").getAsString());

				LayoutBaker.populateLayoutWithControls((LinearLayout) form.findViewById(R.id.form_content), entry.getValue());

				forms.addView(form);
			}

			ObservationProperty primary = o.getPrimaryField();
			TextView primaryView = (TextView) findViewById(R.id.primary_field);
			if (primary == null || primary.isEmpty()) {
				primaryView.setVisibility(View.GONE);
			} else {
				setTitle(primary.getValue().toString());
				primaryView.setVisibility(View.VISIBLE);
				primaryView.setText(primary.getValue().toString());
			}

			ObservationProperty secondary = o.getSecondaryField();
			TextView secondaryView = (TextView) findViewById(R.id.secondary_field);
			if (secondary == null || secondary.isEmpty()) {
				secondaryView.setVisibility(View.GONE);
			} else {
				secondaryView.setVisibility(View.VISIBLE);
				secondaryView.setText(secondary.getValue().toString());
			}
            
            TextView timestamp = (TextView) findViewById(R.id.timestamp);
            timestamp.setText(dateFormat.format(o.getTimestamp()));

			Geometry geometry = o.getGeometry();
			ObservationLocation location = new ObservationLocation(geometry);
			TextView locationTextView = (TextView) findViewById(R.id.location);
			LatLng latLng = location.getCentroidLatLng();
			locationTextView.setText(latLngFormat.format(latLng.latitude) + ", " + latLngFormat.format(latLng.longitude));
            
            String provider = o.getProvider();
            if (provider != null) {
                ((TextView) findViewById(R.id.location_provider)).setText("(" + provider + ")");
            } else {
                findViewById(R.id.location_provider).setVisibility(View.GONE);
            }
            
            Float accuracy = o.getAccuracy();
            if (accuracy != null && accuracy > 0) {
                ((TextView) findViewById(R.id.location_accuracy)).setText("\u00B1" + accuracy + "m");
            } else {
                findViewById(R.id.location_accuracy).setVisibility(View.GONE);
            }

			setupImportant(o.getImportant());
			setFavorites();
			setFavoriteImage(isFavorite(o));

			LinearLayout formsLayout = (LinearLayout) findViewById(R.id.forms);
			for (ObservationForm observationForm : o.getForms()) {
				LinearLayout formLayout = (LinearLayout) formsLayout.findViewById(FORM_ID_PREFIX + observationForm.getFormId().intValue());
				LayoutBaker.populateLayout(formLayout, LayoutBaker.ControlGenerationType.VIEW, observationForm.getPropertiesMap());
			}

            LinearLayout galleryLayout = (LinearLayout) findViewById(R.id.image_gallery);
            galleryLayout.removeAllViews();

			if (o.getAttachments().size() == 0) {
				findViewById(R.id.gallery_container).setVisibility(View.GONE);
			} else {
				findViewById(R.id.gallery_container).setVisibility(View.VISIBLE);
				attachmentGallery = new AttachmentGallery(getApplicationContext(), 150, 150);
                attachmentGallery.addOnAttachmentClickListener(new AttachmentGallery.OnAttachmentClickListener() {
                    @Override
                    public void onAttachmentClick(Attachment attachment) {
                        Intent intent = new Intent(getApplicationContext(), AttachmentViewerActivity.class);
                        intent.putExtra(AttachmentViewerActivity.ATTACHMENT_ID, attachment.getId());
                        intent.putExtra(AttachmentViewerActivity.EDITABLE, false);
                        startActivity(intent);
                    }
                });
                attachmentGallery.addAttachments(galleryLayout, o.getAttachments());
			}

			TextView user = (TextView) findViewById(R.id.username);
			String userText = "Unknown User";
			User u = UserHelper.getInstance(this).read(o.getUserId());
			if (u != null) {
				userText = u.getDisplayName();
			}
			user.setText(userText);

			FrameLayout fl = (FrameLayout) findViewById(R.id.sync_status);
			fl.removeAllViews();
			if (o.isDirty()) {
				if (o.hasValidationError()) {
					View errorView = View.inflate(getApplicationContext(), R.layout.observation_error, fl);
					TextView errorText = (TextView) errorView.findViewById(R.id.error_text);
					errorText.setText(o.errorMessage());
				} else {
					View.inflate(getApplicationContext(), R.layout.saved_locally, fl);
				}
			} else {
				View status = View.inflate(getApplicationContext(), R.layout.submitted_on, fl);
				TextView syncDate = (TextView) status.findViewById(R.id.observation_sync_date);
				syncDate.setText(dateFormat.format(o.getLastModified()));
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, e.getMessage(), e);
		}
	}

	private void setupMap() {
		if (map == null) {
			return;
		}

		Geometry geometry = o.getGeometry();
		ObservationLocation location = new ObservationLocation(geometry);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		map.getUiSettings().setZoomControlsEnabled(false);
		map.setMapType(preferences.getInt(getString(R.string.baseLayerKey), getResources().getInteger(R.integer.baseLayerDefaultValue)));

		int dayNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
		if (dayNightMode == Configuration.UI_MODE_NIGHT_NO) {
			map.setMapStyle(null);
		} else {
			map.setMapStyle(MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.map_theme_night));
		}

		if (mapObservation == null) {
			LatLng initialLatLng = getIntent().getParcelableExtra(INITIAL_LOCATION);
			if (initialLatLng == null) {
				initialLatLng = new LatLng(0, 0);
			}
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLatLng, 15));

			map.animateCamera(location.getCameraUpdate(mapFragment.getView(), 15));
		} else {
			mapObservation.remove();
			map.moveCamera(location.getCameraUpdate(mapFragment.getView(), (int)map.getCameraPosition().zoom));
		}

		mapObservation = mapObservationManager.addToMap(o);
	}

	private void getDirections() {
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW, o.getGoogleMapsUri());
		startActivity(intent);
	}

	private void editObservation() {
		if (!UserHelper.getInstance(getApplicationContext()).isCurrentUserPartOfCurrentEvent()) {
			new AlertDialog.Builder(this)
					.setTitle("Not a member of this event")
					.setMessage("You are an administrator and not a member of the current event.  You can not edit this observation.")
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
					}
			}).show();

			return;
		}

		Intent intent = new Intent(this, ObservationEditActivity.class);
		intent.putExtra(ObservationEditActivity.OBSERVATION_ID, o.getId());

		if (map != null) {
			intent.putExtra(ObservationViewActivity.INITIAL_LOCATION, map.getCameraPosition().target);
			intent.putExtra(ObservationViewActivity.INITIAL_ZOOM, map.getCameraPosition().zoom);
		}

		startActivity(intent);
	}

	private void setupImportant(ObservationImportant important) {
		UserHelper userHelper = UserHelper.getInstance(getApplicationContext());

		boolean isImportant = important != null && important.isImportant();

		View imporantView = findViewById(R.id.important);
		if (isImportant) {
			imporantView.setVisibility(View.VISIBLE);

			String displayName = "Unknown user";
			try {
				User user = userHelper.read(important.getUserId());
				displayName = user.getDisplayName();
			} catch (UserException e) {
				Log.e(LOG_NAME, "Error finding user with remote id: " + important.getUserId());
			}
			((TextView) findViewById(R.id.importantUser)).setText(String.format(getString(R.string.important_flagged_by), displayName));

			DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
			((TextView) findViewById(R.id.importantDate)).setText(dateFormat.format(important.getTimestamp()));

			String description = important.getDescription();
			TextView descriptionView = (TextView) findViewById(R.id.importantDescription);
			descriptionView.setVisibility(StringUtils.isNoneEmpty(description) ? View.VISIBLE : View.GONE);
			descriptionView.setText(description);

			findViewById(R.id.addImportant).setVisibility(View.GONE);
			findViewById(R.id.important_actions_button).setVisibility(canFlagObservation() ? View.VISIBLE : View.GONE);
		} else {
			imporantView.setVisibility(View.GONE);
			findViewById(R.id.important_actions_button).setVisibility(View.GONE);
			findViewById(R.id.addImportant).setVisibility(canFlagObservation() ? View.VISIBLE : View.GONE);
		}
	}

	public void onUpdateImportantClick(View v) {
		bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

		final ObservationImportant important = o.getImportant();
		String description = important != null ? important.getDescription() : null;
		ImportantDialog dialog = ImportantDialog.newInstance(description);
		dialog.setOnImportantListener(new ImportantDialog.OnImportantListener() {
			@Override
			public void onImportant(String description) {
				ObservationHelper observationHelper = ObservationHelper.getInstance(getApplicationContext());
				try {
					ObservationImportant important = o.getImportant();
					if (important == null) {
						important = new ObservationImportant();
						o.setImportant(important);
					}

					if (currentUser != null) {
						important.setUserId(currentUser.getRemoteId());
					}

					important.setTimestamp(new Date());
					important.setDescription(description);
					observationHelper.addImportant(o);

					setupImportant(important);
				} catch (ObservationException e) {
					Log.e(LOG_NAME, "Error updating important flag for observation: " + o.getRemoteId());
				}
			}
		});

		FragmentManager fm = getFragmentManager();
		dialog.show(fm, "important");
	}


	public void onRemoveImportantClick(View v) {
		bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

		ImportantRemoveDialog dialog = new ImportantRemoveDialog();
		dialog.setOnRemoveImportantListener(new ImportantRemoveDialog.OnRemoveImportantListener() {
			@Override
			public void onRemoveImportant() {
				ObservationHelper observationHelper = ObservationHelper.getInstance(getApplicationContext());
				try {
					observationHelper.removeImportant(o);
					setupImportant(o.getImportant());
				} catch (ObservationException e) {
					Log.e(LOG_NAME, "Error removing important flag for observation: " + o.getRemoteId());
				}
			}
		});

		FragmentManager fm = getFragmentManager();
		dialog.show(fm, "remove_important");
	}

	private boolean canFlagObservation() {
		return hasEventUpdatePermission || hasUpdatePermissionsInEventAcl();
	}

	private boolean hasUpdatePermissionsInEventAcl() {

		boolean hasEventUpdatePermissionsInAcl = false;

		if (currentUser != null) {
			JsonObject acl = EventHelper.getInstance(getApplicationContext()).getCurrentEvent().getAcl();
			JsonElement userAccess = acl.get(currentUser.getRemoteId());
			if (userAccess != null) {
				JsonElement permissions = userAccess.getAsJsonObject().get("permissions");
				if (permissions != null) {
					hasEventUpdatePermissionsInAcl = permissions.getAsJsonArray().toString().contains("update");
				}
			}
		}

		return hasEventUpdatePermissionsInAcl;
	}

	private void onFavoritesClick(Collection<ObservationFavorite> favorites) {
		Collection<String> userIds = Collections2.transform(favorites, new Function<ObservationFavorite, String>() {
			@Override
			public String apply(ObservationFavorite favorite) {
				return favorite.getUserId();
			}
		});

		Intent intent = new Intent(this, PeopleActivity.class);
		intent.putStringArrayListExtra(PeopleActivity.USER_REMOTE_IDS, new ArrayList<>(userIds));
		startActivity(intent);
	}

	private void toggleFavorite(Observation observation) {
		ObservationHelper observationHelper = ObservationHelper.getInstance(getApplicationContext());
		boolean isFavorite = isFavorite(observation);
		try {
			if (isFavorite) {
				observationHelper.unfavoriteObservation(observation, currentUser);
			} else {
				observationHelper.favoriteObservation(observation, currentUser);
			}

			setFavoriteImage(!isFavorite);
		} catch (ObservationException e) {
			String text = isFavorite ? "Problem unfavoriting observation" : "Problem favoriting observation";
			Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
			Log.e(LOG_NAME, "Could not unfavorite observation", e);
		}
	}

	private boolean isFavorite(Observation observation) {
		boolean isFavorite = false;
		if (currentUser != null) {
			ObservationFavorite favorite = observation.getFavoritesMap().get(currentUser.getRemoteId());
			isFavorite = favorite != null && favorite.isFavorite();
		}

		return isFavorite;
	}

	private void setFavorites() {
		Integer favoritesCount = Collections2.filter(o.getFavorites(), new Predicate<ObservationFavorite>() {
			@Override
			public boolean apply(ObservationFavorite favorite) {
				return favorite.isFavorite();
			}
		}).size();


		findViewById(R.id.favorites).setVisibility(favoritesCount > 0 ? View.VISIBLE : View.GONE) ;
		if (favoritesCount > 0) {
			TextView favoriteCountView = (TextView) findViewById(R.id.favoritesCount);
			favoriteCountView.setText(favoritesCount.toString());

			TextView favoritesLabel = (TextView) findViewById(R.id.favoritesLabel);
			favoritesLabel.setText(favoritesCount == 1 ? "FAVORITE" : "FAVORITES");
		}
	}

	private void setFavoriteImage(boolean isFavorite) {
		if (isFavorite) {
			favoriteIcon.setColorFilter(ContextCompat.getColor(this, R.color.observation_favorite_active));
		} else {
			favoriteIcon.setColorFilter(ContextCompat.getColor(this, R.color.observation_favorite_inactive));
		}
	}

	private void share(final Observation observation) {
		new ObservationShareTask(this, observation).execute();
	}

}