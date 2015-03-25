package mil.nga.giat.mage.sdk.login;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import mil.nga.giat.mage.sdk.R;

/**
 * Deals with login tasks and their configuration.
 * 
 * @author wiedemanns
 *
 */
public class LoginTaskFactory {

	private static final String LOG_NAME = LocalAuthLoginTask.class.getName();

	private LoginTaskFactory() {
	}

	private static LoginTaskFactory loginTaskFactory;
	private static Context mContext;

	public static LoginTaskFactory getInstance(final Context context) {
		if (context == null) {
			return null;
		}
		if (loginTaskFactory == null) {
			loginTaskFactory = new LoginTaskFactory();
		}
		mContext = context;
		return loginTaskFactory;
	}
	
	public boolean isLocalLogin() {
		String className = PreferenceManager.getDefaultSharedPreferences(mContext).getString(mContext.getString(R.string.loginTaskKey), mContext.getString(R.string.loginTaskDefaultValue));
		return className.equals(LocalAuthLoginTask.class.getCanonicalName());
	}

	/**
	 * Retrieves the correct login module from the configuration.
	 * 
	 * @param delegate
	 * @param context
	 * @return
	 */
	public AbstractAccountTask getLoginTask(AccountDelegate delegate, Context context) {
		String className = PreferenceManager.getDefaultSharedPreferences(mContext).getString(mContext.getString(R.string.loginTaskKey), mContext.getString(R.string.loginTaskDefaultValue));
		
		try {
			Class<?> c = Class.forName(className);
			Constructor[] constructors = c.getConstructors();
			for (Constructor constructor : constructors) {
				Class<?>[] params = constructor.getParameterTypes();
				if (params.length == 2 && params[0].isAssignableFrom(AccountDelegate.class) && params[1].isAssignableFrom(Context.class)) {
					return (AbstractAccountTask) constructor.newInstance(delegate, context);
				}
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "Problem making new instance of " + String.valueOf(className) + ".", e);
		}
		return new FormAuthLoginTask(delegate, context);
	}
}
