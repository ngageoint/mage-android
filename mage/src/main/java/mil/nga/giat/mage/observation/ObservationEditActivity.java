package mil.nga.giat.mage.observation;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import mil.nga.giat.mage.BuildConfig;
import mil.nga.giat.mage.LandingActivity;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.form.DateFormField;
import mil.nga.giat.mage.form.FieldType;
import mil.nga.giat.mage.form.Form;
import mil.nga.giat.mage.form.FormField;
import mil.nga.giat.mage.form.FormFragment;
import mil.nga.giat.mage.form.FormMode;
import mil.nga.giat.mage.form.FormPreferences;
import mil.nga.giat.mage.form.FormViewModel;
import mil.nga.giat.mage.form.GeometryFormField;
import mil.nga.giat.mage.form.field.EditDate;
import mil.nga.giat.mage.form.field.EditGeometry;
import mil.nga.giat.mage.form.field.Field;
import mil.nga.giat.mage.form.field.dialog.DateFieldDialog;
import mil.nga.giat.mage.form.field.dialog.GeometryFieldDialog;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationForm;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.datastore.observation.State;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.Permission;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

public class ObservationEditActivity extends AppCompatActivity implements OnMapReadyCallback, OnCameraIdleListener {

	private static final String LOG_NAME = ObservationEditActivity.class.getName();

	private static final int PERMISSIONS_REQUEST_CAMERA = 100;
	private static final int PERMISSIONS_REQUEST_VIDEO = 200;
	private static final int PERMISSIONS_REQUEST_AUDIO = 300;
	private static final int PERMISSIONS_REQUEST_STORAGE = 400;

	public static final String OBSERVATION_ID = "OBSERVATION_ID";
	public static final String OBSERVATION_FORM_ID = "OBSERVATION_FORM_ID";
	public static final String LOCATION = "LOCATION";
	public static final String INITIAL_LOCATION = "INITIAL_LOCATION";
	public static final String INITIAL_ZOOM = "INITIAL_ZOOM";
	private static final String CURRENT_MEDIA_PATH = "CURRENT_MEDIA_PATH";

	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
	private static final int CAPTURE_VOICE_ACTIVITY_REQUEST_CODE = 300;
	private static final int GALLERY_ACTIVITY_REQUEST_CODE = 400;

	private static final long NEW_OBSERVATION = -1L;
	private static final long NO_FORM = -1L;

	private ArrayList<Attachment> attachmentsToCreate = new ArrayList<>();

	private boolean isNewObservation;
	private Observation observation;
	private User currentUser;
	private boolean hasObservationDeletePermission;
	private GoogleMap map;
	private MapObservation mapObservation;
	private Circle accuracyCircle;
	private long locationElapsedTimeMilliseconds = 0;
	private MapFragment mapFragment;
	private MapObservationManager mapObservationManager;
	private FormFragment formFragment;

	private FormViewModel model;

