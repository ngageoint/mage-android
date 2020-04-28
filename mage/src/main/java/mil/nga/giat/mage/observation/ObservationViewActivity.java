package mil.nga.giat.mage.observation;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.form.Form;
import mil.nga.giat.mage.form.FormMode;
import mil.nga.giat.mage.form.FormViewModel;
import mil.nga.giat.mage.map.marker.ObservationBitmapFactory;
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
import mil.nga.giat.mage.widget.CoordinateView;
import mil.nga.sf.Geometry;

public class ObservationViewActivity extends AppCompatActivity implements OnMapReadyCallback, OnCameraIdleListener {

	private static final String LOG_NAME = ObservationViewActivity.class.getName();

	public static String OBSERVATION_ID = "OBSERVATION_ID";
	public static String INITIAL_LOCATION = "INITIAL_LOCATION";
	public static String INITIAL_ZOOM = "INITIAL_ZOOM";

	private FormViewModel model;
	private DateFormat dateFormat;
	private GoogleMap map;
    private AttachmentGallery attachmentGallery;
	private IObservationEventListener observationEventListener;
	private Observation o;
	private User currentUser;
	private boolean hasEventUpdatePermission;
	private boolean canEditObservation;
	private MapObservation mapObservation;
	private MapFragment mapFragment;
	private MapObservationManager mapObservationManager;
	private IconTask iconTask;

