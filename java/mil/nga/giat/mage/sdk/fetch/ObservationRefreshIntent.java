package mil.nga.giat.mage.sdk.fetch;

import android.content.Intent;
import android.util.Log;

import mil.nga.giat.mage.sdk.ConnectivityAwareIntentService;
import mil.nga.giat.mage.sdk.login.LoginTaskFactory;

public class ObservationRefreshIntent extends ConnectivityAwareIntentService {

	public static final String ACTION_OBSERVATIONS_REFRESHED = "mil.nga.giat.mage.sdk.service.ACTION_OBSERVATIONS_REFRESHED";
	public static final String EXTRA_OBSERVATIONS_REFRESH_STATUS = "EXTRA_OBSERVATIONS_REFRESH_STATUS";

	private static final String LOG_NAME = ObservationRefreshIntent.class.getName();


	public ObservationRefreshIntent() {
		super(LOG_NAME);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		super.onHandleIntent(intent);

		String status = null;
		if (isConnected && !LoginTaskFactory.getInstance(getApplicationContext()).isLocalLogin()) {
			new ObservationServerFetch(getApplicationContext()).fetch(false);
		} else {
			status = "No connection";
			Log.d(LOG_NAME, "The device is currently disconnected, not performing fetch.");
		}

		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(ACTION_OBSERVATIONS_REFRESHED);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		broadcastIntent.putExtra(EXTRA_OBSERVATIONS_REFRESH_STATUS, status);

		sendBroadcast(broadcastIntent);
	}
}
