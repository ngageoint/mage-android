package mil.nga.giat.mage.sdk.event;


public interface IConnectivityEventListener extends IEventListener {

	public void onAllDisconnected();
	
	public void onAnyConnected();
	
	public void onWifiConnected();
	
	public void onWifiDisconnected();
	
	public void onMobileDataConnected();
	
	public void onMobileDataDisconnected();
}
