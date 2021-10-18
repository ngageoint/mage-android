package mil.nga.giat.mage.sdk.event;


import mil.nga.giat.mage.sdk.datastore.user.User;

public interface IUserEventListener extends IEventListener {
	void onUserCreated(User user);
	void onUserUpdated(User user);
	void onUserIconUpdated(User user);
	void onUserAvatarUpdated(User user);
}
