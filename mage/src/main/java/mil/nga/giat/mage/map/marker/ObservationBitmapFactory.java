package mil.nga.giat.mage.map.marker;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.common.io.Closeables;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.fetch.EventIconFetchIntentService;

public class ObservationBitmapFactory {
	
	private static final String LOG_NAME = ObservationBitmapFactory.class.getName();

	private static final String DEFAULT_ASSET = "markers/default.png";
	private static final String ICON_PREFIX = "icon.";

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public static Bitmap bitmap(Context context, Observation observation) {
		InputStream iconStream = getIconStream(context, observation);

		// scale the image to a good size
		Bitmap bitmap = BitmapFactory.decodeStream(iconStream);
		Integer maxDimension = Math.max(bitmap.getWidth(), bitmap.getHeight());
		float density = context.getResources().getDisplayMetrics().xdpi; //context.getResources().getDisplayMetrics().densityDpi;
		double scale = (density/3.5) / maxDimension;
		int outWidth = Double.valueOf(scale*Integer.valueOf(bitmap.getWidth()).doubleValue()).intValue();
		int outHeight = Double.valueOf(scale*Integer.valueOf(bitmap.getHeight()).doubleValue()).intValue();
		bitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, true);

		Closeables.closeQuietly(iconStream);

		return bitmap;
	}

	public static BitmapDescriptor bitmapDescriptor(Context context, Observation observation) {
		Bitmap bitmap = bitmap(context, observation);
		return BitmapDescriptorFactory.fromBitmap(bitmap);
	}

	private static final FileFilter fileFilter = new FileFilter() {
	    @Override
	    public boolean accept(File pathname) {
	        return pathname.isFile() && pathname.getName().startsWith(ICON_PREFIX);
	    }
	};

	/**
	 * Figure out which icon to navigate to
	 *
	 * @param observation
	 * @return
	 */
	private static InputStream getIconStream(Context context, Observation observation) {
		InputStream iconStream = null;
		if (observation != null) {

			// make path from type and variant
			File path = new File(new File(new File(context.getFilesDir() + EventIconFetchIntentService.OBSERVATION_ICON_PATH), observation.getEvent().getRemoteId()), "icons");

			Stack<String> iconProperties = new Stack<>();

			ObservationProperty secondaryField = observation.getSecondaryField();
			if (secondaryField != null) {
				iconProperties.add(secondaryField.getValue().toString());
			}

			ObservationProperty primaryField = observation.getPrimaryField();
			if (primaryField != null) {
				iconProperties.add(primaryField.getValue().toString());
			}

			if (observation.getForms().size() > 0) {
				iconProperties.add(observation.getForms().iterator().next().getFormId().toString());
			}

			path = recurseGetIconPath(iconProperties, path, 0);

			if (path != null && path.exists() && path.isFile()) {
				try {
					iconStream = new FileInputStream(path);
				} catch (FileNotFoundException e) {
					Log.e(LOG_NAME, "Cannot find icon.", e);
				}
			}

			if (iconStream != null) {
				Log.i(LOG_NAME, "path for icon stream: " + path.getAbsolutePath());
			}
		}

		if (iconStream == null) {
			Log.i(LOG_NAME, "Could not find icon, using default " + DEFAULT_ASSET);

			try {
				iconStream = context.getAssets().open(DEFAULT_ASSET);
			} catch (IOException e) {
				Log.e(LOG_NAME, "Cannot find default icon.", e);
			}
		}

		return iconStream;
	}

	private static File recurseGetIconPath(Stack<String> iconProperties, File path, int i) {

		if (iconProperties.size() > 0) {
			String property = iconProperties.pop();
			if (property != null && path.exists()) {
				if (!property.trim().isEmpty() && new File(path, property).exists()) {
					return recurseGetIconPath(iconProperties, new File(path, property), i + 1);
				}
			}
		}

		while (path != null && path.listFiles(fileFilter) != null && path.listFiles(fileFilter).length == 0 && i >= 0) {
			path = path.getParentFile();
			i--;
		}
		if (path == null || !path.exists()) return null;

		File[] files = path.listFiles(fileFilter);
		if (files == null || files.length == 0) {
			return null;
		}

		return files[0];

	}
}