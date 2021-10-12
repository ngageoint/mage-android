package mil.nga.giat.mage.sdk.fetch;

import android.content.Context;
import android.util.Log;

import java.util.Date;

import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.http.resource.UserResource;

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

				User user = userResource.getUser(userId);
				if (user != null) {
					user.setFetchedDate(new Date());
					userHelper.createOrUpdate(user);
				}
			}
		} catch(Exception e) {
            Log.e(LOG_NAME, "Problem fetching users.", e);
        }
	}
}
