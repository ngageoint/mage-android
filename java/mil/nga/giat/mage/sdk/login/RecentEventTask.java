package mil.nga.giat.mage.sdk.login;

import android.content.Context;
import android.util.Log;

import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.http.resource.UserResource;

/**
 * Updates user's recent event
 *
 * @author wiedemanns
 *
 */
public class RecentEventTask extends AbstractAccountTask {

    private static final String LOG_NAME = RecentEventTask.class.getName();

    private UserResource userResource;

    public RecentEventTask(AccountDelegate delegate, Context context) {
        super(delegate, context);
        userResource = new UserResource(context);
    }

    @Override
    protected AccountStatus doInBackground(String... params) {
        // get the user's recent event
        String userRecentEventRemoteId = params[0];

        try {
            Event userRecentEvent = EventHelper.getInstance(mApplicationContext).read(userRecentEventRemoteId);

            // tell the server and update the local store
            if(ConnectivityUtility.isOnline(mApplicationContext) && !LoginTaskFactory.getInstance(mApplicationContext).isLocalLogin()) {
                User currentUser = userHelper.readCurrentUser();

                User user = userResource.addRecentEvent(currentUser, userRecentEvent);
                if (user != null) {
                    return new AccountStatus(AccountStatus.Status.SUCCESSFUL_LOGIN);
                }
            }
        } catch(Exception e) {
            Log.e(LOG_NAME, "Unable to get current event.", e);
        }

        return new AccountStatus(AccountStatus.Status.FAILED_LOGIN);
    }
}
