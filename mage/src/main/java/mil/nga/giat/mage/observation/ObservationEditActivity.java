package mil.nga.giat.mage.observation;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.event.EventBannerFragment;
import mil.nga.giat.mage.form.LayoutBaker;
import mil.nga.giat.mage.form.LayoutBaker.ControlGenerationType;
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

public class ObservationEditActivity extends Activity {

	private static final String LOG_NAME = ObservationEditActivity.class.getName();

    private final DateFormat iso8601Format = DateFormatFactory.ISO8601();

	public static final String OBSERVATION_ID = "OBSERVATION_ID";
	public static final String LOCATION = "LOCATION";
	public static final String INITIAL_LOCATION = "INITIAL_LOCATION";
	public static final String INITIAL_ZOOM = "INITIAL_ZOOM";
	private static final String CURRENT_MEDIA_URI = "CURRENT_MEDIA_URI";

	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
	private static final int CAPTURE_VOICE_ACTIVITY_REQUEST_CODE = 300;
	private static final int GALLERY_ACTIVITY_REQUEST_CODE = 400;
	private static final int ATTACHMENT_VIEW_ACTIVITY_REQUEST_CODE = 500;
	private static final int LOCATION_EDIT_ACTIVITY_REQUEST_CODE = 600;

	private static final long NEW_OBSERVATION = -1L;

	private final DecimalFormat latLngFormat = new DecimalFormat("###.#####");
	private ArrayList<Attachment> attachments = new ArrayList<Attachment>();
	private Location l;
	private Observation o;
	private GoogleMap map;
	private Marker observationMarker;
	private Circle accuracyCircle;
	private long locationElapsedTimeMilliseconds = 0;

	private Uri currentMediaUri;

