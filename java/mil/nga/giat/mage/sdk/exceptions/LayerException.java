package mil.nga.giat.mage.sdk.exceptions;

/**
 * A generic Layer exception.
 * 
 * @author wiedemanns
 * 
 */
public class LayerException extends Exception {

	private static final long serialVersionUID = -8158708974451163516L;

	public LayerException() {
		super();
	}

	public LayerException(String message) {
		super(message);
	}

	public LayerException(String message, Throwable cause) {
		super(message, cause);
	}

	public LayerException(Throwable cause) {
		super(cause);
	}

}
