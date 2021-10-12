package mil.nga.giat.mage.sdk.exceptions;

/**
 * A generic Observation exception.
 * 
 * @author wiedemanns
 * 
 */
public class ObservationException extends Exception {

	private static final long serialVersionUID = 3643427262387236746L;

	public ObservationException() {
		super();
	}

	public ObservationException(String message) {
		super(message);
	}

	public ObservationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ObservationException(Throwable cause) {
		super(cause);
	}

}
