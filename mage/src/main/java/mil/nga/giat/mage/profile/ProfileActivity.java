package mil.nga.giat.mage.profile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.SpannableString;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.ColorUtils;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import mil.nga.giat.mage.MageApplication;
import mil.nga.giat.mage.R;
import mil.nga.giat.mage.glide.GlideApp;
import mil.nga.giat.mage.glide.model.Avatar;
import mil.nga.giat.mage.glide.target.MarkerTarget;
import mil.nga.giat.mage.glide.transform.LocationAgeTransformation;
import mil.nga.giat.mage.login.LoginActivity;
import mil.nga.giat.mage.map.MapAndViewProvider;
import mil.nga.giat.mage.map.annotation.MapAnnotation;
import mil.nga.giat.mage.database.model.location.Location;
import mil.nga.giat.mage.data.datasource.location.LocationLocalDataSource;
import mil.nga.giat.mage.database.model.location.LocationProperty;
import mil.nga.giat.mage.database.model.event.Event;
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource;
import mil.nga.giat.mage.database.model.user.User;
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.utils.MediaUtility;
import mil.nga.giat.mage.utils.GeometryKt;
import mil.nga.giat.mage.widget.CoordinateView;
import mil.nga.sf.Point;
import mil.nga.sf.util.GeometryUtils;

@AndroidEntryPoint
public class ProfileActivity extends AppCompatActivity implements MapAndViewProvider.OnMapAndViewReadyListener {

	public enum ResultType { NAVIGATE }

	private static final String LOG_NAME = ProfileActivity.class.getName();

	private static final String CURRENT_MEDIA_PATH = "CURRENT_MEDIA_PATH";

	public static String USER_ID_EXTRA = "USER_ID_EXTRA";
	public static String RESULT_TYPE_EXTRA = "RESULT_TYPE_EXTRA";

	@Inject MageApplication application;
	@Inject UserLocalDataSource userLocalDataSource;
	@Inject EventLocalDataSource eventLocalDataSource;
	@Inject LocationLocalDataSource locationLocalDataSource;

	private String currentMediaPath;
	private User user;
	private Location location;
	private boolean isCurrentUser;
	
	private LatLng latLng = new LatLng(0, 0);
	private String coordinate;
	BottomSheetDialog profileActionDialog;
	BottomSheetDialog avatarActionsDialog;

	private SupportMapFragment mapFragment;

	private TextView phone;

	public static class TakeSelfie extends ActivityResultContract<Uri, Boolean> {
		@NonNull
		@Override
		public Intent createIntent(@NonNull Context context, @NonNull Uri input) {
			return new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
					.putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
					.putExtra(MediaStore.EXTRA_OUTPUT, input);
		}

		@Override
		public Boolean parseResult(int resultCode, @Nullable Intent result) {
			return resultCode == Activity.RESULT_OK;
		}
	}

	private final ActivityResultLauncher<String> requestPermissions =
			registerForActivityResult(new ActivityResultContracts.RequestPermission(), this::onCameraPermission);

	private final ActivityResultLauncher<Uri> getCameraAvatar =
		registerForActivityResult(new TakeSelfie(), this::onImageResult);

