package mil.nga.giat.mage.sdk.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.base.Predicate;
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

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.gson.deserializer.UserDeserializer;
import mil.nga.giat.mage.sdk.http.client.HttpClientManager;
import mil.nga.giat.mage.sdk.utils.DateFormatFactory;
import mil.nga.giat.mage.sdk.utils.DeviceUuidFactory;
import mil.nga.giat.mage.sdk.utils.PasswordUtility;

/**
 * Performs login to specified server with username and password.
 *
 * @author wiedemanns
 */
public class FormAuthLoginTask extends AbstractAccountTask {

	private static final String LOG_NAME = FormAuthLoginTask.class.getName();
	private DateFormat iso8601Format = DateFormatFactory.ISO8601();

	private volatile AccountStatus callbackStatus = null;

	public FormAuthLoginTask(AccountDelegate delegate, Context context) {
		super(delegate, context);
	}

	/**
	 * Called from execute
	 *
	 * @param params Should contain username, password, and serverURL; in that
	 *               order.
	 * @return On success, {@link AccountStatus#getAccountInformation()}
	 * contains the user's token
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
		Boolean needToRegisterDevice = Boolean.valueOf(params[3]);

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);

		// Make sure you have connectivity
		if (!ConnectivityUtility.isOnline(mApplicationContext)) {
			// try disconnected login
			try {
				String oldUsername = sharedPreferences.getString(mApplicationContext.getString(R.string.usernameKey), mApplicationContext.getString(R.string.usernameDefaultValue));
				String serverURLPref = sharedPreferences.getString(mApplicationContext.getString(R.string.serverURLKey), mApplicationContext.getString(R.string.serverURLDefaultValue));
				String oldPasswordHash = sharedPreferences.getString(mApplicationContext.getString(R.string.passwordHashKey), null);
				if (oldUsername != null && oldPasswordHash != null && !oldPasswordHash.trim().isEmpty()) {
					if (oldUsername.equals(username) && serverURL.equals(serverURLPref) && PasswordUtility.equal(password, oldPasswordHash)) {
						// put the token expiration information in the shared preferences
						long tokenExpirationLength = Math.max(sharedPreferences.getLong(mApplicationContext.getString(R.string.tokenExpirationLengthKey), 0), 0);
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
			final URL sURL = new URL(serverURL);

			ConnectivityUtility.isResolvable(sURL.getHost(), new Predicate<Exception>() {
				@Override
				public boolean apply(Exception e) {
					if (e == null) {
						callbackStatus = new AccountStatus(AccountStatus.Status.SUCCESSFUL_LOGIN);
						return true;
					} else {
						List<Integer> errorIndices = new ArrayList<Integer>();
						errorIndices.add(2);
						List<String> errorMessages = new ArrayList<String>();
						errorMessages.add("Bad hostname");
						callbackStatus = new AccountStatus(AccountStatus.Status.FAILED_LOGIN, errorIndices, errorMessages);
						return false;
					}
				}
			});

			int sleepcount = 0;
			while (callbackStatus == null && sleepcount < 60) {
				try {
					Thread.sleep(500);
				} catch (Exception e) {
					Log.e(LOG_NAME, "Problem sleeping.");
				} finally {
					sleepcount++;
				}
			}

			if (callbackStatus == null) {
				return new AccountStatus(AccountStatus.Status.FAILED_LOGIN);
			} else if (!callbackStatus.getStatus().equals(AccountStatus.Status.SUCCESSFUL_LOGIN)) {
				return callbackStatus;
			}

			// else the callback was a success

			HttpEntity entity = null;
			try {
				DefaultHttpClient httpClient = HttpClientManager.getInstance(mApplicationContext).getHttpClient();
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
				nameValuePairs.add(new BasicNameValuePair("password", password));
				nameValuePairs.add(new BasicNameValuePair("uid", uuid));
				nameValuePairs.add(new BasicNameValuePair("username", username));
				String buildVersion = sharedPreferences.getString(mApplicationContext.getString(R.string.buildVersionKey), null);
				if (buildVersion != null) {
					nameValuePairs.add(new BasicNameValuePair("appVersion", buildVersion));
				}

				UrlEncodedFormEntity authParams = new UrlEncodedFormEntity(nameValuePairs);

				// Does the device need to be registered?
				if (needToRegisterDevice) {
					AccountStatus.Status regStatus = registerDevice(serverURL, authParams);

					if (regStatus.equals(AccountStatus.Status.SUCCESSFUL_REGISTRATION)) {
						return new AccountStatus(regStatus);
					} else if (regStatus == AccountStatus.Status.FAILED_LOGIN) {
						return new AccountStatus(regStatus);
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
					Log.d(LOG_NAME, "Storing token: " + String.valueOf(sharedPreferences.getString(mApplicationContext.getString(R.string.tokenKey), null)));
					try {
						Date tokenExpiration = iso8601Format.parse(json.getString("expirationDate").trim());
						long tokenExpirationLength = tokenExpiration.getTime() - (new Date()).getTime();
						editor.putString(mApplicationContext.getString(R.string.tokenExpirationDateKey), iso8601Format.format(tokenExpiration)).commit();
						editor.putLong(mApplicationContext.getString(R.string.tokenExpirationLengthKey), tokenExpirationLength).commit();
					} catch (java.text.ParseException e) {
						Log.e(LOG_NAME, "Problem parsing token expiration date.", e);
					}

					// initialize the current user
					JSONObject userJson = json.getJSONObject("user");

					// if username is different, then clear the db
					String oldUsername = sharedPreferences.getString(mApplicationContext.getString(R.string.usernameKey), mApplicationContext.getString(R.string.usernameDefaultValue));
					String newUsername = userJson.getString("username");
					if (oldUsername == null || !oldUsername.equals(newUsername)) {
						DaoStore.getInstance(mApplicationContext).resetDatabase();
					}

					final Gson userDeserializer = UserDeserializer.getGsonBuilder(mApplicationContext);

					Map.Entry<User, Collection<String>> entry = userDeserializer.fromJson(userJson.toString(), new com.google.common.reflect.TypeToken<Map.Entry<User, Collection<String>>>() {
					}.getType());
					User user = entry.getKey();
					if (user != null) {
						user.setCurrentUser(true);
						user.setFetchedDate(new Date());
						user = userHelper.createOrUpdate(user);
						editor.putString(mApplicationContext.getString(R.string.displayNameKey), user.getDisplayName()).commit();
					} else {
						Log.e(LOG_NAME, "Unable to Deserializer user.");
						List<Integer> errorIndices = new ArrayList<Integer>();
						errorIndices.add(2);
						List<String> errorMessages = new ArrayList<String>();
						errorMessages.add("Problem retrieving your user.");
						return new AccountStatus(AccountStatus.Status.FAILED_LOGIN, errorIndices, errorMessages);
					}

					return new AccountStatus(AccountStatus.Status.SUCCESSFUL_LOGIN, new ArrayList<Integer>(), new ArrayList<String>(), json);
				} else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
					entity = response.getEntity();
					entity.consumeContent();
					// Could be that the device is not registered.
					if (!needToRegisterDevice) {
						// Try to register it
						params[3] = Boolean.TRUE.toString();
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

		} catch (MalformedURLException e) {
			List<Integer> errorIndices = new ArrayList<Integer>();
			errorIndices.add(2);
			List<String> errorMessages = new ArrayList<String>();
			errorMessages.add("Bad URL");
			return new AccountStatus(AccountStatus.Status.FAILED_LOGIN, errorIndices, errorMessages);
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
				if (jsonObject.getBoolean("registered")) {
					return AccountStatus.Status.ALREADY_REGISTERED;
				} else {
					// device registration has been submitted
					return AccountStatus.Status.SUCCESSFUL_REGISTRATION;
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
