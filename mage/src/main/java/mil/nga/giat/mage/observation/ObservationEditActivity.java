package mil.nga.giat.mage.observation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
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

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.event.EventBannerFragment;
import mil.nga.giat.mage.form.LayoutBaker;
import mil.nga.giat.mage.form.LayoutBaker.ControlGenerationType;
import mil.nga.giat.mage.form.MageSelectView;
import mil.nga.giat.mage.form.MageSpinner;
import mil.nga.giat.mage.form.MageTextView;
import mil.nga.giat.mage.map.marker.ObservationBitmapFactory;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.datastore.observation.State;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.utils.DateFormatFactory;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

public class ObservationEditActivity extends Activity implements OnMapReadyCallback {

	private static final String LOG_NAME = ObservationEditActivity.class.getName();

	private static final int PERMISSIONS_REQUEST_CAMERA = 100;
	private static final int PERMISSIONS_REQUEST_VIDEO = 200;
	private static final int PERMISSIONS_REQUEST_AUDIO = 300;
	private static final int PERMISSIONS_REQUEST_STORAGE = 400;

	private final DateFormat iso8601Format = DateFormatFactory.ISO8601();

	public static final String OBSERVATION_ID = "OBSERVATION_ID";
	public static final String LOCATION = "LOCATION";
	public static final String INITIAL_LOCATION = "INITIAL_LOCATION";
	public static final String INITIAL_ZOOM = "INITIAL_ZOOM";
	private static final String CURRENT_MEDIA_PATH = "CURRENT_MEDIA_PATH";

	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
	private static final int CAPTURE_VOICE_ACTIVITY_REQUEST_CODE = 300;
	private static final int GALLERY_ACTIVITY_REQUEST_CODE = 400;
	private static final int LOCATION_EDIT_ACTIVITY_REQUEST_CODE = 600;
	private static final int SELECT_ACTIVITY_REQUEST_CODE = 700;

	private static final long NEW_OBSERVATION = -1L;

	private static Integer FIELD_ID_SELECT = 7;

	private Map<String, View> fieldIdMap = new HashMap<>(); //FieldId + " " + UnqiueId / View

	private final DecimalFormat latLngFormat = new DecimalFormat("###.#####");
	private ArrayList<Attachment> attachmentsToCreate = new ArrayList<>();

    private Location l;
	private Observation observation;
	private GoogleMap map;
	private Marker observationMarker;
	private Circle accuracyCircle;
	private long locationElapsedTimeMilliseconds = 0;

    private LinearLayout attachmentLayout;
    private AttachmentGallery attachmentGallery;

	private String currentMediaPath;

	// control key to default position
	private static Map<String, Integer> spinnersLastPositions = new HashMap<>();

