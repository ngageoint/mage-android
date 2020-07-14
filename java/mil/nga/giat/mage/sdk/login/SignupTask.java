package mil.nga.giat.mage.sdk.login;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;

import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.http.resource.UserResource;
import mil.nga.giat.mage.sdk.utils.DeviceUuidFactory;

/**
 * Creates a new local user account
 */
public class SignupTask extends AsyncTask<String, Void, SignupStatus> {

	public interface SignupDelegate {
		void onSignupComplete(SignupStatus status);
	}

	private static final String LOG_NAME = SignupTask.class.getName();

	protected Context applicationContext;
	protected SignupDelegate delegate;
	
	public SignupTask(Context applicationContext, SignupDelegate delegate) {
		this.applicationContext = applicationContext;
		this.delegate = delegate;
	}

	/**
	 * Called from execute
	 * 
	 * @param params
	 *            Should contain displayname, username, email, password,
	 *            and serverURL; in that order.
	 * @return On success, returns {@link SignupStatus}
	 */
	@Override
	protected SignupStatus doInBackground(String... params) {

		// get inputs
		String username = params[0];
		String displayName = params[1];
		String email = params[2];
		String phone = params[3];
		String password = params[4];
		String serverURL = params[5];

		// Make sure you have connectivity
		if (!ConnectivityUtility.isOnline(applicationContext)) {
			return new SignupStatus.Builder(SignupStatus.Status.FAILED_SIGNUP)
					.message("No Connection")
					.build();
		}

        String uid = new DeviceUuidFactory(applicationContext).getDeviceUuid().toString();
		if (uid == null) {
			return new SignupStatus.Builder(SignupStatus.Status.FAILED_SIGNUP)
					.message("Problem generating device uuid")
					.build();
		}

		// is server a valid URL? (already checked username and password)
		try {
			new URL(serverURL);
		} catch (MalformedURLException e) {
			return new SignupStatus.Builder(SignupStatus.Status.FAILED_SIGNUP)
					.message("Bad Server URL")
					.build();
		}

		try {
			UserResource userResource = new UserResource(applicationContext);
			JsonObject jsonUser = userResource.createUser(username, displayName, email, phone, uid, password);

			return new SignupStatus.Builder(SignupStatus.Status.SUCCESSFUL_SIGNUP)
					.user(jsonUser)
					.build();
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem signing up.", e);
			String message = StringUtils.isBlank(e.getMessage()) ? null : e.getMessage();
			return new SignupStatus.Builder(SignupStatus.Status.FAILED_SIGNUP)
					.message(message)
					.build();
		}
	}

	@Override
	protected void onPostExecute(SignupStatus status) {
		super.onPostExecute(status);

		delegate.onSignupComplete(status);
	}
}
