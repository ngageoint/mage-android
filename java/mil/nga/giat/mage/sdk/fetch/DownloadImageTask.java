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
import mil.nga.giat.mage.sdk.http.resource.UserResource;
import mil.nga.giat.mage.sdk.utils.MediaUtility;
import mil.nga.giat.mage.sdk.utils.UserUtility;

/**
 * Basic task to download and save images on the filesystem.
 *
 */
public class DownloadImageTask extends AsyncTask<Void, Void, Void> {

	private int MAX_DIMENSION = 1024;

	public enum ImageType {
		AVATAR,
		ICON
	}

	public interface OnImageDownloadListener {
		void complete();
	}

	protected static final String LOG_NAME = DownloadImageTask.class.getName();

	protected Context context;
	protected ImageType imageType;
	protected Collection<User> users;
	protected UserResource userResource;
	protected UserHelper userHelper;
	protected boolean overwriteLocalFiles;
	protected OnImageDownloadListener listener = null;

	public DownloadImageTask(Context context, Collection<User> users, ImageType imageType, boolean overwriteLocalFiles) {
		this.context = context;
		this.users = users;
		this.imageType = imageType;
		this.overwriteLocalFiles = overwriteLocalFiles;
		this.userResource = new UserResource(context);
		this.userHelper = UserHelper.getInstance(context);
	}

	public DownloadImageTask(Context context, Collection<User> users, ImageType imageType, boolean overwriteLocalFiles, OnImageDownloadListener listener) {
		this.context = context;
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
			String newLocalFilePath = null;
			String currentLocalFilePath = null;
			switch (imageType) {
				case AVATAR:
					newLocalFilePath = MediaUtility.getAvatarDirectory(context) + "/" + user.getId() + ".png";
					currentLocalFilePath = user.getAvatarPath();
					break;
				case ICON:
					newLocalFilePath = MediaUtility.getUserIconDirectory(context) + "/" + user.getId() + ".png";
					currentLocalFilePath = user.getIconPath();
					break;
			}

			File localFile = new File(newLocalFilePath);
			if (overwriteLocalFiles || currentLocalFilePath == null) {
				if (localFile.exists() && localFile.isFile()) {
					localFile.delete();
				}
			} else {
				if (localFile.exists()) {
					Log.e(LOG_NAME, "File already exists, not downloading.");
					continue;
				}
			}

			Log.d(LOG_NAME, "Downloading icon for user " + user.getDisplayName() + " and saving to " + localFile + ".");

			InputStream in = null;

			BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
			try {
				in = imageType == ImageType.AVATAR ? userResource.getAvatar(user) : userResource.getIcon(user);

				bitmapOptions.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(in, null, bitmapOptions);
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

			FileOutputStream out = null;
			try {
				bitmapOptions.inJustDecodeBounds = false;
				bitmapOptions.inSampleSize = calculateInSampleSize(bitmapOptions);;
				in = imageType == ImageType.AVATAR ? userResource.getAvatar(user) : userResource.getIcon(user);
				Bitmap image = BitmapFactory.decodeStream(in, null, bitmapOptions);

				out = new FileOutputStream(localFile);
				image.compress(Bitmap.CompressFormat.PNG, 90, out);

				switch (imageType) {
					case AVATAR:
						userHelper.setAvatarPath(user, newLocalFilePath);
						break;
					case ICON:
						userHelper.setIconPath(user, newLocalFilePath);
						break;
				}

			} catch (Exception e) {
				Log.e(LOG_NAME, "Problem downloading image.");

				// TODO should probably create a service for this task and cancel/stop
				// the service when the user logs out.
				if (UserUtility.getInstance(context).isTokenExpired()) {
					// If we could not download the image due to token expiration
					// don't continue to try the rest
					Log.i(LOG_NAME, "Token expired stop downloading images");
					break;
				}
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
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void param) {
		if (listener != null) {
			listener.complete();
		}
	}

	private int calculateInSampleSize(BitmapFactory.Options options) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > MAX_DIMENSION || width > MAX_DIMENSION) {
			// Calculate the largest inSampleSize value that is a power of 2 and will ensure
			// height and width is smaller than the max image we can process
			while ((height / inSampleSize) >= MAX_DIMENSION && (height / inSampleSize) >= MAX_DIMENSION) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

}