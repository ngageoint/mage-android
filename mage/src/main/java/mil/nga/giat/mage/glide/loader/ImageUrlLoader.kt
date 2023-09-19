package mil.nga.giat.mage.glide.loader

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader
import mil.nga.giat.mage.database.model.observation.Attachment
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.InputStream

class ImageUrlLoader private constructor(concreteLoader: ModelLoader<GlideUrl, InputStream>) : BaseGlideUrlLoader<Attachment>(concreteLoader, null) {

    class Factory : ModelLoaderFactory<Attachment, InputStream> {

        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Attachment, InputStream> {
            return ImageUrlLoader(multiFactory.build(GlideUrl::class.java, InputStream::class.java))
        }

        override fun teardown() {}
    }

    override fun getUrl(attachment: Attachment, width: Int, height: Int, options: Options): String {
        val url = attachment.url.toHttpUrlOrNull()!!
                .newBuilder()
                .addQueryParameter("size", width.coerceAtLeast(height).toString())
                .build()

        return url.toString()
    }

    override fun handles(model: Attachment): Boolean {
        return model.url != null && model.contentType?.contains("image", ignoreCase = true) ?: false
    }
}
