package mil.nga.giat.mage.sdk.http;

import android.app.Application;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.event.IEventDispatcher;
import mil.nga.giat.mage.sdk.event.ISessionEventListener;
import mil.nga.giat.mage.sdk.utils.UserUtility;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

/**
 * Always use the {@link HttpClientManager#httpClient} for making ALL
 * requests to the server. This class adds request and response interceptors to
 * pass things like a token and handle errors like 403 and 401.
 *
 * @author newmanw
 */
public class HttpClientManager implements IEventDispatcher<ISessionEventListener> {

    private static final String LOG_NAME = HttpClientManager.class.getName();

    private static HttpClientManager instance;

    private Application context;
    private String userAgent;
    private OkHttpClient client;

    private Collection<ISessionEventListener> listeners = new CopyOnWriteArrayList<>();

    public static synchronized HttpClientManager initialize(Application context) {
        if (instance != null) {
            throw new Error("attempt to initialize " + HttpClientManager.class.getName() + " singleton more than once");
        }

        String userAgent = System.getProperty("http.agent");
        userAgent = userAgent == null ? "" : userAgent;

        instance = new HttpClientManager(context, userAgent);

        return instance;
    }

    public static HttpClientManager getInstance() {
        return instance;
    }

    private HttpClientManager(Application context, String userAgent) {
        this.context = context;
        this.userAgent = userAgent;

        initializeClient();
    }

    private void initializeClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);

        builder.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request.Builder builder = chain.request().newBuilder();

                // add token
                String token = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.tokenKey), null);
                if (token != null && !token.trim().isEmpty()) {
                    builder.addHeader("Authorization", "Bearer " + token);
                }

                builder.addHeader("User-Agent", userAgent);

                Response response = chain.proceed(builder.build());

                int statusCode = response.code();
                if (statusCode == HTTP_UNAUTHORIZED) {
                    UserUtility userUtility = UserUtility.getInstance(context);

                    // If token has not expired yet, expire it and send notification to listeners
                    if (!userUtility.isTokenExpired()) {
                        UserUtility.getInstance(context).clearTokenInformation();

                        for (ISessionEventListener listener : listeners) {
                            listener.onTokenExpired();
                        }
                    }

                    Log.w(LOG_NAME, "TOKEN EXPIRED");
                } else if (statusCode == HTTP_NOT_FOUND) {
                    Log.w(LOG_NAME, "404 Not Found.");
                }

                return response;
            }
        });

        client = builder.build();
    }

    public OkHttpClient httpClient() {
        return client;
    }

    @Override
    public boolean addListener(ISessionEventListener listener) {
        return listeners.add(listener);
    }

    @Override
    public boolean removeListener(ISessionEventListener listener) {
        return listeners.remove(listener);
    }

}