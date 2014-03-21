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
import mil.nga.giat.mage.sdk.datastore.common.GeometryType;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.State;
import mil.nga.giat.mage.sdk.utils.MediaUtils;
import android.annotation.TargetApi;
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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class ObservationEditActivity extends FragmentActivity {

	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
	private static final int CAPTURE_VOICE_ACTIVITY_REQUEST_CODE = 300;
	private static final int GALLERY_ACTIVITY_REQUEST_CODE = 400;
	private static final int ATTACHMENT_VIEW_ACTIVITY_REQUEST_CODE = 500;

	Date date;
	DecimalFormat latLngFormat = new DecimalFormat("###.######");
	List<String> attachmentPaths = new ArrayList<String>();
	double lat;
	double lon;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.observation_editor);
		this.setTitle("Create New Observation");

		Intent intent = getIntent();
		lat = intent.getDoubleExtra("latitude", 0.0);
		lon = intent.getDoubleExtra("longitude", 0.0);

		GoogleMap map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.background_map)).getMap();

		LatLng location = new LatLng(lat, lon);
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 16));
		map.addMarker(new MarkerOptions().position(location));

		((TextView) findViewById(R.id.location)).setText(latLngFormat.format(location.latitude) + ", " + latLngFormat.format(location.longitude));
		date = new Date();
		((TextView) findViewById(R.id.date)).setText(date.toString());
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Always call the superclass so it can restore the view hierarchy
		super.onRestoreInstanceState(savedInstanceState);

		lat = savedInstanceState.getDouble("lat");
		lon = savedInstanceState.getDouble("lon");
		attachmentPaths.addAll(Arrays.asList(savedInstanceState.getStringArray("attachmentPaths")));

		for (String path : attachmentPaths) {
			addImageToGallery(path);
		}

		LinearLayout form = (LinearLayout) findViewById(R.id.form);
		populatePropertyFieldsFromSaved(form, savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putDouble("lat", lat);
		outState.putDouble("lon", lon);
		outState.putStringArray("attachmentPaths", attachmentPaths.toArray(new String[attachmentPaths.size()]));
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

			Observation observation = new Observation();
			observation.setState(new State("active"));
			observation.setGeometry(new Geometry("[" + lat + "," + lon + "]", new GeometryType("point")));
			
			Map<String, String> propertyMap = new HashMap<String, String>();
			LinearLayout form = (LinearLayout) findViewById(R.id.form);
			savePropertyFieldsToMap(form, propertyMap);
			propertyMap.put("TYPE", (String) ((Spinner) findViewById(R.id.type_spinner)).getSelectedItem());
			propertyMap.put("OBSERVATION_DATE", String.valueOf(date.getTime()));
			
			observation.setPropertiesMap(propertyMap);

			Collection<Attachment> attachments = new ArrayList<Attachment>();
			for (String path : attachmentPaths) {
				Attachment a = new Attachment();
				a.setLocal_path(path);
				attachments.add(a);
			}
			observation.setAttachments(attachments);

			ObservationHelper oh = ObservationHelper.getInstance(getApplicationContext());
			try {
				Observation newObs = oh.createObservation(observation);
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
		// TODO test when we get a 4.3 device
		// intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		startActivityForResult(intent, GALLERY_ACTIVITY_REQUEST_CODE);
	}

	private void addImageToGallery(final String absPath) {
		LinearLayout l = (LinearLayout) findViewById(R.id.image_gallery);
		ImageView iv = new ImageView(getApplicationContext());
		try {
			
			if (absPath.endsWith(".mp4")) {
				Drawable[] layers = new Drawable[2];
				Resources r = getResources();
				layers[0] = new BitmapDrawable(r, ThumbnailUtils.createVideoThumbnail(absPath, MediaStore.Video.Thumbnails.MICRO_KIND));
				layers[1] = r.getDrawable(R.drawable.ic_video_white_2x);
				LayerDrawable ld = new LayerDrawable(layers);
				iv.setImageDrawable(ld);
			} else if (absPath.endsWith(".mp3") || absPath.endsWith("m4a")) {
				iv.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_microphone));
			} else {
				iv.setImageBitmap(MediaUtils.getThumbnail(new File(absPath), 100));
			}
			LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
			iv.setLayoutParams(lp);
			iv.setPadding(0, 0, 10, 0);
			iv.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent intent = new Intent(v.getContext(), ImageViewerActivity.class);
					intent.setData(Uri.fromFile(new File(absPath)));
					intent.putExtra(ImageViewerActivity.EDITABLE, true);
					startActivityForResult(intent, ATTACHMENT_VIEW_ACTIVITY_REQUEST_CODE);
				}
			});
			l.addView(iv);
			Log.d("image", "Set the image gallery to have an image with absolute path " + absPath);
		} catch (Exception e) {
			Log.e("exception", "Error making image", e);
		}
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
			String path = MediaUtils.getFileAbsolutePath(data.getData(), getApplicationContext());
			attachmentPaths.add(path);
			addImageToGallery(path);
			break;
		case ATTACHMENT_VIEW_ACTIVITY_REQUEST_CODE:
			if (data.getData() != null && data.getBooleanExtra("REMOVE", false)) {
				int idx = attachmentPaths.indexOf(MediaUtils.getFileAbsolutePath(data.getData(), getApplicationContext()));
				attachmentPaths.remove(idx);
				LinearLayout l = (LinearLayout) findViewById(R.id.image_gallery);
				l.removeViewAt(idx);
			}
			break;
		}
	}

	// TODO test when we get a 4.3 device
	@TargetApi(16)
	private void handleClipData(Intent data) {
		if (Build.VERSION.SDK_INT >= 16) {
			Log.d("picker", "data 2 is " + data.getClipData());
		}
	}
}