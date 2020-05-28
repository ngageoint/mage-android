package mil.nga.giat.mage.sdk.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.JsonObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.http.resource.DeviceResource;
import mil.nga.giat.mage.sdk.http.resource.UserResource;
import mil.nga.giat.mage.sdk.jackson.deserializer.UserDeserializer;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.utils.DeviceUuidFactory;
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory;
import retrofit2.Response;

/**
 * Performs login to specified server with username and password.
 *
 * @author wnewman
 */
public class LdapLoginTask extends AbstractAccountTask {

	private static final String LOG_NAME = LdapLoginTask.class.getName();
	private DateFormat iso8601Format = ISO8601DateFormatFactory.ISO8601();
	private UserDeserializer userDeserializer;

	public LdapLoginTask(AccountDelegate delegate, Context context) {
		super(delegate, context);

		userDeserializer = new UserDeserializer(context);
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
		JsonObject parameters = new JsonObject();
		parameters.addProperty("username", params[0]);
		parameters.addProperty("password", params[1]);

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);

		String uuid = new DeviceUuidFactory(mApplicationContext).getDeviceUuid().toString();
		if (uuid == null) {
			List<Integer> errorIndices = new ArrayList<>();
			errorIndices.add(2);
			List<String> errorMessages = new ArrayList<>();
			errorMessages.add("Problem generating device uuid");
			return new AccountStatus(AccountStatus.Status.FAILED_LOGIN, errorIndices, errorMessages);
		}

		try {
			UserResource userResource = new UserResource(mApplicationContext);
			Response<JsonObject> response = userResource.signin("ldap", parameters);

			if (response.isSuccessful()) {
				JsonObject authorizeResponse = userResource.authorize("ldap", uuid);
				if (authorizeResponse == null) {
					DeviceResource deviceResource = new DeviceResource(mApplicationContext);
					JsonObject deviceJson = deviceResource.createDevice("ldap", uuid);
					if (deviceJson.get("registered").getAsBoolean()) {
						return new AccountStatus(AccountStatus.Status.ALREADY_REGISTERED);
					} else {
						return new AccountStatus(AccountStatus.Status.SUCCESSFUL_REGISTRATION);
					}
				}

				// check server api version to ensure compatibility before continuing
				JsonObject serverVersion = authorizeResponse.get("api").getAsJsonObject().get("version").getAsJsonObject();
				if (!PreferenceHelper.getInstance(mApplicationContext).validateServerVersion(serverVersion.get("major").getAsInt(), serverVersion.get("minor").getAsInt())) {
					Log.e(LOG_NAME, "Server version not compatible");
					return new AccountStatus(AccountStatus.Status.INVALID_SERVER);
				}

				// put the token information in the shared preferences
				SharedPreferences.Editor editor = sharedPreferences.edit();

				editor.putString(mApplicationContext.getString(R.string.tokenKey), authorizeResponse.get("token").getAsString().trim());
				try {
					Date tokenExpiration = iso8601Format.parse(authorizeResponse.get("expirationDate").getAsString().trim());
					long tokenExpirationLength = tokenExpiration.getTime() - (new Date()).getTime();
					editor.putString(mApplicationContext.getString(R.string.tokenExpirationDateKey), iso8601Format.format(tokenExpiration));
					editor.putLong(mApplicationContext.getString(R.string.tokenExpirationLengthKey), tokenExpirationLength);
				} catch (java.text.ParseException e) {
					Log.e(LOG_NAME, "Problem parsing token expiration date.", e);
				}

				// initialize the current user
				JsonObject userJson = authorizeResponse.getAsJsonObject("user");

				// if username is different, then clear the db
				String oldUsername = sharedPreferences.getString(mApplicationContext.getString(R.string.usernameKey), mApplicationContext.getString(R.string.usernameDefaultValue));
				String newUsername = userJson.get("username").getAsString();
				if (oldUsername == null || !oldUsername.equals(newUsername)) {
					DaoStore.getInstance(mApplicationContext).resetDatabase();
				}

				User user = userDeserializer.parseUser(userJson.toString());
				if (user != null) {
					user.setFetchedDate(new Date());
					user = userHelper.createOrUpdate(user);

					userHelper.setCurrentUser(user);

					editor.putString(mApplicationContext.getString(R.string.displayNameKey), user.getDisplayName());
				} else {
					Log.e(LOG_NAME, "Unable to Deserializer user.");
					List<Integer> errorIndices = new ArrayList<>();
					errorIndices.add(2);
					List<String> errorMessages = new ArrayList<>();
					errorMessages.add("Problem retrieving your user.");
					return new AccountStatus(AccountStatus.Status.FAILED_LOGIN, errorIndices, errorMessages);
				}

				editor.apply();

				return new AccountStatus(AccountStatus.Status.SUCCESSFUL_LOGIN, new ArrayList<Integer>(), new ArrayList<String>(), authorizeResponse);
			} else {
				// TODO use ldap error message
				String errorMessage = "Please check your username and password and try again.";
				if (response.errorBody() != null) {
					errorMessage = response.errorBody().string();
				}

				List<Integer> errorIndices = new ArrayList<>();
				errorIndices.add(2);
				List<String> errorMessages = new ArrayList<>();
				errorMessages.add(errorMessage);
				return new AccountStatus(AccountStatus.Status.FAILED_LOGIN, errorIndices, errorMessages);
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem logging in.", e);
			return new AccountStatus(AccountStatus.Status.FAILED_LOGIN);
		}
	}

	private AccountStatus.Status registerDevice(String uid) {
		try {
			DeviceResource deviceResource = new DeviceResource(mApplicationContext);
			JsonObject deviceJson = deviceResource.createDevice("local", uid);
			if (deviceJson != null) {
				if (deviceJson.get("registered").getAsBoolean()) {
					return AccountStatus.Status.ALREADY_REGISTERED;
				} else {
					// device registration has been submitted
					return AccountStatus.Status.SUCCESSFUL_REGISTRATION;
				}
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem registering device.", e);
		}

		return AccountStatus.Status.FAILED_LOGIN;
	}
}
