package mil.nga.giat.mage.profile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.marker.LocationBitmapFactory;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.profile.UpdateProfileTask;
import mil.nga.giat.mage.sdk.utils.MediaUtility;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class MyProfileFragment extends Fragment {

	private static final String LOG_NAME = MyProfileFragment.class.getName();
	
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int GALLERY_ACTIVITY_REQUEST_CODE = 400;
	
	public static String INITIAL_LOCATION = "INITIAL_LOCATION";
	public static String INITIAL_ZOOM = "INITIAL_ZOOM";
	public static String USER_ID = "USER_ID";
	
	private Uri currentMediaUri;
	private User user;
	
	private MapView mapView;
	
	@Override
	public void onDestroy() {
		mapView.onDestroy();
		super.onDestroy();
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mapView.onLowMemory();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mapView.onResume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mapView.onPause();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_profile, container, false);
		
		String userToLoad = getActivity().getIntent().getStringExtra(USER_ID);
		try {
			if (userToLoad != null) {
				user = UserHelper.getInstance(getActivity().getApplicationContext()).read(userToLoad);
			} else {
				user = UserHelper.getInstance(getActivity().getApplicationContext()).readCurrentUser();
			}
			
		} catch (UserException e) {
			e.printStackTrace();
		}
		final Long userId = user.getId();
		
		mapView = (MapView) rootView.findViewById(R.id.mapView);
		mapView.onCreate(savedInstanceState);
		MapsInitializer.initialize(getActivity().getApplicationContext());
		
		LatLng latLng = getActivity().getIntent().getParcelableExtra(INITIAL_LOCATION);
		if (latLng == null) {
			latLng = new LatLng(0, 0);
		}
		float zoom = getActivity().getIntent().getFloatExtra(INITIAL_ZOOM, 0);
		mapView.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
		List<Location> lastLocation = LocationHelper.getInstance(getActivity()).getUserLocations(userId, getActivity(), 1, true);
		
		LatLng location = new LatLng(0,0);
		
		if (!lastLocation.isEmpty()) {
			Geometry geo = lastLocation.get(0).getLocationGeometry().getGeometry();
			if (geo instanceof Point) {
				Point point = (Point) geo;
				location = new LatLng(point.getY(), point.getX());
				MarkerOptions options = new MarkerOptions().position(location).visible(true);
				
				Marker marker = mapView.getMap().addMarker(options);
				marker.setIcon(LocationBitmapFactory.bitmapDescriptor(getActivity(), lastLocation.get(0), lastLocation.get(0).getUser()));
				mapView.getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
			}
		}
		
		TextView realName = (TextView)rootView.findViewById(R.id.realName);
		TextView username = (TextView)rootView.findViewById(R.id.username);
		TextView phone = (TextView)rootView.findViewById(R.id.phone);
		TextView email = (TextView)rootView.findViewById(R.id.email);
		
		realName.setText(user.getFirstname() + " " + user.getLastname());
		username.setText("(" + user.getUsername() + ")");
		if (user.getPrimaryPhone() == null) {
			phone.setVisibility(View.GONE);
		} else {
			phone.setVisibility(View.VISIBLE);
			phone.setText(user.getPrimaryPhone());
		}
		
		if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
			email.setVisibility(View.VISIBLE);
			email.setText(user.getEmail());
		} else {
			email.setVisibility(View.GONE);
		}
		ImageView iv = (ImageView)rootView.findViewById(R.id.profile_picture);
		String avatarUrl = null;
		if (user.getAvatarUrl() != null) {
			avatarUrl = user.getAvatarUrl() + "?access_token=" + PreferenceHelper.getInstance(getActivity().getApplicationContext()).getValue(R.string.tokenKey);
			new DownloadImageTask(iv).execute(avatarUrl);
		}
		
		final Intent intent = new Intent(getActivity().getApplicationContext(), ProfilePictureViewerActivity.class);
		intent.putExtra(ProfilePictureViewerActivity.IMAGE_URL, avatarUrl);
		intent.putExtra(ProfilePictureViewerActivity.USER_FIRSTNAME, user.getFirstname());
		intent.putExtra(ProfilePictureViewerActivity.USER_LASTNAME, user.getLastname());
		
		rootView.findViewById(R.id.profile_picture).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try {
					if (userId.equals(UserHelper.getInstance(getActivity().getApplicationContext()).readCurrentUser().getId())) {
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					    builder.setItems(R.array.profileImageChoices, new DialogInterface.OnClickListener() {
						   public void onClick(DialogInterface dialog, int which) {
							   switch (which) {
							   case 0:
									startActivityForResult(intent, 1);
									break;
							   case 1:
								   // change the picture from the gallery
								   Intent intent = new Intent();
									intent.setType("image/*");
									intent.setAction(Intent.ACTION_GET_CONTENT);
									if (Build.VERSION.SDK_INT >= 18) {
										intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
									}
									startActivityForResult(intent, GALLERY_ACTIVITY_REQUEST_CODE);
								   break;
							   case 2:
								   // change the picture from the camera
								   Intent caputreIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
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
							        	caputreIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentMediaUri);
							            startActivityForResult(caputreIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
							        }
								   break;
							   }
						   }
					    });
						AlertDialog d = builder.create();
						d.show();
					} else {
						startActivityForResult(intent, 1);
					}
				} catch (Exception e) {
					Log.e(LOG_NAME, "Problem setting profile picture.");
				}
			}
		});
		
		return rootView;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK) {
			return;
		}
		String filePath = null;
		switch (requestCode) {
		case CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE:
			MediaUtility.addImageToGallery(getActivity().getApplicationContext(), currentMediaUri);
			filePath = MediaUtility.getFileAbsolutePath(currentMediaUri, getActivity());
			
			break;
		case GALLERY_ACTIVITY_REQUEST_CODE:
			List<Uri> uris = getUris(data);
			for (Uri uri : uris) {
				filePath = MediaUtility.getPath(getActivity().getApplicationContext(), uri);
			}
			break;
		}
		if (filePath != null) {
			Bitmap b = MediaUtility.resizeAndRoundCorners((MediaUtility.orientImage(new File(filePath))), 150);
			ImageView iv = (ImageView)getActivity().findViewById(R.id.profile_picture);
			iv.setImageBitmap(b);
			user.setLocalAvatarPath(filePath);
			UpdateProfileTask task = new UpdateProfileTask(user, getActivity());
			task.execute(filePath);
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
	
	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
	    ImageView bmImage;

	    public DownloadImageTask(ImageView bmImage) {
	        this.bmImage = bmImage;
	    }

	    protected Bitmap doInBackground(String... urls) {
	        String urldisplay = urls[0];
	        Bitmap mIcon11 = null;
	        try {
	            InputStream in = new java.net.URL(urldisplay).openStream();
	            mIcon11 = BitmapFactory.decodeStream(in);
	        } catch (Exception e) {
	            Log.e(LOG_NAME, e.getMessage());
	            e.printStackTrace();
	        }
	        return mIcon11;
	    }

	    protected void onPostExecute(Bitmap bitmap) {
	    	if (bitmap != null) {
	    		bmImage.setImageBitmap(MediaUtility.resizeAndRoundCorners(bitmap, 150));
	    	}
	    }
	}
}
