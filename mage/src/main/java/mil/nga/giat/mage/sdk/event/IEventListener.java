package mil.nga.giat.mage.sdk.event;

import java.util.EventListener;

/**
 * Part of a small event framework. Used to pass events to different parts of
 * the mdk. When locations are saved, when tokens expire, etc...
 * 
 * @author wiedemanns
 *
 */
public interface IEventListener extends EventListener {
	void onError(Throwable error);
}
