package mil.nga.giat.mage.sdk.connectivity;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;

/**
 * Utility that dealing with connection like information. Connectivity, mac
 * address, etc.
 * 
 * @author wiedemannse
 * 
 */
public class ConnectivityUtility {

	private static final String LOG_NAME = ConnectivityUtility.class.getName();
	
	/**
	 * Used to check for connectivity
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isOnline(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

	public static boolean isResolvable(String hostname) throws Exception {
		return new IsResolvable().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, hostname).get(30, TimeUnit.SECONDS);
	}
	
	private static class IsResolvable extends AsyncTask<String, Void, Boolean> {
		@Override
		protected Boolean doInBackground(String... arg0) {
			try {
				InetAddress.getByName(arg0[0]);
			} catch (UnknownHostException e) {
				return false;
			}
			return true;
		}
	}

	public static boolean canConnect(InetAddress address, int port) {
		Socket socket = new Socket();
		SocketAddress socketAddress = new InetSocketAddress(address, port);
		try {
			// Only try for 2 seconds before giving up
			socket.connect(socketAddress, 2000);
		} catch (IOException e) {
			// Something went wrong during the connection
			return false;
		} finally {
			// Always close the socket after we're done
			if (socket.isConnected()) {
				try {
					socket.close();
				} catch (IOException e) {
					// Nothing we can do here
					e.printStackTrace();
				}
			}
		}

		return true;
	}
	
	/**
	 * Get the Wi-Fi mac address, used to login
	 */
	public static String getMacAddress(Context context) {
		WifiManager wifiManager = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE));
		String macAddress = wifiManager.getConnectionInfo().getMacAddress();
		if(macAddress == null && !wifiManager.isWifiEnabled()) {
			wifiManager.setWifiEnabled(true);
		}
//		if (macAddress == null || macAddress.isEmpty()) {
//			try {
//				Collection<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
//				for (NetworkInterface i : interfaces) {
//					if (i != null) {
//						byte[] mac = i.getHardwareAddress();
//						if (mac != null) {
//							StringBuilder buf = new StringBuilder();
//							for (int idx = 0; idx < mac.length; idx++) {
//								buf.append(String.format("%02X:", mac[idx]));								
//							}
//							macAddress = buf.toString().trim();
//							macAddress.substring(0, macAddress.length() - 1);
//						}
//						break;
//					}
//				}
//			} catch (Exception e) {
//				Log.e(LOG_NAME, "Error retriving mac address.", e);
//			}
//		}
		return macAddress;
	}
	
}