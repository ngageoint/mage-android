package mil.nga.giat.mage.sdk.login;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.http.resource.UserResource;
import mil.nga.giat.mage.sdk.utils.DeviceUuidFactory;

/**
 * Creates a user
 * 
 * @author wiedemanns
 * 
 */
public class SignupTask extends AbstractAccountTask {

	private static final String LOG_NAME = SignupTask.class.getName();
	
	public SignupTask(AccountDelegate delegate, Context applicationContext) {
		super(delegate, applicationContext);
	}

	/**
	 * Called from execute
	 * 
	 * @param params
	 *            Should contain displayname, username, email, password,
	 *            and serverURL; in that order.
	 * @return On success, {@link AccountStatus#getAccountInformation()}
	 *         contains the username
	 */
	@Override
	protected AccountStatus doInBackground(String... params) {

		// get inputs
		String displayName = params[0];
		String username = params[1];
		String email = params[2];
		String password = params[3];
		String serverURL = params[4];

		// Make sure you have connectivity
		if (!ConnectivityUtility.isOnline(mApplicationContext)) {
			List<Integer> errorIndices = new ArrayList<Integer>();
			errorIndices.add(5);
			List<String> errorMessages = new ArrayList<String>();
			errorMessages.add("No connection");
			return new AccountStatus(AccountStatus.Status.FAILED_SIGNUP, errorIndices, errorMessages);
		}

        String uuid = new DeviceUuidFactory(mApplicationContext).getDeviceUuid().toString();
		if (uuid == null) {
			List<Integer> errorIndices = new ArrayList<Integer>();
			errorIndices.add(5);
			List<String> errorMessages = new ArrayList<String>();
			errorMessages.add("Problem generating device uuid");
			return new AccountStatus(AccountStatus.Status.FAILED_SIGNUP, errorIndices, errorMessages);
		}

		// is server a valid URL? (already checked username and password)
		try {
			new URL(serverURL);
		} catch (MalformedURLException e) {
			List<Integer> errorIndices = new ArrayList<Integer>();
			errorIndices.add(5);
			List<String> errorMessages = new ArrayList<String>();
			errorMessages.add("Bad URL");
			return new AccountStatus(AccountStatus.Status.FAILED_SIGNUP, errorIndices, errorMessages);
		}

		try {
			UserResource userResource = new UserResource(mApplicationContext);
			JsonObject jsonUser = userResource.createUser(username, displayName, email, uuid, password);
			return new AccountStatus(AccountStatus.Status.SUCCESSFUL_SIGNUP, new ArrayList<Integer>(), new ArrayList<String>(), jsonUser);
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem signing up.", e);
			if (!StringUtils.isBlank(e.getMessage())) {
				List<Integer> errorIndices = new ArrayList<Integer>();
				errorIndices.add(5);
				List<String> errorMessages = new ArrayList<String>();
				errorMessages.add(e.getMessage());
				return new AccountStatus(AccountStatus.Status.FAILED_SIGNUP, errorIndices, errorMessages);
			}
			return new AccountStatus(AccountStatus.Status.FAILED_SIGNUP);
		}
	}
}
