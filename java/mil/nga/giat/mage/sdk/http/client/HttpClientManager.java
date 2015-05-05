package mil.nga.giat.mage.sdk.http.client;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.security.KeyStore;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.event.IEventDispatcher;
import mil.nga.giat.mage.sdk.event.IUserEventListener;
import mil.nga.giat.mage.sdk.http.entity.GzipDecompressingEntity;
import mil.nga.giat.mage.sdk.utils.UserUtility;

/**
 * Always use the {@link HttpClientManager#getHttpClient()} for making ALL
 * requests to the server. This class adds request and response interceptors to
 * pass things like a token and handle errors like 403 and 401.
 *
 * @author wiedemanns
 *
 */
public class HttpClientManager implements IEventDispatcher<IUserEventListener> {

	private static final String LOG_NAME = HttpClientManager.class.getName();

	private static Collection<IUserEventListener> listeners = new CopyOnWriteArrayList<IUserEventListener>();

	private HttpClientManager() {
	}

	private static HttpClientManager httpClientManager;
	private static Context mContext;

	public static HttpClientManager getInstance(final Context context) {
		if (context == null) {
			return null;
		}
		mContext = context;
		if (httpClientManager == null) {
			httpClientManager = new HttpClientManager();
		}
		return httpClientManager;
	}

	public DefaultHttpClient getHttpClient() {
		BasicHttpParams params = new BasicHttpParams();
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		// register http?
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));

		SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
		// Some servers don't have a CA...
		Boolean noServerCA = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(mContext.getString(R.string.noServerCAKey), mContext.getResources().getBoolean(R.bool.noServerCADefaultValue));
		if(noServerCA) {
			try {
				KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
				trustStore.load(null, null);
				sslSocketFactory = new NoCASSLSocketFactory(trustStore);
			} catch(Exception e) {
				Log.e(LOG_NAME, "Problem overwriting trust manager.");
			}
		}
		schemeRegistry.register(new Scheme("https", sslSocketFactory, 443));
		ClientConnectionManager cm = new SingleClientConnManager(params, schemeRegistry);
		DefaultHttpClient httpClient = new DefaultHttpClient(cm, params);
		httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 25000);
		httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 30000);
		String userAgent = System.getProperty("http.agent");
		userAgent = (userAgent == null) ? "" : userAgent;
		httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, userAgent);
		// add the token to every request!
		httpClient.addRequestInterceptor(new HttpRequestInterceptor() {
			@Override
			public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {

				// add token
				String token = PreferenceManager.getDefaultSharedPreferences(mContext).getString(mContext.getString(R.string.tokenKey), null);
				if (token != null && !token.trim().isEmpty()) {
					request.addHeader("Authorization", "Bearer " + token);
				}
				// add Accept-Encoding:gzip
				request.addHeader("Accept-Encoding", "gzip");
			}
		});
		httpClient.addResponseInterceptor(new HttpResponseInterceptor() {

			@Override
			public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
				// token is expired?
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == HttpStatus.SC_FORBIDDEN || statusCode == HttpStatus.SC_UNAUTHORIZED) {
					UserUtility.getInstance(mContext).clearTokenInformation();
					for (IUserEventListener listener : listeners) {
						listener.onTokenExpired();
					}
					Log.w(LOG_NAME, "TOKEN EXPIRED");
					return;
				} else if (statusCode == HttpStatus.SC_NOT_FOUND) {
					Log.w(LOG_NAME, "404 Not Found.");
				}

				// unzip if needed
				HttpEntity entity = response.getEntity();
				Header ceheader = entity.getContentEncoding();
				if (ceheader != null) {
					for (HeaderElement codec : ceheader.getElements()) {
						if (codec.getName().equalsIgnoreCase("gzip")) {
							response.setEntity(new GzipDecompressingEntity(response.getEntity()));
							return;
						}
					}
				}
			}
		});
		return httpClient;
	}

	@Override
	public boolean addListener(IUserEventListener listener) {
		return listeners.add(listener);
	}

	@Override
	public boolean removeListener(IUserEventListener listener) {
		return listeners.remove(listener);
	}
}