	private LinearLayout attachmentLayout;
	private AttachmentGallery attachmentGallery;
	private String currentMediaPath;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_observation_edit);

		model = ViewModelProviders.of(this).get(FormViewModel.class);
		model.setFormMode(FormMode.EDIT);

		final long observationId = getIntent().getLongExtra(OBSERVATION_ID, NEW_OBSERVATION);
		final long formId = getIntent().getLongExtra(OBSERVATION_FORM_ID, NO_FORM);

		isNewObservation = observationId == NEW_OBSERVATION;

		try {
			currentUser = UserHelper.getInstance(this).readCurrentUser();
			hasObservationDeletePermission = currentUser.getRole().getPermissions().getPermissions().contains(Permission.DELETE_OBSERVATION);
		} catch (UserException e) {
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

		FormField<Date> timestampField = new DateFormField(0, FieldType.DATE, "timestamp", "Date", true, false);
		model.setTimestamp(timestampField);
		EditDate editTimestamp = findViewById(R.id.date);
		editTimestamp.bind(timestampField);
		editTimestamp.setOnEditDateClickListener(new Function1<FormField<Date>, Unit>() {
			@Override
			public Unit invoke(FormField<Date> dateFormField) {
				DateFieldDialog dialog = DateFieldDialog.Companion.newInstance();
				dialog.show(getSupportFragmentManager(), "TIMESTAMP_FIELD_DIALOG");
				return null;
			}
		});

		FormField<ObservationLocation> geometryField = new GeometryFormField(0, FieldType.GEOMETRY, "geometry", "Location", true, false);
		model.setLocation(geometryField);
		EditGeometry editGeometry = findViewById(R.id.geometry);
		editGeometry.bind(geometryField);
		findViewById(R.id.geometry_edit).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				GeometryFieldDialog dialog = GeometryFieldDialog.Companion.newInstance();
				dialog.show(getSupportFragmentManager(), "GEOMETRY_FIELD_DIALOG");
			}
		});

		Map<Long, JsonObject> formMap = EventHelper.getInstance(getApplicationContext()).getCurrentEvent().getFormMap();

		Form form;
		if (isNewObservation) {
			observation = new Observation();

			if (model.getForm().getValue() == null) {
				FormPreferences formPreferences = new FormPreferences(getApplicationContext(), event, formId);

				form = Form.Companion.fromJson(formMap.get(formId));
				if (form != null) {
					model.setForm(form, formPreferences.getDefaults());
				}
			}
		} else {
			try {
				observation = ObservationHelper.getInstance(getApplicationContext()).read(getIntent().getLongExtra(OBSERVATION_ID, 0L));
                if (!observation.getForms().isEmpty() && model.getForm().getValue() == null) {
					ObservationForm observationForm = observation.getForms().iterator().next();
					JsonObject formJson = formMap.get(observationForm.getFormId());
					form = Form.Companion.fromJson(formJson);

					Map<String, Object> values = new HashMap<>();
					for (Map.Entry<String, ObservationProperty> entry : observationForm.getPropertiesMap().entrySet()) {
						values.put(entry.getKey(), entry.getValue().getValue());
					}

					if (form != null) {
						model.setForm(form, values);
					}
				}
			} catch (ObservationException oe) {
				Log.e(LOG_NAME, "Problem reading observation.", oe);
				return;
			}
		}

		// Get the form for this observation
		if (model.getForm().getValue() != null) {
			LinearLayout formLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.observation_editor_form, (ViewGroup) findViewById(R.id.forms), true);
			TextView name = formLayout.findViewById(R.id.form_name);
			name.setText(model.getForm().getValue().getName());

			formFragment = new FormFragment();
			getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.form_content, formFragment, "EDIT_FORM_FRAGMENT")
					.commit();
		}

		attachmentLayout = findViewById(R.id.image_gallery);
		attachmentGallery = new AttachmentGallery(getApplicationContext(), 200, 200);
		attachmentGallery.addOnAttachmentClickListener(new AttachmentGallery.OnAttachmentClickListener() {
			@Override
			public void onAttachmentClick(Attachment attachment) {
				Intent intent = new Intent(getApplicationContext(), AttachmentViewerActivity.class);

				if (attachment.getId() != null) {
					intent.putExtra(AttachmentViewerActivity.ATTACHMENT_ID, attachment.getId());
				} else {
					intent.putExtra(AttachmentViewerActivity.ATTACHMENT_PATH, attachment.getLocalPath());
				}

				intent.putExtra(AttachmentViewerActivity.EDITABLE, false);
				startActivity(intent);
			}
		});

		mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);

		hideKeyboardOnClick(findViewById(android.R.id.content));

		if (isNewObservation) {
			getSupportActionBar().setTitle("New Observation");
			ObservationLocation location = getIntent().getParcelableExtra(LOCATION);
			model.getTimestamp().getValue().setValue(new Date());
			model.getLocation().getValue().setValue(location);

			observation.setEvent(EventHelper.getInstance(getApplicationContext()).getCurrentEvent());
			observation.setGeometry(location.getGeometry());

			try {
				User u = UserHelper.getInstance(getApplicationContext()).readCurrentUser();
				if (u != null) {
					observation.setUserId(u.getRemoteId());
				}
			} catch (UserException ue) {
				ue.printStackTrace();
			}
		} else {
			getSupportActionBar().setTitle("Edit Observation");
			// this is an edit of an existing observation
			attachmentGallery.addAttachments(attachmentLayout, observation.getAttachments());

			ObservationLocation location = new ObservationLocation(observation);
			model.getLocation().getValue().setValue(location);
			model.getTimestamp().getValue().setValue(observation.getTimestamp());
		}

		findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				saveObservation();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();

		if (!isNewObservation && canDeleteObservation()) {
			inflater.inflate(R.menu.observation_delete_menu, menu);
		}

		return true;
	}

	/**
	 * Hides keyboard when clicking elsewhere
	 */
	private void hideKeyboardOnClick(View view) {
		// Set up touch listener for non-text box views to hide keyboard.
		if (!(view instanceof EditText) && !(view instanceof Button)) {
			view.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
					if (getCurrentFocus() != null) {
						inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
					}
					return false;
				}
			});
		}

		// If a layout container, iterate over children and seed recursion.
		if (view instanceof ViewGroup) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				View innerView = ((ViewGroup) view).getChildAt(i);
				hideKeyboardOnClick(innerView);
			}
		}
	}

	@Override
	public void onMapReady(GoogleMap map) {
		this.map = map;
		map.getUiSettings().setZoomControlsEnabled(false);

		mapObservationManager = new MapObservationManager(this, map);
		map.setOnCameraIdleListener(this);
	}

	@Override
	public void onCameraIdle() {
		map.setOnCameraIdleListener(null);
		setupMap();
	}

	private void setupMap() {
		if (map == null) return;

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		map.setMapType(preferences.getInt(getString(R.string.baseLayerKey), getResources().getInteger(R.integer.baseLayerDefaultValue)));

		int dayNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
		if (dayNightMode == Configuration.UI_MODE_NIGHT_NO) {
			map.setMapStyle(null);
		} else {
			map.setMapStyle(MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.map_theme_night));
		}

		ObservationLocation location = model.getLocation().getValue().getValue();

		LatLng initialLatLng = getIntent().getParcelableExtra(INITIAL_LOCATION);
		if (initialLatLng == null) {
			initialLatLng = new LatLng(0,0);
		}

		float zoom = getIntent().getFloatExtra(INITIAL_ZOOM, 0);
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLatLng, zoom));
		map.getUiSettings().setMapToolbarEnabled(false);
		map.animateCamera(location.getCameraUpdate(mapFragment.getView(), true, 1.0f/6));

		if (accuracyCircle != null) {
			accuracyCircle.remove();
		}

		CircleOptions circle = location.getAccuracyCircle(getResources());
		if (circle != null) {
			accuracyCircle = map.addCircle(circle);
		}

		if (mapObservation != null) {
			mapObservation.remove();
		}
		mapObservation = mapObservationManager.addToMap(observation);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Always call the superclass so it can restore the view hierarchy
		super.onRestoreInstanceState(savedInstanceState);

		attachmentsToCreate = savedInstanceState.getParcelableArrayList("attachmentsToCreate");
		for (Attachment a : attachmentsToCreate) {
			attachmentGallery.addAttachment(attachmentLayout, a);
		}

		currentMediaPath = savedInstanceState.getString(CURRENT_MEDIA_PATH);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putParcelableArrayList("attachmentsToCreate", attachmentsToCreate);
		outState.putString(CURRENT_MEDIA_PATH, currentMediaPath);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			new AlertDialog.Builder(this)
					.setTitle("Discard Changes")
					.setMessage(R.string.cancel_edit)
					.setPositiveButton(R.string.discard_changes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					}).setNegativeButton(R.string.no, null)
					.show();
		}

		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

			case android.R.id.home:
				new AlertDialog.Builder(this)
						.setTitle("Discard Changes")
						.setMessage(R.string.cancel_edit)
						.setPositiveButton(R.string.discard_changes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								finish();
							}
						}).setNegativeButton(R.string.no, null)
						.show();

				break;
			case R.id.observation_archive:
				onArchiveObservation();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void onArchiveObservation() {
		new AlertDialog.Builder(this)
				.setTitle("Delete Observation")
				.setMessage("Are you sure you want to remove this observation?")
				.setPositiveButton("Delete", new Dialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						archiveObservation();
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void archiveObservation() {
		try {
			ObservationHelper.getInstance(this).archive(observation);
			Intent intent = new Intent(this, LandingActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		} catch (ObservationException e) {
			Log.e(LOG_NAME, "Error archiving observation");
		}
	}

	private boolean canDeleteObservation() {
		return hasObservationDeletePermission || isCurrentUsersObservation() || hasUpdatePermissionsInEventAcl();
	}

	private boolean isCurrentUsersObservation() {
		boolean isCurrentUsersObservation = false;

		if (currentUser != null) {
			isCurrentUsersObservation = currentUser.getRemoteId().equals(observation.getUserId());
		}

		return isCurrentUsersObservation;
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

	private void saveObservation() {
		if (!validateForm()) {
			return;
		}

		observation.setState(State.ACTIVE);
		observation.setDirty(true);
		observation.setTimestamp(model.getTimestamp().getValue().getValue());

		ObservationLocation location = model.getLocation().getValue().getValue();
		observation.setGeometry(location.getGeometry());
		observation.setAccuracy(location.getAccuracy());

		String provider = location.getProvider();
		if (provider == null || provider.trim().isEmpty()) {
			provider = "manual";
		}
		observation.setProvider(provider);

		if (!"manual".equalsIgnoreCase(provider)) {
			observation.setLocationDelta(Long.toString(locationElapsedTimeMilliseconds));
		}

		Collection<ObservationForm> observationForms = new ArrayList<>();
		Form form = model.getForm().getValue();
		if (form != null) {
			Collection<ObservationProperty> properties = new ArrayList<>();
			for (FormField<Object> field: form.getFields()) {
				Serializable value = field.serialize();
				if (value != null) {
					properties.add(new ObservationProperty(field.getName(), value));
				}
			}

			ObservationForm observationForm = new ObservationForm();
			observationForm.setFormId(form.getId());
			observationForm.addProperties(properties);
			observationForms.add(observationForm);
		}
		observation.addForms(observationForms);

		observation.getAttachments().addAll(attachmentsToCreate);

		ObservationHelper oh = ObservationHelper.getInstance(getApplicationContext());
		try {
			if (observation.getId() == null) {
				Observation newObs = oh.create(observation);
				Log.i(LOG_NAME, "Created new observation with id: " + newObs.getId());
			} else {
				oh.update(observation);
				Log.i(LOG_NAME, "Updated observation with remote id: " + observation.getRemoteId());
			}

			finish();
		} catch (Exception e) {
			Log.e(LOG_NAME, e.getMessage(), e);
		}
	}

	private boolean validateForm() {
		if (formFragment == null) return true;

		List<Field<?>> invalid = new ArrayList<>();

		for (Field<?> editField : formFragment.getEditFields()) {
			if (!editField.validate(true)) {
				invalid.add(editField);
			}
		}


		if (!invalid.isEmpty()) {
			// scroll to first invalid control
			View firstInvalid = invalid.get(0);
			findViewById(R.id.properties).scrollTo(0, firstInvalid.getBottom());
			firstInvalid.clearFocus();
			firstInvalid.requestFocus();
			firstInvalid.requestFocusFromTouch();

			return false;
		}

		return true;
	}

	public void onCameraClick(View v) {
		if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
				ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(ObservationEditActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CAMERA);
		} else {
			launchCameraIntent();
		}
	}

	public void onVideoClick(View v) {
		if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
				ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(ObservationEditActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_VIDEO);
		} else {
			launchVideoIntent();
		}
	}

	public void onAudioClick(View v) {
		if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
				ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(ObservationEditActivity.this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_AUDIO);
		} else {
			launchAudioIntent();
		}
	}

	public void onGalleryClick(View v) {
		if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(ObservationEditActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_STORAGE);
		} else {
			launchGalleryIntent();
		}
	}

	private void launchCameraIntent() {
		try {
			File file = MediaUtility.createImageFile();
			currentMediaPath = file.getAbsolutePath();
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, getUriForFile(file));
			intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
		} catch (IOException e) {
			Log.e(LOG_NAME, "Error creating video media file", e);
		}
	}

	private void launchVideoIntent() {
		try {
			File file = MediaUtility.createVideoFile();
			currentMediaPath = file.getAbsolutePath();
			Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, getUriForFile(file));
			intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
		} catch (IOException e) {
			Log.e(LOG_NAME, "Error creating video media file", e);
		}
	}

	private void launchGalleryIntent() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*, video/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		}
		startActivityForResult(intent, GALLERY_ACTIVITY_REQUEST_CODE);
	}

	private void launchAudioIntent() {
		Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
		List<ResolveInfo> resolveInfo = getApplicationContext().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		if (resolveInfo.size() > 0) {
			startActivityForResult(intent, CAPTURE_VOICE_ACTIVITY_REQUEST_CODE);
		} else {
			Toast.makeText(getApplicationContext(), "Device has no voice recorder application.", Toast.LENGTH_SHORT).show();
		}
	}

	private Uri getUriForFile(File file) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			return FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", file);
		} else {
			return Uri.fromFile(file);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		switch (requestCode) {
			case PERMISSIONS_REQUEST_CAMERA:
			case PERMISSIONS_REQUEST_VIDEO: {
				Map<String, Integer> grants = new HashMap<>();
				grants.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
				grants.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);

				for (int i = 0; i < grantResults.length; i++) {
					grants.put(permissions[i], grantResults[i]);
				}

				if (grants.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
						grants.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
					if (requestCode == PERMISSIONS_REQUEST_CAMERA) {
						launchCameraIntent();
					} else {
						launchVideoIntent();
					}
				} else if ((!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) && grants.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) ||
						(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) && grants.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) ||
						!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

					// User denied camera or storage with never ask again.  Since they will get here
					// by clicking the camera button give them a dialog that will
					// guide them to settings if they want to enable the permission
					showDisabledPermissionsDialog(
							getResources().getString(R.string.camera_access_title),
							getResources().getString(R.string.camera_access_message));
				}

				break;
			}
			case PERMISSIONS_REQUEST_AUDIO: {
				Map<String, Integer> grants = new HashMap<String, Integer>();
				grants.put(Manifest.permission.RECORD_AUDIO, PackageManager.PERMISSION_GRANTED);
				grants.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);

				if (grants.get(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
						grants.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
					launchAudioIntent();
				} else if ((!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO) && grants.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) ||
						(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) && grants.get(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) ||
						!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO) && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

					// User denied camera or storage with never ask again.  Since they will get here
					// by clicking the camera button give them a dialog that will
					// guide them to settings if they want to enable the permission
					showDisabledPermissionsDialog(
							getResources().getString(R.string.camera_access_title),
							getResources().getString(R.string.camera_access_message));
				}

				break;
			}
			case PERMISSIONS_REQUEST_STORAGE: {
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					launchGalleryIntent();
				} else {
					if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
						// User denied storage with never ask again.  Since they will get here
						// by clicking the gallery button give them a dialog that will
						// guide them to settings if they want to enable the permission
						showDisabledPermissionsDialog(
								getResources().getString(R.string.gallery_access_title),
								getResources().getString(R.string.gallery_access_message));
					}
				}

				break;
			}
		}
	}

	private void showDisabledPermissionsDialog(String title, String message) {
		new AlertDialog.Builder(this)
				.setTitle(title)
				.setMessage(message)
				.setPositiveButton(R.string.settings, new Dialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
						intent.setData(Uri.fromParts("package", getApplicationContext().getPackageName(), null));
						startActivity(intent);
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode != RESULT_OK) {
			return;
		}
		switch (requestCode) {
			case CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE:
			case CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE:
				Attachment capture = new Attachment();
				capture.setLocalPath(currentMediaPath);
				attachmentsToCreate.add(capture);
				attachmentGallery.addAttachment(attachmentLayout, capture);
				MediaUtility.addImageToGallery(getApplicationContext(), Uri.fromFile(new File(currentMediaPath)));

				break;
			case GALLERY_ACTIVITY_REQUEST_CODE:
			case CAPTURE_VOICE_ACTIVITY_REQUEST_CODE:
				Collection<Uri> uris = getUris(data);

				for (Uri uri : uris) {
					try {
						File file = MediaUtility.copyMediaFromUri(getApplicationContext(), uri);
						Attachment a = new Attachment();
						a.setLocalPath(file.getAbsolutePath());
						attachmentsToCreate.add(a);
						attachmentGallery.addAttachment(attachmentLayout, a);
					} catch (IOException e) {
						Log.e(LOG_NAME, "Error copying gallery file to local storage", e);
					}
				}
				break;
		}
	}

	private Collection<Uri> getUris(Intent intent) {
		Set<Uri> uris = new HashSet<>();
		if (intent.getData() != null) {
			uris.add(intent.getData());
		}
		uris.addAll(getClipDataUris(intent));
		return uris;
	}

	@TargetApi(16)
	private Collection<Uri> getClipDataUris(Intent intent) {
		Collection<Uri> uris = new ArrayList<>();
		ClipData cd = intent.getClipData();
		if (cd != null) {
			for (int i = 0; i < cd.getItemCount(); i++) {
				uris.add(cd.getItemAt(i).getUri());
			}
		}
		return uris;
	}
}
