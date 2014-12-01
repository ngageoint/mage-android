package mil.nga.giat.mage.sdk.utils;

import java.text.ParseException;
import java.util.Date;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Utility that currently deals mostly with the user's token information.
 * 
 * @author wiedemannse
 * 
 */
public class UserUtility {

	private static final String LOG_NAME = UserUtility.class.getName();

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
				return new Date().after(DateUtility.getISO8601().parse(tokenExpirationDateString));
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
