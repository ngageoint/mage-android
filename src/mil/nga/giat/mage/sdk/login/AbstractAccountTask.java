package mil.nga.giat.mage.sdk.login;

import android.content.Context;
import android.os.AsyncTask;

/**
 * New login and signup tasks must extend this!
 * 
 * @author wiedemannse
 * 
 */
public abstract class AbstractAccountTask extends AsyncTask<String, Void, AccountStatus> {

	protected AccountDelegate mDelegate;
	protected Context mApplicationContext;

	public AbstractAccountTask(AccountDelegate delegate, Context applicationContext) {
		mDelegate = delegate;
		mApplicationContext = applicationContext;
	}

	@Override
	protected void onPostExecute(AccountStatus accountStatus) {
		mDelegate.finishAccount(accountStatus);
		super.onPostExecute(accountStatus);
	}
}
