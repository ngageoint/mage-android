package mil.nga.giat.mage.glide

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.module.AppGlideModule
import mil.nga.giat.mage.glide.loader.*
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import mil.nga.giat.mage.sdk.datastore.user.User
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Created by wnewman
 */
@GlideModule
class MageGlideModule : AppGlideModule() {

    companion object {
        private val DEFAULT_DISK_CACHE_SIZE = 250 * 1024 * 1024
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setLogLevel(Log.ERROR)
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, DEFAULT_DISK_CACHE_SIZE.toLong()))
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.append(Attachment::class.java, InputStream::class.java, AudioLoader.Factory())
        registry.append(Attachment::class.java, ByteBuffer::class.java, VideoUrlLoader.Factory(context))
        registry.append(Attachment::class.java, Bitmap::class.java, VideoFileLoader.Factory())
        registry.append(Attachment::class.java, InputStream::class.java, ImageUrlLoader.Factory(context))
        registry.append(Attachment::class.java, InputStream::class.java, ImageFileLoader.Factory())
        registry.append(User::class.java, InputStream::class.java, AvatarUrlLoader.Factory(context))
    }
}
