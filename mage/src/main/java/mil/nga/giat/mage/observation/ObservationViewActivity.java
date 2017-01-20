package mil.nga.giat.mage.observation;

import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.form.LayoutBaker;
import mil.nga.giat.mage.form.LayoutBaker.ControlGenerationType;
import mil.nga.giat.mage.map.marker.ObservationBitmapFactory;
import mil.nga.giat.mage.people.PeopleActivity;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationFavorite;
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
import mil.nga.giat.mage.sdk.utils.DateFormatFactory;

public class ObservationViewActivity extends AppCompatActivity implements OnMapReadyCallback {

	private static final String LOG_NAME = ObservationViewActivity.class.getName();

	public static String OBSERVATION_ID = "OBSERVATION_ID";
	public static String INITIAL_LOCATION = "INITIAL_LOCATION";
	public static String INITIAL_ZOOM = "INITIAL_ZOOM";

	private final DateFormat dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault());
	private GoogleMap map;
    private AttachmentGallery attachmentGallery;
	private IObservationEventListener observationEventListener;
	private Observation o;
	private User currentUser;
	private boolean canEditObservation = false;
	private Marker marker;
	private DecimalFormat latLngFormat = new DecimalFormat("###.#####");

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.observation_viewer);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		Event event = EventHelper.getInstance(getApplicationContext()).getCurrentEvent();
		if (event != null) {
			getSupportActionBar().setSubtitle(event.getName());
		}

		try {
			LayoutBaker.populateLayoutWithControls((LinearLayout) findViewById(R.id.propertyContainer), LayoutBaker.createControlsFromJson(this, ControlGenerationType.VIEW, ObservationHelper.getInstance(getApplicationContext()).read(getIntent().getLongExtra(OBSERVATION_ID, 0L)).getEvent().getForm()));
		} catch(Exception e) {
			Log.e(LOG_NAME, "Problem getting observation.", e);
		}

		try {
			User user = UserHelper.getInstance(getApplicationContext()).readCurrentUser();
			Collection<Permission> permissions = user.getRole().getPermissions().getPermissions();
			canEditObservation = permissions.contains(Permission.UPDATE_OBSERVATION_ALL) || permissions.contains(Permission.UPDATE_OBSERVATION_EVENT);
		} catch (UserException e) {
			Log.e(LOG_NAME, "Cannot read current user", e);
		}

		final FloatingActionButton editButton = (FloatingActionButton) findViewById(R.id.edit_button);
		editButton.setVisibility(canEditObservation ? View.VISIBLE : View.GONE);
		editButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				editObservation();
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

				for (Observation observation : observations) {
					if (o == null || (observation.getId().equals(o.getId()) && !observation.getLastModified().equals(o.getLastModified()))) {
						ObservationViewActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								setupObservation();
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

		((MapFragment) getFragmentManager().findFragmentById(R.id.mini_map)).getMapAsync(this);
  	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				this.finish();
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
		setupObservation();
	}

	private void setupObservation() {
		try {
			final ImageView favoriteIcon = (ImageView) findViewById(R.id.favoriteIcon);
			if (o == null) {
				favoriteIcon.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						toggleFavorite(o, favoriteIcon);
					}
				});

				findViewById(R.id.favorites).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onFavoritesClick(o.getFavorites());
					}
				});
			}

			o = ObservationHelper.getInstance(getApplicationContext()).read(getIntent().getLongExtra(OBSERVATION_ID, 0L));

			Map<String, ObservationProperty> propertiesMap = o.getPropertiesMap();

			ObservationProperty observationProperty = propertiesMap.get("type");
			if (observationProperty != null) {
				this.setTitle(observationProperty.getValue().toString());
			}

			Geometry geo = o.getGeometry();
			if (geo instanceof Point) {
				Point pointGeo = (Point) geo;
				((TextView) findViewById(R.id.location)).setText(latLngFormat.format(pointGeo.getY()) + ", " + latLngFormat.format(pointGeo.getX()));
				if (propertiesMap.containsKey("provider")) {
					((TextView) findViewById(R.id.location_provider)).setText("(" + propertiesMap.get("provider").getValue() + ")");
				} else {
					findViewById(R.id.location_provider).setVisibility(View.GONE);
				}
				if (propertiesMap.containsKey("accuracy") && Float.parseFloat(propertiesMap.get("accuracy").getValue().toString()) > 0f) {
					((TextView) findViewById(R.id.location_accuracy)).setText("\u00B1" + propertiesMap.get("accuracy").getValue().toString() + "m");
				} else {
					findViewById(R.id.location_accuracy).setVisibility(View.GONE);
				}

				map.getUiSettings().setZoomControlsEnabled(false);

				if (marker == null) {
					LatLng latLng = getIntent().getParcelableExtra(INITIAL_LOCATION);
					if (latLng == null) {
						latLng = new LatLng(0, 0);
					}
					float zoom = getIntent().getFloatExtra(INITIAL_ZOOM, 0);
					map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

					LatLng location = new LatLng(pointGeo.getY(), pointGeo.getX());
					map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
					marker = map.addMarker(new MarkerOptions().position(location).icon(ObservationBitmapFactory.bitmapDescriptor(this, o)));
				} else {
					LatLng location = new LatLng(pointGeo.getY(), pointGeo.getX());
					map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, map.getCameraPosition().zoom));
					marker.setPosition(location);
				}
			}

			setupImportant(o.getImportant());
			setFavorites(o.getFavorites());
			setFavoriteImage(favoriteIcon, isFavorite(o));

			LayoutBaker.populateLayoutFromMap((LinearLayout) findViewById(R.id.propertyContainer), ControlGenerationType.VIEW, o.getPropertiesMap());
			LayoutBaker.populateLayoutFromMap((LinearLayout) findViewById(R.id.topPropertyContainer), ControlGenerationType.VIEW, o.getPropertiesMap());

            LinearLayout galleryLayout = (LinearLayout) findViewById(R.id.image_gallery);
            galleryLayout.removeAllViews();
			if (o.getAttachments().size() == 0) {
				findViewById(R.id.gallery_container).setVisibility(View.GONE);
			} else {
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
				View.inflate(getApplicationContext(), R.layout.saved_locally, fl);
			} else {
				View status = View.inflate(getApplicationContext(), R.layout.submitted_on, fl);
				TextView syncDate = (TextView) status.findViewById(R.id.observation_sync_date);
				syncDate.setText(dateFormat.format(o.getLastModified()));
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, e.getMessage(), e);
		}
	}

	private void editObservation() {
		if(!UserHelper.getInstance(getApplicationContext()).isCurrentUserPartOfCurrentEvent()) {
			new AlertDialog.Builder(this).setTitle("Not a member of this event").setMessage("You are an administrator and not a member of the current event.  You can not edit this observation.").setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
				}
			}).show();
		} else {
			Intent intent = new Intent(this, ObservationEditActivity.class);
			intent.putExtra(ObservationEditActivity.OBSERVATION_ID, o.getId());

			if (map != null) {
				intent.putExtra(ObservationViewActivity.INITIAL_LOCATION, map.getCameraPosition().target);
				intent.putExtra(ObservationViewActivity.INITIAL_ZOOM, map.getCameraPosition().zoom);
			}

			startActivityForResult(intent, 2);
		}

	}

	private void setupImportant(ObservationImportant important) {
		UserHelper userHelper = UserHelper.getInstance(getApplicationContext());

		boolean isImportant = important != null && important.isImportant();
		boolean canEdit = false;
		try {
			currentUser = userHelper.readCurrentUser();
			canEdit = currentUser.getRole().getPermissions().getPermissions().contains(Permission.UPDATE_EVENT);
		} catch (UserException e) {
			Log.e(LOG_NAME, "Could not read current user", e);
		}

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
			findViewById(R.id.importantActions).setVisibility(canEdit ? View.VISIBLE : View.GONE);
		} else {
			imporantView.setVisibility(View.GONE);
			findViewById(R.id.importantActions).setVisibility(View.GONE);
			findViewById(R.id.addImportant).setVisibility(canEdit ? View.VISIBLE : View.GONE);
		}
	}

	public void onUpdateImportantClick(View v) {
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

	private void toggleFavorite(Observation observation, ImageView imageView) {
		ObservationHelper observationHelper = ObservationHelper.getInstance(getApplicationContext());
		boolean isFavorite = isFavorite(observation);
		try {
			if (isFavorite) {
				observationHelper.unfavoriteObservation(observation, currentUser);
			} else {
				observationHelper.favoriteObservation(observation, currentUser);
			}

			setFavoriteImage(imageView, !isFavorite);
		} catch (ObservationException e) {
			String text = isFavorite ? "Problem unfavoriting observation" : "Problem favoriting observation";
			Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
			Log.e(LOG_NAME, "Could not unfavorite observation", e);
		}
	}

	private boolean isFavorite(Observation observation) {
		boolean isFavorite = false;
		try {
			currentUser = UserHelper.getInstance(getApplicationContext()).readCurrentUser();
			if (currentUser != null) {
				ObservationFavorite favorite = observation.getFavoritesMap().get(currentUser.getRemoteId());
				isFavorite = favorite != null && favorite.isFavorite();
			}
		} catch (UserException e) {
			Log.e(LOG_NAME, "Could not get user", e);
		}

		return isFavorite;
	}

	private void setFavorites(Collection<ObservationFavorite> favorites) {
		Integer favoritesCount = favorites.size();
		findViewById(R.id.favorites).setVisibility(favoritesCount > 0 ? View.VISIBLE : View.GONE) ;
		if (favoritesCount > 0) {
			TextView favoriteCountView = (TextView) findViewById(R.id.favoritesCount);
			favoriteCountView.setText(favoritesCount.toString());

			TextView favoritesLabel = (TextView) findViewById(R.id.favoritesLabel);
			favoritesLabel.setText(favoritesCount == 1 ? "FAVORITE" : "FAVORITES");
		}
	}

	private void setFavoriteImage(ImageView imageView, boolean isFavorite) {
		if (isFavorite) {
			imageView.setColorFilter(ContextCompat.getColor(this, R.color.observation_favorite_active));
		} else {
			imageView.setColorFilter(ContextCompat.getColor(this, R.color.observation_favorite_inactive));
		}
	}

	private void share(final Observation observation) {
		new ObservationShareTask(this, observation).execute();
	}
}