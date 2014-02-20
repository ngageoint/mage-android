package mil.nga.giat.mage.login;

public class LocalAuthLoginTask extends AbstractLoginTask {

	public LocalAuthLoginTask(LoginActivity delegate) {
		super(delegate);
	}

	@Override
	protected Boolean doInBackground(String... params) {

		// get inputs
		String username = params[0];
		String password = params[1];

		// TODO: actual local authorization implementation
		if (!username.isEmpty() && password.equals("12345")) {
			return Boolean.TRUE;
		}

		return Boolean.FALSE;
	}
}
