package mil.nga.giat.mage.sdk.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.squareup.okhttp.ResponseBody;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.http.resource.ApiResource;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by wnewman on 1/4/18.
 */

public class ServerApi {
    public interface ServerApiListener {
        void onApi(boolean valid, Exception error);
    }

    private static final String LOG_NAME = ServerApi.class.getName();
    private static final String SERVER_API_PREFERENCE_PREFIX = "g";

    private Context context;

    public ServerApi(Context context) {
        this.context = context;
    }

    public void validateServerApi(final String url, final ServerApiListener apiListener) {
        ApiResource apiResource = new ApiResource(context);

        apiResource.getApi(url, new Callback<ResponseBody>() {
            @Override
            public void onResponse(Response<ResponseBody> response, Retrofit retrofit) {
                try {
                    JSONObject apiJson = new JSONObject(response.body().string());
                    removeValues(SERVER_API_PREFERENCE_PREFIX);
                    populateValues(SERVER_API_PREFERENCE_PREFIX, apiJson);

                    String message = null;
                    boolean isValid = isApiValid();
                    if (!isValid) {
                        message = "Application is not compatible with server. Please upgrade your application or talk to your MAGE administrator";
                    }
                    apiListener.onApi(isValid, null);
                } catch (Exception e) {
                    Log.e(LOG_NAME, "Problem reading server api settings: " + url, e);
                    apiListener.onApi(false, e);
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                Log.e(LOG_NAME, "Problem reading server api settings: " + url, t);
                apiListener.onApi(false, new Exception(t));
            }
        });
    }

    private boolean isApiValid() {
        // check versions
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        Integer serverMajorVersion = null;
        if (sharedPreferences.contains(context.getString(R.string.serverVersionMajorKey))) {
            serverMajorVersion = sharedPreferences.getInt(context.getString(R.string.serverVersionMajorKey), 0);
        }

        Integer serverMinorVersion = null;
        if (sharedPreferences.contains(context.getString(R.string.serverVersionMinorKey))) {
            serverMinorVersion = sharedPreferences.getInt(context.getString(R.string.serverVersionMinorKey), 0);
        }

        return PreferenceHelper.getInstance(context).validateServerVersion(serverMajorVersion, serverMinorVersion);
    }

    /**
     * Flattens the json from the server and puts key, value pairs in the DefaultSharedPreferences
     *
     * @param sharedPreferenceName preference name
     * @param json json value
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
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
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
                            editor.putInt(keyString, (Integer) value);
                        } else if(value instanceof Short) {
                            editor.putInt(keyString, ((Short)value).intValue());
                        } else {
                            Log.e(LOG_NAME, keyString + " with value " + String.valueOf(value) + " is not of valid number type. Skipping this key-value pair.");
                        }
                    } else if(value instanceof Boolean) {
                        editor.putBoolean(keyString, (Boolean) value);
                    } else if(value instanceof String) {
                        editor.putString(keyString, (String) value);
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
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (String key : sharedPreferences.getAll().keySet()) {
            if (key.startsWith(sharedPreferenceName)) {
                editor.remove(key);
            }
        }

        editor.commit();
    }
}
