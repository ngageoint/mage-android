package mil.nga.giat.mage.wearable;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;

import mil.nga.giat.mage.BuildConfig;

public class InitializeMAGEWearBridge {

	private static final String LOG_NAME = InitializeMAGEWearBridge.class.getName();

	public static Boolean startBridgeIfWearBuild(Context context) {
		if (BuildConfig.isWearBuild) {
			final String MAGEWearBridgeClassName = "mil.nga.giat.mage.wearable.bridge.MAGEWearBridge";

			try {
				Class<?> c = Class.forName(MAGEWearBridgeClassName);
				Method getInstanceMethod = c.getMethod("getInstance", Context.class);

				Object mageWearBridge = getInstanceMethod.invoke(null, context);

				Method startBridgeMethod = c.getMethod("startBridge");
				startBridgeMethod.invoke(mageWearBridge);

				return true;
			} catch (Exception e) {
				Log.e(LOG_NAME, MAGEWearBridgeClassName + " is missing.  Unable to start the bridge.");
				return false;
			}
		}
		return true;
	}
}