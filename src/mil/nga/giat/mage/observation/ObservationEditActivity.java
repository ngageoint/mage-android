package mil.nga.giat.mage.observation;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.database.orm.observation.Geometry;
import mil.nga.giat.mage.sdk.database.orm.observation.GeometryType;
import mil.nga.giat.mage.sdk.database.orm.observation.Observation;
import mil.nga.giat.mage.sdk.database.orm.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.database.orm.observation.Property;
import mil.nga.giat.mage.sdk.database.orm.observation.State;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ObservationEditActivity extends FragmentActivity {
	
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
	private Uri fileUri;
	
	Observation o;
	Date d;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.observation_editor);
		this.setTitle("Create New Observation");
		
		GoogleMap map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
		
		// TODO debugging location
		LatLng location = new LatLng(39.7, -104.0);
		
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
		
		map.addMarker(new MarkerOptions().position(location));
		((TextView)findViewById(R.id.location)).setText(location.latitude + ", " + location.longitude);
		d = new Date();
		((TextView)findViewById(R.id.date)).setText(d.toString());
		
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
			
			double lat = 39.7;
			double lon = -104.0;
			
			
			//o.setGeometry(new Geometry(lat, lon), new GeometryType("point"));
			o = new Observation();
			o.setState(new State("active"));
			o.setGeometry(new Geometry("[" + lat + "," + lon + "]", new GeometryType("point")));
			Collection<Property> properties = new ArrayList<Property>();
			properties.add(new Property("OBSERVATION_DATE", String.valueOf(d.getTime())));
			properties.add(new Property("TYPE", (String)((Spinner)findViewById(R.id.type_spinner)).getSelectedItem()));
			properties.add(new Property("LEVEL", ((EditText)findViewById(R.id.level)).getText().toString()));
			properties.add(new Property("TEAM", ((EditText)findViewById(R.id.team)).getText().toString()));
			properties.add(new Property("DESCRIPTION", ((EditText)findViewById(R.id.description)).getText().toString()));
			
			ObservationHelper oh = ObservationHelper.getInstance(getApplicationContext());
			try {
				Observation newObs = oh.createObservation(o);
				System.out.println(newObs.getPk_id());
			} catch (Exception e) {
				
			}
			
			break;
		}

		return super.onOptionsItemSelected(item);
	}
	
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;

	/** Create a file Uri for saving an image or video */
	private static Uri getOutputMediaFileUri(int type){
	      return Uri.fromFile(getOutputMediaFile(type));
	}

	/** Create a File for saving an image or video */
	private static File getOutputMediaFile(int type){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.

	    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
	              Environment.DIRECTORY_PICTURES), "MyCameraApp");
	    // This location works best if you want the created images to be shared
	    // between applications and persist after your app has been uninstalled.

	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.d("MyCameraApp", "failed to create directory");
	            return null;
	        }
	    }

	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    File mediaFile;
	    if (type == MEDIA_TYPE_IMAGE){
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "IMG_"+ timeStamp + ".jpg");
	    } else if(type == MEDIA_TYPE_VIDEO) {
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "VID_"+ timeStamp + ".mp4");
	    } else {
	        return null;
	    }

	    return mediaFile;
	}
	
	public void cameraButtonPressed(View v) {
		Log.d("Observation Edit", "Camera button pressed");
	    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	    Log.d("Observation Edit", "Starting Intent");
	    startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
	    Log.d("Observation Edit", "Started camera");
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
	        if (resultCode == RESULT_OK) {
	            // Image captured and saved to fileUri specified in the Intent
	            Toast.makeText(this, "Image saved to:\n" +
	                     data.getData(), Toast.LENGTH_LONG).show();
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