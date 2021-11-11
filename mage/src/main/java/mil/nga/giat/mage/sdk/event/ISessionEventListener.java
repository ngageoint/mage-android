package mil.nga.giat.mage.sdk.event;


public interface ISessionEventListener extends IEventListener {

	void onTokenExpired();

}
