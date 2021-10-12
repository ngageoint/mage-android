package mil.nga.giat.mage.sdk.exceptions;

/**
 * A generic Location exception.
 * 
 * @author wiedemanns
 * 
 */
public class LocationException extends Exception {

	private static final long serialVersionUID = -1452269689898249342L;

	public LocationException() {
		super();
	}

	public LocationException(String message) {
		super(message);
	}

	public LocationException(String message, Throwable cause) {
		super(message, cause);
	}

	public LocationException(Throwable cause) {
		super(cause);
	}

}
