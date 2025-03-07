package mil.nga.giat.mage.glide.loader

import android.webkit.MimeTypeMap
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import mil.nga.giat.mage.R
import mil.nga.giat.mage.database.model.observation.Attachment
import java.io.InputStream
import java.util.*

class DocumentLoader private constructor(
    private val loader: ModelLoader<Int, InputStream>
) : ModelLoader<Attachment, InputStream> {

    override fun buildLoadData(model: Attachment, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
        return loader.buildLoadData(R.drawable.ic_attachment_200dp, width, height, options)
    }

    override fun handles(model: Attachment): Boolean {
        val contentType = if (model.contentType == null) {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(model.localPath)
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase(Locale.getDefault()))
        } else model.contentType

        return arrayOf("audio/", "video/", "image/").none { contentType?.startsWith(it) == true }
    }

    class Factory : ModelLoaderFactory<Attachment, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Attachment, InputStream> {
            return DocumentLoader(multiFactory.build(Int::class.java, InputStream::class.java))
        }

        override fun teardown() {}
    }
}
