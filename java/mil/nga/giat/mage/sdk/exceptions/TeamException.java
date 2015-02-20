package mil.nga.giat.mage.sdk.exceptions;

/**
 * A generic Team exception.
 *
 * @author wiedemanns
 *
 */
public class TeamException extends Exception {

    private static final long serialVersionUID = 113731522201817720L;

    public TeamException() {
        super();
    }

    public TeamException(String message) {
        super(message);
    }

    public TeamException(String message, Throwable cause) {
        super(message, cause);
    }

    public TeamException(Throwable cause) {
        super(cause);
    }

}
