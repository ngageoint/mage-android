package mil.nga.giat.mage.sdk.event;


import mil.nga.giat.mage.sdk.datastore.user.User;

public interface IUserEventListener extends IEventListener {
	public void onUserCreated(User user);
	public void onUserUpdated(User user);
	public void onUserIconUpdated(User user);
	public void onUserAvatarUpdated(User user);
}
