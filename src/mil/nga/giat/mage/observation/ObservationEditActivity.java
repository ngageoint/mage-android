package mil.nga.giat.mage.observation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.form.MageEditText;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.common.Geometry;
import mil.nga.giat.mage.sdk.datastore.common.GeometryType;
import mil.nga.giat.mage.sdk.datastore.common.Property;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.State;
import mil.nga.giat.mage.sdk.utils.GeometryUtil;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class ObservationEditActivity extends FragmentActivity {
	
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
	
	Date date;
	DecimalFormat latLngFormat = new DecimalFormat("###.######");
	List<String> attachmentUris = new ArrayList<String>();
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

		GoogleMap map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
		
		LatLng location = new LatLng(lat, lon);
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
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
	    attachmentUris.addAll(Arrays.asList(savedInstanceState.getStringArray("attachmentUris")));
	    
	    for (String uri : attachmentUris) {
	    	addImageToGallery(Uri.parse(uri));
	    }
	    
	    LinearLayout form = (LinearLayout)findViewById(R.id.form);
	    for (int i = 0; i < form.getChildCount(); i++) {
			View v = form.getChildAt(i);
			if (v instanceof MageEditText) {
				MageEditText text = (MageEditText)v;
				text.setText(savedInstanceState.getString(text.getPropertyKey()));
			}
		}
	}
	
	@Override
    protected void onSaveInstanceState (Bundle outState) {
		outState.putDouble("lat", lat);
		outState.putDouble("lon", lon);
		outState.putStringArray("attachmentUris", attachmentUris.toArray(new String[attachmentUris.size()]));
		outState.putString("LEVEL", ((EditText)findViewById(R.id.level)).getText().toString());
		outState.putString("TYPE", (String)((Spinner)findViewById(R.id.type_spinner)).getSelectedItem());
		outState.putString("LEVEL", ((EditText)findViewById(R.id.level)).getText().toString());
		outState.putString("TEAM", ((EditText)findViewById(R.id.team)).getText().toString());
		outState.putString("DESCRIPTION", ((EditText)findViewById(R.id.description)).getText().toString());
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
		System.out.println("STarting the observation view");
		switch (item.getItemId()) {
	
		case R.id.observation_save:
			System.out.println("SAVE");
			
			Observation observation = new Observation();
			observation.setState(new State("active"));
			observation.setGeometry(new Geometry("[" + lat + "," + lon + "]", new GeometryType("point")));
			
			Collection<Property> properties = new ArrayList<Property>();
			properties.add(new Property("OBSERVATION_DATE", String.valueOf(date.getTime())));
			properties.add(new Property("TYPE", (String)((Spinner)findViewById(R.id.type_spinner)).getSelectedItem()));
			properties.add(new Property("LEVEL", ((EditText)findViewById(R.id.level)).getText().toString()));
			properties.add(new Property("TEAM", ((EditText)findViewById(R.id.team)).getText().toString()));
			properties.add(new Property("DESCRIPTION", ((EditText)findViewById(R.id.description)).getText().toString()));			
			observation.setProperties(properties);
			
			observation.setProperties(properties);
			Collection<Attachment> attachments = new ArrayList<Attachment>();
			for (String uri : attachmentUris) {
				Attachment a = new Attachment();
				a.setLocal_path(uri);
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
		Log.d("Observation Edit", "Camera button pressed");
	    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	    Log.d("Observation Edit", "Starting Intent");
	    startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
	    Log.d("Observation Edit", "Started camera");
	}
	
	private int getPowerOfTwoForSampleRatio(double ratio){
        int k = Integer.highestOneBit((int)Math.floor(ratio));
        if(k==0) return 1;
        else return k;
    }
	
	private Bitmap getThumbnail(Uri uri) throws FileNotFoundException, IOException{
        InputStream input = this.getContentResolver().openInputStream(uri);

        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither=true;//optional
        onlyBoundsOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1))
            return null;

        int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

        double ratio = (originalSize > 50) ? (originalSize / 50) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither=true;//optional
        bitmapOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        input = this.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();
        return bitmap;
    }
	
	private void addImageToGallery(Uri uri) {
		LinearLayout l = (LinearLayout)findViewById(R.id.image_gallery);
        ImageView iv = new ImageView(getApplicationContext());
        try {
            iv.setImageBitmap(getThumbnail(uri));
            LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
            iv.setLayoutParams(lp);
            l.addView(iv);
            Log.d("image", "Set the image gallery to have an image with uri " + uri);
        } catch (Exception e) {
        	Log.e("exception", "Error making image", e);
        }
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
	        if (resultCode == RESULT_OK) {
	            // Image captured and saved to fileUri specified in the Intent
	            Toast.makeText(this, "Image saved to:\n" +
	                     data.getData(), Toast.LENGTH_LONG).show();
	            attachmentUris.add(data.getData().toString());
	            addImageToGallery(data.getData());
	            
	        } else if (resultCode == RESULT_CANCELED) {
	            // User cancelled the image capture
	        } else {
	            // Image capture failed, advise user
	        }
	    }

	    if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
	        if (resultCode == RESULT_OK) {
	            // Video captured and saved to fileUri specified in the Intent
	            Toast.makeText(this, "Video saved to:\n" +
	                     data.getData(), Toast.LENGTH_LONG).show();
	        } else if (resultCode == RESULT_CANCELED) {
	            // User cancelled the video capture
	        } else {
	            // Video capture failed, advise user
	        }
	    }
	}
	
}