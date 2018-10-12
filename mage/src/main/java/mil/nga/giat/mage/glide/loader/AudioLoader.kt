package mil.nga.giat.mage.glide.loader

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
        return model.contentType?.contains("audio", ignoreCase = true) ?: false
    }

    class Factory : ModelLoaderFactory<Attachment, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Attachment, InputStream> {
            return AudioLoader(multiFactory.build(Int::class.java, InputStream::class.java))
        }

        override fun teardown() {}
    }
}
