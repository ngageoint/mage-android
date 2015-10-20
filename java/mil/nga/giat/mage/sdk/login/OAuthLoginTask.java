package mil.nga.giat.mage.sdk.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

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
 * @author wnewmanw
 */
public class OAuthLoginTask extends AbstractAccountTask {

	private static final String LOG_NAME = OAuthLoginTask.class.getName();
	private DateFormat iso8601Format = DateFormatFactory.ISO8601();

	private volatile AccountStatus callbackStatus = null;

	public OAuthLoginTask(AccountDelegate delegate, Context context) {
		super(delegate, context);
	}

	/**
	 * Called from execute
	 *
	 * @param params Should contain oauth serverURL oauth login JSON info as first param
	 * @return On success, {@link AccountStatus#getAccountInformation()}
	 * contains the user's token
	 */
	@Override
	protected AccountStatus doInBackground(String... params) {
		String url = params[0];
		String json = params[1];

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mApplicationContext).edit();

		try {
			JSONObject jsonObject = new JSONObject(json);

			if (jsonObject.has("token") && jsonObject.has("user")) {
				// put the token information in the shared preferences
				String token = jsonObject.getString("token");
				Log.d(LOG_NAME, "Storing token: " + token);
				editor.putString(mApplicationContext.getString(mil.nga.giat.mage.sdk.R.string.tokenKey), token.trim());
				try {
					Date tokenExpiration = iso8601Format.parse(jsonObject.getString("expirationDate").trim());
					long tokenExpirationLength = tokenExpiration.getTime() - (new Date()).getTime();
					editor.putString(mApplicationContext.getString(mil.nga.giat.mage.sdk.R.string.tokenExpirationDateKey), iso8601Format.format(tokenExpiration));
					editor.putLong(mApplicationContext.getString(mil.nga.giat.mage.sdk.R.string.tokenExpirationLengthKey), tokenExpirationLength);
				} catch (java.text.ParseException e) {
					Log.e(LOG_NAME, "Problem parsing token expiration date.", e);
				}

				JSONObject userJson = jsonObject.getJSONObject("user");

				// if user id is different, then clear the db
				String oldUserId = sharedPreferences.getString(mApplicationContext.getString(R.string.userIdKey), null);
				String newUserId = userJson.getString("id");
				if (oldUserId == null || !oldUserId.equals(newUserId)) {
					DaoStore.getInstance(mApplicationContext).resetDatabase();
				}

				User user = UserDeserializer.getGsonBuilder(mApplicationContext).fromJson(userJson.toString(), User.class);
				if (user != null) {
					user.setCurrentUser(true);
					user.setFetchedDate(new Date());
					user = UserHelper.getInstance(mApplicationContext).createOrUpdate(user);
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
				editor.putString(mApplicationContext.getString(R.string.serverURLKey), url);
				editor.commit();

				PreferenceHelper.getInstance(mApplicationContext).logKeyValuePairs();

				editor.commit();
				return new AccountStatus(AccountStatus.Status.SUCCESSFUL_LOGIN, new ArrayList<Integer>(), new ArrayList<String>(), jsonObject);
			} else {
				if (jsonObject.has("device")) {
					JSONObject device = jsonObject.getJSONObject("device");
					if (device != null && !device.getBoolean("registered")) {
						JSONObject userJson = jsonObject.getJSONObject("user");
						String userId = userJson.getString("id");

						editor.putString(mApplicationContext.getString(R.string.userIdKey), userId);
						editor.putString(mApplicationContext.getString(R.string.serverURLKey), url);

						return new AccountStatus(AccountStatus.Status.SUCCESSFUL_REGISTRATION);
					}
				}

				return new AccountStatus(AccountStatus.Status.FAILED_LOGIN);
			}
		} catch (JSONException e) {
			Log.e(LOG_NAME, "Problem with oauth login attempt", e);
			return new AccountStatus(AccountStatus.Status.FAILED_LOGIN);
		}
	}
}
