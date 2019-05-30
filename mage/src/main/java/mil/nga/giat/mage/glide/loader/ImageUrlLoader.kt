package mil.nga.giat.mage.glide.loader

import android.content.Context
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import okhttp3.HttpUrl
import java.io.InputStream

class ImageUrlLoader private constructor(private val context: Context, concreteLoader: ModelLoader<GlideUrl, InputStream>) : BaseGlideUrlLoader<Attachment>(concreteLoader, null) {

    class Factory(private val context: Context) : ModelLoaderFactory<Attachment, InputStream> {

        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Attachment, InputStream> {
            return ImageUrlLoader(context, multiFactory.build(GlideUrl::class.java, InputStream::class.java))
        }

        override fun teardown() {}
    }

    override fun getUrl(attachment: Attachment, width: Int, height: Int, options: Options): String? {
        val url = HttpUrl.parse(attachment.url)!!
                .newBuilder()
                .addQueryParameter("size", Math.max(width, height).toString())
                .build()

        return url.toString()
    }

    override fun handles(model: Attachment): Boolean {
        return model.url != null && model.contentType?.contains("image", ignoreCase = true) ?: false
    }
}
