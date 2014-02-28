package mil.nga.giat.mage.sdk.login;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class LoginTaskFactory {

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

	public AbstractAccountTask getLoginTask(AccountDelegate delegate, Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		String className = sharedPreferences.getString("loginTask", FormAuthLoginTask.class.getCanonicalName());
		try {
			Class<?> c = Class.forName(className);
			Constructor[] constructors = c.getConstructors();
			for (Constructor constructor : constructors) {
				Class<?>[] params = constructor.getParameterTypes();
				if (params.length == 2 && params[0].isAssignableFrom(AccountDelegate.class) && params[1].isAssignableFrom(Context.class)) {
					return (AbstractAccountTask) constructor.newInstance(delegate, context);
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return new FormAuthLoginTask(delegate, context);
	}
}
