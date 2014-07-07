package mil.nga.giat.mage.map.marker;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

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

		// scale the image to a good size
		Bitmap bitmap = BitmapFactory.decodeStream(iconStream);
		Integer inLength = Math.max(bitmap.getWidth(), bitmap.getHeight());
		Integer density = context.getResources().getDisplayMetrics().densityDpi;
		Integer outLength = Math.max(context.getResources().getDisplayMetrics().widthPixels, context.getResources().getDisplayMetrics().heightPixels);
		Double scale = Double.valueOf((outLength.doubleValue()/inLength.doubleValue()))*(density.doubleValue()/Double.valueOf(DisplayMetrics.DENSITY_LOW)) * 1.0/50.0;
		int outWidth = Double.valueOf(scale*Integer.valueOf(bitmap.getWidth()).doubleValue()).intValue();
		int outHeight = Double.valueOf(scale*Integer.valueOf(bitmap.getHeight()).doubleValue()).intValue();
		bitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, true);

		return bitmap;
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
		
				if (type != null && path.exists()) {
					String typeString = type.getValue().toString();
					if (typeString != null && !typeString.trim().isEmpty() && new File(path, typeString).exists()) {
						path = new File(path, typeString);
						if (variant != null) {
							String variantString = variant.getValue().toString();
							if (variantString != null && !variantString.trim().isEmpty() && new File(path, variantString).exists()) {
								path = new File(path, variantString).listFiles()[0];
							} else {
								path = path.listFiles(fileFilter)[0];
							}
						} else {
							path = path.listFiles(fileFilter)[0];
						}
					} else {
						path = path.listFiles(fileFilter)[0];
					}
				} else {
					path = path.listFiles(fileFilter)[0];
				}

				if (path.exists() && path.isFile()) {
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
}