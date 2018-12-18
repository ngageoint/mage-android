package mil.nga.giat.mage.glide.loader

import android.webkit.MimeTypeMap
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import mil.nga.giat.mage.sdk.datastore.user.User
import java.io.File
import java.io.InputStream

class AvatarFileLoader private constructor(private val fileLoader: ModelLoader<File, InputStream>) : ModelLoader<User, InputStream> {

    override fun buildLoadData(model: User, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
        return fileLoader.buildLoadData(File(model.userLocal.localAvatarPath), width, height, options)
    }

    override fun handles(user: User): Boolean {
        user.userLocal?.localAvatarPath?.let {
            if (!File(it).exists()) return false

            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(it)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase())
            return mimeType?.contains("image", ignoreCase = true) ?: false
        }

        return false
    }

    class Factory : ModelLoaderFactory<User, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<User, InputStream> {
            return AvatarFileLoader(multiFactory.build(File::class.java, InputStream::class.java))
        }

        override fun teardown() {}
    }
}
