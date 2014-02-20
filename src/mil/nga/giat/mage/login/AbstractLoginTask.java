package mil.nga.giat.mage.login;

import mil.nga.giat.mage.MainActivity;
import android.content.Intent;
import android.os.AsyncTask;

/**
 * New login modules/task should extend this!
 * 
 * @author wiedemannse
 * 
 */
public abstract class AbstractLoginTask extends AsyncTask<String, Void, Boolean> {

	protected LoginActivity mDelegate;

	public AbstractLoginTask(LoginActivity delegate) {
		mDelegate = delegate;
	}

	@Override
	protected void onPostExecute(Boolean status) {
		if (status) {
			Intent intent = new Intent(mDelegate.getApplicationContext(), MainActivity.class);
			mDelegate.startActivity(intent);
			mDelegate.finish();
		} else {
			mDelegate.getUsernameEditText().setError("Check your username");
			mDelegate.getPasswordEditText().setError("Check your password");
			mDelegate.getUsernameEditText().requestFocus();
		}
		super.onPostExecute(status);
	}
}
