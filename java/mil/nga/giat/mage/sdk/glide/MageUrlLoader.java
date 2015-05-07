package mil.nga.giat.mage.sdk.glide;

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.integration.volley.VolleyUrlLoader;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import mil.nga.giat.mage.sdk.R;

public class MageUrlLoader extends VolleyUrlLoader {

	private static final String LOG_NAME = MageUrlLoader.class.getName();
	
	public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {
		private RequestQueue requestQueue;

		public Factory() {
		}

		public Factory(RequestQueue requestQueue) {
			this.requestQueue = requestQueue;
		}

		protected RequestQueue getRequestQueue(Context context) {
			if (requestQueue == null) {
				requestQueue = Volley.newRequestQueue(context);
			}
			return requestQueue;
		}

		@Override
		public ModelLoader<GlideUrl, InputStream> build(Context context, GenericLoaderFactory factories) {
			return new MageUrlLoader(getRequestQueue(context), context);
		}

		@Override
		public void teardown() {
			if (requestQueue != null) {
				requestQueue.stop();
				requestQueue.cancelAll(new RequestQueue.RequestFilter() {

					@Override
					public boolean apply(Request<?> request) {
						return true;
					}
				});
				requestQueue = null;
			}

		}
	}

	private Context context;

	public MageUrlLoader(RequestQueue requestQueue, Context context) {
		super(requestQueue);
		this.context = context.getApplicationContext();
	}
	
	
	@Override
	public DataFetcher<InputStream> getResourceFetcher(GlideUrl url, int width, int height) {
		try {
			List<NameValuePair> params = URLEncodedUtils.parse(url.toURL().toURI(), "UTF-8");
			Uri.Builder uriBuilder = Uri.parse(url.toURL().toURI().toString()).buildUpon();
			String token = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.tokenKey), null);
			uriBuilder.appendQueryParameter("access_token", token);
			uriBuilder.appendQueryParameter("size", String.valueOf(Math.max(width, height)));

			for (NameValuePair param : params) {
				if(param.getName() != null && param.getName().equalsIgnoreCase("avatar")) {
					uriBuilder.appendQueryParameter("_dc", String.valueOf(System.currentTimeMillis()));
					break;
				}
			}

			url = new GlideUrl(uriBuilder.build().toString());
			Log.d(LOG_NAME, "Loading image: " + url);
		} catch(Exception e) {
			Log.e(LOG_NAME, e.getMessage(), e);
		}
		return super.getResourceFetcher(url, width, height);
	}

}
