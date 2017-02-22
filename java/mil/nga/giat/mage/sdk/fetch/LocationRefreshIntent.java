package mil.nga.giat.mage.sdk.fetch;

import android.content.Intent;
import android.util.Log;

import mil.nga.giat.mage.sdk.ConnectivityAwareIntentService;
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;

public class LocationRefreshIntent extends ConnectivityAwareIntentService {

	public static final String ACTION_LOCATIONS_REFRESHED = "mil.nga.giat.mage.sdk.service.ACTION_LOCATIONS_REFRESHED";
	public static final String EXTRA_LOCATIONS_REFRESH_STATUS = "EXTRA_LOCATIONS_REFRESH_STATUS";

	private static final String LOG_NAME = LocationRefreshIntent.class.getName();


	public LocationRefreshIntent() {
		super(LOG_NAME);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		super.onHandleIntent(intent);

		String status = null;
		if (isConnected && !LoginTaskFactory.getInstance(getApplicationContext()).isLocalLogin()) {
			new LocationServerFetch(getApplicationContext()).fetch();
		} else {
			status = "No connection";
			Log.d(LOG_NAME, "The device is currently disconnected, not performing fetch.");
		}

		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(ACTION_LOCATIONS_REFRESHED);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		broadcastIntent.putExtra(EXTRA_LOCATIONS_REFRESH_STATUS, status);

		sendBroadcast(broadcastIntent);
	}
}
