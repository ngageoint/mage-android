package mil.nga.giat.mage.sdk.connectivity;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import mil.nga.giat.mage.sdk.event.IConnectivityEventListener;
import mil.nga.giat.mage.sdk.event.IEventDispatcher;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;


/**
 * When network connectivity changes (lost or gained) notify the right listeners.
 * When we gain connectivity, wait for a little bit to make sure we keep it before notifying listeners.
 *
 * @author wiedemanns
 */
public class NetworkChangeReceiver extends BroadcastReceiver implements IEventDispatcher<IConnectivityEventListener> {

	/**
	 * Singleton.
	 */
	private static NetworkChangeReceiver mNetworkChangeReceiver;

	/**
	 * Do not use!
	 */
	public NetworkChangeReceiver() {
		
	}
	
	public static NetworkChangeReceiver getInstance() {
		if (mNetworkChangeReceiver == null) {
			mNetworkChangeReceiver = new NetworkChangeReceiver();
		}
		return mNetworkChangeReceiver;
	}	
	
	private static final int sleepDelay = 15; // in seconds
	
	private static final String LOG_NAME = NetworkChangeReceiver.class.getName();

	private static Collection<IConnectivityEventListener> listeners = new CopyOnWriteArrayList<IConnectivityEventListener>();

	private static ScheduledExecutorService connectionFutureWorker = Executors.newSingleThreadScheduledExecutor();
	private static ScheduledFuture<?> connectionDataFuture = null;
	private static Boolean oldConnectionAvailabilityState = null;	
	
	private static ScheduledExecutorService wifiFutureWorker = Executors.newSingleThreadScheduledExecutor();
	private static ScheduledFuture<?> wifiFuture = null;
	private static Boolean oldWifiAvailabilityState = null;

	private static ScheduledExecutorService mobileFutureWorker = Executors.newSingleThreadScheduledExecutor();
	private static ScheduledFuture<?> mobileDataFuture = null;
	private static Boolean oldMobileDataAvailabilityState = null;
	
	@Override
	public void onReceive(final Context context, final Intent intent) {
		final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		final NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		
		final boolean newWifiAvailabilityState = wifi.isConnected();
		final boolean newMobileDataAvailabilityState = mobile.isConnected();
		final boolean newConnectionAvailabilityState = newWifiAvailabilityState || newMobileDataAvailabilityState;
		
		// set the old state if it's the first time through!
		if(oldWifiAvailabilityState == null) {
			oldWifiAvailabilityState = !newWifiAvailabilityState;
		}
		
		if(oldMobileDataAvailabilityState == null) {
			oldMobileDataAvailabilityState = !newMobileDataAvailabilityState;
		}
		
		if(oldConnectionAvailabilityState == null) {
			oldConnectionAvailabilityState = !newConnectionAvailabilityState;
		}
		
		// was there a change in wifi?
		if (oldWifiAvailabilityState ^ newWifiAvailabilityState) {
			// is wifi now on?
			if(newWifiAvailabilityState) {
				Runnable task = new Runnable() {
					public void run() {
						Log.d(LOG_NAME, "WIFI IS ON");
						for (IConnectivityEventListener listener : listeners) {
							listener.onWifiConnected();
						}
					}
				};
				wifiFuture = wifiFutureWorker.schedule(task, sleepDelay, TimeUnit.SECONDS);	
			} else {
				if(wifiFuture != null) {
					wifiFuture.cancel(false);
					wifiFuture = null;
				}
				Log.d(LOG_NAME, "WIFI IS OFF");
				for (IConnectivityEventListener listener : listeners) {
					listener.onWifiDisconnected();
				}
			}
		}
		
		// was there a change in mobile data?
		if (oldMobileDataAvailabilityState ^ newMobileDataAvailabilityState) {
			// is mobile data now on?
			if(newMobileDataAvailabilityState) {
				Runnable task = new Runnable() {
					public void run() {
						Log.d(LOG_NAME, "MOBILE DATA IS ON");
						for (IConnectivityEventListener listener : listeners) {
							listener.onMobileDataConnected();
						}
					}
				};
				mobileDataFuture = mobileFutureWorker.schedule(task, sleepDelay, TimeUnit.SECONDS);	
			} else {
				if(mobileDataFuture != null) {
					mobileDataFuture.cancel(false);
					mobileDataFuture = null;
				}
				Log.d(LOG_NAME, "MOBILE DATA IS OFF");
				for (IConnectivityEventListener listener : listeners) {
					listener.onMobileDataDisconnected();
				}
			}
		}
		
		// was there a change in general connectivity?
		if (oldConnectionAvailabilityState ^ newConnectionAvailabilityState) {
			// is mobile data now on?
			if(newConnectionAvailabilityState) {
				Runnable task = new Runnable() {
					public void run() {
						Log.d(LOG_NAME, "CONNECTIVITY IS ON");
						for (IConnectivityEventListener listener : listeners) {
							listener.onAnyConnected();
						}
					}
				};
				connectionDataFuture = connectionFutureWorker.schedule(task, sleepDelay, TimeUnit.SECONDS);	
			} else {
				if(connectionDataFuture != null) {
					connectionDataFuture.cancel(false);
					connectionDataFuture = null;
				}
				Log.d(LOG_NAME, "CONNECTIVITY IS OFF");
				for (IConnectivityEventListener listener : listeners) {
					listener.onAllDisconnected();
				}
			}
		}
		
		// set the old states!
		oldWifiAvailabilityState = newWifiAvailabilityState;
		oldMobileDataAvailabilityState = newMobileDataAvailabilityState;
		oldConnectionAvailabilityState = newConnectionAvailabilityState;
	}

	@Override
	public boolean addListener(IConnectivityEventListener listener) {
		return listeners.add(listener);
	}

	@Override
	public boolean removeListener(IConnectivityEventListener listener) {
		return listeners.remove(listener);
	}
}