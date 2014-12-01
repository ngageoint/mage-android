package mil.nga.giat.mage.sdk.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.user.Role;
import mil.nga.giat.mage.sdk.datastore.user.RoleHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.exceptions.LoginException;
import mil.nga.giat.mage.sdk.utils.DateFormatFactory;

/**
 * A Task intended to be used for local authentication only. Testing or off-line
 * modes perhaps.  TODO: throw {@link LoginException}
 * 
 * @author travis
 * 
 */
public class LocalAuthLoginTask extends AbstractAccountTask {

	private static final String LOG_NAME = LocalAuthLoginTask.class.getName();
    private DateFormat iso8601Format = DateFormatFactory.ISO8601();

	protected RoleHelper roleHelper;
	protected LocationHelper locationHelper;
	
	public LocalAuthLoginTask(AccountDelegate delegate, Context applicationContext) {
		super(delegate, applicationContext);
		roleHelper = RoleHelper.getInstance(applicationContext);
		locationHelper = LocationHelper.getInstance(applicationContext);
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

		try {
			// use a hash of the password as the token
			String md5Password = Arrays.toString(MessageDigest.getInstance("MD5").digest(password.getBytes("UTF-8")));
			// put the token information in the shared preferences
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
			Editor editor = sharedPreferences.edit();
			editor.putString(mApplicationContext.getString(R.string.tokenKey), md5Password).commit();
			// FIXME : 8 hours for now?
			editor.putString(mApplicationContext.getString(R.string.tokenExpirationDateKey), iso8601Format.format(new Date(new Date().getTime() + 8 * 60 * 60 * 1000))).commit();
		} catch (NoSuchAlgorithmException nsae) {
			nsae.printStackTrace();
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
		}

		// initialize local active user
		try {
			
			// FIXME : delete all locations for now
			locationHelper.deleteAll();
			
			// delte roles
			roleHelper.deleteAll();
			
			// delete active user(s)
			userHelper.deleteCurrentUsers();
			
			Role defaultRole = new Role("NA", "LOCAL", "Local Auth", null);
			defaultRole = roleHelper.create(defaultRole);
			
			// create new active user.
			User currentUser = new User("NA", "unknown", username, "", username, defaultRole, null, null, null);
			currentUser.setCurrentUser(Boolean.TRUE);
			currentUser = userHelper.create(currentUser);
		} catch (Exception e) {
			// for now, treat as a warning. Not a great state to be in.
			Log.e(LOG_NAME, "Unable to initialize a local Active User.");
		}

		return new AccountStatus(AccountStatus.Status.SUCCESSFUL_LOGIN);
	}
}
