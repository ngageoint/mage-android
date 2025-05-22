package mil.nga.giat.mage.glide.loader

import android.net.Uri
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import mil.nga.giat.mage.map.annotation.BaseObservationStyle
import mil.nga.giat.mage.map.annotation.MapAnnotation
import java.io.InputStream

class MapIconLoader private constructor(
    private val uriLoader: ModelLoader<Uri, InputStream>,
) : ModelLoader<MapAnnotation<*>, InputStream> {

    override fun buildLoadData(model: MapAnnotation<*>, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
        val iconStyle = model.style as BaseObservationStyle
        return if (iconStyle.uri == null && !model.allowEmptyIcon) {
            null
        } else if (iconStyle.uri != null) {
            uriLoader.buildLoadData(iconStyle.uri, width, height, options)
        } else {
            val uri = Uri.parse("file:///android_asset/markers/empty.png")
            uriLoader.buildLoadData(uri, 1, 1, options)
        }
    }

    override fun handles(model: MapAnnotation<*>): Boolean = model.style is BaseObservationStyle

    class Factory : ModelLoaderFactory<MapAnnotation<*>, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<MapAnnotation<*>, InputStream> {
            return MapIconLoader(
                multiFactory.build(Uri::class.java, InputStream::class.java),
            )
        }

        override fun teardown() {}
    }
}
