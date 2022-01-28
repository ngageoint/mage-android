package mil.nga.giat.mage.sdk.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.Compatibility;
import mil.nga.giat.mage.sdk.http.resource.ApiResource;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by wnewman on 1/4/18.
 */

public class ServerApi {
    public interface ServerApiListener {
        void onApi(boolean valid, Exception error);
    }

    private static final String LOG_NAME = ServerApi.class.getName();
    private static final String SERVER_API_PREFERENCE_PREFIX_REGEX = "^g[A-Z]\\w*";
    private static final String SERVER_API_PREFERENCE_PREFIX = "g";
    private static final String SERVER_API_AUTHENTICATION_STRATEGIES_KEY = "authenticationStrategies";

    private final Context context;

    public ServerApi(Context context) {
        this.context = context;
    }

    public void validateServerApi(final String url, final ServerApiListener apiListener) {
        ApiResource apiResource = new ApiResource(context);

        apiResource.getApi(url, new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        JSONObject apiJson = new JSONObject(response.body().string());
                        removeValues();
                        populateValues(SERVER_API_PREFERENCE_PREFIX, apiJson);
                        parseAuthenticationStrategies(apiJson);

                        String message = null;
                        boolean isValid = isApiValid();
                        if (!isValid) {
                            message = "Application is not compatible with server. Please upgrade your application or talk to your MAGE administrator";
                        }
                        apiListener.onApi(isValid, null);
                    } else {
                        apiListener.onApi(false, null);
                    }
                } catch (Exception e) {
                    Log.e(LOG_NAME, "Problem reading server api settings: " + url, e);
                    apiListener.onApi(false, e);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, final Throwable t) {
                Log.e(LOG_NAME, "Problem reading server api settings: " + url, t);
                apiListener.onApi(false, new Exception(t));
            }
        });
    }

    private boolean isApiValid() {
        // check versions
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        Integer majorVersion = null;
        if (sharedPreferences.contains(context.getString(R.string.serverVersionMajorKey))) {
            majorVersion = sharedPreferences.getInt(context.getString(R.string.serverVersionMajorKey), 0);
        }

        Integer minorVersion = null;
        if (sharedPreferences.contains(context.getString(R.string.serverVersionMinorKey))) {
            minorVersion = sharedPreferences.getInt(context.getString(R.string.serverVersionMinorKey), 0);
        }

        return Compatibility.Companion.isCompatibleWith(majorVersion, minorVersion);
    }

    private void parseAuthenticationStrategies(JSONObject json) {
        try {
            Object value = json.get(SERVER_API_AUTHENTICATION_STRATEGIES_KEY);
            if (value instanceof  JSONObject) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(context.getResources().getString(R.string.authenticationStrategiesKey), value.toString());
                editor.apply();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
                    Log.i(LOG_NAME, keyString + " is " + sharedPreferences.getAll().get(keyString) + ".  Setting it to " + value + ".");

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
                            Log.e(LOG_NAME, keyString + " with value " + value + " is not of valid number type. Skipping this key-value pair.");
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
                            Log.e(LOG_NAME, keyString + " with value " + value + " is not of valid type. Skipping this key-value pair.");
                        }
                    }

                    editor.commit();
                }
            } catch (JSONException je) {
                je.printStackTrace();
            }
        }
    }

    private void removeValues() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (String key : sharedPreferences.getAll().keySet()) {
            if (key.matches(SERVER_API_PREFERENCE_PREFIX_REGEX)) {
                editor.remove(key);
            }
        }

        editor.commit();
    }
}