	private final ActivityResultLauncher<String> getGalleryAvatar =
			registerForActivityResult(new ActivityResultContracts.GetContent(), this::onDocumentResult);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_profile);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		final Context context = getApplicationContext();

		long userId = getIntent().getLongExtra(USER_ID_EXTRA, -1);
		try {
			if (userId != -1) {
				user = userLocalDataSource.read(userId);
				isCurrentUser = false;
			} else {
				user = userLocalDataSource.readCurrentUser();
				isCurrentUser = true;
			}

			Event event = eventLocalDataSource.getCurrentEvent();
			List<Location> locations = locationLocalDataSource.getUserLocations(user.getId(), event.getId(), 1, true);
			if (!locations.isEmpty()) {
				location = locations.get(0);
				Point point = GeometryUtils.getCentroid(location.getGeometry());
				latLng = new LatLng(point.getY(), point.getX());
			}
		} catch (UserException ue) {
			Log.e(LOG_NAME, "Problem finding user.", ue);
		}

		mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		new MapAndViewProvider(mapFragment).getMapAndViewAsync(this);

		final String displayName = user.getDisplayName();

		getSupportActionBar().setTitle(isCurrentUser ? "My Profile" : displayName);

		final TextView name = findViewById(R.id.display_name);
		name.setText(displayName);

		phone = findViewById(R.id.phone);
		View phoneLayout = findViewById(R.id.phone_layout);
		phoneLayout.setOnLongClickListener(v -> {
			onPhoneLongCLick(v);
			return true;
		});

		if (StringUtils.isNotBlank(user.getPrimaryPhone())) {
			SpannableString primaryPhone = new SpannableString(user.getPrimaryPhone());
			phone.setText(primaryPhone);
			phoneLayout.setVisibility(View.VISIBLE);
		} else {
			phoneLayout.setVisibility(View.GONE);
		}

		TextView email = findViewById(R.id.email);
		View emailLayout = findViewById(R.id.email_layout);
		emailLayout.setOnLongClickListener(v -> {
			onEmailLongCLick(v);
			return true;
		});

		if (StringUtils.isNotBlank(user.getEmail())) {
			SpannableString emailAddress = new SpannableString(user.getEmail());
			email.setText(emailAddress);
			emailLayout.setVisibility(View.VISIBLE);
		} else {
			emailLayout.setVisibility(View.GONE);
		}

		View locationLayout = findViewById(R.id.location_layout);
		locationLayout.setOnLongClickListener(v -> {
			onLocationLongCLick(v);
			return true;
		});

		if (location != null) {
			final CoordinateView coordinateView = findViewById(R.id.location);
			Point point = GeometryUtils.getCentroid(location.getGeometry());
			coordinateView.setLatLng(new LatLng(point.getY(), point.getX()));
			locationLayout.setVisibility(View.VISIBLE);
			coordinate = coordinateView.getText().toString();

			LocationProperty accuracyProperty = location.getPropertiesMap().get("accuracy");
			if (accuracyProperty != null) {
				float accuracy = Float.parseFloat(accuracyProperty.getValue().toString());
				final TextView accuracyView = findViewById(R.id.location_accuracy);
				accuracyView.setText(String.format(Locale.getDefault(), "GPS ± %.2f", accuracy));
			}

			LocationProperty accuracyType = location.getPropertiesMap().get("accuracy_type");
			if (accuracyType != null && "COARSE".equals(accuracyType.getValue())) {
				findViewById(R.id.location_accuracy_warning).setVisibility(View.VISIBLE);
			}
		} else {
			locationLayout.setVisibility(View.GONE);
		}

		final ImageView imageView = findViewById(R.id.avatar);
		GlideApp.with(this)
				.load(Avatar.Companion.forUser(user))
				.circleCrop()
				.placeholder(R.drawable.ic_person_gray_24dp)
				.fallback(R.drawable.ic_person_gray_24dp)
				.error(R.drawable.ic_person_gray_24dp)
				.into(imageView);

		avatarActionsDialog = new BottomSheetDialog(ProfileActivity.this);
		@SuppressLint("InflateParams") final View avatarBottomSheetView = getLayoutInflater().inflate(R.layout.dialog_avatar_actions, null);
		avatarActionsDialog.setContentView(avatarBottomSheetView);
		findViewById(R.id.avatar).setOnClickListener(v -> onAvatarClick());

		avatarBottomSheetView.findViewById(R.id.view_avatar_layout).setOnClickListener(v -> viewAvatar());

		avatarBottomSheetView.findViewById(R.id.gallery_avatar_layout).setOnClickListener(v -> updateAvatarFromGallery());

		avatarBottomSheetView.findViewById(R.id.camera_avatar_layout).setOnClickListener(v -> onCameraAction());

		if (isCurrentUser) {
			profileActionDialog = new BottomSheetDialog(ProfileActivity.this);
			@SuppressLint("InflateParams") View sheetView = getLayoutInflater().inflate(R.layout.fragment_profile_actions, null);
			profileActionDialog.setContentView(sheetView);

			sheetView.findViewById(R.id.change_password_layout).setOnClickListener(v -> changePassword());

			sheetView.findViewById(R.id.logout_layout).setOnClickListener(v -> logout());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(@NonNull Menu menu) {
		if (isCurrentUser) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.profile_menu, menu);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				this.finish();
				return true;
			case R.id.profile_actions:
				profileActionDialog.show();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void changePassword() {
		Intent intent = new Intent(this, ChangePasswordActivity.class);
		startActivity(intent);
		profileActionDialog.cancel();
	}

	private void viewAvatar() {
		Intent intent = ProfilePictureViewerActivity.Companion.intent(getApplicationContext(), user);
		startActivity(intent);
		avatarActionsDialog.cancel();
	}

	private void onCameraAction() {
		requestPermissions.launch(Manifest.permission.CAMERA);
	}

	private void updateAvatarFromCamera() {
		File file = new File(
			getExternalFilesDir(Environment.DIRECTORY_PICTURES),
			UUID.randomUUID().toString() + ".jpg"
		);
		currentMediaPath = file.getAbsolutePath();

		Uri uri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".fileprovider", file);

		getCameraAvatar.launch(uri);

		avatarActionsDialog.cancel();
	}

	private void updateAvatarFromGallery() {
		getGalleryAvatar.launch("image/*");
		avatarActionsDialog.cancel();
	}

	private void logout() {
		application.onLogout(true);
		Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
		finish();
	}

	private void onAvatarClick() {
		try {
			if (isCurrentUser) {
				avatarActionsDialog.show();
			} else {
				Intent intent = ProfilePictureViewerActivity.Companion.intent(getApplicationContext(), user);
				startActivityForResult(intent, 1);
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem setting profile picture.");
		}
	}

	@Override
	public void onMapAndViewReady(GoogleMap map) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		map.setMapType(preferences.getInt(getString(R.string.baseLayerKey), getResources().getInteger(R.integer.baseLayerDefaultValue)));

		int dayNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
		if (dayNightMode == Configuration.UI_MODE_NIGHT_NO) {
			map.setMapStyle(null);
		} else {
			map.setMapStyle(MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.map_theme_night));
		}

		if (latLng != null) {
			Marker marker = map.addMarker(new MarkerOptions()
				.position(latLng)
				.visible(false));

			if (location != null) {
				marker.setTag(location.getId());
			}

			LocationAgeTransformation transformation = new LocationAgeTransformation(application, location.getTimestamp().getTime());

			MapAnnotation<Long> feature = MapAnnotation.Companion.fromUser(user, location);
			Glide.with(this)
					.asBitmap()
					.load(feature)
					.transform(transformation)
					.error(R.drawable.default_marker)
					.into(new MarkerTarget(getApplicationContext(), marker, 32, 32, true));

			LocationProperty accuracyProperty = location.getPropertiesMap().get("accuracy");
			if (accuracyProperty != null) {
				float accuracy = Float.parseFloat(accuracyProperty.getValue().toString());

				int color = transformation.locationColor();
				map.addCircle(new CircleOptions()
					.center(latLng)
					.radius(accuracy)
					.fillColor(ColorUtils.setAlphaComponent(color, (int) (256 * .20)))
					.strokeColor(ColorUtils.setAlphaComponent(color, (int) (256 * .87)))
					.strokeWidth(2.0f));

				double latitudePadding = (accuracy / 111325);
				LatLngBounds bounds = new LatLngBounds(
						new LatLng(latLng.latitude - latitudePadding, latLng.longitude),
						new LatLng(latLng.latitude + latitudePadding, latLng.longitude));

				int minDimension = Math.min(mapFragment.getView().getWidth(), mapFragment.getView().getHeight());
				int padding = (int) Math.floor(minDimension / 5f);
				map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
			} else {
				map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
			}
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(CURRENT_MEDIA_PATH, currentMediaPath);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		currentMediaPath  = savedInstanceState.getString(CURRENT_MEDIA_PATH);
	}

	private void onCameraPermission(Boolean result) {
		if (!result && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
			new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
					.setTitle(getResources().getString(R.string.camera_access_title))
					.setMessage(getResources().getString(R.string.camera_access_message))
					.setPositiveButton(R.string.settings, (dialog, which) -> {
						Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
						intent.setData(Uri.fromParts("package", getApplicationContext().getPackageName(), null));
						startActivity(intent);
					})
					.setNegativeButton(android.R.string.cancel, null)
					.show();
		} else if (result) {
			updateAvatarFromCamera();
		}
	}

	private void onImageResult(Boolean result) {
		if (result) {
			if (currentMediaPath != null) {
				final Context context = getApplicationContext();
				try {
					user = userLocalDataSource.setAvatarPath(user, currentMediaPath);
				} catch (UserException e) {
					Log.e(LOG_NAME, "Error setting local avatar path", e);
				}

				final ImageView iv = findViewById(R.id.avatar);
				GlideApp.with(context)
						.load(Avatar.Companion.forUser(user))
						.circleCrop()
						.into(iv);

				AvatarSyncWorker.Companion.scheduleWork(getApplicationContext());
			}
		}

		currentMediaPath = null;
	}

	private void onDocumentResult(Uri uri) {
		try {
			File avatarFile = MediaUtility.copyMediaFromUri(this, uri);
			String filePath = avatarFile.getAbsolutePath();

			final Context context = getApplicationContext();
			try {
				user = userLocalDataSource.setAvatarPath(user, filePath);
			} catch (UserException e) {
				Log.e(LOG_NAME, "Error setting local avatar path", e);
			}

			final ImageView iv = findViewById(R.id.avatar);
			GlideApp.with(context)
					.load(Avatar.Companion.forUser(user))
					.circleCrop()
					.into(iv);

			AvatarSyncWorker.Companion.scheduleWork(getApplicationContext());
		} catch (IOException e) {
			Log.e(LOG_NAME, "Error copying gallery file for avatar to local storage", e);
		}
	}

	public void onLocationClick(View view) {
		new AlertDialog.Builder(this)
				.setTitle(getResources().getString(R.string.navigation_choice_title))
				.setItems(R.array.navigationOptions, (dialog, which) -> {
					switch (which) {
						case 0: {
							Intent intent = new Intent(Intent.ACTION_VIEW, GeometryKt.googleMapsUri(location.getGeometry()));
							startActivity(intent);
							break;
						}
						case 1: {
							Intent intent = new Intent();
							intent.putExtra(USER_ID_EXTRA, user.getId());
							intent.putExtra(RESULT_TYPE_EXTRA, ResultType.NAVIGATE);
							setResult(Activity.RESULT_OK, intent);
							finish();
						}
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void onLocationLongCLick(final View view) {
		String[] items = {"Go to location", "Copy to clipboard"};
		View titleView = getLayoutInflater().inflate(R.layout.alert_primary_title, null);
		TextView title = titleView.findViewById(R.id.alertTitle);
		title.setText(coordinate);
		ImageView icon = titleView.findViewById(R.id.icon);
		icon.setImageResource(R.drawable.ic_place_white_24dp);

		AlertDialog dialog = new AlertDialog.Builder(this)
				.setCustomTitle(titleView)
				.setItems(items, (dialog1, item) -> {
					if (item == 0) {
						onLocationClick(view);
					} else {
						ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						ClipData clip = ClipData.newPlainText("Location", coordinate);
						clipboard.setPrimaryClip(clip);
					}
				})
				.create();

		dialog.show();
	}

	public void onPhoneClick(View view) {
		try {
			Intent callIntent = new Intent(Intent.ACTION_DIAL);
			callIntent.setData(Uri.parse("tel:" + phone.getText().toString()));
			startActivity(callIntent);
		} catch (ActivityNotFoundException ae) {
			Toast.makeText(ProfileActivity.this, "Could not call user.", Toast.LENGTH_SHORT).show();
			Log.e(LOG_NAME, "Could not call user.", ae);
		}
	}

	private void onPhoneLongCLick(final View view) {
		String[] items = {"Call", "Send a message", "Copy to clipboard", };
		View titleView = getLayoutInflater().inflate(R.layout.alert_primary_title, null);
		TextView title = titleView.findViewById(R.id.alertTitle);
		title.setText(user.getPrimaryPhone());
		ImageView icon = titleView.findViewById(R.id.icon);
		icon.setImageResource(R.drawable.ic_phone_white_24dp);

		AlertDialog dialog = new AlertDialog.Builder(this)
				.setCustomTitle(titleView)
				.setItems(items, (dialog1, item) -> {
					if (item == 0) {
						onPhoneClick(view);
					} else if (item == 1) {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse("sms:" + user.getPrimaryPhone()));
						startActivity(intent);
					} else {
						ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						ClipData clip = ClipData.newPlainText("Phone", user.getPrimaryPhone());
						clipboard.setPrimaryClip(clip);
					}
				})
				.create();

		dialog.show();
	}

	public void onEmailClick(View view) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("mailto:" + user.getEmail()));
		startActivity(intent);
	}

	private void onEmailLongCLick(final View view) {
		String[] items = {"Email", "Copy to clipboard"};
		View titleView = getLayoutInflater().inflate(R.layout.alert_primary_title, null);
		TextView title = titleView.findViewById(R.id.alertTitle);
		title.setText(user.getEmail());
		ImageView icon = titleView.findViewById(R.id.icon);
		icon.setImageResource(R.drawable.ic_email_white_24dp);

		new AlertDialog.Builder(this)
		.setCustomTitle(titleView)
			.setItems(items, (dialog, item) -> {
				if (item == 0) {
					onEmailClick(view);
				} else {
					ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("Email", user.getEmail());
					clipboard.setPrimaryClip(clip);
				}
			})
			.create()
			.show();
	}
}
