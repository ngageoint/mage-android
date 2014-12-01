package mil.nga.giat.mage.sdk;

import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.connectivity.NetworkChangeReceiver;
import mil.nga.giat.mage.sdk.event.IConnectivityEventListener;
import android.app.IntentService;
import android.content.Intent;

public abstract class ConnectivityAwareIntentService extends IntentService implements IConnectivityEventListener {

	public ConnectivityAwareIntentService(String name) {
		super(name);
	}

	protected Boolean isConnected = Boolean.TRUE;

	protected boolean isCanceled = false;

	private static final String LOG_NAME = ConnectivityAwareIntentService.class.getName();

	@Override
	public void onError(Throwable error) {
				
	}

	@Override
	public void onAllDisconnected() {
		isConnected = Boolean.FALSE;
	}

	@Override
	public void onAnyConnected() {
		isConnected = Boolean.TRUE;
	}

	@Override
	public void onWifiConnected() {
		//if more granular connectivity management is ever needed.  i.e. for attachments?  		
	}

	@Override
	public void onWifiDisconnected() {
		//if more granular connectivity management is ever needed.  i.e. for attachments?		
	}

	@Override
	public void onMobileDataConnected() {
		//if more granular connectivity management is ever needed.  i.e. for attachments?		
	}

	@Override
	public void onMobileDataDisconnected() {
		//if more granular connectivity management is ever needed.  i.e. for attachments?	
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		//set up initial connection state
		isConnected = ConnectivityUtility.isOnline(getApplicationContext());
		//enable connectivity event handling
		NetworkChangeReceiver.getInstance().addListener(this);
	}
	
	@Override
	public void onDestroy() {
		isCanceled = true;
		super.onDestroy();
	}
}
