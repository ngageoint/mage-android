package mil.nga.giat.mage.sdk.event;


public interface IConnectivityEventListener extends IEventListener {

	void onAllDisconnected();
	
	void onAnyConnected();
	
	void onWifiConnected();
	
	void onWifiDisconnected();
	
	void onMobileDataConnected();
	
	void onMobileDataDisconnected();
}
