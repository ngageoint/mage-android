package mil.nga.giat.mage.sdk.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import mil.nga.giat.mage.sdk.R;

/**
 * Loads the default configuration from the local property files, and also loads
 * the server configuration.
 * 
 * @author wiedemanns
 * 
 */
public class PreferenceHelper implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String LOG_NAME = PreferenceHelper.class.getName();

	private PreferenceHelper() {
	}

	private static PreferenceHelper preferenceHelper;
	private static Context mContext;

	public static PreferenceHelper getInstance(final Context context) {
		if (context == null) {
			return null;
		}
		mContext = context;
		if (preferenceHelper == null) {
			preferenceHelper = new PreferenceHelper();
		}
		return preferenceHelper;
	}

	/**
	 * Should probably be called only once to initialize the settings and
	 * properties.
	 * 
	 */
	public synchronized void initialize(Boolean forceReinitialize, final Class<?>... xmlClasses) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

		// TODO preserve the server url.
		// We really should have seperate preference files for each user.  Server url will be part
		// of the 'global' preferences and not cleared when a different user logs in
		String oldServerURL = PreferenceManager.getDefaultSharedPreferences(mContext).getString(mContext.getString(R.string.serverURLKey), mContext.getString(R.string.serverURLDefaultValue));

		String oldBuildVersion = sharedPreferences.getString(mContext.getString(R.string.buildVersionKey), null);
		String newBuildVersion = null;
		try {
			newBuildVersion = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
		} catch (NameNotFoundException nnfe) {
			Log.e(LOG_NAME , "Problem retrieving build version.", nnfe);
		}
		if(!StringUtils.isBlank(oldBuildVersion) && !StringUtils.isBlank(newBuildVersion)) {
			String oldMajorVersion = oldBuildVersion.split("\\.")[0];
			String newMajorVersion = newBuildVersion.split("\\.")[0];

			if(!oldMajorVersion.equals(newMajorVersion)) {
				forceReinitialize = true;
			}
		}

		if (forceReinitialize) {
			sharedPreferences.edit().clear().commit();
		}
		Set<Integer> resourcesToLoad = new LinkedHashSet<>();

		for (Class xmlClass : xmlClasses) {
			for (Field field : xmlClass.getDeclaredFields()) {
				if (field.getName().endsWith("preference")) {
					try {
						resourcesToLoad.add(field.getInt(new R.xml()));
					} catch (Exception e) {
						Log.e(LOG_NAME, "Error loading preference file", e);
					}
				}
			}
		}
		
		// load preferences from mdk xml files first
		initializeLocal(resourcesToLoad.toArray((new Integer[resourcesToLoad.size()])));

		// add programmatic preferences
		Editor editor = sharedPreferences.edit();
		if (!StringUtils.isBlank(newBuildVersion)) {
			editor.putString(mContext.getString(R.string.buildVersionKey), newBuildVersion).commit();
		}

		// add back in the server url
		editor.putString(mContext.getString(R.string.serverURLKey), oldServerURL).commit();

		logKeyValuePairs();
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
	}

	public void logKeyValuePairs() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		// log all preference values
		for(Map.Entry<String, ?> e : sharedPreferences.getAll().entrySet()) {
			String key = e.getKey();
			Object value = e.getValue();
			String valueType = (value == null) ? null : value.getClass().getName();
			Log.d(LOG_NAME, "SharedPreferences contains (key, value, type): (" + String.valueOf(key) + ", " + String.valueOf(value) + ", " + String.valueOf(valueType) + ")");
		}
	}

	public JSONObject getAuthenticationStrategies() {
		JSONObject strategies = new JSONObject();

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		String strategiesJson = preferences.getString(mContext.getResources().getString(R.string.authenticationStrategiesKey), null);
		if (strategiesJson != null) {
			try {
				strategies = new JSONObject(strategiesJson);
			} catch (JSONException e) {
				Log.e(LOG_NAME, "Error parsing authentication strategies", e);
			}
		}

		return  strategies;
	}

	public boolean containsLocalAuthentication() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		for (String key : sharedPreferences.getAll().keySet()) {
			if (key.startsWith("gAuthenticationStrategiesLocal")) {
				return true;
			}
		}

		return false;
	}

	public boolean containsGoogleAuthentication() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		for (String key : sharedPreferences.getAll().keySet()) {
			if (key.startsWith("gAuthenticationStrategiesGoogle")) {
				return true;
			}
		}

		return false;
	}

	private synchronized void initializeLocal(Integer... xmlFiles) {
		for (int id : xmlFiles) {
			Log.d(LOG_NAME, "Loading resources from: " + mContext.getResources().getResourceEntryName(id));
			PreferenceManager.setDefaultValues(mContext, id, true);
		}
	}

	public boolean validateServerVersion(Integer majorVersion, Integer minorVersion) {

		// check versions
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		Integer compatibleMajorVersion = sharedPreferences.getInt(mContext.getString(R.string.compatibleVersionMajorKey), mContext.getResources().getInteger(R.integer.compatibleVersionMajorDefaultValue));
		Integer compatibleMinorVersion = sharedPreferences.getInt(mContext.getString(R.string.compatibleVersionMinorKey), mContext.getResources().getInteger(R.integer.compatibleVersionMinorDefaultValue));

		if (majorVersion == null || minorVersion == null) {
			return false;
		} else {
			Log.d(LOG_NAME, "server major version: " + majorVersion);
			Log.d(LOG_NAME, "server minor version: " + minorVersion);

			Log.d(LOG_NAME, "compatibleMajorVersion: " + compatibleMajorVersion);
			Log.d(LOG_NAME, "compatibleMinorVersion: " + compatibleMinorVersion);

			if (!compatibleMajorVersion.equals(majorVersion)) {
				return false;
			} else if (compatibleMinorVersion > minorVersion) {
				return false;
			} else {
				return true;
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Map<String, ?> sharedPreferenceMap = sharedPreferences.getAll();
		Object value = sharedPreferenceMap.get(key);
		String valueType = (value == null) ? null : value.getClass().getName();
		Log.d(LOG_NAME, "SharedPreferences changed. Now contains (key, value, type): (" + String.valueOf(key) + ", " + String.valueOf(value) + ", " + String.valueOf(valueType) + ")");
	}
}
