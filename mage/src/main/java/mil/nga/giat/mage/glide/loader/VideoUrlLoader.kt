package mil.nga.giat.mage.glide.loader

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.support.v7.preference.PreferenceManager
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer


/**
 * Created by wnewman
 */
class VideoUrlLoader private constructor(private val context: Context) : ModelLoader<Attachment, ByteBuffer> {

    class Factory(private val context: Context) : ModelLoaderFactory<Attachment, ByteBuffer> {

        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Attachment, ByteBuffer> {
            return VideoUrlLoader(context)
        }

        override fun teardown() {}
    }

    class AttachmentVideoDataFetcher internal constructor(private val context: Context, private val model: Attachment) : DataFetcher<ByteBuffer> {

        override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in ByteBuffer>) {
            val mediaMetadataRetriever = MediaMetadataRetriever()

            val token = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(mil.nga.giat.mage.sdk.R.string.tokenKey), "")

            try {
                mediaMetadataRetriever.setDataSource(model.url, mapOf("authorization" to "Bearer $token"))
                val bitmap = mediaMetadataRetriever.getFrameAtTime(100)
                callback.onDataReady(bitmapToByte(bitmap))
            } finally {
                mediaMetadataRetriever.release()
            }
        }

        private fun bitmapToByte(bitmap: Bitmap): ByteBuffer {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            return ByteBuffer.wrap(stream.toByteArray())
        }

        override fun cleanup() {}
        override fun cancel() {}

        override fun getDataClass(): Class<ByteBuffer> {
            return ByteBuffer::class.java
        }

        override fun getDataSource(): DataSource {
            return DataSource.REMOTE
        }
    }

    override fun buildLoadData(model: Attachment, width: Int, height: Int, options: Options): ModelLoader.LoadData<ByteBuffer>? {
        return ModelLoader.LoadData(ObjectKey(model.url), AttachmentVideoDataFetcher(context, model))
    }

    override fun handles(model: Attachment): Boolean {
        return model.contentType?.contains("video", ignoreCase = true) ?: false
    }
}
