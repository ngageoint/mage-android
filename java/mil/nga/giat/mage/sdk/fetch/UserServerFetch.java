package mil.nga.giat.mage.sdk.fetch;

import android.content.Context;
import android.util.Log;

import java.util.Date;

import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;
import mil.nga.giat.mage.sdk.retrofit.resource.UserResource;

public class UserServerFetch extends AbstractServerFetch {

	public UserServerFetch(Context context) {
		super(context);
	}

	private static final String LOG_NAME = UserServerFetch.class.getName();

	public void fetch(String... userIds) throws Exception {

		try {
			UserResource userResource = new UserResource(mContext);
			UserHelper userHelper = UserHelper.getInstance(mContext);

			// loop over all the ids
			for (String userId : userIds) {
				if (userId.equals("-1")) {
					continue;
				}

				boolean isCurrentUser = false;
				// is this a request for the current user?
				if (userId.equalsIgnoreCase("myself")) {
					isCurrentUser = true;
				} else {
					try {
						User u = userHelper.readCurrentUser();
						if (u != null) {
							String rid = u.getRemoteId();
							if (rid != null && rid.equalsIgnoreCase(userId)) {
								isCurrentUser = true;
							}
						}
					} catch (UserException e) {
						Log.e(LOG_NAME, "Could not get current users.");
					}
				}

				User user = userResource.getUser(userId);

				if (user != null) {
					user.setCurrentUser(isCurrentUser);
					user.setFetchedDate(new Date());
					userHelper.createOrUpdate(user);
				}
			}
		} catch(Exception e) {
            Log.e(LOG_NAME, "Problem fetching users.", e);
        }
	}
}
