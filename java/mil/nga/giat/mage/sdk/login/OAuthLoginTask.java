package mil.nga.giat.mage.sdk.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.gson.deserializer.UserDeserializer;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.utils.DateFormatFactory;

/**
 * Performs login to specified oauth server.
 *
 * @author newmanw
 *
 */
public class OAuthLoginTask extends AbstractAccountTask {

	private static final String LOG_NAME = OAuthLoginTask.class.getName();
	private DateFormat iso8601Format = DateFormatFactory.ISO8601();

	public OAuthLoginTask(AccountDelegate delegate, Context context) {
		super(delegate, context);
	}

	/**
	 * Called from execute
	 *
	 * @param params Should contain oauth login JSON info as first param
	 * @return On success, {@link AccountStatus#getAccountInformation()}
	 * contains the user's token
	 */
	@Override
	protected AccountStatus doInBackground(String... params) {
		String json = params[0];

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).edit();

		try {
			JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();

			if (jsonObject.has("token") && jsonObject.has("user")) {
				// put the token information in the shared preferences
				String token = jsonObject.get("token").getAsString();
				Log.d(LOG_NAME, "Storing token: " + token);
				editor.putString(mApplicationContext.getString(mil.nga.giat.mage.sdk.R.string.tokenKey), token.trim());
				try {
					Date tokenExpiration = iso8601Format.parse(jsonObject.get("expirationDate").getAsString().trim());
					long tokenExpirationLength = tokenExpiration.getTime() - (new Date()).getTime();
					editor.putString(mApplicationContext.getString(mil.nga.giat.mage.sdk.R.string.tokenExpirationDateKey), iso8601Format.format(tokenExpiration));
					editor.putLong(mApplicationContext.getString(mil.nga.giat.mage.sdk.R.string.tokenExpirationLengthKey), tokenExpirationLength);
				} catch (java.text.ParseException e) {
					Log.e(LOG_NAME, "Problem parsing token expiration date.", e);
				}

				JsonObject userJson = jsonObject.getAsJsonObject("user");

				// if user id is different, then clear the db
				String oldUserId = sharedPreferences.getString(mApplicationContext.getString(R.string.userIdKey), null);
				String newUserId = userJson.get("id").getAsString();
				if (oldUserId == null || !oldUserId.equals(newUserId)) {
					DaoStore.getInstance(mApplicationContext).resetDatabase();
				}

				Gson userDeserializer = UserDeserializer.getGsonBuilder(mApplicationContext);
				User user = userDeserializer.fromJson(userJson.toString(), User.class);
				if (user != null) {
					user.setFetchedDate(new Date());
					UserHelper userHelper = UserHelper.getInstance(mApplicationContext);
					user = userHelper.createOrUpdate(user);

					userHelper.setCurrentUser(user);
				} else {
					Log.e(LOG_NAME, "Unable to Deserializer user.");
					List<Integer> errorIndices = new ArrayList<Integer>();
					errorIndices.add(2);
					List<String> errorMessages = new ArrayList<String>();
					errorMessages.add("Problem retrieving your user.");
					return new AccountStatus(AccountStatus.Status.FAILED_LOGIN, errorIndices, errorMessages);
				}

				editor.putString(mApplicationContext.getString(R.string.userIdKey), newUserId);
				editor.putString(mApplicationContext.getString(R.string.displayNameKey), user.getDisplayName());

				PreferenceHelper.getInstance(mApplicationContext).logKeyValuePairs();

				editor.commit();
				return new AccountStatus(AccountStatus.Status.SUCCESSFUL_LOGIN, new ArrayList<Integer>(), new ArrayList<String>(), jsonObject);
			} else {
				if (jsonObject.has("device")) {
					JsonObject device = jsonObject.getAsJsonObject("device");
					if (device != null && !device.get("registered").getAsBoolean()) {
						JsonObject userJson = jsonObject.getAsJsonObject("user");
						String userId = userJson.get("id").getAsString();

						editor.putString(mApplicationContext.getString(R.string.userIdKey), userId);

						return new AccountStatus(AccountStatus.Status.SUCCESSFUL_REGISTRATION);
					}
				}

				return new AccountStatus(AccountStatus.Status.FAILED_LOGIN);
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem with oauth login attempt", e);
			return new AccountStatus(AccountStatus.Status.FAILED_LOGIN);
		}
	}
}