	ImageView favoriteIcon;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_observation_view);

		model = ViewModelProviders.of(this).get(FormViewModel.class);
		model.setFormMode(FormMode.VIEW);

		dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), getApplicationContext());
		try {
			currentUser = UserHelper.getInstance(this).readCurrentUser();
			hasEventUpdatePermission = currentUser.getRole().getPermissions().getPermissions().contains(Permission.UPDATE_EVENT);
		} catch (Exception e) {
			Log.e(LOG_NAME, "Cannot read current user");
		}

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
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

		favoriteIcon = findViewById(R.id.favorite_icon);
		favoriteIcon.setOnClickListener(v -> toggleFavorite(o));
		findViewById(R.id.directions_icon).setOnClickListener(v -> getDirections());

		findViewById(R.id.favorites).setOnClickListener(v -> onFavoritesClick(o.getFavorites()));

		final FloatingActionButton editButton = findViewById(R.id.edit_button);
		editButton.setVisibility(canEditObservation ? View.VISIBLE : View.GONE);

		editButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				editObservation();
			}
		});

		findViewById(R.id.important_icon).setOnClickListener(v -> onImportantClick());

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
						ObservationViewActivity.this.runOnUiThread(() -> {
							o = observation;
							setupObservation();
							setupMap();
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

		mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mini_map);
		mapFragment.getMapAsync(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		ObservationHelper.getInstance(getApplicationContext()).addListener(observationEventListener);
		setupObservation();
	}


	@Override
	protected void onPause() {
		super.onPause();

		ObservationHelper.getInstance(getApplicationContext()).removeListener(observationEventListener);

		if (iconTask != null) {
			iconTask.cancel(false);
			iconTask = null;
		}
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
		super.onDestroy();

		if (observationEventListener != null) {
			ObservationHelper.getInstance(getApplicationContext()).removeListener(observationEventListener);
		}
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
			if (!o.getForms().isEmpty()) {
				ObservationForm observationForm = o.getForms().iterator().next();
				Map<Long, JsonObject> formMap = EventHelper.getInstance(getApplicationContext()).getCurrentEvent().getFormMap();
				JsonObject formJson = formMap.get(observationForm.getFormId());
				Form form = Form.Companion.fromJson(formJson);

				Map<String, Object> values = new HashMap<>();
				for (Map.Entry<String, ObservationProperty> entry : observationForm.getPropertiesMap().entrySet()) {
					values.put(entry.getKey(), entry.getValue().getValue());
				}

				model.setForm(form, values);

				ViewGroup formsLayout = findViewById(R.id.forms);
				formsLayout.removeAllViews();

				LinearLayout formLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.observation_editor_form, formsLayout, true);
				TextView name = formLayout.findViewById(R.id.form_name);
				name.setText(form.getName());
			}

			Drawable markerPlaceholder = DrawableCompat.wrap(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_place_white_48dp));
			DrawableCompat.setTint(markerPlaceholder, ContextCompat.getColor(getApplicationContext(), R.color.icon));
			DrawableCompat.setTintMode(markerPlaceholder, PorterDuff.Mode.SRC_IN);

			ImageView markerView = findViewById(R.id.observation_marker);
			markerView.setImageDrawable(markerPlaceholder);
			iconTask = new IconTask(markerView);
			iconTask.execute(o);

			ObservationProperty primary = o.getPrimaryField();
			TextView primaryView = findViewById(R.id.primary);
			if (primary == null || primary.isEmpty()) {
				primaryView.setVisibility(View.GONE);
			} else {
				setTitle(primary.getValue().toString());
				primaryView.setVisibility(View.VISIBLE);
				primaryView.setText(primary.getValue().toString());
			}

			ObservationProperty secondary = o.getSecondaryField();
			TextView secondaryView = findViewById(R.id.secondary);
			if (secondary == null || secondary.isEmpty()) {
				secondaryView.setVisibility(View.GONE);
			} else {
				secondaryView.setVisibility(View.VISIBLE);
				secondaryView.setText(secondary.getValue().toString());
			}
            
            TextView timestamp = findViewById(R.id.time);
            timestamp.setText(dateFormat.format(o.getTimestamp()));

            // TODO add location info
			Geometry geometry = o.getGeometry();
			ObservationLocation location = new ObservationLocation(geometry);
			CoordinateView locationTextView = findViewById(R.id.location);
			LatLng latLng = location.getCentroidLatLng();
			locationTextView.setLatLng(latLng);

            String provider = o.getProvider();
            if (provider != null) {
                ((TextView) findViewById(R.id.location_provider)).setText(provider.toUpperCase());
            } else {
                findViewById(R.id.location_provider).setVisibility(View.GONE);
            }

            Float accuracy = o.getAccuracy();
            if (accuracy != null && accuracy > 0) {
                ((TextView) findViewById(R.id.location_accuracy)).setText("\u00B1 " + accuracy + "m");
            } else {
                findViewById(R.id.location_accuracy).setVisibility(View.GONE);
            }

			setupImportant(o.getImportant());

			setFavorites();
			setFavoriteIcon(isFavorite(o));

            LinearLayout galleryLayout = findViewById(R.id.image_gallery);
            galleryLayout.removeAllViews();

			if (o.getAttachments().size() == 0) {
				findViewById(R.id.gallery_container).setVisibility(View.GONE);
			} else {
				findViewById(R.id.gallery_container).setVisibility(View.VISIBLE);
				attachmentGallery = new AttachmentGallery(getBaseContext(), 150, 150);
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

			TextView user = findViewById(R.id.user);
			String userText = "Unknown User";
			User u = UserHelper.getInstance(this).read(o.getUserId());
			if (u != null) {
				userText = u.getDisplayName();
			}
			user.setText(userText);

			FrameLayout fl = findViewById(R.id.sync_status);
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
				TextView syncDate = status.findViewById(R.id.observation_sync_date);
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
		View importantView = findViewById(R.id.important);

		boolean isImportant = important != null && important.isImportant();
		importantView.setVisibility(isImportant ? View.VISIBLE : View.GONE);

		if (isImportant) {
			try {
				TextView overline = findViewById(R.id.important_overline);
				User user = UserHelper.getInstance(getApplicationContext()).read(important.getUserId());
				overline.setText(String.format("FLAGGED BY %s", user.getDisplayName().toUpperCase()));
			} catch (UserException e) {
				e.printStackTrace();
			}

			TextView description = findViewById(R.id.important_description);
			description.setText(important.getDescription());
		}

		ImageView importantIcon = findViewById(R.id.important_icon);
		if (isImportant) {
			importantIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_flag_white_24dp));
			importantIcon.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.observation_flag_active));
		} else {
			importantIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_flag_outlined_white_24dp));
			importantIcon.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.observation_flag_inactive));
		}
	}

	private void onImportantClick() {
		ObservationImportant important = o.getImportant();
		boolean isImportant = important != null && important.isImportant();
		if (isImportant) {
			BottomSheetDialog dialog = new BottomSheetDialog(this);
			View view = getLayoutInflater().inflate(R.layout.view_important_bottom_sheet, null);
			view.findViewById(R.id.update_button).setOnClickListener(v -> {
				onUpdateImportantClick();
				dialog.dismiss();
			});
			view.findViewById(R.id.remove_button).setOnClickListener(v -> {
				onRemoveImportantClick();
				dialog.dismiss();
			});
			dialog.setContentView(view);
			dialog.show();
		} else {
			onUpdateImportantClick();
		}
	}

	public void onUpdateImportantClick() {
		ImportantDialog dialog = ImportantDialog.newInstance(o.getImportant());
		dialog.setOnImportantListener(description1 -> {
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
				important.setDescription(description1);
				observationHelper.addImportant(o);

				setupImportant(important);
			} catch (ObservationException e) {
				Log.e(LOG_NAME, "Error updating important flag for observation: " + o.getRemoteId());
			}
		});

		FragmentManager fm = getSupportFragmentManager();
		dialog.show(fm, "important");
	}


	public void onRemoveImportantClick() {
		ImportantRemoveDialog dialog = new ImportantRemoveDialog();
		dialog.setOnRemoveImportantListener(() -> {
			ObservationHelper observationHelper = ObservationHelper.getInstance(getApplicationContext());
			try {
				observationHelper.removeImportant(o);
				setupImportant(o.getImportant());
			} catch (ObservationException e) {
				Log.e(LOG_NAME, "Error removing important flag for observation: " + o.getRemoteId());
			}
		});

		FragmentManager fm = getSupportFragmentManager();
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
		Collection<String> userIds = Collections2.transform(favorites, favorite -> favorite.getUserId());

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

			setFavoriteIcon(!isFavorite);
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
			TextView favoriteCountView = findViewById(R.id.favoritesCount);
			favoriteCountView.setText(favoritesCount.toString());

			TextView favoritesLabel = findViewById(R.id.favoritesLabel);
			favoritesLabel.setText(favoritesCount == 1 ? "FAVORITE" : "FAVORITES");
		}
	}

	private void setFavoriteIcon(boolean isFavorite) {
		if (isFavorite) {
			favoriteIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_favorite_white_24dp));
			favoriteIcon.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.observation_favorite_active));
		} else {
			favoriteIcon.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_favorite_border_white_24dp));
			favoriteIcon.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.observation_favorite_inactive));
		}
	}

	private void share(final Observation observation) {
		new ObservationShareTask(this, observation).execute();
	}

	class IconTask extends AsyncTask<Observation, Void, Bitmap> {
		private final WeakReference<ImageView> reference;

		public IconTask(ImageView imageView) {
			this.reference = new WeakReference<>(imageView);
		}

		@Override
		protected Bitmap doInBackground(Observation... observations) {
			return ObservationBitmapFactory.bitmap(getApplicationContext(), observations[0]);
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (isCancelled()) {
				bitmap = null;
			}

			ImageView imageView = reference.get();
			if (imageView != null && bitmap != null) {
				imageView.setImageBitmap(bitmap);
			}
		}
	}
}