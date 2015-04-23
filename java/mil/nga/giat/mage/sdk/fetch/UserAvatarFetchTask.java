package mil.nga.giat.mage.sdk.fetch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

public class UserAvatarFetchTask extends AsyncTask<User, Void, Void> {
	private static final String LOG_NAME = UserAvatarFetchTask.class.getName();
	
    Context context;

    public UserAvatarFetchTask(Context context) {
        this.context = context;
    }

    protected Void doInBackground(User... users) {
    	String token = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.tokenKey), null);
    	for (User user : users) {
    		Log.d(LOG_NAME, "Fetching avatar at url: " + user.getAvatarUrl());
	        String urldisplay = user.getAvatarUrl() + "?access_token=" + token;

			int imageWidth = -1;
			int imageHeight = -1;

			InputStream in = null;
			try {
				in = new java.net.URL(urldisplay).openStream();
				BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
				bitmapOptions.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(in, null, bitmapOptions);
				imageWidth = bitmapOptions.outWidth;
				imageHeight = bitmapOptions.outHeight;
			} catch (Exception e) {
				Log.e(LOG_NAME, e.getMessage());
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if(Math.max(imageWidth, imageHeight) <= 1024) {
				FileOutputStream out = null;
				try {
					in = new java.net.URL(urldisplay).openStream();
					Bitmap avatar = BitmapFactory.decodeStream(in);

					String localPath = MediaUtility.getAvatarDirectory() + "/" + user.getId();
					out = new FileOutputStream(localPath);
					avatar.compress(Bitmap.CompressFormat.PNG, 90, out);
					user.setLocalAvatarPath(localPath);
					UserHelper.getInstance(context).update(user);
				} catch (Exception e) {
					Log.e(LOG_NAME, e.getMessage());
				} finally {
					try {
						if (in != null) {
							in.close();
						}
						if (out != null) {
							out.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
    	}
    	return null;
    }
}
