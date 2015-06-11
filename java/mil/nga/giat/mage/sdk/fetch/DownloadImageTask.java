package mil.nga.giat.mage.sdk.fetch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.sdk.http.client.HttpClientManager;

/**
 * Basic task to download and save images on the filesystem.
 *
 */
public abstract class DownloadImageTask extends AsyncTask<Void, Void, Void> {

	protected static final String LOG_NAME = DownloadImageTask.class.getName();

	protected final Context context;
	protected final List<String> urls;
	protected final List<String> localFilePaths;
	protected final List<Boolean> errors = new ArrayList<Boolean>();
	protected final Boolean overwriteLocalFiles;
	private DefaultHttpClient httpclient;

	public DownloadImageTask(Context context, List<String> urls, List<String> localFilePaths, Boolean overwriteLocalFiles) {
		this.context = context;
		if (urls.size() != localFilePaths.size()) {
			throw new IllegalArgumentException("Lists must be same size");
		}

		this.urls = urls;
		this.localFilePaths = localFilePaths;
		this.overwriteLocalFiles = overwriteLocalFiles;
		this.httpclient = HttpClientManager.getInstance(context).getHttpClient();
	}

	protected Void doInBackground(Void... v) {

		for (int i = 0; i < urls.size(); i++) {
			errors.add(false);
			String urlString = urls.get(i);
			String localFilePath = localFilePaths.get(i);

			URL url = null;
			try {
				url = new URL(urlString);
			} catch (MalformedURLException mue) {
				Log.e(LOG_NAME, "Bad URL: " + urlString + ".  Not downloading.", mue);
				errors.set(i, true);
				continue;
			}

			File localFile = new File(localFilePath);
			if(!overwriteLocalFiles) {
				if (localFile.exists()) {
					Log.e(LOG_NAME, "File already exists, not downloading.");
					continue;
				}

				if (!localFile.canWrite()) {
					Log.e(LOG_NAME, "Can not write to: " + localFile + ".  Skipping.");
					continue;
				}
			} else {
				if (localFile.exists() && localFile.isFile()) {
					localFile.delete();
				}
			}

			Log.d(LOG_NAME, "Downloading " + url + " and saving to " + localFile + ".");

			int imageWidth = -1;
			int imageHeight = -1;
			InputStream in = null;

			HttpEntity entity = null;
			try {
				HttpGet get = new HttpGet(url.toURI());
				HttpResponse response = httpclient.execute(get);
				entity = response.getEntity();
				in = entity.getContent();

				BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
				bitmapOptions.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(in, null, bitmapOptions);
				imageWidth = bitmapOptions.outWidth;
				imageHeight = bitmapOptions.outHeight;
			} catch (Exception e) {
				errors.set(i, true);
				Log.e(LOG_NAME, e.getMessage(), e);
			} finally {
				try {
					if (entity != null) {
						entity.consumeContent();
					}
				} catch (Exception e) {
					Log.w(LOG_NAME, "Trouble cleaning up after request.", e);
				}
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
				entity = null;
				try {
					HttpGet get = new HttpGet(url.toURI());
					HttpResponse response = httpclient.execute(get);
					entity = response.getEntity();
					in = entity.getContent();
					Bitmap image = BitmapFactory.decodeStream(in);

					out = new FileOutputStream(localFile);
					image.compress(Bitmap.CompressFormat.PNG, 90, out);
				} catch (Exception e) {
					errors.set(i, true);
					Log.e(LOG_NAME, "Problem downloading image.");
				} finally {
					try {
						if (entity != null) {
							entity.consumeContent();
						}
					} catch (Exception e) {
						Log.w(LOG_NAME, "Trouble cleaning up after request.", e);
					}
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
				Log.w(LOG_NAME, urlString + " was too big to download.  Skipping.");
				errors.set(i, true);
			}
		}
		return null;
	}
}