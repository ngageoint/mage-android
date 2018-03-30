package mil.nga.giat.mage.profile;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.design.widget.BottomSheetDialog;
import android.support.v13.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.nga.giat.mage.BuildConfig;
import mil.nga.giat.mage.MAGE;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.map.marker.LocationBitmapFactory;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.datastore.user.UserLocal;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.fetch.DownloadImageTask;
import mil.nga.giat.mage.sdk.profile.UpdateProfileTask;
import mil.nga.giat.mage.sdk.utils.MediaUtility;
import mil.nga.wkb.geom.Geometry;
import mil.nga.wkb.geom.Point;
import mil.nga.wkb.util.GeometryUtils;

public class ProfileActivity extends AppCompatActivity implements OnMapReadyCallback {

	private static final String LOG_NAME = ProfileActivity.class.getName();

	private static final String CURRENT_MEDIA_PATH = "CURRENT_MEDIA_PATH";

	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int GALLERY_ACTIVITY_REQUEST_CODE = 200;
	private static final int PERMISSIONS_REQUEST_CAMERA = 300;
	private static final int PERMISSIONS_REQUEST_STORAGE = 400;

	public static String USER_ID = "USER_ID";
	
	private String currentMediaPath;
	private User user;
	private boolean isCurrentUser;
	
