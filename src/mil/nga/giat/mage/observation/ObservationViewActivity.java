package mil.nga.giat.mage.observation;

import java.io.File;
import java.util.Collection;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.form.MageTextView;
import mil.nga.giat.mage.sdk.datastore.common.Property;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.utils.MediaUtils;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class ObservationViewActivity extends FragmentActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.observation_viewer);
		try {
			Observation o = ObservationHelper.getInstance(getApplicationContext()).readObservation(1L);
			Collection<Property> properties = o.getProperties();
			String coordinates = o.getGeometry().getCoordinates();
			String[] coordinateSplit = coordinates.split("\\[|,|\\]");
			
			((TextView)findViewById(R.id.location)).setText(coordinateSplit[1] + ", " + coordinateSplit[2]);
			GoogleMap map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mini_map)).getMap();
			
			LatLng location = new LatLng(Double.parseDouble(coordinateSplit[1]), Double.parseDouble(coordinateSplit[2]));
			
			map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
			
			map.addMarker(new MarkerOptions().position(location));
			
			LinearLayout propertyContainer = (LinearLayout)findViewById(R.id.propertyContainer);
			for (int i = 0; i < propertyContainer.getChildCount(); i++) {
				View v = propertyContainer.getChildAt(i);
				if (v instanceof MageTextView) {
					String propertyKey = ((MageTextView)v).getPropertyKey();
					for (Property p : properties) {
						if (propertyKey.equals(p.getKey())) {
							((MageTextView)v).setText(p.getValue());
						}
					}
				}
			}
			Log.d("view", "there are " + o.getAttachments().size() + " attachments");
			for (Attachment a : o.getAttachments()) {
				addImageToGallery(a.getLocal_path());
			}
		} catch (Exception e) {
			
		}
		
		this.setTitle("Observation Type");
	}
	
	private void addImageToGallery(final String absPath) {
		Log.d("view", "Abs path is: " + absPath);
		LinearLayout l = (LinearLayout) findViewById(R.id.image_gallery);
		ImageView iv = new ImageView(getApplicationContext());
		try {
			if (absPath.endsWith(".mp4")) {
				Drawable[] layers = new Drawable[2];
				Resources r = getResources();
				layers[0] = new BitmapDrawable(r, ThumbnailUtils.createVideoThumbnail(absPath, MediaStore.Video.Thumbnails.MICRO_KIND));
				layers[1] = r.getDrawable(R.drawable.ic_video_white);
				LayerDrawable ld = new LayerDrawable(layers);
				iv.setImageDrawable(ld);
			} else if (absPath.endsWith(".mp3") || absPath.endsWith("m4a")) {
				iv.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_microphone));
			} else {
				try {
				iv.setImageBitmap(MediaUtils.getThumbnail(new File(absPath), 100));
				} catch (Exception e) {
					
				}
			}
			LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
			iv.setLayoutParams(lp);
			iv.setPadding(0, 0, 10, 0);
//			iv.setOnClickListener(new View.OnClickListener() {
//
//				@Override
//				public void onClick(View v) {
//					Intent intent = new Intent(v.getContext(), ImageViewerActivity.class);
//					intent.setData(uri);
//					startActivityForResult(intent, ATTACHMENT_VIEW_ACTIVITY_REQUEST_CODE);
//				}
//			});
			l.addView(iv);
			Log.d("image", "Set the image gallery to have an image with uri " + absPath);
		} catch (Exception e) {
			Log.e("exception", "Error making image", e);
		}
	}
}
