package mil.nga.giat.mage.geopackage.media

import android.app.Application
import android.webkit.MimeTypeMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mil.nga.geopackage.GeoPackageCache
import mil.nga.geopackage.GeoPackageFactory
import mil.nga.geopackage.extension.related.RelatedTablesExtension
import java.io.File
import javax.inject.Inject

enum class GeoPackageMediaType {
   IMAGE,
   VIDEO,
   AUDIO,
   OTHER;

   companion object {
      fun fromContentType(contentType: String): GeoPackageMediaType {
         return when {
            contentType.contains("image/") -> IMAGE
            contentType.contains("video/") -> VIDEO
            contentType.contains("audio/") -> AUDIO
            else -> OTHER
         }
      }
   }
}
data class GeoPackageMedia(val path: String, val type: GeoPackageMediaType)

@HiltViewModel
open class GeoPackageMediaViewModel @Inject constructor(
   val application: Application
) : ViewModel() {
   private var file: File? = null

   private val geoPackageCache = GeoPackageCache(GeoPackageFactory.getManager(application))

   private val _geoPackageMedia = MutableLiveData<GeoPackageMedia>()
   val geoPackageMedia: LiveData<GeoPackageMedia> = _geoPackageMedia

   override fun onCleared() {
      super.onCleared()

      file?.delete()
      file = null
   }

   fun setMedia(geoPackageName: String, mediaTable: String, mediaId: Long) {
      viewModelScope.launch(Dispatchers.IO) {
         getMedia(geoPackageName, mediaTable, mediaId)?.let { media ->
            _geoPackageMedia.postValue(media)
         }
      }
   }

   private fun getMedia(
      geoPackageName: String,
      mediaTable: String,
      mediaId: Long
   ): GeoPackageMedia? {
      val geoPackage = geoPackageCache.getOrOpen(geoPackageName)
      val relatedTablesExtension = RelatedTablesExtension(geoPackage)
      val mediaDao = relatedTablesExtension.getMediaDao(mediaTable)
      val mediaRow = mediaDao.getRow(mediaDao.queryForIdRow(mediaId))
      return mediaRow.columns.getColumnIndex("content_type", false)?.let { index ->
         val contentType = mediaRow.getValue(index).toString()
         val path = saveMedia(geoPackageName, mediaTable, mediaId, mediaRow.data, contentType)
         GeoPackageMedia(path, GeoPackageMediaType.fromContentType(contentType))
      }
   }

   private fun saveMedia(
      geoPackageName: String,
      mediaTable: String,
      mediaId: Long,
      data: ByteArray,
      contentType: String
   ): String {
      val directory = File(application.cacheDir, "geopackage").apply {
         mkdirs()
      }

      val tempFile = File.createTempFile(
         "${geoPackageName}_${mediaTable}_${mediaId}",
         ".${MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)}",
         directory
      )

      tempFile.writeBytes(data)
      file = tempFile

      return tempFile.absolutePath
   }
}