	private MapView mapView;
	private LatLng latLng = new LatLng(0, 0);
	private BitmapDescriptor icon;
	BottomSheetDialog profileActionDialog;
	BottomSheetDialog avatarActionsDialog;

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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.fragment_profile);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		final Context context = getApplicationContext();

		String userToLoad = getIntent().getStringExtra(USER_ID);
		try {
			user = UserHelper.getInstance(context).readCurrentUser();

			if (userToLoad != null) {
				user = UserHelper.getInstance(context).read(userToLoad);
				isCurrentUser = false;
			} else {
				isCurrentUser = true;
			}

			Event event = EventHelper.getInstance(context).getCurrentEvent();
			List<Location> lastLocation = LocationHelper.getInstance(context).getUserLocations(user.getId(), event.getId(), 1, true);
			if (!lastLocation.isEmpty()) {
				Geometry geo = lastLocation.get(0).getGeometry();
				Point point = GeometryUtils.getCentroid(geo);
				latLng = new LatLng(point.getY(), point.getX());
				icon = LocationBitmapFactory.bitmapDescriptor(context, lastLocation.get(0), user);
			}
		} catch (UserException ue) {
			Log.e(LOG_NAME, "Problem finding user.", ue);
		}

		MapsInitializer.initialize(context);

		mapView = (MapView) findViewById(R.id.mapView);
		mapView.onCreate(savedInstanceState);
		mapView.getMapAsync(this);

		final String displayName = user.getDisplayName();

		getSupportActionBar().setTitle(isCurrentUser ? "My Profile" : displayName);

		final TextView realNameTextView = (TextView) findViewById(R.id.realName);
		realNameTextView.setText(displayName);
		final TextView phoneTextView = (TextView) findViewById(R.id.phone);

		View phoneLayout = findViewById(R.id.phone_layout);
		if (StringUtils.isNotBlank(user.getPrimaryPhone())) {
			SpannableString primaryPhone = new SpannableString(user.getPrimaryPhone());
			phoneTextView.setText(primaryPhone);
			phoneTextView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						Intent callIntent = new Intent(Intent.ACTION_DIAL);
						callIntent.setData(Uri.parse("tel:" + phoneTextView.getText().toString()));
						startActivity(callIntent);
					} catch (ActivityNotFoundException ae) {
						Toast.makeText(ProfileActivity.this, "Could not call user.", Toast.LENGTH_SHORT).show();
						Log.e(LOG_NAME, "Could not call user.", ae);
					}
				}
			});
			phoneLayout.setVisibility(View.VISIBLE);
		} else {
			phoneLayout.setVisibility(View.GONE);
		}

		final TextView emailTextView = (TextView) findViewById(R.id.email);
		View emailLayout = findViewById(R.id.email_layout);
		if (StringUtils.isNotBlank(user.getEmail())) {
			SpannableString emailAddress = new SpannableString(user.getEmail());
			emailTextView.setText(emailAddress);
			emailTextView.setOnClickListener(new View.OnClickListener() {
				 @Override
				 public void onClick(View v) {
					 Intent emailIntent = new Intent(Intent.ACTION_SEND);
					 emailIntent.setType("message/rfc822");
					 emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailTextView.getText().toString()});
					 emailIntent.putExtra(Intent.EXTRA_SUBJECT, "MAGE");
					 try {
						 startActivity(Intent.createChooser(emailIntent, "Send mail..."));
					 } catch (ActivityNotFoundException ae) {
						 Toast.makeText(ProfileActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
						 Log.e(LOG_NAME, "Could not email user.", ae);
					 }
				 }
			 });
			emailLayout.setVisibility(View.VISIBLE);
		} else {
			emailLayout.setVisibility(View.GONE);
		}

		final ImageView imageView = (ImageView) findViewById(R.id.profile_picture);
		String avatarUrl = user.getAvatarUrl();
		String localAvatarPath = user.getUserLocal().getLocalAvatarPath();

		Drawable defaultPersonIcon = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_person_white_48dp));
		DrawableCompat.setTint(defaultPersonIcon, ContextCompat.getColor(context, R.color.icon));
		DrawableCompat.setTintMode(defaultPersonIcon, PorterDuff.Mode.SRC_ATOP);
		imageView.setImageDrawable(defaultPersonIcon);

		if (StringUtils.isNotBlank(localAvatarPath)) {
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

						@Override
						public void onLoadFailed(Exception e, Drawable errorDrawable) {
							downloadAvatar(context, imageView);
						}
					});
		} else {
			if (avatarUrl != null) {
				downloadAvatar(context, imageView);
			}
		}

		avatarActionsDialog = new BottomSheetDialog(ProfileActivity.this);
		final View avatarBottomSheetView = getLayoutInflater().inflate(R.layout.dialog_avatar_actions, null);
		avatarActionsDialog.setContentView(avatarBottomSheetView);
		findViewById(R.id.profile_picture).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onAvatarClick();
			}
		});

		avatarBottomSheetView.findViewById(R.id.view_avatar_layout).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				viewAvatar();
			}
		});

		avatarBottomSheetView.findViewById(R.id.gallery_avatar_layout).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateAvatarFromGallery();
			}
		});

		avatarBottomSheetView.findViewById(R.id.camera_avatar_layout).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateAvatarFromCamera();
			}
		});

		View profileActions = findViewById(R.id.profile_actions_button);
		profileActions.setVisibility(isCurrentUser ? View.VISIBLE : View.GONE);
		if (isCurrentUser) {
			profileActionDialog = new BottomSheetDialog(ProfileActivity.this);
			View sheetView = getLayoutInflater().inflate(R.layout.fragment_profile_actions, null);
			profileActionDialog.setContentView(sheetView);
			profileActions.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					profileActionDialog.show();
				}
			});

			sheetView.findViewById(R.id.change_password_layout).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					changePassword();
				}
			});

			sheetView.findViewById(R.id.logout_layout).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					logout();
				}
			});
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				this.finish();
				return true;
		}
		return true;
	}

	private void changePassword() {
		Intent intent = new Intent(this, ChangePasswordActivity.class);
		startActivity(intent);
		profileActionDialog.cancel();
	}

	private void viewAvatar() {
		Intent intent = new Intent(getApplicationContext(), ProfilePictureViewerActivity.class);
		intent.putExtra(ProfilePictureViewerActivity.USER_ID, user.getId());
		startActivity(intent);
		avatarActionsDialog.cancel();
	}

	private void updateAvatarFromGallery() {
		if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(ProfileActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_STORAGE);
		} else {
			launchGalleryIntent();
		}
		avatarActionsDialog.cancel();
	}

	private void updateAvatarFromCamera() {
		if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
			ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(ProfileActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CAMERA);
		} else {
			launchCameraIntent();
		}
		avatarActionsDialog.cancel();
	}

	private void logout() {
		((MAGE) getApplication()).onLogout(true, null);
		Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
		finish();
	}

	private void downloadAvatar(final Context context, final ImageView imageView) {
		new DownloadImageTask(context, Collections.singletonList(user), DownloadImageTask.ImageType.AVATAR, false, new DownloadImageTask.OnImageDownloadListener() {
			@Override
			public void complete() {
				try {
					user = UserHelper.getInstance(context).read(user.getId());
					UserLocal userLocal = user.getUserLocal();
					if (userLocal.getLocalAvatarPath() != null) {
						Glide.with(context)
								.load(userLocal.getLocalAvatarPath())
								.asBitmap()
								.fallback(R.drawable.ic_person_gray_48dp)
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
				} catch (UserException e) {
				}
			}
		}).execute();
	}

	private void onAvatarClick() {
		final Context context = getApplicationContext();
		try {
			if (isCurrentUser) {
				avatarActionsDialog.show();
			} else {
				final Intent intent = new Intent(context, ProfilePictureViewerActivity.class);
				intent.putExtra(ProfilePictureViewerActivity.USER_ID, user.getId());
				startActivityForResult(intent, 1);
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem setting profile picture.");
		}
	}

	private void launchCameraIntent() {
		try {
			File file = MediaUtility.createImageFile();
			currentMediaPath = file.getAbsolutePath();
			Uri uri = getUriForFile(file);
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
			intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
			intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
		} catch (IOException e) {
			Log.e(LOG_NAME, "Error creating video media file", e);
		}
	}

	private Uri getUriForFile(File file) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			return FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", file);
		} else {
			return Uri.fromFile(file);
		}
	}

	private void launchGalleryIntent() {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		Log.i(LOG_NAME, "build version sdk int: " + Build.VERSION.SDK_INT);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		}
		startActivityForResult(intent, GALLERY_ACTIVITY_REQUEST_CODE);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case PERMISSIONS_REQUEST_CAMERA: {
				Map<String, Integer> grants = new HashMap<>();
				grants.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
				grants.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);

				for (int i = 0; i < grantResults.length; i++) {
					grants.put(permissions[i], grantResults[i]);
				}

				if (grants.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
					grants.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
					launchCameraIntent();
				} else if ((!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) && grants.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) ||
						(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) && grants.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) ||
						!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

					// User denied camera or storage with never ask again.  Since they will get here
					// by clicking the camera button give them a dialog that will
					// guide them to settings if they want to enable the permission
					showDisabledPermissionsDialog(
							getResources().getString(R.string.camera_access_title),
							getResources().getString(R.string.camera_access_message));
				}

				break;
			}
			case PERMISSIONS_REQUEST_STORAGE: {
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					launchGalleryIntent();
				} else {
					if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
						// User denied storage with never ask again.  Since they will get here
						// by clicking the gallery button give them a dialog that will
						// guide them to settings if they want to enable the permission
						showDisabledPermissionsDialog(
								getResources().getString(R.string.gallery_access_title),
								getResources().getString(R.string.gallery_access_message));
					}
				}

				break;
			}
		}
	}

	private void showDisabledPermissionsDialog(String title, String message) {
		new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
				.setTitle(title)
				.setMessage(message)
				.setPositiveButton(R.string.settings, new Dialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
						intent.setData(Uri.fromParts("package", getApplicationContext().getPackageName(), null));
						startActivity(intent);
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	@Override
	public void onMapReady(GoogleMap map) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		map.setMapType(preferences.getInt(getString(R.string.baseLayerKey), getResources().getInteger(R.integer.baseLayerDefaultValue)));

		int dayNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
		if (dayNightMode == Configuration.UI_MODE_NIGHT_NO) {
			map.setMapStyle(null);
		} else {
			map.setMapStyle(MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.map_theme_night));
		}


		if (latLng != null && icon != null) {
			map.addMarker(new MarkerOptions()
					.position(latLng)
					.icon(icon));

			map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode != AppCompatActivity.RESULT_OK) {
			return;
		}
		String filePath = null;
		switch (requestCode) {
			case CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE:
				filePath = currentMediaPath;
				File file = new File(currentMediaPath);
				MediaUtility.addImageToGallery(getApplicationContext(), Uri.fromFile(file));
				break;
			case GALLERY_ACTIVITY_REQUEST_CODE:
				List<Uri> uris = getUris(data);
				for (Uri uri : uris) {
					try {
						File avatarFile = MediaUtility.copyMediaFromUri(this, uri);
						filePath = avatarFile.getAbsolutePath();
					} catch (IOException e) {
						Log.e(LOG_NAME, "Error copying gallery file for avatar to local storage", e);
					}
				}
				break;
		}

		if (filePath != null) {
			final Context context = getApplicationContext();
			final ImageView iv = (ImageView) findViewById(R.id.profile_picture);
			Glide.with(context).load(filePath).centerCrop().into(iv);
			Glide.with(context)
					.load(filePath)
					.asBitmap()
					.fallback(R.id.profile_picture)
					.centerCrop()
					.into(new BitmapImageViewTarget(iv) {
						@Override
						protected void setResource(Bitmap resource) {
							RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), resource);
							circularBitmapDrawable.setCircular(true);
							iv.setImageDrawable(circularBitmapDrawable);
						}
					});

			try {
				UserHelper.getInstance(context).setAvatarPath(user, filePath);
			} catch (UserException e) {
				Log.e(LOG_NAME, "Error setting local avatar path", e);
			}

			UpdateProfileTask task = new UpdateProfileTask(user, this);
			task.execute(filePath);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(CURRENT_MEDIA_PATH, currentMediaPath);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		currentMediaPath  = savedInstanceState.getString(CURRENT_MEDIA_PATH);
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
		List<Uri> uris = new ArrayList<>();
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
