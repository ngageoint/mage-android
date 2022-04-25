package mil.nga.giat.mage.sdk.login;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import mil.nga.giat.mage.network.gson.user.UserTypeAdapter;
import mil.nga.giat.mage.sdk.Compatibility;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.http.resource.DeviceResource;
import mil.nga.giat.mage.sdk.utils.DeviceUuidFactory;
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory;
import retrofit2.Response;

/**
 * Performs login to specified oauth server.
 *
 * @author newmanw
 *
 */
public class AuthorizationTask extends AsyncTask<String, Void, AuthorizationStatus> {

	public interface AuthorizationDelegate {
		void onAuthorizationComplete(AuthorizationStatus status);
	}

	private static final String LOG_NAME = AuthorizationTask.class.getName();

	private final Context applicationContext;
	private final AuthorizationDelegate delegate;
	private final UserTypeAdapter userTypeAdapter;

	public AuthorizationTask(Context applicationContext, AuthorizationDelegate delegate) {
		this.applicationContext = applicationContext;
		this.delegate = delegate;
		userTypeAdapter = new UserTypeAdapter(applicationContext);
	}

	/**
	 * Called from execute
	 *
	 * @param params Should contain authentication strategy and authentication token
	 */
	@Override
	protected AuthorizationStatus doInBackground(String... params) {
		String jwt = params[0];

		try {
			DeviceResource deviceResource = new DeviceResource(applicationContext);
			String uid = new DeviceUuidFactory(applicationContext).getDeviceUuid().toString();
			Response<JsonObject> authorizeResponse = deviceResource.authorize(jwt, uid);
			if (authorizeResponse == null || !authorizeResponse.isSuccessful()) {
				int code = authorizeResponse == null ? 401 : authorizeResponse.code();
				AuthorizationStatus.Status status = code == 403 ? AuthorizationStatus.Status.FAILED_AUTHORIZATION : AuthorizationStatus.Status.FAILED_AUTHENTICATION;
				return new AuthorizationStatus.Builder(status).build();
			}

			JsonObject authorization = authorizeResponse.body();

			// check server api version to ensure compatibility before continuing
			JsonObject serverVersion = authorization.get("api").getAsJsonObject().get("version").getAsJsonObject();
			if (!Compatibility.Companion.isCompatibleWith(serverVersion.get("major").getAsInt(), serverVersion.get("minor").getAsInt())) {
				Log.e(LOG_NAME, "Server version not compatible");
				return new AuthorizationStatus.Builder(AuthorizationStatus.Status.INVALID_SERVER).build();
			}

			// Successful login, put the token information in the shared preferences
			String token = authorization.get("token").getAsString();
			Date tokenExpiration = null;
			try {
				tokenExpiration = ISO8601DateFormatFactory.ISO8601().parse(authorization.get("expirationDate").getAsString().trim());
			} catch (ParseException e) {
				Log.e(LOG_NAME, "Problem parsing token expiration date.", e);
			}

			JsonObject userJson = authorization.getAsJsonObject("user");
			JsonReader reader = new JsonReader(new StringReader(userJson.toString()));
			User user = userTypeAdapter.read(reader);
			return new AuthorizationStatus.Builder(AuthorizationStatus.Status.SUCCESSFUL_AUTHORIZATION)
					.authorization(user, token)
					.tokenExpiration(tokenExpiration)
					.build();
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem with authorization attempt", e);
			return new AuthorizationStatus.Builder(AuthorizationStatus.Status.FAILED_AUTHORIZATION).build();
		}
	}

	@Override
	protected void onPostExecute(AuthorizationStatus status) {
		super.onPostExecute(status);

		delegate.onAuthorizationComplete(status);
	}
}
