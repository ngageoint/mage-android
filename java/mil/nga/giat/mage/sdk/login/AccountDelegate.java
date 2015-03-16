package mil.nga.giat.mage.sdk.login;

/**
 * Use this class when logging in or signing up. {@link #finishAccount} is a
 * callback used when logging in or signing up.
 * 
 * Could be a typed interface if needed!
 * 
 * @author wiedemanns
 * 
 */
public interface AccountDelegate {

	/**
	 * Use this to report the status of your login or signup request back to the
	 * user.
	 * 
	 */
	public void finishAccount(AccountStatus accountStatus);
}
