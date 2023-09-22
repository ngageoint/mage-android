package mil.nga.giat.mage.glide

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.Excludes
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpLibraryGlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mil.nga.giat.mage.database.model.observation.Attachment
import mil.nga.giat.mage.glide.loader.AudioLoader
import mil.nga.giat.mage.glide.loader.AvatarLoader
import mil.nga.giat.mage.glide.loader.DocumentLoader
import mil.nga.giat.mage.glide.loader.ImageFileLoader
import mil.nga.giat.mage.glide.loader.ImageUrlLoader
import mil.nga.giat.mage.glide.loader.MapIconLoader
import mil.nga.giat.mage.glide.loader.VideoFileLoader
import mil.nga.giat.mage.glide.loader.VideoUrlLoader
import mil.nga.giat.mage.glide.model.Avatar
import mil.nga.giat.mage.map.annotation.MapAnnotation
import okhttp3.OkHttpClient
import java.io.InputStream
import java.nio.ByteBuffer

@GlideModule
@Excludes(OkHttpLibraryGlideModule::class)
class MageGlideModule : AppGlideModule() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface GlideEntryPoint {
        fun provideOkHttpClient(): OkHttpClient
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, DEFAULT_DISK_CACHE_SIZE.toLong()))
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val entryPoint = EntryPoints.get(context, GlideEntryPoint::class.java)
        val httpClient = entryPoint.provideOkHttpClient()
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(httpClient))

        registry.append(Attachment::class.java, InputStream::class.java, AudioLoader.Factory())
        registry.append(Attachment::class.java, ByteBuffer::class.java, VideoUrlLoader.Factory(context))
        registry.append(Attachment::class.java, Bitmap::class.java, VideoFileLoader.Factory())
        registry.append(Attachment::class.java, InputStream::class.java, ImageUrlLoader.Factory())
        registry.append(Attachment::class.java, InputStream::class.java, ImageFileLoader.Factory())
        registry.append(Attachment::class.java, InputStream::class.java, DocumentLoader.Factory())
        registry.append(Avatar::class.java, InputStream::class.java, AvatarLoader.Factory(context))
        registry.append(MapAnnotation::class.java, InputStream::class.java, MapIconLoader.Factory())
    }

    companion object {
        private const val DEFAULT_DISK_CACHE_SIZE = 250 * 1024 * 1024
    }
}
