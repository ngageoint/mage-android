package mil.nga.giat.mage.sdk.preferences;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.http.client.HttpClientManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Predicate;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Loads the default configuration from the local property files, and also loads
 * the server configuration.
 * 
 * TODO: add setValue methods that provide similar functionality as getValue
 * 
 * @author wiedemannse
 * 
 */
public class PreferenceHelper {

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
	public synchronized void initialize(Boolean forceReinitialize, Integer... xmlFiles) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		if (forceReinitialize) {
			// clear the old ones
			sharedPreferences.edit().clear().commit();
		}
		Set<Integer> resourcesToLoad = new LinkedHashSet<Integer>();
		resourcesToLoad.add(R.xml.overrides);
		resourcesToLoad.add(R.xml.mdkprivatepreferences);
		resourcesToLoad.add(R.xml.mdkpublicpreferences);
		resourcesToLoad.add(R.xml.locationpreferences);
		resourcesToLoad.add(R.xml.fetchpreferences);
		
		/*final Class<R.xml> c = R.xml.class;
		final Field[] fields = c.getDeclaredFields();
		// add any other files you might have added
		for (int i = 0, max = fields.length; i < max; i++) {
		    try {
		        final int resourceId = fields[i].getInt(new R.xml());
		        resourcesToLoad.add(resourceId);
		    } catch (Exception e) {
		        continue;
		    }
		}*/
		
		// load preferences from mdk xml files first
		initializeLocal(resourcesToLoad.toArray((new Integer[0])));

		// add programmatic preferences
		Editor editor = sharedPreferences.edit();
		try {
			editor.putString(mContext.getString(R.string.buildVersionKey), mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName).commit();
		} catch (NameNotFoundException nnfe) {
			nnfe.printStackTrace();
		}

		// load other xml files
		initializeLocal(xmlFiles);
	}

	private synchronized void initializeLocal(Integer... xmlFiles) {
		for (int id : xmlFiles) {
			Log.d(LOG_NAME, "Loading resources from: " + mContext.getResources().getResourceEntryName(id));
			PreferenceManager.setDefaultValues(mContext, id, true);
		}
	}

	public synchronized void readRemoteApi(URL serverURL, Predicate<Exception> callback) {
        new RemotePreferenceColonizationApi(callback).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverURL);
	}

	private class RemotePreferenceColonizationApi extends AsyncTask<URL, Void, Exception> {

        private Predicate<Exception> callback = null;

        public RemotePreferenceColonizationApi (Predicate<Exception> callback) {
            this.callback = callback;
        }

		@Override
		protected Exception doInBackground(URL... arg0) {
			URL serverURL = arg0[0];
			return initializeApi(serverURL);
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
						Log.i(LOG_NAME, keyString + " is " + sharedPreferences.getString(keyString, "empty") + ".  Setting it to " + value.toString() + ".");
						editor.putString(keyString, value.toString()).commit();
					}
				} catch (JSONException je) {
					je.printStackTrace();
				}
			}
		}

		private Exception initializeApi(URL serverURL) {
            Exception exception = null;
			HttpEntity entity = null;
			try {
				DefaultHttpClient httpclient = HttpClientManager.getInstance(mContext).getHttpClient();
                URL apiURL = new URL(serverURL, "api");
				HttpGet get = new HttpGet(apiURL.toURI());
				HttpResponse response = httpclient.execute(get);
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					entity = response.getEntity();
					JSONObject json = new JSONObject(EntityUtils.toString(entity));
					// preface all global
					populateValues("g", json);
				} else {
					entity = response.getEntity();
					String error = EntityUtils.toString(entity);
					Log.e(LOG_NAME, "Bad request.");
					Log.e(LOG_NAME, error);
                    exception = new Exception("Bad request." + error);
				}
			} catch (Exception e) {
				Log.e(LOG_NAME, "Problem reading server api settings: " + serverURL, e);
                exception = new Exception("Problem reading server api settings: " + serverURL);
			} finally {
				try {
					if (entity != null) {
						entity.consumeContent();
					}
				} catch (Exception e) {
				}
			}
            return exception;
		}
	}

	public final String getValue(int key) {
		return getValue(key, String.class, null);
	}

	/**
	 * Use this method to get values of correct type form shared preferences.
	 * Does not work with collections yet!
	 * 
	 * @param <T>
	 * @param key
	 * @param valueType
	 * @param defaultValue
	 * @return
	 */
	public final <T extends Object> T getValue(int key, Class<T> valueType, int defaultValue) {
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		String defaultValueString = sharedPreferences.getString(mContext.getString(defaultValue), mContext.getString(defaultValue));
		Object defaultReturnValue = null;
		if (valueType.equals(String.class)) {
			defaultReturnValue = defaultValueString;
		} else {
			try {
				Method valueOfMethod = valueType.getMethod("valueOf", String.class);
				defaultReturnValue = valueOfMethod.invoke(valueType, defaultValueString);
			} catch (NoSuchMethodException nsme) {
				nsme.printStackTrace();
			} catch (IllegalAccessException iae) {
				iae.printStackTrace();
			} catch (IllegalArgumentException iae) {
				iae.printStackTrace();
			} catch (InvocationTargetException ite) {
				ite.printStackTrace();
			}
		}

		return getValue(key, valueType, (T) defaultReturnValue);
	}

	public final <T extends Object> T getValue(int key, Class<T> valueType, T defaultValue) {
		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		String stringValue = sharedPreferences.getString(mContext.getString(key), null);
		if (stringValue != null) {
			if (valueType.equals(String.class)) {
				return (T) stringValue;
			} else {
				try {
					Method valueOfMethod = valueType.getMethod("valueOf", String.class);
					Object returnValue = valueOfMethod.invoke(valueType, stringValue);
					return (T) returnValue;
				} catch (NoSuchMethodException nsme) {
					nsme.printStackTrace();
				} catch (IllegalAccessException iae) {
					iae.printStackTrace();
				} catch (IllegalArgumentException iae) {
					iae.printStackTrace();
				} catch (InvocationTargetException ite) {
					ite.printStackTrace();
				}
			}
		}

		return defaultValue;
	}
}
