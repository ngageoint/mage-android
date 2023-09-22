package mil.nga.giat.mage.glide.loader

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.webkit.MimeTypeMap

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey

import mil.nga.giat.mage.database.model.observation.Attachment

/**
 * Created by wnewman
 */
class VideoFileLoader : ModelLoader<Attachment, Bitmap> {

    class Factory : ModelLoaderFactory<Attachment, Bitmap> {

        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Attachment, Bitmap> {
            return VideoFileLoader()
        }

        override fun teardown() {}
    }

    class AttachmentVideoDataFetcher internal constructor(private val model: Attachment) : DataFetcher<Bitmap> {

        override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Bitmap>) {
            val bitmap = ThumbnailUtils.createVideoThumbnail(model.localPath, MediaStore.Video.Thumbnails.MINI_KIND)
            callback.onDataReady(bitmap)
        }

        override fun cleanup() {}

        override fun cancel() {}

        override fun getDataClass(): Class<Bitmap> {
            return Bitmap::class.java
        }

        override fun getDataSource(): DataSource {
            return DataSource.LOCAL
        }
    }

    override fun buildLoadData(model: Attachment, width: Int, height: Int, options: Options): ModelLoader.LoadData<Bitmap>? {
        return ModelLoader.LoadData(ObjectKey(model.localPath), AttachmentVideoDataFetcher(model))
    }

    override fun handles(model: Attachment): Boolean {
        model.localPath?.let {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(it)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase())
            return mimeType?.contains("video", ignoreCase = true) ?: false
        }

        return false
    }
}
