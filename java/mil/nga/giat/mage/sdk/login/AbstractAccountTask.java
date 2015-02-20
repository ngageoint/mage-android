package mil.nga.giat.mage.sdk.login;

import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import android.content.Context;
import android.os.AsyncTask;

/**
 * New login and signup tasks must extend this!
 * 
 * @author wiedemanns
 * 
 */
public abstract class AbstractAccountTask extends AsyncTask<String, Void, AccountStatus> {

	protected AccountDelegate mDelegate;
	protected Context mApplicationContext;

	protected UserHelper userHelper;
	
	public AbstractAccountTask(AccountDelegate delegate, Context applicationContext) {
		mDelegate = delegate;
		mApplicationContext = applicationContext;
		userHelper = UserHelper.getInstance(applicationContext);
	}

	@Override
	protected void onPostExecute(AccountStatus accountStatus) {
		mDelegate.finishAccount(accountStatus);
		super.onPostExecute(accountStatus);
	}
}