	// control key to default position
	private static Map<String, Integer> spinnersLastPositions = new HashMap<String, Integer>();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.observation_editor);

		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.beginTransaction().add(R.id.observation_edit_event_holder, new EventBannerFragment()).commit();
		
		final long observationId = getIntent().getLongExtra(OBSERVATION_ID, NEW_OBSERVATION);

		JsonObject dynamicFormJson = null;
		if (observationId == NEW_OBSERVATION) {
			o = new Observation();
			dynamicFormJson = EventHelper.getInstance(getApplicationContext()).getCurrentEvent().getForm();
		} else {
			try {
				o = ObservationHelper.getInstance(getApplicationContext()).read(getIntent().getLongExtra(OBSERVATION_ID, 0L));
			} catch (ObservationException oe) {
				Log.e(LOG_NAME, "Problem reading observation.", oe);
				return;
			}
			dynamicFormJson = o.getEvent().getForm();
		}

		List<View> controls = LayoutBaker.createControlsFromJson(this, ControlGenerationType.EDIT, dynamicFormJson);

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
				mageSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
						MageSpinner ms = ((MageSpinner) parent);
						String k = ms.getPropertyKey();
						if (observationId == NEW_OBSERVATION) {
							spinnersLastPositions.put(k, position);
						}

						JsonObject dynamicFormJson = o.getEvent().getForm();
						
						// get variantField
						JsonElement variantField = dynamicFormJson.get("variantField");
						String variantFieldString = null;
						if(variantField != null && !variantField.isJsonNull()) {
							variantFieldString = variantField.getAsString();
						}
						
						if (k.equals("type") || (variantFieldString != null && k.equals(variantFieldString))) {
							onTypeOrVariantChanged(k, parent.getItemAtPosition(position).toString());
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
					}
				});
			}
		}

		// add dynamic controls to view
		LayoutBaker.populateLayoutWithControls((LinearLayout) findViewById(R.id.location_dynamic_form), controls);

		hideKeyboardOnClick(findViewById(R.id.observation_edit));

		if (observationId == NEW_OBSERVATION) {
			this.setTitle("Create New Observation");
			l = getIntent().getParcelableExtra(LOCATION);

            o.setEvent(EventHelper.getInstance(getApplicationContext()).getCurrentEvent());
			o.setTimestamp(new Date());
			List<ObservationProperty> properties = new ArrayList<ObservationProperty>();
			properties.add(new ObservationProperty("timestamp", iso8601Format.format(o.getTimestamp())));
			o.addProperties(properties);
			try {
				User u = UserHelper.getInstance(getApplicationContext()).readCurrentUser();
				if (u != null) {
					o.setUserId(u.getRemoteId());
				}
			} catch (UserException ue) {
				ue.printStackTrace();
			}
			LayoutBaker.populateLayoutFromMap((LinearLayout) findViewById(R.id.form), o.getPropertiesMap());
		} else {
			this.setTitle("Edit Observation");
			// this is an edit of an existing observation
			attachments.addAll(o.getAttachments());
			for (Attachment a : attachments) {
				addAttachmentToGallery(a);
			}

			Map<String, ObservationProperty> propertiesMap = o.getPropertiesMap();
			Geometry geo = o.getGeometry();
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
			LayoutBaker.populateLayoutFromMap((LinearLayout) findViewById(R.id.form), propertiesMap);
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
				intent.putExtra(LocationEditActivity.MARKER_BITMAP, ObservationBitmapFactory.bitmap(ObservationEditActivity.this, o));
				startActivityForResult(intent, LOCATION_EDIT_ACTIVITY_REQUEST_CODE);
			}
		});

		setupMap();
	}
	
	/**
	 * Hides keyboard when clicking elsewhere
	 * 
	 * @param view
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
	
	@SuppressLint("NewApi")
	private long getElapsedTimeInMilliseconds() {
		long elapsedTimeInMilliseconds = 0;
		if (o.getPropertiesMap().containsKey("delta")) {
			elapsedTimeInMilliseconds = Long.parseLong(o.getPropertiesMap().get("delta").getValue().toString());
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
		
		if (o.getRemoteId() == null) {
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

	private void setupMap() {
		map = ((MapFragment) getFragmentManager().findFragmentById(R.id.background_map)).getMap();
        map.getUiSettings().setZoomControlsEnabled(false);

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
			observationMarker.setIcon(ObservationBitmapFactory.bitmapDescriptor(this, o));
		} else {
			observationMarker = map.addMarker(new MarkerOptions().position(location).icon(ObservationBitmapFactory.bitmapDescriptor(this, o)));
		}
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Always call the superclass so it can restore the view hierarchy
		super.onRestoreInstanceState(savedInstanceState);

		l = savedInstanceState.getParcelable("location");
		attachments = savedInstanceState.getParcelableArrayList("attachments");

		for (Attachment a : attachments) {
			addAttachmentToGallery(a);
		}

		LinearLayout form = (LinearLayout) findViewById(R.id.form);
		LayoutBaker.populateLayoutFromBundle(form, savedInstanceState);
		currentMediaUri = savedInstanceState.getParcelable(CURRENT_MEDIA_URI);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		LayoutBaker.populateBundleFromLayout((LinearLayout) findViewById(R.id.form), outState);
		outState.putParcelable("location", l);
		outState.putParcelableArrayList("attachments", new ArrayList<Attachment>(attachments));
		outState.putParcelable(CURRENT_MEDIA_URI, currentMediaUri);
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
			o.setState(State.ACTIVE);
			o.setDirty(true);
			o.setGeometry(new GeometryFactory().createPoint(new Coordinate(l.getLongitude(), l.getLatitude())));

			if (!LayoutBaker.checkAndFlagRequiredFields((LinearLayout) findViewById(R.id.form))) {
				return super.onOptionsItemSelected(item);
			}

			Map<String, ObservationProperty> propertyMap = LayoutBaker.populateMapFromLayout((LinearLayout) findViewById(R.id.form));

			try {
				o.setTimestamp(iso8601Format.parse(propertyMap.get("timestamp").getValue().toString()));
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

			o.addProperties(propertyMap.values());

			o.setAttachments(attachments);

			ObservationHelper oh = ObservationHelper.getInstance(getApplicationContext());
			try {
				if (o.getId() == null) {
					Observation newObs = oh.create(o);
					Log.i(LOG_NAME, "Created new observation with id: " + newObs.getId());
				} else {
					oh.update(o);
					Log.i(LOG_NAME, "Updated observation with remote id: " + o.getRemoteId());
				}
				finish();
			} catch (Exception e) {
				Log.e(LOG_NAME, e.getMessage(), e);
			}

			break;
		case R.id.observation_cancel:
			new AlertDialog.Builder(this).setTitle("Discard Changes").setMessage(R.string.cancel_edit).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
				}
			}).show();
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	public void cameraButtonPressed(View v) {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		File f = null;
        try {
        	f = MediaUtility.createImageFile();
        } catch (IOException ex) {
            // Error occurred while creating the File
        	ex.printStackTrace();
        }
        // Continue only if the File was successfully created
        if (f != null) {
        	currentMediaUri = Uri.fromFile(f);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, currentMediaUri);
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        }
	}

	public void videoButtonPressed(View v) {
		Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		currentMediaUri = getOutputVideoUri(); // create a file to save the video in specific folder
		
		if (currentMediaUri != null) {
			intent.putExtra(MediaStore.EXTRA_OUTPUT, currentMediaUri);
		}
		startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
	}
	
	private static Uri getOutputVideoUri() {
		if (Environment.getExternalStorageState() == null) {
			return null;
		}

		File mediaStorage = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "MAGE_VIDEO");
		if (!mediaStorage.exists() && !mediaStorage.mkdirs()) {
			Log.e(LOG_NAME, "failed to create directory: " + mediaStorage);
			return null;
		}

		// Create a media file name
        DateFormat dateFormat = DateFormatFactory.format("yyyyMMdd_HHmmss", Locale.getDefault());
		String timeStamp = dateFormat.format(new Date());
		//File mediaFile = new File(mediaStorage, "VID_" + timeStamp + ".mp4");
		try {
			File mediaFile = File.createTempFile(
				"VID_" + timeStamp,  /* prefix */
				".mp4",         /* suffix */
				mediaStorage      /* directory */
		    );
			return Uri.fromFile(mediaFile);
		} catch (Exception e) {
			Log.e(LOG_NAME, "failed to create temp video file: " + mediaStorage + "/VID_" + timeStamp + ".mp4", e);
			return null;
		}
	}

	public void voiceButtonPressed(View v) {
		Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
		List<ResolveInfo> resolveInfo = getApplicationContext().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		if (resolveInfo.size() > 0) {
			startActivityForResult(intent, CAPTURE_VOICE_ACTIVITY_REQUEST_CODE);
		} else {
			Toast.makeText(getApplicationContext(), "Device has no voice recorder.", Toast.LENGTH_SHORT).show();
		}
	}

	public void fromGalleryButtonPressed(View v) {
		Intent intent = new Intent();
		intent.setType("image/*, video/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		Log.i(LOG_NAME, "build version sdk int: " + Build.VERSION.SDK_INT);
		if (Build.VERSION.SDK_INT >= 18) {
			intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		}
		startActivityForResult(intent, GALLERY_ACTIVITY_REQUEST_CODE);
	}

	private void addAttachmentToGallery(final Attachment a) {
		LinearLayout l = (LinearLayout) findViewById(R.id.image_gallery);
		
		final String absPath = a.getLocalPath();
		final String remoteId = a.getRemoteId();
		ImageView iv = new ImageView(getApplicationContext());
		LayoutParams lp = new LayoutParams(100, 100);
		iv.setLayoutParams(lp);
		iv.setPadding(0, 0, 10, 0);
		iv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(v.getContext(), AttachmentViewerActivity.class);
				intent.putExtra(AttachmentViewerActivity.ATTACHMENT, a);
				intent.putExtra(AttachmentViewerActivity.EDITABLE, (o.getRemoteId() == null));
				startActivityForResult(intent, ATTACHMENT_VIEW_ACTIVITY_REQUEST_CODE);
			}
		});
		l.addView(iv);
		
		// get content type from everywhere I can think of
		String contentType = a.getContentType();
		if (contentType == null || "".equalsIgnoreCase(contentType) || "application/octet-stream".equalsIgnoreCase(contentType)) {
			String name = a.getName();
			if (name == null) {
				name = a.getLocalPath();
				if (name == null) {
					name = a.getRemotePath();
				}
			}
			contentType = MediaUtility.getMimeType(name);
		}
		
		if (absPath != null) {
			if (contentType == null) {
				Glide.with(getApplicationContext()).load(R.drawable.ic_email_attachment).into(iv);
			} else if (contentType.startsWith("image")) {
				Glide.with(getApplicationContext()).load(new File(absPath)).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
			} else if (contentType.startsWith("video")) {
				Glide.with(getApplicationContext()).load(R.drawable.ic_video_2x).into(iv);
			} else if (contentType.startsWith("audio")) {
				Glide.with(getApplicationContext()).load(R.drawable.ic_microphone).into(iv);
			}
		} else if (remoteId != null) {
			String url = a.getUrl();
			if (contentType == null) {
				Glide.with(getApplicationContext()).load(R.drawable.ic_email_attachment).into(iv);
			} else if (contentType.startsWith("image")) {
				Glide.with(getApplicationContext()).load(url).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
			} else if (contentType.startsWith("video")) {
				Glide.with(getApplicationContext()).load(R.drawable.ic_video_2x).into(iv);
			} else if (contentType.startsWith("audio")) {
				Glide.with(getApplicationContext()).load(R.drawable.ic_microphone).into(iv);
			} 
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) {
			return;
		}
		switch (requestCode) {
		case CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE:
		case CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE:
			MediaUtility.addImageToGallery(getApplicationContext(), currentMediaUri);
			Attachment capture = new Attachment();
			capture.setLocalPath(MediaUtility.getFileAbsolutePath(currentMediaUri, this));
			attachments.add(capture);
			addAttachmentToGallery(capture);
			break;
		case GALLERY_ACTIVITY_REQUEST_CODE:
		case CAPTURE_VOICE_ACTIVITY_REQUEST_CODE:
			List<Uri> uris = getUris(data);
			for (Uri uri : uris) {
				String path = MediaUtility.getPath(getApplicationContext(), uri);
				Attachment a = new Attachment();
				a.setLocalPath(path);
				attachments.add(a);
				addAttachmentToGallery(a);
			}
			break;
		case ATTACHMENT_VIEW_ACTIVITY_REQUEST_CODE:
			Attachment remove = data.getParcelableExtra(AttachmentViewerActivity.ATTACHMENT);
			Boolean shouldRemove = data.getBooleanExtra(AttachmentViewerActivity.SHOULD_REMOVE, false);
			if (remove != null && shouldRemove) {
				int idx = attachments.indexOf(remove);
				attachments.remove(remove);
				LinearLayout l = (LinearLayout) findViewById(R.id.image_gallery);
				l.removeViewAt(idx);
			}
			break;
		case LOCATION_EDIT_ACTIVITY_REQUEST_CODE:
			l = data.getParcelableExtra(LocationEditActivity.LOCATION);
			setupMap();
			break;
		}
	}

	private List<Uri> getUris(Intent intent) {
		List<Uri> uris = new ArrayList<Uri>();
		uris.addAll(getClipDataUris(intent));
		if (intent.getData() != null) {
			uris.add(intent.getData());
		}
		return uris;
	}
	
	@TargetApi(16)
	private List<Uri> getClipDataUris(Intent intent) {
		List<Uri> uris = new ArrayList<Uri>();
		if (Build.VERSION.SDK_INT >= 16) {
			ClipData cd = intent.getClipData();
			if (cd != null) {
				for (int i = 0; i < cd.getItemCount(); i++) {
					uris.add(cd.getItemAt(i).getUri());
				}
			}
		}
		return uris;
	}

	public void onTypeOrVariantChanged(String field, String value) {
		o.addProperties(Collections.singleton(new ObservationProperty(field, value)));
		if (observationMarker != null) {
			observationMarker.remove();
		}
		observationMarker = map.addMarker(new MarkerOptions().position(new LatLng(l.getLatitude(), l.getLongitude())).icon(ObservationBitmapFactory.bitmapDescriptor(this, o)));
	}
}
