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
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;

/**
 * Utility that currently deals mostly with the user's token information.
 * 
 * @author wiedemannse
 * 
 */
public class UserUtility {

	private static final String LOG_NAME = UserUtility.class.getName();
    private DateFormat iso8601Format = DateFormatFactory.ISO8601();

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

	public synchronized final Boolean isTokenExpired() {
		String token = PreferenceHelper.getInstance(mContext).getValue(R.string.tokenKey);
		if (token == null || token.trim().isEmpty()) {
			return true;
		}
		String tokenExpirationDateString = PreferenceHelper.getInstance(mContext).getValue(R.string.tokenExpirationDateKey);
		if (!tokenExpirationDateString.isEmpty()) {

			try {
				return new Date().after(iso8601Format.parse(tokenExpirationDateString));
			} catch (ParseException pe) {
				Log.e(LOG_NAME, "Problem paring token date.", pe);
			}

		}
		return true;
	}

	public synchronized final void clearTokenInformation() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		Editor editor = sharedPreferences.edit();
		editor.putString(mContext.getString(R.string.tokenKey), "").commit();
		editor.putString(mContext.getString(R.string.tokenExpirationDateKey), "").commit();
	}
}
