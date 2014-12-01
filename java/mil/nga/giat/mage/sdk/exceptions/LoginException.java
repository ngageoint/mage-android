package mil.nga.giat.mage.sdk.exceptions;

/**
 * A generic Login exception.
 * 
 * @author wiedemannse
 * 
 */
public class LoginException extends Exception {

	private static final long serialVersionUID = -7559398506009212419L;

	public LoginException() {
		super();
	}

	public LoginException(String message) {
		super(message);
	}

	public LoginException(String message, Throwable cause) {
		super(message, cause);
	}

	public LoginException(Throwable cause) {
		super(cause);
	}

}
