package mil.nga.giat.mage.sdk.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import mil.nga.giat.mage.sdk.R;

/**
 * Utility that currently deals mostly with the user's token information.
 * 
 * @author wiedemanns
 * 
 */
public class UserUtility {

	private static final String LOG_NAME = UserUtility.class.getName();
    private DateFormat iso8601Format = ISO8601DateFormatFactory.ISO8601();

	private UserUtility() {
	}

	private static UserUtility userUtility;
	private static Context mContext;

	public static UserUtility getInstance(final Context context) {
		if (context == null) {
			return null;
		}
		if (userUtility == null) {
			userUtility = new UserUtility();
		}
		mContext = context;
		return userUtility;
	}

    // TODO token info is really a function of login type
    // this should probably be in the auth module as something more generic,
    // in case we ever go to a different login module
	public synchronized final Boolean isTokenExpired() {
		String tokenExpirationDateString = PreferenceManager.getDefaultSharedPreferences(mContext).getString(mContext.getString(R.string.tokenExpirationDateKey), null);
		if (tokenExpirationDateString != null && !tokenExpirationDateString.isEmpty()) {
			try {
				return new Date().after(iso8601Format.parse(tokenExpirationDateString));
			} catch (ParseException pe) {
				Log.e(LOG_NAME, "Problem paring token date.", pe);
			}
		}
		return true;
	}

    // TODO token info is really a function of login type
    // this should probably be in the auth module as something more generic,
    // in case we ever go to a different login module
	public synchronized final void clearTokenInformation() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		Editor editor = sharedPreferences.edit();
		editor.remove(mContext.getString(R.string.tokenKey)).commit();
        editor.remove(mContext.getString(R.string.tokenExpirationDateKey)).commit();
	}
}
