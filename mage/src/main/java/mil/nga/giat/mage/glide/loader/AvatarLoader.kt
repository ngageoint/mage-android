package mil.nga.giat.mage.glide.loader

import android.content.Context
import android.text.TextUtils
import androidx.preference.PreferenceManager
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import mil.nga.giat.mage.R
import mil.nga.giat.mage.glide.model.Avatar
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.io.InputStream

class AvatarLoader private constructor(private val context: Context, private val urlLoader: ModelLoader<GlideUrl, InputStream>, private val fileLoader: ModelLoader<File, InputStream>) : ModelLoader<Avatar, InputStream> {
    class Factory(private val context: Context) : ModelLoaderFactory<Avatar, InputStream> {

        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Avatar, InputStream> {
            val urlLoader = multiFactory.build(GlideUrl::class.java, InputStream::class.java)
            val fileLoader = multiFactory.build(File::class.java, InputStream::class.java)

            return AvatarLoader(context, urlLoader, fileLoader)
        }

        override fun teardown() {}
    }

    override fun buildLoadData(model: Avatar, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
        return if (model.localUri != null) {
            fileLoader.buildLoadData(File(model.localUri), width, height, options)
        } else if (model.remoteUri != null) {
            val stringURL = getUrl(model)
            if (TextUtils.isEmpty(stringURL)) {
                return null
            }

            val url = GlideUrl(stringURL)
            urlLoader.buildLoadData(url, width, height, options)
        } else {
            null
        }
    }

    private fun getUrl(user: Avatar): String? {
        if (user.remoteUri == null) return null

        var url = user.remoteUri.toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("_dc", user.lastModified.toString())
                ?.build()

        // TODO can remove this once server bug is fixed to return full avatar url
        if (url == null) {
            PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue))?.let {
                url = (it + user.remoteUri).toHttpUrlOrNull()
                        ?.newBuilder()
                        ?.addQueryParameter("_dc", user.lastModified.toString())
                        ?.build()
            }
        }

        return url?.toString()
    }

    override fun handles(user: Avatar): Boolean {
        return true
    }
}
