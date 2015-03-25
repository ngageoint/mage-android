package mil.nga.giat.mage.sdk.glide;

import android.content.Context;
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

import java.io.InputStream;

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
		String s = url.toString();
		String token = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.tokenKey), null);
		s += "?access_token=" + token + "&size=" + (width < height ? height : width);
		if (s.contains("avatar")) {
			// this is a user avatar, let's defeat the cache on them
			s += "&_dc=" + System.currentTimeMillis();
		}
		Log.d(LOG_NAME, "Loading image: " + s);
		url = new GlideUrl(s);
		return super.getResourceFetcher(url, width, height);
	}

}
