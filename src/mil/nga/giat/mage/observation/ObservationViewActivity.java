package mil.nga.giat.mage.observation;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.form.MageTextView;
import mil.nga.giat.mage.sdk.datastore.common.Geometry;
import mil.nga.giat.mage.sdk.datastore.common.PointGeometry;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.utils.MediaUtility;
import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class ObservationViewActivity extends FragmentActivity {
	
	public static String OBSERVATION_ID = "OBSERVATION_ID";
	private static final int ATTACHMENT_VIEW_ACTIVITY_REQUEST_CODE = 500;
	private Observation o;
	private Map<String, String> propertiesMap;
	DecimalFormat latLngFormat = new DecimalFormat("###.######");
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm zz");
	
	public class AttachmentGalleryTask extends AsyncTask<Attachment, ImageView, Boolean> {

		@Override
		protected Boolean doInBackground(Attachment... params) {
			String token = PreferenceHelper.getInstance(getApplicationContext()).getValue(R.string.tokenKey);
			for (Attachment a : params) {
				final String absPath = a.getLocalPath();
				ImageView iv = new ImageView(getApplicationContext());
				LayoutParams lp = new LayoutParams(100, 100);
				iv.setLayoutParams(lp);
				iv.setPadding(0, 0, 10, 0);
				iv.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(v.getContext(), AttachmentViewerActivity.class);
						intent.setData(Uri.fromFile(new File(absPath)));
						intent.putExtra(AttachmentViewerActivity.EDITABLE, false);
						startActivityForResult(intent, ATTACHMENT_VIEW_ACTIVITY_REQUEST_CODE);
					}
				});
				try {
					if (absPath != null && absPath.endsWith(".mp4")) {
						Drawable[] layers = new Drawable[2];
						Resources r = getResources();
						layers[0] = new BitmapDrawable(r, ThumbnailUtils.createVideoThumbnail(absPath, MediaStore.Video.Thumbnails.MICRO_KIND));
						layers[1] = r.getDrawable(R.drawable.ic_video_white_2x);
						LayerDrawable ld = new LayerDrawable(layers);
						iv.setImageDrawable(ld);
					} else if (absPath != null && (absPath.endsWith(".mp3") || absPath.endsWith("m4a"))) {
						Glide.load(R.drawable.ic_microphone).into(iv);
//						iv.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_microphone));
					} else {
						if (a.getRemoteId() != null) {
							String url = a.getUrl() + "?access_token=" + token;
							Glide.load(url).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
						} else {
							Glide.load(new File(absPath)).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
						}
					}
					Log.d("image", "Set the image gallery to have an image with uri " + absPath);
				} catch (Exception e) {
					Log.e("exception", "Error making image", e);
				}
				
				publishProgress(iv);
			}
			return true;
		}
		
		protected void onProgressUpdate(ImageView... progress) {
			LinearLayout l = (LinearLayout) findViewById(R.id.image_gallery);
			l.addView(progress[0]);
		}		
	}
	
	private void createImageViews(ViewGroup gallery) {
		String token = PreferenceHelper.getInstance(getApplicationContext()).getValue(R.string.tokenKey);
		for (final Attachment a : o.getAttachments()) {
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
			gallery.addView(iv);
			
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
				String url = a.getUrl() + "?access_token=" + token;
				if (contentType.startsWith("image")) {
					Glide.load(url).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
				} else if (contentType.startsWith("video")) {
					Glide.load(R.drawable.ic_video_2x).into(iv);
				} else if (contentType.startsWith("audio")) {
					Glide.load(R.drawable.ic_microphone).into(iv);
				}
			}
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; goto parent activity.
	            this.finish();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.observation_viewer);
		ActionBar actionBar = getActionBar();

		actionBar.setDisplayHomeAsUpEnabled(true);
		try {
			o = ObservationHelper.getInstance(getApplicationContext()).readByPrimaryKey(getIntent().getLongExtra(OBSERVATION_ID, 0L));
			propertiesMap = o.getPropertiesMap();
			Geometry geo = o.getObservationGeometry().getGeometry();
			if(geo instanceof PointGeometry) {
				PointGeometry pointGeo = (PointGeometry)geo;
				((TextView)findViewById(R.id.location)).setText(latLngFormat.format(pointGeo.getLatitude()) + ", " + latLngFormat.format(pointGeo.getLongitude()));
				if(propertiesMap.containsKey("LOCATION_PROVIDER")) {
					((TextView)findViewById(R.id.location_provider)).setText("("+propertiesMap.get("LOCATION_PROVIDER")+")");
				}
				if (propertiesMap.containsKey("LOCATION_ACCURACY")) {
					((TextView)findViewById(R.id.location_accuracy)).setText("\u00B1" + propertiesMap.get("LOCATION_ACCURACY") + "m");
				}
				GoogleMap map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mini_map)).getMap();
				
				LatLng location = new LatLng(pointGeo.getLatitude(), pointGeo.getLongitude());
				
				map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
				
				map.addMarker(new MarkerOptions().position(location));				
			}

			LinearLayout propertyContainer = (LinearLayout)findViewById(R.id.propertyContainer);
			populatePropertyFields(propertyContainer);
			
			populatePropertyFields((LinearLayout)findViewById(R.id.topPropertyContainer));
			
			if (o.getAttachments().size() == 0) {
				findViewById(R.id.image_gallery).setVisibility(View.GONE);
			} else {
				LinearLayout l = (LinearLayout) findViewById(R.id.image_gallery);
				createImageViews(l);
			}
			
			TextView user = (TextView)findViewById(R.id.username);
			user.setText(o.getPropertiesMap().get("userId"));
			
			FrameLayout fl = (FrameLayout)findViewById(R.id.sync_status);
			if (o.getRemoteId() == null) {
				View.inflate(getApplicationContext(), R.layout.saved_locally, fl);
			} else {
				View status = View.inflate(getApplicationContext(), R.layout.submitted_on, fl);
				TextView syncDate = (TextView)status.findViewById(R.id.observation_sync_date);
				syncDate.setText(sdf.format(o.getLastModified()));
			}
		} catch (Exception e) {
			Log.e("observation view", e.getMessage(), e);
		}
		
		this.setTitle(propertiesMap.get("TYPE"));
	}
	
	private void populatePropertyFields(LinearLayout ll) {
		for (int i = 0; i < ll.getChildCount(); i++) {
			View v = ll.getChildAt(i);
			if (v instanceof MageTextView) {
				MageTextView m = (MageTextView)v;
				String propertyKey = m.getPropertyKey();
				String propertyValue = propertiesMap.get(propertyKey);
				if (propertyValue == null) continue;
				switch(m.getPropertyType()) {
				case STRING:
				case MULTILINE:
					m.setText(propertyValue);
					break;
				case USER:
					
					break;
				case DATE:
					m.setText(new Date(Long.parseLong(propertyValue)).toString());
					break;
				case LOCATION:
					
					break;
				case MULTICHOICE:
					
					break;
				}
			} else if (v instanceof LinearLayout) {
				populatePropertyFields((LinearLayout)v);
			}
		}
	}
}