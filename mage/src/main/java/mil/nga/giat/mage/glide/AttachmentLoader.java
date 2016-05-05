package mil.nga.giat.mage.glide;

import android.content.Context;
import android.preference.PreferenceManager;

import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.Headers;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.squareup.okhttp.HttpUrl;

import java.io.InputStream;

import mil.nga.giat.mage.sdk.datastore.observation.Attachment;

public class AttachmentLoader extends BaseGlideUrlLoader<Attachment> {

	private Context context;

	public AttachmentLoader(Context context) {
		super(context);

		this.context = context;
	}

	@Override
	protected Headers getHeaders(Attachment model, int width, int height) {
		String token = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(mil.nga.giat.mage.sdk.R.string.tokenKey), null);
		return new LazyHeaders.Builder().addHeader("Authorization", "Bearer " + token).build();
	}

	@Override
	protected String getUrl(Attachment attachment, int width, int height) {
		if (attachment.getUrl() == null) return null;

		HttpUrl url = HttpUrl.parse(attachment.getUrl())
				.newBuilder()
				.addQueryParameter("size", String.valueOf(Math.max(width, height)))
				.build();

		return url.toString();
	}

	public static class Factory implements ModelLoaderFactory<Attachment, InputStream> {
		@Override public StreamModelLoader<Attachment> build(Context context, GenericLoaderFactory factories) {
			return new AttachmentLoader(context);
		}

		@Override public void teardown() {
		}
	}
}
