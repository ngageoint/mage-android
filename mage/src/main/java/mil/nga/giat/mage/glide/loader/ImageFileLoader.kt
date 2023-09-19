package mil.nga.giat.mage.glide.loader

import android.webkit.MimeTypeMap
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import mil.nga.giat.mage.database.model.observation.Attachment
import java.io.File
import java.io.InputStream

class ImageFileLoader private constructor(private val fileLoader: ModelLoader<File, InputStream>) : ModelLoader<Attachment, InputStream> {

    override fun buildLoadData(model: Attachment, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
        return fileLoader.buildLoadData(File(model.localPath), width, height, options)
    }

    override fun handles(model: Attachment): Boolean {
        model.localPath?.let {
            if (!File(it).exists()) return false

            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(it)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase())
            return mimeType?.contains("image", ignoreCase = true) ?: false
        }

        return false
    }

    class Factory : ModelLoaderFactory<Attachment, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Attachment, InputStream> {
            return ImageFileLoader(multiFactory.build(File::class.java, InputStream::class.java))
        }

        override fun teardown() {}
    }
}
