package mil.nga.giat.mage.sdk.retrofit;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.event.IEventDispatcher;
import mil.nga.giat.mage.sdk.event.IUserEventListener;
import mil.nga.giat.mage.sdk.utils.UserUtility;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

/**
 * Always use the {@link HttpClient#httpClient} for making ALL
 * requests to the server. This class adds request and response interceptors to
 * pass things like a token and handle errors like 403 and 401.
 *
 * @author newmanw
 */
public class HttpClient implements IEventDispatcher<IUserEventListener> {

    private static final String LOG_NAME = HttpClient.class.getName();

    private static OkHttpClient httpClient;

    private static Collection<IUserEventListener> listeners = new CopyOnWriteArrayList<>();

    public static OkHttpClient httpClient(final Context context) {
        if (httpClient == null) {
            httpClient = new OkHttpClient();

            httpClient.interceptors().add(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request.Builder builder = chain.request().newBuilder();

                    // add token
                    String token = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.tokenKey), null);
                    if (token != null && !token.trim().isEmpty()) {
                        builder.addHeader("Authorization", "Bearer " + token);
                    }

                    // add Accept-Encoding:gzip
                    builder.addHeader("Accept-Encoding", "gzip");

                    Response response = chain.proceed(builder.build());

                    int statusCode = response.code();
                    if (statusCode == HTTP_UNAUTHORIZED) {
                        UserUtility.getInstance(context).clearTokenInformation();
                        for (IUserEventListener listener : listeners) {
                            listener.onTokenExpired();
                        }
                        Log.w(LOG_NAME, "TOKEN EXPIRED");
                    } else if (statusCode == HTTP_NOT_FOUND) {
                        Log.w(LOG_NAME, "404 Not Found.");
                    }

//                    // unzip if needed
//                    HttpEntity entity = response.getEntity();
//                    Header ceheader = entity.getContentEncoding();
//                    if (ceheader != null) {
//                        for (HeaderElement codec : ceheader.getElements()) {
//                            if (codec.getName().equalsIgnoreCase("gzip")) {
//                                response.setEntity(new GzipDecompressingEntity(response.getEntity()));
//                                return;
//                            }
//                        }
//                    }

                    return response;
                }
            });
        }

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