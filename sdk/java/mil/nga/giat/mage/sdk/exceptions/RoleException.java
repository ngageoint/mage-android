package mil.nga.giat.mage.sdk.exceptions;

/**
 * A generic Role exception.
 * 
 * @author wiedemanns
 * 
 */
public class RoleException extends Exception {

	private static final long serialVersionUID = 122590919908423091L;

	public RoleException() {
		super();
	}

	public RoleException(String message) {
		super(message);
	}

	public RoleException(String message, Throwable cause) {
		super(message, cause);
	}

	public RoleException(Throwable cause) {
		super(cause);
	}

}
