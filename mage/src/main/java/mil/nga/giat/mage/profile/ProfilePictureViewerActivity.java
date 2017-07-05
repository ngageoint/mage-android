package mil.nga.giat.mage.profile;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Collections;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.datastore.user.UserLocal;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.fetch.DownloadImageTask;

public class ProfilePictureViewerActivity extends AppCompatActivity {
	private static final String LOG_NAME = ProfilePictureViewerActivity.class.getName();
	
	public final static String USER_ID = "USER_ID";

	private static final int MAX_DOWNLOAD_ATTEMPS = 1;

	ImageView imageView;
	View progress;
	int attempts;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.attachment_viewer);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		Long userID = getIntent().getLongExtra(USER_ID, -1);

		imageView = (ImageView)findViewById(R.id.image);
		progress = findViewById(R.id.progress);

		attempts = 0;

		if (userID >= 0) {
			try {
				final User user = UserHelper.getInstance(getApplicationContext()).read(userID);
				final UserLocal userLocal = user.getUserLocal();
				this.setTitle(user.getDisplayName());

				String localAvatarPath = userLocal.getLocalAvatarPath();

				if (StringUtils.isNotBlank(localAvatarPath)) {
					setProfilePicture(user);
				} else if (user.getAvatarUrl() != null) {
					downloadProfilePicture(user);
				} else {
					progress.setVisibility(View.GONE);
					findViewById(R.id.no_content).setVisibility(View.VISIBLE);
				}
			} catch(Exception e) {
				Log.e(LOG_NAME, "Could not set title.", e);
			}
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

	private void downloadProfilePicture(final User user) {
		if (attempts >= MAX_DOWNLOAD_ATTEMPS) {
			showErrorDialog();
			return;
		}

		attempts++;

		if (user.getAvatarUrl() != null) {
			new DownloadImageTask(getApplicationContext(), Collections.singletonList(user), DownloadImageTask.ImageType.AVATAR, true, new DownloadImageTask.OnImageDownloadListener() {
				@Override
				public void complete() {
					try {
						User updatedUser = UserHelper.getInstance(getApplicationContext()).read(user.getId());
						String avatarPath = updatedUser.getUserLocal().getLocalAvatarPath();
						if (avatarPath != null) {
							setProfilePicture(updatedUser);
						} else {
							showErrorDialog();
						}
					} catch (UserException e) {
						e.printStackTrace();
					}

				}
			}).execute();
		}
	}

	private void setProfilePicture(final User user) {
		Glide.with(this)
				.load(new File(user.getUserLocal().getLocalAvatarPath()))
				.listener(new RequestListener<File, GlideDrawable>() {
					@Override
					public boolean onException(Exception e, File model, Target<GlideDrawable> target, boolean isFirstResource) {
						downloadProfilePicture(user);
						return false;
					}

					@Override
					public boolean onResourceReady(GlideDrawable resource, File model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
						progress.setVisibility(View.GONE);
						return false;
					}
				})
				.into(imageView);
	}

	private void showErrorDialog() {
		new AlertDialog.Builder(ProfilePictureViewerActivity.this, R.style.AppCompatAlertDialogStyle)
				.setTitle("Error Downloading Avatar")
				.setMessage("MAGE could not download this users avatar.  Please try again later.")
				.setPositiveButton(android.R.string.ok, null)
				.create()
				.show();
	}
}
