package mil.nga.giat.mage.sdk.login;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.exceptions.LoginException;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.fetch.InitialFetchIntentService;
import mil.nga.giat.mage.sdk.gson.deserializer.UserDeserializer;
import mil.nga.giat.mage.sdk.http.client.HttpClientManager;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.utils.DateFormatFactory;
import mil.nga.giat.mage.sdk.utils.DeviceUuidFactory;
import mil.nga.giat.mage.sdk.utils.PasswordUtility;

/**
 * Performs login to specified server with username and password. TODO: Should
 * this also handle device registration?  TODO: throw {@link LoginException}
 * 
 * @author wiedemannse
 * 
 */
public class FormAuthLoginTask extends AbstractAccountTask {

	private static final String LOG_NAME = FormAuthLoginTask.class.getName();
    private DateFormat iso8601Format = DateFormatFactory.ISO8601();
	
	public FormAuthLoginTask(AccountDelegate delegate, Context context) {
		super(delegate, context);
	}

    /**
	 * Called from execute
	 * @param params
	 *            Should contain username, password, and serverURL; in that
	 *            order.
	 * @return On success, {@link AccountStatus#getAccountInformation()}
	 *         contains the user's token
	 */
	@Override
	protected AccountStatus doInBackground(String... params) {
		return login(params);
	}
	
	private AccountStatus login(String... params) {
		// get inputs
		String username = params[0];
		String password = params[1];
		String serverURL = params[2];

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);

        // Make sure you have connectivity
		if (!ConnectivityUtility.isOnline(mApplicationContext)) {
            // disconnected login?
			try {
				String oldUsername = PreferenceHelper.getInstance(mApplicationContext).getValue(R.string.usernameKey);
				String serverURLPref = PreferenceHelper.getInstance(mApplicationContext).getValue(R.string.serverURLKey);
				String oldPasswordHash = PreferenceHelper.getInstance(mApplicationContext).getValue(R.string.passwordHashKey);
				if (oldUsername != null && oldPasswordHash != null && !oldPasswordHash.trim().isEmpty()) {
                    if(oldUsername.equals(username) && serverURL.equals(serverURLPref) && PasswordUtility.check(password, oldPasswordHash)) {
                        // put the token expiration information in the shared preferences
                        long tokenExpirationLength = sharedPreferences.getLong(mApplicationContext.getString(R.string.tokenExpirationLengthKey), 0);
                        Date tokenExpiration = new Date(System.currentTimeMillis() + tokenExpirationLength);
                        sharedPreferences.edit().putString(mApplicationContext.getString(R.string.tokenExpirationDateKey), iso8601Format.format(tokenExpiration)).commit();

                        return new AccountStatus(AccountStatus.Status.DISCONNECTED_LOGIN);
                    } else {
                        return new AccountStatus(AccountStatus.Status.FAILED_LOGIN);
                    }
				}
			} catch (Exception e) {
                Log.e(LOG_NAME, "Could not hash password", e);
			}

            List<Integer> errorIndices = new ArrayList<Integer>();
            errorIndices.add(2);
            List<String> errorMessages = new ArrayList<String>();
            errorMessages.add("No connection.");
			return new AccountStatus(AccountStatus.Status.FAILED_LOGIN, errorIndices, errorMessages);
		}

		String uuid = new DeviceUuidFactory(mApplicationContext).getDeviceUuid().toString();
		if (uuid == null) {
			List<Integer> errorIndices = new ArrayList<Integer>();
			errorIndices.add(2);
			List<String> errorMessages = new ArrayList<String>();
			errorMessages.add("Problem generating device uuid");
			return new AccountStatus(AccountStatus.Status.FAILED_LOGIN, errorIndices, errorMessages);
		}

