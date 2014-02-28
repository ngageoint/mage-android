package mil.nga.giat.mage.sdk.login;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.mage.sdk.utils.ConnectivityUtility;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

/**
 * Creates a user
 * 
 * @author wiedemannse
 * 
 */
public class SignupTask extends AbstractAccountTask {


	public SignupTask(AccountDelegate delegate, Context applicationContext) {
		super(delegate, applicationContext);
	}

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
			return new AccountStatus(Boolean.FALSE, errorIndices, errorMessages);
		}

		String macAddress = ConnectivityUtility.getMacAddress(mApplicationContext);
		if (macAddress == null) {
			List<Integer> errorIndices = new ArrayList<Integer>();
			errorIndices.add(5);
			List<String> errorMessages = new ArrayList<String>();
			errorMessages.add("No mac address found on device");
			return new AccountStatus(Boolean.FALSE, errorIndices, errorMessages);
		}

		// is server a valid URL? (already checked username and password)
		try {
			new URL(serverURL);
		} catch (MalformedURLException e) {
			List<Integer> errorIndices = new ArrayList<Integer>();
			errorIndices.add(5);
			List<String> errorMessages = new ArrayList<String>();
			errorMessages.add("Bad URL");
			return new AccountStatus(Boolean.FALSE, errorIndices, errorMessages);
		}

		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpPost post = new HttpPost(new URL(new URL(serverURL), "api/users").toURI());

			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
			nameValuePairs.add(new BasicNameValuePair("password", password));
			nameValuePairs.add(new BasicNameValuePair("passwordconfirm", password));
			nameValuePairs.add(new BasicNameValuePair("uid", macAddress));
			nameValuePairs.add(new BasicNameValuePair("username", username));
			nameValuePairs.add(new BasicNameValuePair("email", email));
			nameValuePairs.add(new BasicNameValuePair("firstname", firstname));
			nameValuePairs.add(new BasicNameValuePair("lastname", lastname));
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = httpclient.execute(post);

			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				JSONObject json = new JSONObject(EntityUtils.toString(response.getEntity()));
				System.out.println(json.getString("firstname"));
				return new AccountStatus(Boolean.TRUE);
			}
		} catch (MalformedURLException e) {
			// already checked for this!
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new AccountStatus(Boolean.FALSE);
	}
}
