package mil.nga.giat.mage.profile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;

import mil.nga.giat.mage.sdk.utils.MediaUtility;

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

	private static final String LOG_NAME = DownloadImageTask.class.getName();
	private ImageView bmImage;

	public DownloadImageTask(ImageView bmImage) {
		this.bmImage = bmImage;
	}

	protected Bitmap doInBackground(String... urls) {
		String urldisplay = urls[0];

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

		Bitmap imageToFetch = null;
		if(Math.max(imageWidth, imageHeight) <= 1024) {
			try {
				in = new java.net.URL(urldisplay).openStream();
				imageToFetch = BitmapFactory.decodeStream(in);
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
		}

		return imageToFetch;
	}

	protected void onPostExecute(Bitmap bitmap) {
		if (bitmap != null) {
			bmImage.setImageBitmap(MediaUtility.resizeAndRoundCorners(bitmap, 150));
		}
	}
}