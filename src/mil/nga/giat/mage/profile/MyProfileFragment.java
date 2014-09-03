package mil.nga.giat.mage.profile;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.observation.AttachmentViewerActivity;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.ImageView;
import android.widget.TextView;

public class MyProfileFragment extends Fragment {

	private static final String LOG_NAME = MyProfileFragment.class.getName();
	
	public static String INITIAL_LOCATION = "INITIAL_LOCATION";
	public static String INITIAL_ZOOM = "INITIAL_ZOOM";
	public static String USER_ID = "USER_ID";
	
	private View rootView;
	private Marker marker;
	private GoogleMap miniMap;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_profile, container, false);
		
		User user = null;
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
		Fragment tempFragment = getFragmentManager().findFragmentById(R.id.mini_map);
		if (tempFragment != null) {
			miniMap = ((MapFragment) tempFragment).getMap();

			LatLng latLng = getActivity().getIntent().getParcelableExtra(INITIAL_LOCATION);
			if (latLng == null) {
				latLng = new LatLng(0, 0);
			}

			float zoom = getActivity().getIntent().getFloatExtra(INITIAL_ZOOM, 0);

			miniMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

			LatLng location = new LatLng(0,0);//pointGeo.getY(), pointGeo.getX());
			miniMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
			if (marker != null) {
				marker.remove();
				marker = null;
			}
			marker = miniMap.addMarker(new MarkerOptions().position(location));//.icon(ObservationBitmapFactory.bitmapDescriptor(this, o)));
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
		final String avatarUrl = user.getAvatarUrl() + "?access_token=" + PreferenceHelper.getInstance(getActivity().getApplicationContext()).getValue(R.string.tokenKey);
		new DownloadImageTask(iv).execute(avatarUrl);
		
//		Glide.load(user.getAvatarUrl()).placeholder(R.drawable.missing_avatar).centerCrop().into(iv);
		Log.e(LOG_NAME, "Current user id: " + user.getFirstname());
		rootView.findViewById(R.id.profile_picture).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try {
					if (userId == UserHelper.getInstance(getActivity().getApplicationContext()).readCurrentUser().getId()) {
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					    builder.setItems(R.array.profileImageChoices, new DialogInterface.OnClickListener() {
					               public void onClick(DialogInterface dialog, int which) {
					               // The 'which' argument contains the index position
					               // of the selected item
					           }
					    });
						AlertDialog d = builder.create();
						d.show();
					} else {
						Intent intent = new Intent(v.getContext(), AttachmentViewerActivity.class);
						intent.setData(Uri.parse(avatarUrl));
						intent.putExtra(AttachmentViewerActivity.EDITABLE, false);
						startActivityForResult(intent, 1);
					}
				} catch (Exception e) {
					
				}
			}
		});
		
		return rootView;
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
	            Log.e("Error", e.getMessage());
	            e.printStackTrace();
	        }
	        return mIcon11;
	    }

	    protected void onPostExecute(Bitmap bitmap) {
	    	Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap
	                .getHeight(), Config.ARGB_8888);
	        Canvas canvas = new Canvas(result);

	        final int color = 0xff424242;
	        final Paint paint = new Paint();
	        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
	        final RectF rectF = new RectF(rect);
	        final float roundPx = 40.0f;

	        paint.setAntiAlias(true);
	        canvas.drawARGB(0, 0, 0, 0);
	        paint.setColor(color);
	        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

	        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
	        canvas.drawBitmap(bitmap, rect, rect, paint);

	        bmImage.setImageBitmap(result);
	    }
	}
}