		// is server a valid URL? (already checked username and password)
		try {
			URL sURL = new URL(serverURL);
			
			// Make sure host exists
			try {
				if (!ConnectivityUtility.isResolvable(sURL.getHost())) {
					List<Integer> errorIndices = new ArrayList<Integer>();
					errorIndices.add(2);
					List<String> errorMessages = new ArrayList<String>();
					errorMessages.add("Bad hostname");
					return new AccountStatus(AccountStatus.Status.FAILED_LOGIN, errorIndices, errorMessages);
				}
			} catch (Exception e) {
				List<Integer> errorIndices = new ArrayList<Integer>();
				errorIndices.add(2);
				List<String> errorMessages = new ArrayList<String>();
				errorMessages.add("Bad hostname");
				return new AccountStatus(AccountStatus.Status.FAILED_LOGIN, errorIndices, errorMessages);
			}
			
			try {
				PreferenceHelper.getInstance(mApplicationContext).readRemoteApi(sURL);
			} catch (Exception e) {
				List<Integer> errorIndices = new ArrayList<Integer>();
				errorIndices.add(2);
				List<String> errorMessages = new ArrayList<String>();
				errorMessages.add("Problem connecting to server");
				return new AccountStatus(AccountStatus.Status.FAILED_LOGIN, errorIndices, errorMessages);
			}
		} catch (MalformedURLException e) {
			List<Integer> errorIndices = new ArrayList<Integer>();
			errorIndices.add(2);
			List<String> errorMessages = new ArrayList<String>();
			errorMessages.add("Bad URL");
			return new AccountStatus(AccountStatus.Status.FAILED_LOGIN, errorIndices, errorMessages);
		}

		HttpEntity entity = null;
		try {
			DefaultHttpClient httpClient = HttpClientManager.getInstance(mApplicationContext).getHttpClient();
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
			nameValuePairs.add(new BasicNameValuePair("password", password));
			nameValuePairs.add(new BasicNameValuePair("uid", uuid));
			nameValuePairs.add(new BasicNameValuePair("username", username));
			
			//adding MAGE version from AndroidManifest
			try {
				String version = mApplicationContext.getPackageManager().getPackageInfo(mApplicationContext.getPackageName(), 0).versionName;
				nameValuePairs.add(new BasicNameValuePair("mageVersion", version));
			}
			catch (NameNotFoundException nnfe) {
				Log.w(LOG_NAME, "Unable to read versionName from AndroidManifest.", nnfe);
			}			
			
			UrlEncodedFormEntity authParams = new UrlEncodedFormEntity(nameValuePairs);
			
			// If we think we need to register, go do it
			if(!sharedPreferences.getBoolean(mApplicationContext.getString(R.string.deviceRegisteredKey), false)) {
				AccountStatus.Status regStatus = registerDevice(serverURL, authParams);
				
				if (regStatus == AccountStatus.Status.SUCCESSFUL_REGISTRATION) {
					return new AccountStatus(AccountStatus.Status.SUCCESSFUL_REGISTRATION, new ArrayList<Integer>(), new ArrayList<String>());
				} else if (regStatus == AccountStatus.Status.FAILED_LOGIN) {
					return new AccountStatus(AccountStatus.Status.FAILED_LOGIN);
				}
			}
			HttpPost post = new HttpPost(new URL(new URL(serverURL), "api/login").toURI());
			post.setEntity(authParams);
			HttpResponse response = httpClient.execute(post);

			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				entity = response.getEntity();
				JSONObject json = new JSONObject(EntityUtils.toString(entity));
				
				// put the token information in the shared preferences
				Editor editor = sharedPreferences.edit();
				editor.putString(mApplicationContext.getString(R.string.tokenKey), json.getString("token").trim()).commit();
				Log.d(LOG_NAME, "Storing token: " + PreferenceHelper.getInstance(mApplicationContext).getValue(R.string.tokenKey));
				try {
                    Date tokenExpiration = iso8601Format.parse(json.getString("expirationDate").trim());
                    long tokenExpirationLength = tokenExpiration.getTime() - (new Date()).getTime();
					editor.putString(mApplicationContext.getString(R.string.tokenExpirationDateKey), iso8601Format.format(tokenExpiration)).commit();
                    editor.putLong(mApplicationContext.getString(R.string.tokenExpirationLengthKey), tokenExpirationLength).commit();
				} catch (java.text.ParseException e) {
					Log.e(LOG_NAME, "Problem parsing token expiration date.", e);
				}
				
				// initialize local active user
				try {
					JSONObject userJson = json.getJSONObject("user");
					
					// if username is different, then clear the db
					String oldUsername = PreferenceHelper.getInstance(mApplicationContext).getValue(R.string.usernameKey);
					String newUsername = userJson.getString("username");
					if (oldUsername == null || !oldUsername.equals(newUsername)) {
						DaoStore.getInstance(mApplicationContext).resetDatabase();
					}
					
					final Gson userDeserializer = UserDeserializer.getGsonBuilder(mApplicationContext);

					User user = userDeserializer.fromJson(userJson.toString(), User.class);
					if (user != null) {
						User oldUser = userHelper.read(user.getRemoteId());
						if (oldUser == null) {
							user.setCurrentUser(true);
							user.setFetchedDate(new Date());
							user = userHelper.create(user);
							Log.d(LOG_NAME, "created user with remote_id " + user.getRemoteId());
						} else {
							// TODO: perform update?
							user.setId(oldUser.getId());
							user.setCurrentUser(true);
							user.setFetchedDate(new Date());
							userHelper.update(user);
							Log.d(LOG_NAME, "updated user with remote_id " + user.getRemoteId());
						}
					}
				} catch (UserException e) {
					// for now, treat as a warning. Not a great state to be in.
					Log.w(LOG_NAME, "Unable to initialize a local Active User.");
				}

				return new AccountStatus(AccountStatus.Status.SUCCESSFUL_LOGIN, new ArrayList<Integer>(), new ArrayList<String>(), json);
			} else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
				
