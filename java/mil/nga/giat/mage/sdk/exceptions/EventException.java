package mil.nga.giat.mage.sdk.exceptions;

/**
 * A generic Event exception.
 *
 * @author wiedemannse
 *
 */
public class EventException extends Exception {

    private static final long serialVersionUID = 121300084217645100L;

    public EventException() {
        super();
    }

    public EventException(String message) {
        super(message);
    }

    public EventException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventException(Throwable cause) {
        super(cause);
    }

}
