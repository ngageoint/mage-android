package mil.nga.giat.mage.sdk.fetch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.retrofit.resource.UserResource;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

/**
 * Basic task to download and save images on the filesystem.
 *
 */
public class DownloadImageTask extends AsyncTask<Void, Void, Void> {

	public enum ImageType {
		AVATAR,
		ICON
	}

	public interface OnImageDownloadListener {
		void complete();
	}

	protected static final String LOG_NAME = DownloadImageTask.class.getName();

	protected ImageType imageType;
	protected Collection<User> users;
	protected UserResource userResource;
	protected UserHelper userHelper;
	protected boolean overwriteLocalFiles;
	protected OnImageDownloadListener listener = null;

	public DownloadImageTask(Context context, Collection<User> users, ImageType imageType, boolean overwriteLocalFiles) {
		this.users = users;
		this.imageType = imageType;
		this.overwriteLocalFiles = overwriteLocalFiles;
		this.userResource = new UserResource(context);
		this.userHelper = UserHelper.getInstance(context);
	}

	public DownloadImageTask(Context context, Collection<User> users, ImageType imageType, boolean overwriteLocalFiles, OnImageDownloadListener listener) {
		this.users = users;
		this.imageType = imageType;
		this.overwriteLocalFiles = overwriteLocalFiles;
		this.userResource = new UserResource(context);
		this.userHelper = UserHelper.getInstance(context);
		this.listener = listener;
	}

	@Override
	protected Void doInBackground(Void... params) {

		for (User user : users) {
			String localFilePath = null;
			switch (imageType) {
				case AVATAR:
					localFilePath = MediaUtility.getAvatarDirectory() + "/" + user.getId() + ".png";
					break;
				case ICON:
					localFilePath = MediaUtility.getUserIconDirectory() + "/" + user.getId() + ".png";
					break;
			}

			File localFile = new File(localFilePath);
			if(!overwriteLocalFiles) {
				if (localFile.exists()) {
					Log.e(LOG_NAME, "File already exists, not downloading.");
					continue;
				}
			} else {
				if (localFile.exists() && localFile.isFile()) {
					localFile.delete();
				}
			}

			Log.d(LOG_NAME, "Downloading icon for user " + user.getDisplayName() + " and saving to " + localFile + ".");

			int imageWidth = Integer.MAX_VALUE;
			int imageHeight = Integer.MAX_VALUE;
			InputStream in = null;

			try {
				in = imageType == ImageType.AVATAR ? userResource.getAvatar(user) : userResource.getIcon(user);

				BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
				bitmapOptions.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(in, null, bitmapOptions);
				imageWidth = bitmapOptions.outWidth;
				imageHeight = bitmapOptions.outHeight;
			} catch (Exception e) {
				Log.e(LOG_NAME, e.getMessage(), e);
				continue;
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (Math.max(imageWidth, imageHeight) <= 1024) {
				FileOutputStream out = null;
				try {
					in = imageType == ImageType.AVATAR ? userResource.getAvatar(user) : userResource.getIcon(user);
					Bitmap image = BitmapFactory.decodeStream(in);

					out = new FileOutputStream(localFile);
					image.compress(Bitmap.CompressFormat.PNG, 90, out);

					switch (imageType) {
						case AVATAR:
							user.setLocalAvatarPath(localFilePath);
							break;
						case ICON:
							user.setLocalIconPath(localFilePath);
							break;
					}

					userHelper.update(user);
				} catch (Exception e) {
					Log.e(LOG_NAME, "Problem downloading image.");
				} finally {
					try {
						if (in != null) {
							in.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						if (out != null) {
							out.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				Log.w(LOG_NAME, "User '" + user.getDisplayName() + "' avatar was too big to download.  Skipping.");
			}
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void param) {
		if (listener != null) {
			listener.complete();
		}
	}

}