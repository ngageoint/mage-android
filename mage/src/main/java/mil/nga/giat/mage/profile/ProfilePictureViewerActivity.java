package mil.nga.giat.mage.profile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
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
				this.setTitle(user.getFirstname() + " " + user.getLastname());

				String avatarUrl = user.getAvatarUrl();
				String localAvatarPath = user.getLocalAvatarPath();

				if(StringUtils.isNotBlank(localAvatarPath)) {
					File f = new File(localAvatarPath);
					setProfilePicture(f, imageView);
				} else {
					if (avatarUrl != null) {
						String localFilePath = MediaUtility.getAvatarDirectory() + "/" + user.getId() + ".png";

						DownloadImageTask avatarImageTask = new DownloadImageTask(getApplicationContext(), Collections.singletonList(avatarUrl), Collections.singletonList(localFilePath), false) {
							@Override
							protected void onPostExecute(Void aVoid) {
								if(!errors.get(0)) {
									String lap = localFilePaths.get(0);
									user.setLocalAvatarPath(lap);
									File f = new File(user.getLocalAvatarPath());
									setProfilePicture(f, imageView);
								}
							}
						};
						avatarImageTask.execute();
					}
				}
			} catch(Exception e) {
				Log.e(LOG_NAME, "Could not set title.", e);
			}
		}

		findViewById(R.id.remove_btn).setVisibility(View.GONE);
	}

	private void setProfilePicture(File file, ImageView imageView) {
		if(file.exists() && file.canRead()) {
			try {
				imageView.setImageBitmap(MediaUtility.resizeAndRoundCorners(BitmapFactory.decodeStream(new FileInputStream(file)), 500));
			} catch(Exception e) {
				Log.e(LOG_NAME, "Problem setting profile picture.");
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
