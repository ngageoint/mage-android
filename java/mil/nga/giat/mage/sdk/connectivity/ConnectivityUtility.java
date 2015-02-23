package mil.nga.giat.mage.sdk.connectivity;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import com.google.common.base.Predicate;

/**
 * Utility that deals with network connectivity.
 * 
 * @author wiedemanns
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

	public static void isResolvable(String hostname, Predicate<Exception> callback) {
		new IsResolvable(callback).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, hostname);
	}
	
	private static class IsResolvable extends AsyncTask<String, Void, Exception> {

        private Predicate<Exception> callback = null;

        public IsResolvable (Predicate<Exception> callback) {
            this.callback = callback;
        }

		@Override
		protected Exception doInBackground(String... arg0) {
			try {
				InetAddress.getByName(arg0[0]);
			} catch (UnknownHostException e) {
                return e;
			}
			return null;
		}

        @Override
        protected void onPostExecute(Exception e) {
            super.onPostExecute(e);
            if(callback != null) {
                callback.apply(e);
            }
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
}