	private List<View> controls = new ArrayList<>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.observation_editor);

		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.beginTransaction().add(R.id.observation_edit_event_holder, new EventBannerFragment()).commit();

        attachmentLayout = (LinearLayout) findViewById(R.id.image_gallery);
        attachmentGallery = new AttachmentGallery(getApplicationContext(), 100, 100);
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

		final long observationId = getIntent().getLongExtra(OBSERVATION_ID, NEW_OBSERVATION);
		JsonObject dynamicFormJson;
		if (observationId == NEW_OBSERVATION) {
			observation = new Observation();
			dynamicFormJson = EventHelper.getInstance(getApplicationContext()).getCurrentEvent().getForm();
		} else {
			try {
				observation = ObservationHelper.getInstance(getApplicationContext()).read(getIntent().getLongExtra(OBSERVATION_ID, 0L));
				dynamicFormJson = observation.getEvent().getForm();
			} catch (ObservationException oe) {
				Log.e(LOG_NAME, "Problem reading observation.", oe);
				return;
			}
		}

		controls = LayoutBaker.createControlsFromJson(this, ControlGenerationType.EDIT, dynamicFormJson);
		for (View view : controls) {
			if (view instanceof MageSpinner) {
				MageSpinner mageSpinner = (MageSpinner) view;
				String key = mageSpinner.getPropertyKey();
				Integer spinnerPosition = spinnersLastPositions.get(key);
				if (spinnerPosition == null) {
					spinnerPosition = mageSpinner.getSelectedItemPosition();
				}
				spinnerPosition = Math.min(Math.max(0, spinnerPosition), mageSpinner.getAdapter().getCount());
				spinnersLastPositions.put(key, spinnerPosition);

				mageSpinner.setSelection(spinnerPosition);
				mageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
						MageSpinner ms = ((MageSpinner) parent);
						String k = ms.getPropertyKey();
						if (observationId == NEW_OBSERVATION) {
							spinnersLastPositions.put(k, position);
						}

						JsonObject dynamicFormJson = observation.getEvent().getForm();

						// get variantField
						JsonElement variantField = dynamicFormJson.get("variantField");
						String variantFieldString = null;
						if(variantField != null && !variantField.isJsonNull()) {
							variantFieldString = variantField.getAsString();
						}

						if (k.equals("type") || (variantFieldString != null && k.equals(variantFieldString))) {
							observation.addProperties(Collections.singleton(new ObservationProperty(k, parent.getItemAtPosition(position).toString())));
							updateMapIcon();
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
					}
				});
			} else if (view instanceof MageSelectView) {
				final MageSelectView selectView = (MageSelectView) view;
				fieldIdMap.put(getSelectId(selectView.getId()), selectView);

				selectView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						selectClick(selectView);
					}
				});
			} else if( view instanceof  LinearLayout) {
				LinearLayout currentView = (LinearLayout) view;
				for (int index = 0; index < currentView.getChildCount(); index++) {
					View childView = currentView.getChildAt(index);
					if (childView instanceof MageSelectView) {
						final MageSelectView childSelectView = (MageSelectView) childView;
						fieldIdMap.put(getSelectId(childSelectView.getId()), childSelectView);

						childSelectView.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								selectClick(childSelectView);
							}
						});
					}
				}
			}
		}

		// add dynamic controls to view
		LayoutBaker.populateLayoutWithControls((LinearLayout) findViewById(R.id.location_dynamic_form), controls);

		((MapFragment) getFragmentManager().findFragmentById(R.id.background_map)).getMapAsync(this);

		hideKeyboardOnClick(findViewById(R.id.observation_edit));

		if (observationId == NEW_OBSERVATION) {
			this.setTitle("Create New Observation");
			l = getIntent().getParcelableExtra(LOCATION);

            observation.setEvent(EventHelper.getInstance(getApplicationContext()).getCurrentEvent());
			observation.setTimestamp(new Date());
			Serializable timestamp = iso8601Format.format(observation.getTimestamp());
			ObservationProperty timestampProperty = new ObservationProperty("timestamp", timestamp);

			Map<String, ObservationProperty> propertyMap = LayoutBaker.populateMapFromLayout((LinearLayout) findViewById(R.id.form));
			propertyMap.put("timestamp", timestampProperty);

			observation.addProperties(propertyMap.values());
			try {
				User u = UserHelper.getInstance(getApplicationContext()).readCurrentUser();
				if (u != null) {
					observation.setUserId(u.getRemoteId());
				}
			} catch (UserException ue) {
				ue.printStackTrace();
			}
			LayoutBaker.populateLayoutFromMap((LinearLayout) findViewById(R.id.form), ControlGenerationType.EDIT, observation.getPropertiesMap());
		} else {
			this.setTitle("Edit Observation");
			// this is an edit of an existing observation
            attachmentGallery.addAttachments(attachmentLayout, observation.getAttachments());

			Map<String, ObservationProperty> propertiesMap = observation.getPropertiesMap();
			Geometry geo = observation.getGeometry();
			if (geo instanceof Point) {
				Point point = (Point) geo;
				String provider = "manual";
				if (propertiesMap.get("provider") != null) {
					provider = propertiesMap.get("provider").getValue().toString();
				}
				l = new Location(provider);
				if (propertiesMap.containsKey("accuracy")) {
					l.setAccuracy(Float.parseFloat(propertiesMap.get("accuracy").getValue().toString()));
				}
				l.setLatitude(point.getY());
				l.setLongitude(point.getX());
			}
			LayoutBaker.populateLayoutFromMap((LinearLayout) findViewById(R.id.form), ControlGenerationType.EDIT, propertiesMap);
		}

		findViewById(R.id.date_edit).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(ObservationEditActivity.this);
				// Get the layout inflater
				LayoutInflater inflater = getLayoutInflater();
				View dialogView = inflater.inflate(R.layout.date_time_dialog, null);
				final DatePicker datePicker = (DatePicker) dialogView.findViewById(R.id.date_picker);
				final TimePicker timePicker = (TimePicker) dialogView.findViewById(R.id.time_picker);

                Serializable value = ((MageTextView) findViewById(R.id.date)).getPropertyValue();
                try {
                    Date date = iso8601Format.parse(value.toString());
                    Calendar c = Calendar.getInstance();
                    c.setTime(date);
                    datePicker.updateDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                    timePicker.setCurrentHour(c.get(Calendar.HOUR_OF_DAY));
                    timePicker.setCurrentMinute(c.get(Calendar.MINUTE));

                } catch (ParseException e) {
                    e.printStackTrace();
                }
                // Inflate and set the layout for the dialog
				// Pass null as the parent view because its going in the dialog layout
				builder.setView(dialogView).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						Calendar c = Calendar.getInstance();
						c.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(), timePicker.getCurrentHour(), timePicker.getCurrentMinute(), 0);
						c.set(Calendar.MILLISECOND, 0);
						((MageTextView) findViewById(R.id.date)).setPropertyValue(c.getTime());
					}
				}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
				AlertDialog ad = builder.create();
				ad.show();
			}
		});

		findViewById(R.id.location_edit).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(ObservationEditActivity.this, LocationEditActivity.class);
				intent.putExtra(LocationEditActivity.LOCATION, l);
				intent.putExtra(LocationEditActivity.MARKER_BITMAP, ObservationBitmapFactory.bitmap(ObservationEditActivity.this, observation));
				startActivityForResult(intent, LOCATION_EDIT_ACTIVITY_REQUEST_CODE);
			}
		});

	}

	/**
	 * Hides keyboard when clicking elsewhere
	 *
	 * @param view view
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

		setupMap();
	}

	private void setupMap() {
		if (map == null) return;

		LatLng location = new LatLng(l.getLatitude(), l.getLongitude());

		((TextView) findViewById(R.id.location)).setText(latLngFormat.format(l.getLatitude()) + ", " + latLngFormat.format(l.getLongitude()));
		if (l.getProvider() != null) {
			((TextView)findViewById(R.id.location_provider)).setText("("+l.getProvider()+")");
		} else {
			findViewById(R.id.location_provider).setVisibility(View.GONE);
		}
		if (l.getAccuracy() != 0) {
			((TextView)findViewById(R.id.location_accuracy)).setText("\u00B1" + l.getAccuracy() + "m");
		} else {
			findViewById(R.id.location_accuracy).setVisibility(View.GONE);
		}

		locationElapsedTimeMilliseconds = getElapsedTimeInMilliseconds();
		if (locationElapsedTimeMilliseconds != 0 && !("manual".equalsIgnoreCase(l.getProvider()))) {
			//String dateText = DateUtils.getRelativeTimeSpanString(System.currentTimeMillis() - locationElapsedTimeMilliseconds, System.currentTimeMillis(), 0).toString();
			String dateText = elapsedTime(locationElapsedTimeMilliseconds);
			((TextView)findViewById(R.id.location_elapsed_time)).setText(dateText);
		} else {
			findViewById(R.id.location_elapsed_time).setVisibility(View.GONE);
		}

		LatLng latLng = getIntent().getParcelableExtra(INITIAL_LOCATION);
		if (latLng == null) {
			latLng = new LatLng(0,0);
		}

		float zoom = getIntent().getFloatExtra(INITIAL_ZOOM, 0);

		map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

		map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 18));

		if (accuracyCircle != null) {
			accuracyCircle.remove();
		}

		CircleOptions circleOptions = new CircleOptions()
				.fillColor(getResources().getColor(R.color.accuracy_circle_fill))
				.strokeColor(getResources().getColor(R.color.accuracy_circle_stroke))
				.strokeWidth(5)
				.center(location)
				.radius(l.getAccuracy());
		accuracyCircle = map.addCircle(circleOptions);

		if (observationMarker != null) {
			observationMarker.setPosition(location);
			// make sure to set the Anchor after this call as well, because the size of the icon might have changed
			observationMarker.setIcon(ObservationBitmapFactory.bitmapDescriptor(this, observation));
			observationMarker.setAnchor(0.5f, 1.0f);
		} else {
			observationMarker = map.addMarker(new MarkerOptions().position(location).icon(ObservationBitmapFactory.bitmapDescriptor(this, observation)));
		}
	}

	@SuppressLint("NewApi")
	private long getElapsedTimeInMilliseconds() {
		long elapsedTimeInMilliseconds = 0;
		if (observation.getPropertiesMap().containsKey("delta")) {
			elapsedTimeInMilliseconds = Long.parseLong(observation.getPropertiesMap().get("delta").getValue().toString());
		}
		if (Build.VERSION.SDK_INT >= 17 && l.getElapsedRealtimeNanos() != 0) {
			elapsedTimeInMilliseconds = ((SystemClock.elapsedRealtimeNanos() - l.getElapsedRealtimeNanos()) / (1000000l));
		} else {
			elapsedTimeInMilliseconds = System.currentTimeMillis() - l.getTime();
		}
		return Math.max(0l, elapsedTimeInMilliseconds);
	}

	private String elapsedTime(long ms) {
		String s = "";

		long sec = ms / 1000;
		long min = sec / 60;

		if (observation.getRemoteId() == null) {
			if (ms < 1000) {
				return "now";
			}
			if (min == 0) {
				s = sec + ((sec == 1) ? " sec ago" : " secs ago");
			} else if (min < 60) {
				s = min + ((min == 1) ? " min ago" : " mins ago");
			} else {
				long hour = Math.round(Math.floor(min / 60));
				s = hour + ((hour == 1) ? " hour ago" : " hours ago");
			}
		} else {
			return "";
		}
		return s;
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Always call the superclass so it can restore the view hierarchy
		super.onRestoreInstanceState(savedInstanceState);

		l = savedInstanceState.getParcelable("location");

        attachmentsToCreate = savedInstanceState.getParcelableArrayList("attachmentsToCreate");
        for (Attachment a : attachmentsToCreate) {
            attachmentGallery.addAttachment(attachmentLayout, a);
        }

		LinearLayout form = (LinearLayout) findViewById(R.id.form);
		LayoutBaker.populateLayoutFromBundle(form, ControlGenerationType.EDIT, savedInstanceState);
		currentMediaPath = savedInstanceState.getParcelable(CURRENT_MEDIA_PATH);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		LayoutBaker.populateBundleFromLayout((LinearLayout) findViewById(R.id.form), outState);
		outState.putParcelable("location", l);
		outState.putParcelableArrayList("attachmentsToCreate", attachmentsToCreate);
		outState.putString(CURRENT_MEDIA_PATH, currentMediaPath);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.observation_edit_menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.observation_save:
			List<View> invalid = LayoutBaker.validateControls(controls);
			if (!invalid.isEmpty()) {
				// scroll to first invalid control
				View firstInvalid = invalid.get(0);
				findViewById(R.id.properties).scrollTo(0, firstInvalid.getBottom());
				firstInvalid.clearFocus();
				firstInvalid.requestFocus();
				break;
			}

			observation.setState(State.ACTIVE);
			observation.setDirty(true);
			observation.setGeometry(new GeometryFactory().createPoint(new Coordinate(l.getLongitude(), l.getLatitude())));

			if (!LayoutBaker.checkAndFlagRequiredFields((LinearLayout) findViewById(R.id.form))) {
				return super.onOptionsItemSelected(item);
			}

			Map<String, ObservationProperty> propertyMap = LayoutBaker.populateMapFromLayout((LinearLayout) findViewById(R.id.form));

			try {
				observation.setTimestamp(iso8601Format.parse(propertyMap.get("timestamp").getValue().toString()));
			} catch (ParseException pe) {
				Log.e(LOG_NAME, "Could not parse timestamp", pe);
			}
			// Add properties that weren't part of the layout
			propertyMap.put("accuracy", new ObservationProperty("accuracy", l.getAccuracy()));
			String provider = l.getProvider();
			if (provider == null || provider.trim().isEmpty()) {
				provider = "manual";
			}

			propertyMap.put("provider", new ObservationProperty("provider", provider));
			if (!"manual".equalsIgnoreCase(provider)) {
				propertyMap.put("delta", new ObservationProperty("delta", Long.toString(locationElapsedTimeMilliseconds)));
			}

			observation.addProperties(propertyMap.values());
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

			break;
		case R.id.observation_cancel:
			new AlertDialog.Builder(this)
				.setTitle("Discard Changes")
				.setMessage(R.string.cancel_edit)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				}).setNegativeButton(R.string.no, null)
				.show();
			break;
		}

		return super.onOptionsItemSelected(item);
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
			ActivityCompat.requestPermissions(ObservationEditActivity.this, new String[]{Manifest.permission.RECORD_AUDIO,  Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_AUDIO);
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
			Uri uri = Uri.fromFile(file);
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
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
			Uri uri = Uri.fromFile(file);
			Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
			intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
		} catch (IOException e) {
			Log.e(LOG_NAME, "Error creating video media file", e);
		}
	}

	private void launchGalleryIntent() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*, video/*");
		Log.i(LOG_NAME, "build version sdk int: " + Build.VERSION.SDK_INT);
		if (Build.VERSION.SDK_INT >=  Build.VERSION_CODES.JELLY_BEAN_MR2) {
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
			Toast.makeText(getApplicationContext(), "Device has no voice recorder.", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
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
		new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
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
						File file = MediaUtility.copyImageFromGallery(getContentResolver().openInputStream(uri));
						Attachment a = new Attachment();
						a.setLocalPath(file.getAbsolutePath());
						attachmentsToCreate.add(a);
						attachmentGallery.addAttachment(attachmentLayout, a);
					} catch (IOException e) {
						Log.e(LOG_NAME, "Error copying gallery file to local storage", e);
					}
				}
				break;
			case LOCATION_EDIT_ACTIVITY_REQUEST_CODE:
				l = data.getParcelableExtra(LocationEditActivity.LOCATION);
				setupMap();
				break;
			case SELECT_ACTIVITY_REQUEST_CODE:
				ArrayList<String> selectedChoices = data.getStringArrayListExtra(SelectEditActivity.SELECT_SELECTED);
				Integer fieldId = data.getIntExtra(SelectEditActivity.FIELD_ID, 0);
				MageSelectView mageSelectView = (MageSelectView) fieldIdMap.get(getSelectId(fieldId));
				Serializable selectedChoicesSerialized = null;
				if (selectedChoices != null) {
					if (mageSelectView.isMultiSelect()) {
						selectedChoicesSerialized = selectedChoices;
					} else {
						if (!selectedChoices.isEmpty()) {
							selectedChoicesSerialized = selectedChoices.get(0);
						}
					}
				}
				mageSelectView.setPropertyValue(selectedChoicesSerialized);


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

	private void updateMapIcon() {
		if (map == null) return;

		if (observationMarker != null) {
			observationMarker.remove();
		}
		observationMarker = map.addMarker(new MarkerOptions().position(new LatLng(l.getLatitude(), l.getLongitude())).icon(ObservationBitmapFactory.bitmapDescriptor(this, observation)));
	}

	private String getSelectId(Integer fieldId) {
		return FIELD_ID_SELECT + " " + fieldId;
	}

	public void selectClick(MageSelectView mageSelectView) {
		JsonObject field = mageSelectView.getJsonObject();
		Boolean isMultiSelect = mageSelectView.isMultiSelect();
		Integer fieldId = mageSelectView.getId();

		Intent intent = new Intent(ObservationEditActivity.this, SelectEditActivity.class);
		JsonArray jsonArray = field.getAsJsonArray(SelectEditActivity.MULTISELECT_JSON_CHOICE_KEY);
		intent.putExtra(SelectEditActivity.SELECT_CHOICES, jsonArray.toString());

		Serializable serializableValue = mageSelectView.getPropertyValue();
		ArrayList<String> selectedValues = null;
		if (serializableValue != null) {
			if (isMultiSelect) {
				selectedValues = (ArrayList<String>) serializableValue;
			} else {
				String selectedValue = (String) serializableValue;
				selectedValues = new ArrayList<String>();
				selectedValues.add(selectedValue);
			}
		}
		intent.putStringArrayListExtra(SelectEditActivity.SELECT_SELECTED, selectedValues);
		intent.putExtra(SelectEditActivity.IS_MULTISELECT, isMultiSelect);
		intent.putExtra(SelectEditActivity.FIELD_ID, fieldId);
		startActivityForResult(intent, SELECT_ACTIVITY_REQUEST_CODE);
	}

}
