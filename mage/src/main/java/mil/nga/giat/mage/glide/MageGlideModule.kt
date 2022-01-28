package mil.nga.giat.mage.glide

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import mil.nga.giat.mage.glide.loader.*
import mil.nga.giat.mage.glide.model.Avatar
import mil.nga.giat.mage.map.annotation.MapAnnotation
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import mil.nga.giat.mage.sdk.http.HttpClientManager
import java.io.InputStream
import java.nio.ByteBuffer

@GlideModule
class MageGlideModule : AppGlideModule() {

    companion object {
        private const val DEFAULT_DISK_CACHE_SIZE = 250 * 1024 * 1024
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, DEFAULT_DISK_CACHE_SIZE.toLong()))
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(HttpClientManager.getInstance().httpClient()))

        registry.append(Attachment::class.java, InputStream::class.java, AudioLoader.Factory())
        registry.append(Attachment::class.java, ByteBuffer::class.java, VideoUrlLoader.Factory(context))
        registry.append(Attachment::class.java, Bitmap::class.java, VideoFileLoader.Factory())
        registry.append(Attachment::class.java, InputStream::class.java, ImageUrlLoader.Factory(context))
        registry.append(Attachment::class.java, InputStream::class.java, ImageFileLoader.Factory())
        registry.append(Avatar::class.java, InputStream::class.java, AvatarLoader.Factory(context))
        registry.append(MapAnnotation::class.java, InputStream::class.java, MapIconLoader.Factory())
    }
}
