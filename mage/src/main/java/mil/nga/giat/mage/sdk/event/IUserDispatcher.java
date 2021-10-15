package mil.nga.giat.mage.sdk.event;

public interface IUserDispatcher {

	/**
	 * Adds a listener
	 * 
	 * @param listener
	 * @return
	 * @throws Exception
	 */
    boolean addListener(final IUserEventListener listener) throws Exception;

	/**
	 * Removes the listener
	 * 
	 * @param listener
	 * @return
	 * @throws Exception
	 */
    boolean removeListener(final IUserEventListener listener) throws Exception;
}
