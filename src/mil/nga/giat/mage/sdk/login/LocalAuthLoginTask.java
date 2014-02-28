package mil.nga.giat.mage.sdk.login;

import android.content.Context;

public class LocalAuthLoginTask extends AbstractAccountTask {

	public LocalAuthLoginTask(AccountDelegate delegate, Context applicationContext) {
		super(delegate, applicationContext);
	}

	@Override
	protected AccountStatus doInBackground(String... params) {

		// get inputs
		String username = params[0];
		String password = params[1];

		// TODO: actual local authorization implementation
		if (!username.isEmpty() && password.equals("12345")) {
			return new AccountStatus(Boolean.TRUE);
		}

		return new AccountStatus(Boolean.FALSE);
	}
}
