package mil.nga.giat.mage.sdk.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import mil.nga.giat.mage.R;

/**
 * Loads the default configuration from the local property files, and also loads
 * the server configuration.
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

		// We really should have separate preference files for each user.  Server url and
		// database version will be part of 'global' preferences and not cleared when a different user logs in
		//  preserve the server url.
		String oldServerURL = PreferenceManager.getDefaultSharedPreferences(mContext).getString(mContext.getString(R.string.serverURLKey), mContext.getString(R.string.serverURLDefaultValue));
		//  preserve the database version.
		int oldDatabaseVersion = PreferenceManager.getDefaultSharedPreferences(mContext).getInt(mContext.getString(R.string.databaseVersionKey), 0);

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
		Editor editor = sharedPreferences.edit();

		if (forceReinitialize) {
			editor.clear();
		}

		Set<Integer> resourcesToLoad = new LinkedHashSet<>();
		for (Class xmlClass : xmlClasses) {
			for (Field field : xmlClass.getDeclaredFields()) {
				if (field.getName().endsWith("preferences")) {
					try {
						resourcesToLoad.add(field.getInt(new android.R.xml()));
					} catch (Exception e) {
						Log.e(LOG_NAME, "Error loading preference file", e);
					}
				}
			}
		}
		
		// load preferences from mdk xml files first
		initializeLocal(resourcesToLoad.toArray((new Integer[resourcesToLoad.size()])));

		// add programmatic preferences

		if (!StringUtils.isBlank(newBuildVersion)) {
			editor.putString(mContext.getString(R.string.buildVersionKey), newBuildVersion);
		}

		// add back in the server url and database version
		editor
			.putString(mContext.getString(R.string.serverURLKey), oldServerURL)
			.putInt(mContext.getString(R.string.databaseVersionKey), oldDatabaseVersion)
			.apply();

		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
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

	private synchronized void initializeLocal(Integer... xmlFiles) {
		for (int id : xmlFiles) {
			Log.d(LOG_NAME, "Loading resources from: " + mContext.getResources().getResourceEntryName(id));
			PreferenceManager.setDefaultValues(mContext, id, true);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Map<String, ?> sharedPreferenceMap = sharedPreferences.getAll();
		Object value = sharedPreferenceMap.get(key);
		String valueType = (value == null) ? null : value.getClass().getName();
		Log.d(LOG_NAME, "SharedPreferences changed. Now contains (key, value, type): (" + key + ", " + value + ", " + valueType + ")");
	}
}
