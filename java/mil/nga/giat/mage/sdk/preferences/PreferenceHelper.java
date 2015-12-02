package mil.nga.giat.mage.sdk.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.base.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.retrofit.resource.ApiResource;

/**
 * Loads the default configuration from the local property files, and also loads
 * the server configuration.
 * 
 * @author wiedemanns
 * 
 */
public class PreferenceHelper implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String LOG_NAME = PreferenceHelper.class.getName();
	
	private final static String DEFAULT_DYNAMIC_FORM = "dynamic-form/default-dynamic-form.json";
	
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
		Set<Integer> resourcesToLoad = new LinkedHashSet<Integer>();

		for(Class c : xmlClasses) {
			final Field[] fields = c.getDeclaredFields();
			// add any other files you might have added
			for (int i = 0, max = fields.length; i < max; i++) {
				try {
					final int resourceId = fields[i].getInt(new R.xml());
					resourcesToLoad.add(resourceId);
				} catch (Exception e) {
					continue;
				}
			}
		}
		
		// load preferences from mdk xml files first
		initializeLocal(resourcesToLoad.toArray((new Integer[resourcesToLoad.size()])));

		// add programmatic preferences
		Editor editor = sharedPreferences.edit();
		if(!StringUtils.isBlank(newBuildVersion)) {
			editor.putString(mContext.getString(R.string.buildVersionKey), newBuildVersion).commit();
		}

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

	public synchronized void readRemoteApi(String url, Predicate<Exception> callback) {
        new RemotePreferenceColonizationApi(callback).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
	}

	public void validateServerApi(final String url, final Predicate<Exception> callback) {
		try {
			final URL serverURL = new URL(url);
			// make sure you can get to the host!
			ConnectivityUtility.isResolvable(serverURL.getHost(), new Predicate<Exception>() {
				@Override
				public boolean apply(Exception e) {
					if (e == null) {
						PreferenceHelper.getInstance(mContext).readRemoteApi(url, new Predicate<Exception>() {
							public boolean apply(Exception e) {
								if (e != null) {
									return callback.apply(new Exception("No server information"));
								} else {
									// check versions
									SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
									Integer compatibleMajorVersion = sharedPreferences.getInt(mContext.getString(R.string.compatibleVersionMajorKey), mContext.getResources().getInteger(R.integer.compatibleVersionMajorDefaultValue));
									Integer compatibleMinorVersion = sharedPreferences.getInt(mContext.getString(R.string.compatibleVersionMinorKey), mContext.getResources().getInteger(R.integer.compatibleVersionMinorDefaultValue));

									boolean hasServerMajorVersion = sharedPreferences.contains(mContext.getString(R.string.serverVersionMajorKey));
									boolean hasServerMinorVersion = sharedPreferences.contains(mContext.getString(R.string.serverVersionMinorKey));

									if (!hasServerMajorVersion || !hasServerMinorVersion) {
										return callback.apply(new Exception("No server version"));
									} else {
										int serverMajorVersion = sharedPreferences.getInt(mContext.getString(R.string.serverVersionMajorKey), 0);
										int serverMinorVersion = sharedPreferences.getInt(mContext.getString(R.string.serverVersionMinorKey), 0);

										Log.d(LOG_NAME, "server major version: " + serverMajorVersion);
										Log.d(LOG_NAME, "server minor version: " + serverMinorVersion);

										Log.d(LOG_NAME, "compatibleMajorVersion: " + compatibleMajorVersion);
										Log.d(LOG_NAME, "compatibleMinorVersion: " + compatibleMinorVersion);

										if (!compatibleMajorVersion.equals(serverMajorVersion)) {
											return callback.apply(new Exception("This app is not compatible with this server"));
										} else if (compatibleMinorVersion > serverMinorVersion) {
											return callback.apply(new Exception("This app is not compatible with this server"));
										} else {
											return callback.apply(null);
										}
									}
								}
							}
						});
					} else {
						return callback.apply(new Exception("Host does not resolve"));
					}

					return true;
				}
			});
		} catch (MalformedURLException mue) {
			callback.apply(new Exception("Bad URL"));
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Map<String, ?> sharedPreferenceMap = sharedPreferences.getAll();
		Object value = sharedPreferenceMap.get(key);
		String valueType = (value == null) ? null : value.getClass().getName();
		Log.d(LOG_NAME, "SharedPreferences changed. Now contains (key, value, type): (" + String.valueOf(key) + ", " + String.valueOf(value) + ", " + String.valueOf(valueType) + ")");
	}

	private class RemotePreferenceColonizationApi extends AsyncTask<String, Void, Exception> {

        private Predicate<Exception> callback = null;

        public RemotePreferenceColonizationApi (Predicate<Exception> callback) {
            this.callback = callback;
        }

		@Override
		protected Exception doInBackground(String... params) {
			String url = params[0];
			return initializeApi(url);
		}

        @Override
        protected void onPostExecute(Exception e) {
            super.onPostExecute(e);
            if(callback != null) {
                callback.apply(e);
            }
        }

        /**
		 * Flattens the json from the server and puts key, value pairs in the DefaultSharedPreferences
		 * 
		 * @param sharedPreferenceName
		 * @param json
		 */
		private void populateValues(String sharedPreferenceName, JSONObject json) {
			@SuppressWarnings("unchecked")
			Iterator<String> iter = json.keys();
			while (iter.hasNext()) {
				String key = iter.next();
				try {
					Object value = json.get(key);
					if (value instanceof JSONObject) {
						populateValues(sharedPreferenceName + Character.toUpperCase(key.charAt(0)) + ((key.length() > 1) ? key.substring(1) : ""), (JSONObject) value);
					} else {
						SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
						Editor editor = sharedPreferences.edit();
						String keyString = sharedPreferenceName + Character.toUpperCase(key.charAt(0)) + ((key.length() > 1) ? key.substring(1) : "");
						Log.i(LOG_NAME, keyString + " is " + String.valueOf(sharedPreferences.getAll().get(keyString)) + ".  Setting it to " + String.valueOf(value) + ".");

						if(value instanceof Number) {
							if(value instanceof Long) {
								editor.putLong(keyString, (Long)value);
							} else if(value instanceof Float) {
								editor.putFloat(keyString, (Float)value);
							} else if(value instanceof Double) {
								editor.putFloat(keyString, ((Double)value).floatValue());
							} else if(value instanceof Integer) {
								editor.putInt(keyString, (Integer)value).commit();
							} else if(value instanceof Short) {
								editor.putInt(keyString, ((Short)value).intValue());
							} else {
								Log.e(LOG_NAME, keyString + " with value " + String.valueOf(value) + " is not of valid number type. Skipping this key-value pair.");
							}
						} else if(value instanceof Boolean) {
							editor.putBoolean(keyString, (Boolean)value).commit();
						} else if(value instanceof String) {
							editor.putString(keyString, (String)value).commit();
						} else if(value instanceof Character) {
							editor.putString(keyString, Character.toString((Character)value));
						} else {
							// don't know what type this is, just use toString
							try {
								editor.putString(keyString, value.toString());
							} catch(Exception e) {
								Log.e(LOG_NAME, keyString + " with value " + String.valueOf(value) + " is not of valid type. Skipping this key-value pair.");
							}
						}

						editor.commit();
					}
				} catch (JSONException je) {
					je.printStackTrace();
				}
			}
		}

		private void removeValues(String sharedPreferenceName) {
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
			SharedPreferences.Editor editor = sharedPreferences.edit();
			for (String key : sharedPreferences.getAll().keySet()) {
				if (key.startsWith(sharedPreferenceName)) {
					editor.remove(key);
				}
			}

			editor.commit();
		}

		private Exception initializeApi(String url) {
			ApiResource apiResource = new ApiResource(mContext);
			try {
				String api = apiResource.getApi(url);
				JSONObject apiJson = new JSONObject(api);
				removeValues("g");
				populateValues("g", apiJson);
			} catch (Exception e) {
				Log.e(LOG_NAME, "Problem reading server api settings: " + url, e);
				return e;
			}

			return null;
		}
	}
}
