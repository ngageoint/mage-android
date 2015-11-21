package mil.nga.giat.mage.sdk.login;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Performs signup to specified oauth server.
 *
 * @author newmanw
 *
 */
public class OAuthSignupTask extends AbstractAccountTask {

	private static final String LOG_NAME = OAuthSignupTask.class.getName();

	public OAuthSignupTask(AccountDelegate delegate, Context applicationContext) {
		super(delegate, applicationContext);
	}

	/**
	 * Called from execute
	 * 
	 * @param params
	 *            Should contain server URL and oauth json response.
	 * @return On success, {@link AccountStatus#getAccountInformation()}
	 *         contains the username
	 */
	@Override
	protected AccountStatus doInBackground(String... params) {
		String json = params[0];

		try {
			JSONObject jsonObject = new JSONObject(json);
			if (jsonObject.has("user")) {
				return new AccountStatus(AccountStatus.Status.SUCCESSFUL_SIGNUP, new ArrayList<Integer>(), new ArrayList<String>(), jsonObject);
			} else {
				return new AccountStatus(AccountStatus.Status.FAILED_SIGNUP, new ArrayList<Integer>(), new ArrayList<String>(), jsonObject);
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem signing up.", e);
		}

		return new AccountStatus(AccountStatus.Status.FAILED_SIGNUP);
	}
}
