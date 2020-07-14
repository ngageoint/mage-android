package mil.nga.giat.mage.sdk.login;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import mil.nga.giat.mage.sdk.connectivity.ConnectivityUtility;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.http.resource.UserResource;

/**
 * Updates user's recent event
 */
public class RecentEventTask extends AsyncTask<String, Void, Boolean> {

    public interface RecentEventDelegate {
        void onRecentEventComplete(Boolean status);
    }

    private static final String LOG_NAME = RecentEventTask.class.getName();

    private Context applicationContext;
    private RecentEventDelegate delegate;
    private UserHelper userHelper;
    private UserResource userResource;

    public RecentEventTask(Context applicationContext, RecentEventDelegate delegate) {
        this.applicationContext = applicationContext;
        this.delegate = delegate;
        userHelper = UserHelper.getInstance(applicationContext);
        userResource = new UserResource(applicationContext);
    }

    @Override
    protected Boolean doInBackground(String... params) {
        // get the user's recent event
        String userRecentEventRemoteId = params[0];

        try {
            Event userRecentEvent = EventHelper.getInstance(applicationContext).read(userRecentEventRemoteId);

            // tell the server and update the local store
            if (ConnectivityUtility.isOnline(applicationContext)) {
                User currentUser = userHelper.readCurrentUser();

                User user = userResource.addRecentEvent(currentUser, userRecentEvent);
                if (user != null) {
                    return true;
                }
            }
        } catch(Exception e) {
            Log.e(LOG_NAME, "Unable to get current event.", e);
        }

        return false;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
    }
}
