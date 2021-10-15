package mil.nga.giat.mage.sdk.exceptions;

/**
 * A generic StaticFeature exception.
 */
public class StaticFeatureException extends Exception {

	private static final long serialVersionUID = 7632029149336541486L;

	public StaticFeatureException(String message) {
		super(message);
	}

	public StaticFeatureException(String message, Throwable cause) {
		super(message, cause);
	}

}
