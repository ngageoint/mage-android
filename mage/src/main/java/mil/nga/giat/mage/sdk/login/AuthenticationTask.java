package mil.nga.giat.mage.sdk.login;

import static mil.nga.giat.mage.sdk.login.AuthenticationStatus.Status.ACCOUNT_CREATED;
import static mil.nga.giat.mage.sdk.login.AuthenticationStatus.Status.DISCONNECTED_AUTHENTICATION;
import static mil.nga.giat.mage.sdk.login.AuthenticationStatus.Status.FAILED_AUTHENTICATION;
import static mil.nga.giat.mage.sdk.login.AuthenticationStatus.Status.SUCCESSFUL_AUTHENTICATION;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.JsonObject;

import java.text.DateFormat;
import java.util.Date;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.http.resource.UserResource;
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory;
import mil.nga.giat.mage.sdk.utils.PasswordUtility;
import retrofit2.Response;

/**
 * Performs username/password authentication.
 */

public class AuthenticationTask extends AsyncTask<String, Void, AuthenticationStatus> {

	public interface AuthenticationDelegate {
		void onAuthenticationComplete(AuthenticationStatus status);
	}

	private static final String LOG_NAME = AuthenticationTask.class.getName();

	private final Context applicationContext;
	private final AuthenticationDelegate delegate;
	private boolean allowDisconnectedLogin = false;
	private final DateFormat iso8601Format = ISO8601DateFormatFactory.ISO8601();

	public AuthenticationTask(Context applicationContext, AuthenticationDelegate delegate) {
		this(applicationContext, false, delegate);
	}

	public AuthenticationTask(Context applicationContext, boolean allowDisconnectedLogin, AuthenticationDelegate delegate) {
		this.applicationContext = applicationContext;
		this.delegate = delegate;
		this.allowDisconnectedLogin = allowDisconnectedLogin;
	}

	/**
	 * @param params Should contain username, password; in that order.
	 * @return {@link AuthenticationStatus}
	 */
	@Override
	protected AuthenticationStatus doInBackground(String... params) {
		return login(params);
	}

	private AuthenticationStatus login(String... params) {
		String username = params[0];
		String password = params[1];
		String strategy = params[2];

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);

		// Try disconnected login
		if (!ConnectivityUtility.isOnline(applicationContext) && allowDisconnectedLogin) {
			try {
				String oldUsername = sharedPreferences.getString(applicationContext.getString(R.string.usernameKey), null);
				String oldPasswordHash = sharedPreferences.getString(applicationContext.getString(R.string.passwordHashKey), null);
				if (oldUsername != null && oldPasswordHash != null && !oldPasswordHash.trim().isEmpty()) {
					if (oldUsername.equals(username) && PasswordUtility.equal(password, oldPasswordHash)) {
						// put the token expiration information in the shared preferences
						long tokenExpirationLength = Math.max(sharedPreferences.getLong(applicationContext.getString(R.string.tokenExpirationLengthKey), 0), 0);
						Date tokenExpiration = new Date(System.currentTimeMillis() + tokenExpirationLength);
						sharedPreferences.edit().putString(applicationContext.getString(R.string.tokenExpirationDateKey), iso8601Format.format(tokenExpiration)).apply();

						return new AuthenticationStatus.Builder(DISCONNECTED_AUTHENTICATION).build();
					} else {
						return new AuthenticationStatus.Builder(FAILED_AUTHENTICATION).build();
					}
				}
			} catch (Exception e) {
				Log.e(LOG_NAME, "Could not hash password", e);
			}

			return new AuthenticationStatus.Builder(FAILED_AUTHENTICATION)
					.message("No Internet Connection")
					.build();
		}

		try {
			UserResource userResource = new UserResource(applicationContext);
			JsonObject parameters = new JsonObject();
			parameters.addProperty("username", username);
			parameters.addProperty("password", password);
			Response<JsonObject> signin = userResource.signin(strategy, parameters);

			if (signin.isSuccessful()) {
				JsonObject json = signin.body();
				String token = json.get("token").getAsString();

				return new AuthenticationStatus.Builder(SUCCESSFUL_AUTHENTICATION)
						.token(token)
						.build();
			} else if (signin.code() == 403) {
				String errorMessage = "User account is not approved, please contact your MAGE administrator to approve your account.";
				if (signin.errorBody() != null) {
					errorMessage = signin.errorBody().string();
				}

				return new AuthenticationStatus.Builder(ACCOUNT_CREATED)
						.message(errorMessage)
						.build();
			} else {
				String errorMessage = "Please check your username and password and try again.";
				if (signin.errorBody() != null) {
					errorMessage = signin.errorBody().string();
				}

				return new AuthenticationStatus.Builder(FAILED_AUTHENTICATION)
						.message(errorMessage)
						.build();
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem logging in.", e);
		}

		return new AuthenticationStatus.Builder(FAILED_AUTHENTICATION).build();
	}

	@Override
	protected void onPostExecute(AuthenticationStatus authenticationStatus) {
		super.onPostExecute(authenticationStatus);

		delegate.onAuthenticationComplete(authenticationStatus);
	}
}