				entity = response.getEntity();
				entity.consumeContent();
				// Could be that the device is not registered.
				if(sharedPreferences.getBoolean(mApplicationContext.getString(R.string.deviceRegisteredKey), false)) {
					// If we think the device was registered but failed to login, try to register it again
					Editor editor = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).edit();
					editor.putBoolean(mApplicationContext.getString(R.string.deviceRegisteredKey), false);
					editor.commit();
					return login(params);
				}
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem logging in.", e);
		} finally {
			try {
				if (entity != null) {
					entity.consumeContent();
				}
			} catch (Exception e) {
			}
		}

		return new AccountStatus(AccountStatus.Status.FAILED_LOGIN);
	}
	
	private AccountStatus.Status registerDevice(String serverURL, UrlEncodedFormEntity authParams) {
		HttpEntity entity = null;
		try {
			DefaultHttpClient httpClient = HttpClientManager.getInstance(mApplicationContext).getHttpClient();
			HttpPost register = new HttpPost(new URL(new URL(serverURL), "api/devices").toURI());
			register.setEntity(authParams);
			HttpResponse response = httpClient.execute(register);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				entity = response.getEntity();
				JSONObject jsonObject = new JSONObject(EntityUtils.toString(entity));
				String token = jsonObject.getString("registered");
				if (token.equalsIgnoreCase("true")) {
					// This device has already been registered and approved, login
					Editor editor = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).edit();
					editor.putBoolean(mApplicationContext.getString(R.string.deviceRegisteredKey), true);
					editor.commit();
					return AccountStatus.Status.ALREADY_REGISTERED;
				} else {
					// device registration has been submitted
					return AccountStatus.Status.SUCCESSFUL_REGISTRATION; //new AccountStatus(AccountStatus.Status.SUCCESSFUL_REGISTRATION, new ArrayList<Integer>(), new ArrayList<String>(), jsonObject);
				}
			} else {
				entity = response.getEntity();
				String error = EntityUtils.toString(entity);
				Log.e(LOG_NAME, "Bad request.");
				Log.e(LOG_NAME, error);
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem registering device.", e);
		} finally {
			try {
				if (entity != null) {
					entity.consumeContent();
				}
			} catch (Exception e) {
			}
		}
		
		return AccountStatus.Status.FAILED_LOGIN;
	}
}
