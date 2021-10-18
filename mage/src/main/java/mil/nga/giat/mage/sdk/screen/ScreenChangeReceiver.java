package mil.nga.giat.mage.sdk.screen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import mil.nga.giat.mage.sdk.event.IEventDispatcher;
import mil.nga.giat.mage.sdk.event.IScreenEventListener;

public class ScreenChangeReceiver extends BroadcastReceiver implements IEventDispatcher<IScreenEventListener> {

	/**
	 * Singleton.
	 */
	private static ScreenChangeReceiver mScreenChangeReceiver;

	/**
	 * Do not use!
	 */
	public ScreenChangeReceiver() {

	}

	public static ScreenChangeReceiver getInstance() {
		if (mScreenChangeReceiver == null) {
			mScreenChangeReceiver = new ScreenChangeReceiver();
		}
		return mScreenChangeReceiver;
	}

	private static final Collection<IScreenEventListener> listeners = new CopyOnWriteArrayList<IScreenEventListener>();

	@Override
	public void onReceive(final Context context, final Intent intent) {
		for (IScreenEventListener listener : listeners) {
			listener.onScreenOn();
		}
	}

	@Override
	public boolean addListener(IScreenEventListener listener) {
		return listeners.add(listener);
	}

	@Override
	public boolean removeListener(IScreenEventListener listener) {
		return listeners.remove(listener);
	}
}