package mil.nga.giat.mage.map.marker;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Stack;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.http.get.MageServerGetRequests;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ObservationBitmapFactory {
	
	private static final String LOG_NAME = ObservationBitmapFactory.class.getName();

	private static final String DEFAULT_ASSET = "markers/default.png";
	private static final String TYPE_PROPERTY = "type";

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public static Bitmap bitmap(Context context, Observation observation) {
		InputStream iconStream = getIconStream(context, observation);
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inDensity = DisplayMetrics.DENSITY_XHIGH;
		options.inTargetDensity = (int) context.getResources().getDisplayMetrics().xdpi;
		return BitmapFactory.decodeStream(iconStream, null, options);
	}

	public static BitmapDescriptor bitmapDescriptor(Context context, Observation observation) {
		Bitmap bitmap = bitmap(context, observation);
		return BitmapDescriptorFactory.fromBitmap(bitmap);
	}

	private static final FileFilter fileFilter = new FileFilter() {
	    @Override
	    public boolean accept(File pathname) {
	        return pathname.isFile();
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
	
			Map<String, ObservationProperty> properties = observation.getPropertiesMap();
			// get type
			ObservationProperty type = properties.get(TYPE_PROPERTY);
			// get variantField
			String dynamicFormString = PreferenceHelper.getInstance(context).getValue(R.string.dynamicFormKey);
			JsonObject dynamicFormJson = new JsonParser().parse(dynamicFormString).getAsJsonObject();
			
			// get variant
			ObservationProperty variant = null;
			JsonElement variantField = dynamicFormJson.get("variantField");
			if(variantField != null && !variantField.isJsonNull()) {
				variant = properties.get(variantField.getAsString());
			}
	
			JsonElement jsonFormId = dynamicFormJson.get("id");
			
			if(jsonFormId != null && !jsonFormId.isJsonNull()) {
				String formId = jsonFormId.getAsString();
				// make path from type and variant
				File path = new File(new File(new File(context.getFilesDir() + MageServerGetRequests.OBSERVATION_ICON_PATH), formId), "icons");
		
				Stack<ObservationProperty> iconProperties = new Stack<ObservationProperty>();
				iconProperties.add(variant);
				iconProperties.add(type);
				
				path = recurseGetIconPath(iconProperties, path, 0);

				if (path != null && path.exists() && path.isFile()) {
					try {
						iconStream = new FileInputStream(path);
					} catch (FileNotFoundException e) {
						Log.e(LOG_NAME, "Can find icon.", e);
					}
				}
			}
		}
		if(iconStream == null) {
			try {
				iconStream = context.getAssets().open(DEFAULT_ASSET);
			} catch (IOException e) {
				Log.e(LOG_NAME, "Can find default icon.", e);
			}
		}

		return iconStream;
	}
	
	private static File recurseGetIconPath(Stack<ObservationProperty> iconProperties, File path, int i) {
		if (iconProperties.size() > 0) {
			ObservationProperty property = iconProperties.pop();
			if (property != null && path.exists()) {
				String propertyString = property.getValue().toString();
				if (propertyString != null && !propertyString.trim().isEmpty() && new File(path, propertyString).exists()) {
					return recurseGetIconPath(iconProperties, new File(path, propertyString), i + 1);
				}
			}
		}
		while (path != null && path.listFiles(fileFilter).length == 0 && i >= 0) {
			path = path.getParentFile();
			i--;
		}
		if (path == null) return null;
		
		File[] files = path.listFiles(fileFilter);
		return files.length == 0 ? null : files[0];
	}
}