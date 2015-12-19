package mil.nga.giat.mage.profile;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.marker.LocationBitmapFactory;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.datastore.user.UserLocal;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.fetch.DownloadImageTask;
import mil.nga.giat.mage.sdk.profile.UpdateProfileTask;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

public class ProfileFragment extends Fragment implements OnMapReadyCallback {

	private static final String LOG_NAME = ProfileFragment.class.getName();
	
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int GALLERY_ACTIVITY_REQUEST_CODE = 400;
	
	public static String USER_ID = "USER_ID";
	
	private Uri currentMediaUri;
	private User user;
	
	private MapView mapView;
	private LatLng latLng = new LatLng(0, 0);
	private float zoom = 0f;
	private BitmapDescriptor icon;
	
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

		final Context context = getActivity().getApplicationContext();

		String userToLoad = getActivity().getIntent().getStringExtra(USER_ID);
		User currentUser = null;
		try {
			currentUser = UserHelper.getInstance(context).readCurrentUser();

			if (userToLoad != null) {
				user = UserHelper.getInstance(context).read(userToLoad);

			} else {
				user = UserHelper.getInstance(context).readCurrentUser();
			}

			List<Location> lastLocation = LocationHelper.getInstance(context).getUserLocations(user.getId(), getActivity(), 1, true);
			if (!lastLocation.isEmpty()) {
				Geometry geo = lastLocation.get(0).getGeometry();
				if (geo instanceof Point) {
					Point point = (Point) geo;
					latLng = new LatLng(point.getY(), point.getX());
					icon = LocationBitmapFactory.bitmapDescriptor(context, lastLocation.get(0), user);
				}
			}
		} catch (UserException ue) {
			Log.e(LOG_NAME, "Problem finding user.", ue);
		}

		MapsInitializer.initialize(context);

		mapView = (MapView) rootView.findViewById(R.id.mapView);
		mapView.onCreate(savedInstanceState);
		mapView.getMapAsync(this);

		final Long userId = user.getId();

		final String displayName = user.getDisplayName();
		getActivity().getActionBar().setTitle(user.equals(currentUser) ? "My Profile" : displayName);

		final TextView realNameTextView = (TextView)rootView.findViewById(R.id.realName);
		realNameTextView.setText(displayName);
		final TextView phoneTextView = (TextView)rootView.findViewById(R.id.phone);

