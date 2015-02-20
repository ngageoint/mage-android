package mil.nga.giat.mage.sdk.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.io.CharStreams;

import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.Date;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.Role;
import mil.nga.giat.mage.sdk.datastore.user.RoleHelper;
import mil.nga.giat.mage.sdk.datastore.user.Team;
import mil.nga.giat.mage.sdk.datastore.user.TeamEvent;
import mil.nga.giat.mage.sdk.datastore.user.TeamHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserTeam;
import mil.nga.giat.mage.sdk.utils.DateFormatFactory;
import mil.nga.giat.mage.sdk.utils.PasswordUtility;

/**
 * Performs a local login.  Local authentication only. Testing or off-line
 * modes perhaps.
 * 
 * @author wiedemanns
 * 
 */
public class LocalAuthLoginTask extends AbstractAccountTask {

	private static final String LOG_NAME = LocalAuthLoginTask.class.getName();
    private DateFormat iso8601Format = DateFormatFactory.ISO8601();
	
	public LocalAuthLoginTask(AccountDelegate delegate, Context applicationContext) {
		super(delegate, applicationContext);
	}

	/**
	 * Called from execute
	 * 
	 * @param params
	 *            Should contain username and password; in that order.
	 */
	@Override
	protected AccountStatus doInBackground(String... params) {

		// retrieve the user name.
		String username = params[0];
		String password = params[1];

        // use a hash of the password as the token
        String hashPassword = "NA";
        try {
            PasswordUtility.getSaltedHash(password);
        } catch(Exception e) {
            Log.e(LOG_NAME, "Could not hash password", e);
        }

        // put the token information in the shared preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
        Editor editor = sharedPreferences.edit();
        editor.putString(mApplicationContext.getString(R.string.tokenKey), hashPassword).commit();
        // TODO : 8 hours from now?
        editor.putString(mApplicationContext.getString(R.string.tokenExpirationDateKey), iso8601Format.format(new Date(new Date().getTime() + 8 * 60 * 60 * 1000))).commit();

        RoleHelper roleHelper = RoleHelper.getInstance(mApplicationContext);
        TeamHelper teamHelper = TeamHelper.getInstance(mApplicationContext);
        EventHelper eventHelper = EventHelper.getInstance(mApplicationContext);

		// initialize local active user
		try {
			// delete all locations for now
            LocationHelper.getInstance(mApplicationContext).deleteAll();
			
			// delte roles
			roleHelper.deleteAll();
			
			// delete active user(s)
			userHelper.deleteCurrentUsers();
			
			Role defaultRole = new Role("NA", "LOCAL", "Local Auth", null);
			defaultRole = roleHelper.create(defaultRole);

            Team defaultTeam = new Team("NA", "LOCAL", "Local Auth");

            final String DEFAULT_DYNAMIC_FORM = "dynamic-form/default-dynamic-form.json";

            String dynamicForm = CharStreams.toString(new InputStreamReader(mApplicationContext.getAssets().open(DEFAULT_DYNAMIC_FORM), "UTF-8"));
            Event defaultEvent = new Event("NA", "LOCAL", "Local Auth", dynamicForm);
            defaultEvent = eventHelper.create(defaultEvent);

			// create new active user.
			User currentUser = new User("NA", "unknown", username, "", username, defaultRole, defaultEvent, null, null, null);
			currentUser.setCurrentUser(Boolean.TRUE);
			currentUser = userHelper.create(currentUser);

            // join tables
            userHelper.create(new UserTeam(currentUser, defaultTeam));
            teamHelper.create(new TeamEvent(defaultTeam, defaultEvent));
		} catch (Exception e) {
			// for now, treat as a warning. Not a great state to be in.
			Log.e(LOG_NAME, "Unable to initialize a local Active User.");
		}

		return new AccountStatus(AccountStatus.Status.SUCCESSFUL_LOGIN);
	}
}
