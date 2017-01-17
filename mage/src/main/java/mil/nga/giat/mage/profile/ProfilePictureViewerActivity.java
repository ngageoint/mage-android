package mil.nga.giat.mage.profile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.datastore.user.UserLocal;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.fetch.DownloadImageTask;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

public class ProfilePictureViewerActivity extends Activity {
	private static final String LOG_NAME = ProfilePictureViewerActivity.class.getName();
	
	public final static String USER_ID = "USER_ID";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.attachment_viewer);
		Intent intent = getIntent();
		final ImageView imageView = (ImageView)findViewById(R.id.image);
		Long userID = intent.getLongExtra(USER_ID, -1);

		if(userID >= 0) {
			try {
				final User user = UserHelper.getInstance(getApplicationContext()).read(userID);
				final UserLocal userLocal = user.getUserLocal();
				this.setTitle(user.getDisplayName());

				String avatarUrl = user.getAvatarUrl();
				String localAvatarPath = userLocal.getLocalAvatarPath();

				if(StringUtils.isNotBlank(localAvatarPath)) {
					File f = new File(localAvatarPath);
					setProfilePicture(f, imageView);
				} else {
					if (avatarUrl != null) {
						new DownloadImageTask(getApplicationContext(), Collections.singletonList(user), DownloadImageTask.ImageType.AVATAR, false, new DownloadImageTask.OnImageDownloadListener() {
							@Override
							public void complete() {
								try {
									User updatedUser = UserHelper.getInstance(getApplicationContext()).read(user.getId());
									String avatarPath = updatedUser.getUserLocal().getLocalAvatarPath();
									if (avatarPath != null) {
										File f = new File(avatarPath);
										setProfilePicture(f, imageView);
									} else {
										new AlertDialog.Builder(ProfilePictureViewerActivity.this, R.style.AppCompatAlertDialogStyle)
												.setTitle("Error Downloading Avatar")
												.setMessage("MAGE could not download this users avatar.  Please try again later.")
												.setPositiveButton(android.R.string.ok, null)
												.create()
												.show();
									}
								} catch (UserException e) {
									e.printStackTrace();
								}


							}
						}).execute();
					}
				}
			} catch(Exception e) {
				Log.e(LOG_NAME, "Could not set title.", e);
			}
		}

//		findViewById(R.id.remove_btn).setVisibility(View.GONE);
	}

	private void setProfilePicture(File file, ImageView imageView) {
		if(file.exists() && file.canRead()) {
			try {
				imageView.setImageBitmap(MediaUtility.resizeAndRoundCorners(BitmapFactory.decodeStream(new FileInputStream(file)), 500));
			} catch(Exception e) {
				Log.e(LOG_NAME, "Problem setting profile picture.", e);
			}
		}
	}

	public void goBack(View v) {
		onBackPressed();
	}

	public void removeImage(View view) {
		throw new UnsupportedOperationException("Shouldn't be able to do this.");
	}
}
