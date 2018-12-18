package mil.nga.giat.mage.glide.loader

import android.content.Context
import android.preference.PreferenceManager
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.*
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader
import mil.nga.giat.mage.sdk.datastore.user.User
import okhttp3.HttpUrl
import java.io.InputStream

class AvatarUrlLoader private constructor(private val context: Context, concreteLoader: ModelLoader<GlideUrl, InputStream>) : BaseGlideUrlLoader<User>(concreteLoader, null) {

    class Factory(private val context: Context) : ModelLoaderFactory<User, InputStream> {

        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<User, InputStream> {
            return AvatarUrlLoader(context, multiFactory.build(GlideUrl::class.java, InputStream::class.java))
        }

        override fun teardown() {}
    }

    override fun getHeaders(model: User?, width: Int, height: Int, options: Options?): Headers? {
        val token = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(mil.nga.giat.mage.sdk.R.string.tokenKey), "")
        return LazyHeaders.Builder().addHeader("Authorization", "Bearer $token").build()
    }

    override fun getUrl(user: User, width: Int, height: Int, options: Options): String? {
        val url = HttpUrl.parse(user.avatarUrl)!!
                .newBuilder()
                .addQueryParameter("size", Math.max(width, height).toString())
                .build()

        return url.toString()
    }

    override fun handles(user: User): Boolean {
        return user.avatarUrl != null
    }
}
