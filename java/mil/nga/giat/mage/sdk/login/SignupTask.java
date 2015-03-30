package mil.nga.giat.mage.sdk.login;

import android.content.Context;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
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
import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.http.client.HttpClientManager;
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
	 *            Should contain firstname, lastname, username, email, password,
	 *            and serverURL; in that order.
	 * @return On success, {@link AccountStatus#getAccountInformation()}
	 *         contains the username
	 */
	@Override
	protected AccountStatus doInBackground(String... params) {

		// get inputs
		String firstname = params[0];
		String lastname = params[1];
		String username = params[2];
		String email = params[3];
		String password = params[4];
		String serverURL = params[5];

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
		HttpEntity entity = null;
		try {
			DefaultHttpClient httpclient = HttpClientManager.getInstance(mApplicationContext).getHttpClient();
			HttpPost post = new HttpPost(new URL(new URL(serverURL), "api/users").toURI());

			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
			nameValuePairs.add(new BasicNameValuePair("password", password));
			nameValuePairs.add(new BasicNameValuePair("passwordconfirm", password));
			nameValuePairs.add(new BasicNameValuePair("uid", uuid));
			nameValuePairs.add(new BasicNameValuePair("username", username));
			nameValuePairs.add(new BasicNameValuePair("email", email));
			nameValuePairs.add(new BasicNameValuePair("firstname", firstname));
			nameValuePairs.add(new BasicNameValuePair("lastname", lastname));
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = httpclient.execute(post);

			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				entity = response.getEntity();
				JSONObject json = new JSONObject(EntityUtils.toString(entity));
				return new AccountStatus(AccountStatus.Status.SUCCESSFUL_SIGNUP, new ArrayList<Integer>(), new ArrayList<String>(), json);
			} else {
				entity = response.getEntity();
				String error = EntityUtils.toString(entity);
				Log.e(LOG_NAME, "Bad request.");
				Log.e(LOG_NAME, error);
				if(!StringUtils.isBlank(error)) {
					List<Integer> errorIndices = new ArrayList<Integer>();
					errorIndices.add(5);
					List<String> errorMessages = new ArrayList<String>();
					errorMessages.add(error);
					return new AccountStatus(AccountStatus.Status.FAILED_SIGNUP, errorIndices, errorMessages);
				}
				return new AccountStatus(AccountStatus.Status.FAILED_SIGNUP);
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem signing up.", e);
		} finally {
			try {
				if (entity != null) {
					entity.consumeContent();
				}
			} catch (Exception e) {
			}
		}

		return new AccountStatus(AccountStatus.Status.FAILED_SIGNUP);
	}
}
