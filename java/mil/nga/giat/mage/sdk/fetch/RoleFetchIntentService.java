package mil.nga.giat.mage.sdk.fetch;

import java.net.URL;

import mil.nga.giat.mage.sdk.ConnectivityAwareIntentService;
import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.user.Role;
import mil.nga.giat.mage.sdk.datastore.user.RoleHelper;
import mil.nga.giat.mage.sdk.gson.deserializer.RoleDeserializer;
import mil.nga.giat.mage.sdk.http.client.HttpClientManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;

/**
 * Gets roles from server. Not currently used!
 * 
 * @author wiedemanns
 * 
 */
public class RoleFetchIntentService extends ConnectivityAwareIntentService {

	private static final String LOG_NAME = RoleFetchIntentService.class.getName();

	public RoleFetchIntentService() {
		super(LOG_NAME);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		super.onHandleIntent(intent);
		if (isConnected) {
			RoleHelper roleHelper = RoleHelper.getInstance(getApplicationContext());

			final Gson roleDeserializer = RoleDeserializer.getGsonBuilder();
			DefaultHttpClient httpclient = HttpClientManager.getInstance(getApplicationContext()).getHttpClient();
			HttpEntity entity = null;
			try {
				URL serverURL = new URL(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(getString(R.string.serverURLKey), getString(R.string.serverURLDefaultValue)));

				URL roleURL = new URL(serverURL, "api/roles");

				Log.d(LOG_NAME, roleURL.toString());
				HttpGet get = new HttpGet(roleURL.toURI());
				HttpResponse response = httpclient.execute(get);
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					entity = response.getEntity();
					JSONArray json = new JSONArray(EntityUtils.toString(entity));
					if (json != null) {
						for (int i = 0; i < json.length(); i++) {
							JSONObject roleJson = (JSONObject) json.get(i);
							if (roleJson != null) {
								Role role = roleDeserializer.fromJson(roleJson.toString(), Role.class);

								if (role != null) {
									roleHelper.createOrUpdate(role);
								}
							}
						}
					}
				} else {
					entity = response.getEntity();
					String error = EntityUtils.toString(entity);
					Log.e(LOG_NAME, "Bad request.");
					Log.e(LOG_NAME, error);
				}
			} catch (Exception e) {
				Log.e(LOG_NAME, "There was a failure when fetching roles.", e);
			} finally {
				try {
					if (entity != null) {
						entity.consumeContent();
					}
				} catch (Exception e) {
				}
			}
		} else {
			Log.e(LOG_NAME, "Not connected.");
		}
	}
}
