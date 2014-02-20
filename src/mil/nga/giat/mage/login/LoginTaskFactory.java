package mil.nga.giat.mage.login;

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

	public AbstractLoginTask getLoginTask(LoginActivity delegate) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		String className = sharedPreferences.getString("loginTask", FormAuthLoginTask.class.getCanonicalName());
		try {
			Class<?> c = Class.forName(className);
			Constructor<?> ct = c.getConstructor(delegate.getClass());
			return (AbstractLoginTask) ct.newInstance(delegate);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return new FormAuthLoginTask(delegate);
	}
}
