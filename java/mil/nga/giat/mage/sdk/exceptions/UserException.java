package mil.nga.giat.mage.sdk.exceptions;

/**
 * A generic User exception.
 * 
 * @author travis
 * 
 */
public class UserException extends Exception {

	private static final long serialVersionUID = -8750495345646167370L;

	public UserException() {
		super();
	}

	public UserException(String message) {
		super(message);
	}

	public UserException(String message, Throwable cause) {
		super(message, cause);
	}

	public UserException(Throwable cause) {
		super(cause);
	}

}
