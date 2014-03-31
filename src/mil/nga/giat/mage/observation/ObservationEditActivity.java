package mil.nga.giat.mage.observation;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.form.MageSpinner;
import mil.nga.giat.mage.form.MageTextView;
import mil.nga.giat.mage.sdk.datastore.common.Geometry;
import mil.nga.giat.mage.sdk.datastore.common.PointGeometry;
import mil.nga.giat.mage.sdk.datastore.common.State;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationGeometry;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.utils.MediaUtility;
import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class ObservationEditActivity extends FragmentActivity {

	public static String OBSERVATION_ID = "OBSERVATION_ID";
	public static String LATITUDE = "LATITUDE";
	public static String LONGITUDE = "LONGITUDE";
	public static String ACCURACY = "ACCURACY";
	public static String OBSERVATION_LOCATION_TYPE = "OBSERVATION_LOCATION_TYPE";
	
	public static String OBSERVATION_LOCATION_TYPE_MANUAL = "MANUAL";
	public static String OBSERVATION_LOCATION_TYPE_GPS = "GPS";
	
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
	private static final int CAPTURE_VOICE_ACTIVITY_REQUEST_CODE = 300;
	private static final int GALLERY_ACTIVITY_REQUEST_CODE = 400;
	private static final int ATTACHMENT_VIEW_ACTIVITY_REQUEST_CODE = 500;
	
	private static final long NEW_OBSERVATION = -1L;

	Date date;
	DecimalFormat latLngFormat = new DecimalFormat("###.######");
	ArrayList<Attachment> attachments = new ArrayList<Attachment>();
	double lat;
	double lon;
	long observationId;
	Observation o;
	Map<String, String> propertiesMap;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.observation_editor);
		
		Intent intent = getIntent();
		observationId = intent.getLongExtra(OBSERVATION_ID, NEW_OBSERVATION);
		
		if (observationId == NEW_OBSERVATION) {
			this.setTitle("Create New Observation");
			lat = intent.getDoubleExtra(LATITUDE, 0.0);
			lon = intent.getDoubleExtra(LONGITUDE, 0.0);
			date = new Date();
			((TextView) findViewById(R.id.date)).setText(date.toString());
			setupMap();
		} else {
			// this is an edit of an existing observation
			try {
				o = ObservationHelper.getInstance(getApplicationContext()).readObservation(getIntent().getLongExtra(OBSERVATION_ID, 0L));
				attachments.addAll(o.getAttachments());
				for (Attachment a : attachments) {
					addAttachmentToGallery(a);
				}
			
				propertiesMap = o.getPropertiesMap();
				Geometry geo = o.getObservationGeometry().getGeometry();
				if(geo instanceof PointGeometry) {
					PointGeometry point = (PointGeometry)geo;
					lat = point.getLatitude();
					lon = point.getLongitude();
					((TextView)findViewById(R.id.location)).setText(point.getLatitude() + ", " + point.getLongitude());
					GoogleMap map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mini_map)).getMap();
					
					LatLng location = new LatLng(point.getLatitude(), point.getLongitude());
					
					map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
					
					map.addMarker(new MarkerOptions().position(location));
				}
			} catch (ObservationException oe) {
				
			}
		}

	}
	
	private void setupMap() {
		GoogleMap map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.background_map)).getMap();

		LatLng location = new LatLng(lat, lon);
		((TextView) findViewById(R.id.location)).setText(latLngFormat.format(location.latitude) + ", " + latLngFormat.format(location.longitude));
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 16));
		map.addMarker(new MarkerOptions().position(location));
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Always call the superclass so it can restore the view hierarchy
		super.onRestoreInstanceState(savedInstanceState);

		lat = savedInstanceState.getDouble("lat");
		lon = savedInstanceState.getDouble("lon");
		attachments = savedInstanceState.getParcelableArrayList("attachments");

		for (Attachment a : attachments) {
			addAttachmentToGallery(a);
		}

		LinearLayout form = (LinearLayout) findViewById(R.id.form);
		populatePropertyFieldsFromSaved(form, savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putDouble("lat", lat);
		outState.putDouble("lon", lon);
		outState.putParcelableArrayList("attachments", new ArrayList<Attachment>(attachments));
		LinearLayout form = (LinearLayout) findViewById(R.id.form);
		savePropertyFieldsToBundle(form, outState);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.observation_edit_menu, menu);
		return true;
	}
	
	private void populatePropertyFieldsFromSaved(LinearLayout ll, Bundle savedInstanceState) {
		for (int i = 0; i < ll.getChildCount(); i++) {
			View v = ll.getChildAt(i);
			if (v instanceof MageTextView) {
				String propertyKey = ((MageTextView)v).getPropertyKey();
				((MageTextView)v).setText(savedInstanceState.getString(propertyKey));
			} else if (v instanceof MageSpinner) {
				MageSpinner spinner = (MageSpinner)v;
				String propertyKey = ((MageSpinner)v).getPropertyKey();
				String value = savedInstanceState.getString(propertyKey);
				int index = 0;
				for (index = 0; index < spinner.getAdapter().getCount(); index++)
			    {
			        if (spinner.getAdapter().getItem(index).equals(value))
			        {
			            spinner.setSelection(index);
			            break;
			        }
			    }
			} else if (v instanceof LinearLayout) {
				populatePropertyFieldsFromSaved((LinearLayout)v, savedInstanceState);
			}
		}
	}
	
	private void savePropertyFieldsToBundle(LinearLayout ll, Bundle outState) {
		for (int i = 0; i < ll.getChildCount(); i++) {
			View v = ll.getChildAt(i);
			if (v instanceof MageTextView) {
				String propertyKey = ((MageTextView)v).getPropertyKey();
				outState.putString(propertyKey, ((MageTextView)v).getText().toString());
			} else if (v instanceof MageSpinner) {
				String propertyKey = ((MageSpinner)v).getPropertyKey();
				outState.putString(propertyKey, (String) ((MageSpinner)v).getSelectedItem());
			} else if (v instanceof LinearLayout) {
				savePropertyFieldsToBundle((LinearLayout)v, outState);
			}
		}
	}
	
	private void savePropertyFieldsToMap(LinearLayout ll, Map<String, String> fields) {
		for (int i = 0; i < ll.getChildCount(); i++) {
			View v = ll.getChildAt(i);
			if (v instanceof MageTextView) {
				String propertyKey = ((MageTextView)v).getPropertyKey();
				fields.put(propertyKey, ((MageTextView)v).getText().toString());
			} else if (v instanceof MageSpinner) {
				String propertyKey = ((MageSpinner)v).getPropertyKey();
				fields.put(propertyKey, (String) ((MageSpinner)v).getSelectedItem());
			} else if (v instanceof LinearLayout) {
				savePropertyFieldsToMap((LinearLayout)v, fields);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		System.out.println("STarting the observation view");
		switch (item.getItemId()) {

		case R.id.observation_save:
			System.out.println("SAVE");
			
			if (o == null) {
				o = new Observation();
			}
			o.setState(State.ACTIVE);
			o.setObservationGeometry(new ObservationGeometry(new PointGeometry(lat, lon)));
			
			Map<String, String> propertyMap = new HashMap<String, String>();
			LinearLayout form = (LinearLayout) findViewById(R.id.form);
			savePropertyFieldsToMap(form, propertyMap);
			propertyMap.put("TYPE", (String) ((Spinner) findViewById(R.id.type_spinner)).getSelectedItem());
			propertyMap.put("OBSERVATION_DATE", String.valueOf(date.getTime()));
			
			o.setPropertiesMap(propertyMap);
			
			o.setAttachments(attachments);

			ObservationHelper oh = ObservationHelper.getInstance(getApplicationContext());
			try {
				Observation newObs = oh.createObservation(o);
				System.out.println(newObs);
			} catch (Exception e) {

			}

			break;
		}

		return super.onOptionsItemSelected(item);
	}

	public void cameraButtonPressed(View v) {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
	}

	public void videoButtonPressed(View v) {
		Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
	}

	public void voiceButtonPressed(View v) {
		Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
		startActivityForResult(intent, CAPTURE_VOICE_ACTIVITY_REQUEST_CODE);
	}

	public void fromGalleryButtonPressed(View v) {
		Intent intent = new Intent();
		intent.setType("image/*, video/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		Log.i("test", "build version sdk int: " + Build.VERSION.SDK_INT);
		if (Build.VERSION.SDK_INT >= 18) {
			intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		}
		startActivityForResult(intent, GALLERY_ACTIVITY_REQUEST_CODE);
	}

	private void addAttachmentToGallery(final Attachment a) {
		String server = PreferenceHelper.getInstance(getApplicationContext()).getValue(R.string.serverURLKey);
		String token = PreferenceHelper.getInstance(getApplicationContext()).getValue(R.string.tokenKey);
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
				intent.putExtra("attachment", a);
				intent.putExtra(AttachmentViewerActivity.EDITABLE, false);
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
			if (contentType.startsWith("image")) {
				Glide.load(new File(absPath)).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
			} else if (contentType.startsWith("video")) {
				Glide.load(R.drawable.ic_video_2x).into(iv);
			} else if (contentType.startsWith("audio")) {
				Glide.load(R.drawable.ic_microphone).into(iv);
			}
		} else if (remoteId != null) {
			String url = server + "/FeatureServer/3/Features/" + o.getRemoteId() + "/attachments/" + a.getRemoteId() + "?access_token=" + token;
			Log.i("test", "url to load is: " + url);
			Log.i("test", "content type is: " + contentType + " name is: " + a.getName());
			if (contentType.startsWith("image")) {
				Glide.load(url).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
			} else if (contentType.startsWith("video")) {
				Glide.load(R.drawable.ic_video_2x).into(iv);
			} else if (contentType.startsWith("audio")) {
				Glide.load(R.drawable.ic_microphone).into(iv);
			}
		}
		
		
		
//		try {
//			
//			if (absPath.endsWith(".mp4")) {
//				Drawable[] layers = new Drawable[2];
//				Resources r = getResources();
//				layers[0] = new BitmapDrawable(r, ThumbnailUtils.createVideoThumbnail(absPath, MediaStore.Video.Thumbnails.MICRO_KIND));
//				layers[1] = r.getDrawable(R.drawable.ic_video_white_2x);
//				LayerDrawable ld = new LayerDrawable(layers);
//				iv.setImageDrawable(ld);
//			} else if (absPath.endsWith(".mp3") || absPath.endsWith("m4a")) {
//				iv.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_microphone));
//			} else {
//				iv.setImageBitmap(MediaUtility.getThumbnail(new File(absPath), 100));
//			}
//			LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
//			iv.setLayoutParams(lp);
//			iv.setPadding(0, 0, 10, 0);
//			iv.setOnClickListener(new View.OnClickListener() {
//
//				@Override
//				public void onClick(View v) {
//					Intent intent = new Intent(v.getContext(), AttachmentViewerActivity.class);
//					intent.setData(Uri.fromFile(new File(absPath)));
//					intent.putExtra(AttachmentViewerActivity.EDITABLE, true);
//					startActivityForResult(intent, ATTACHMENT_VIEW_ACTIVITY_REQUEST_CODE);
//				}
//			});
//			l.addView(iv);
//			Log.d("image", "Set the image gallery to have an image with absolute path " + absPath);
//		} catch (Exception e) {
//			Log.e("exception", "Error making image", e);
//		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK)
			return;
		switch (requestCode) {
		case CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE:
		case CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE:
		case GALLERY_ACTIVITY_REQUEST_CODE:
		case CAPTURE_VOICE_ACTIVITY_REQUEST_CODE:
			ArrayList<Uri> uris = getUris(data);
			for (Uri u : uris) {
				String path = MediaUtility.getPath(getApplicationContext(), u);
				Attachment a = new Attachment();
				a.setLocalPath(path);
				attachments.add(a);
				addAttachmentToGallery(a);
			}
			break;
		case ATTACHMENT_VIEW_ACTIVITY_REQUEST_CODE:
			Attachment remove = data.getParcelableExtra("attachment");
			if (remove != null && data.getBooleanExtra("REMOVE", false)) {
				int idx = attachments.indexOf(remove);
				attachments.remove(remove);
				LinearLayout l = (LinearLayout) findViewById(R.id.image_gallery);
				l.removeViewAt(idx);
			}
			break;
		}
	}

	private ArrayList<Uri> getUris(Intent intent) {
		ArrayList<Uri> uris = new ArrayList<Uri>();
		addClipDataUris(intent, uris);
		if (intent.getData() != null) {
			uris.add(intent.getData());
		}
		return uris;
	}
	
	@TargetApi(16)
	private void addClipDataUris(Intent intent, ArrayList<Uri> uris) {
		if (Build.VERSION.SDK_INT >= 16) {
			ClipData cd = intent.getClipData();
			if (cd == null) return;
			for (int i = 0; i < cd.getItemCount(); i++) {
				uris.add(cd.getItemAt(i).getUri());
			}
		}
	}
}