package mil.nga.giat.mage.glide.loader

import android.webkit.MimeTypeMap
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import mil.nga.giat.mage.R
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import java.io.InputStream

class AudioLoader private constructor(private val fileLoader: ModelLoader<Int, InputStream>) : ModelLoader<Attachment, InputStream> {

    override fun buildLoadData(model: Attachment, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
        return fileLoader.buildLoadData(R.drawable.ic_audio_attachment_200dp, width, height, options)
    }

    override fun handles(model: Attachment): Boolean {
        return if (model.contentType != null) {
            model.contentType?.contains("audio", ignoreCase = true) ?: false
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(model.localPath)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase())
            mimeType?.contains("audio", ignoreCase = true) ?: false
        }
    }

    class Factory : ModelLoaderFactory<Attachment, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Attachment, InputStream> {
            return AudioLoader(multiFactory.build(Int::class.java, InputStream::class.java))
        }

        override fun teardown() {}
    }
}