		if (StringUtils.isNotBlank(user.getPrimaryPhone())) {
			SpannableString primaryPhone = new SpannableString(user.getPrimaryPhone());
			primaryPhone.setSpan(new UnderlineSpan(), 0, primaryPhone.length(), 0);
			phoneTextView.setText(primaryPhone);
			phoneTextView.setOnClickListener(new OnClickListener() {
										 @Override
										 public void onClick(View v) {
											 AlertDialog.Builder mBuilder = new AlertDialog.Builder(context);
											 mBuilder.setMessage("Do you want to call or text " + displayName + "?");
											 mBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
												 public void onClick(DialogInterface dialog, int id) {
													 dialog.cancel();
												 }
											 });
											 mBuilder.setNeutralButton("Call", new DialogInterface.OnClickListener() {
												 public void onClick(DialogInterface dialog, int id) {
													 try {
														 Intent callIntent = new Intent(Intent.ACTION_CALL);
														 callIntent.setData(Uri.parse("tel:" + phoneTextView.getText().toString()));
														 startActivity(callIntent);
													 } catch (ActivityNotFoundException ae) {
														 Toast.makeText(getActivity(), "Could not call user.", Toast.LENGTH_SHORT).show();
														 Log.e(LOG_NAME, "Could not call user.", ae);
													 }
												 }
											 });
											 mBuilder.setPositiveButton("Text", new DialogInterface.OnClickListener() {
												 public void onClick(DialogInterface dialog, int id) {
													 Intent textIntent = new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", phoneTextView.getText().toString(), null));
													 startActivity(textIntent);

												 }
											 });
											 mBuilder.create().show();
										 }
									 }

			);
			phoneTextView.setVisibility(View.VISIBLE);
		} else {
			phoneTextView.setVisibility(View.GONE);
		}

		final TextView emailTextView = (TextView)rootView.findViewById(R.id.email);
		if (StringUtils.isNotBlank(user.getEmail())) {
			SpannableString emailAddress = new SpannableString(user.getEmail());
			emailAddress.setSpan(new UnderlineSpan(), 0, emailAddress.length(), 0);
			emailTextView.setText(emailAddress);
			emailTextView.setOnClickListener(new OnClickListener() {
												 @Override
												 public void onClick(View v) {
													 AlertDialog.Builder mBuilder = new AlertDialog.Builder(context);
													 mBuilder.setMessage("Do you want to email " + displayName + "?");
													 mBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
														 public void onClick(DialogInterface dialog, int id) {
															 dialog.cancel();
														 }
													 });
													 mBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
														 public void onClick(DialogInterface dialog, int id) {

															 Intent emailIntent = new Intent(Intent.ACTION_SEND);
															 emailIntent.setType("message/rfc822");
															 emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailTextView.getText().toString()});
															 emailIntent.putExtra(Intent.EXTRA_SUBJECT, "MAGE");
															 try {
																 startActivity(Intent.createChooser(emailIntent, "Send mail..."));
															 } catch (ActivityNotFoundException ae) {
																 Toast.makeText(getActivity(), "There are no email clients installed.", Toast.LENGTH_SHORT).show();
																 Log.e(LOG_NAME, "Could not email user.", ae);
															 }
														 }
													 });
													 mBuilder.create().show();
												 }
											 }

			);
			emailTextView.setVisibility(View.VISIBLE);
		} else {
			emailTextView.setVisibility(View.GONE);
		}

		final UserLocal userLocal = user.getUserLocal();
		final ImageView imageView = (ImageView)rootView.findViewById(R.id.profile_picture);
		String avatarUrl = user.getAvatarUrl();
		String localAvatarPath = userLocal.getLocalAvatarPath();

		if(StringUtils.isNotBlank(localAvatarPath)) {
			Glide.with(context)
					.load(localAvatarPath)
					.asBitmap()
					.centerCrop()
					.into(new BitmapImageViewTarget(imageView) {
						@Override
						protected void setResource(Bitmap resource) {
							RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), resource);
							circularBitmapDrawable.setCircular(true);
							imageView.setImageDrawable(circularBitmapDrawable);
						}
					});
		} else {
			if (avatarUrl != null) {
				new DownloadImageTask(context, Collections.singletonList(user), DownloadImageTask.ImageType.AVATAR, false, new DownloadImageTask.OnImageDownloadListener() {
					@Override
					public void complete() {
						Glide.with(context)
								.load(userLocal.getLocalAvatarPath())
								.asBitmap()
								.centerCrop()
								.into(new BitmapImageViewTarget(imageView) {
									@Override
									protected void setResource(Bitmap resource) {
										RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), resource);
										circularBitmapDrawable.setCircular(true);
										imageView.setImageDrawable(circularBitmapDrawable);
									}
								});
					}
				}).execute();
			}
		}
		
		final Intent intent = new Intent(context, ProfilePictureViewerActivity.class);
		intent.putExtra(ProfilePictureViewerActivity.USER_ID, user.getId());
		
		rootView.findViewById(R.id.profile_picture).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					if (userId.equals(UserHelper.getInstance(context).readCurrentUser().getId())) {
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
										Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
										if (captureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
											// change the picture from the camera
											File f = null;
											try {
												f = MediaUtility.createMediaFile(context, ".jpg");
											} catch (IOException e) {
												Log.e(LOG_NAME, "Error creating avatar file", e);
											}
											// Continue only if the File was successfully created
											if (f != null) {
												currentMediaUri = Uri.fromFile(f);
												captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentMediaUri);
												startActivityForResult(captureIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
											}
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
	public void onMapReady(GoogleMap map) {
		if (latLng != null && icon != null) {
			map.addMarker(new MarkerOptions()
					.position(latLng)
					.icon(icon));

			map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
		}
	}

	private void setProfilePicture(File file, ImageView imageView) {
		if(file.exists() && file.canRead()) {
			try {
				imageView.setImageBitmap(MediaUtility.resizeAndRoundCorners(BitmapFactory.decodeStream(new FileInputStream(file)), 150));
			} catch(Exception e) {
				Log.e(LOG_NAME, "Problem setting profile picture.");
			}
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

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

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Bitmap.Config.RGB_565;
			options.inSampleSize = 2;
			Bitmap bitmap = BitmapFactory.decodeFile(new File(filePath).getAbsolutePath(), options);
			Bitmap b = MediaUtility.resizeAndRoundCorners(bitmap, 150);
			try {
				b = MediaUtility.orientBitmap(b, new File(filePath).getAbsolutePath(), false);
			} catch (Exception e) {
				Log.e(LOG_NAME, "failed to rotate image", e);
			}

			ImageView iv = (ImageView)getActivity().findViewById(R.id.profile_picture);
			iv.setImageBitmap(b);
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
}
