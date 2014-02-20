package mil.nga.giat.mage.login;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
import android.net.wifi.WifiManager;

/**
 * Performs login to specified server with username and password
 * 
 * @author wiedemannse
 *
 */
public class FormAuthLoginTask extends AbstractLoginTask {

	protected String mToken = null;
	
	public FormAuthLoginTask(LoginActivity delegate) {
		super(delegate);
	}

	@Override
	protected Boolean doInBackground(String... params) {

		// get inputs
		String username = params[0];
		String password = params[1];
		String serverURL = params[2];

		// TODO: Make sure you have connectivity!
		String macAddress = getMacAddress();
		if (macAddress == null) {
			mDelegate.getServerEditText().setError("No connection");
			mDelegate.getServerEditText().requestFocus();
			return Boolean.FALSE;
		}

		// is server a valid URL? (already checked username and password)
		try {
			new URL(serverURL);
		} catch (MalformedURLException e) {
			mDelegate.getServerEditText().setError("Bad URL");
			mDelegate.getServerEditText().requestFocus();
			return Boolean.FALSE;
		}
		
		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpPost post = new HttpPost(new URL(new URL(serverURL), "api/login").toURI());

			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
			nameValuePairs.add(new BasicNameValuePair("password", password));
			nameValuePairs.add(new BasicNameValuePair("uid", macAddress));
			nameValuePairs.add(new BasicNameValuePair("username", username));
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = httpclient.execute(post);

			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				JSONObject json = new JSONObject(EntityUtils.toString(response.getEntity()));
				mToken = json.getString("token");
				return Boolean.TRUE;
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

		return Boolean.FALSE;
	}

	
	@Override
	protected void onPostExecute(Boolean status) {
		if (status) {
			System.out.println("SEW: " + mToken);
		}
		super.onPostExecute(status);
	}
	
	/**
	 * Get the Wi-Fi mac address, used to login
	 */
	protected String getMacAddress() {
		return ((WifiManager) mDelegate.getApplicationContext().getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getMacAddress();
	}
}
