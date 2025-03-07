package mil.nga.giat.mage.geopackage.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import java.io.File


@AndroidEntryPoint
class GeoPackageMediaActivity : AppCompatActivity() {
   private lateinit var viewModel: GeoPackageMediaViewModel

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      viewModel = ViewModelProvider(this).get(GeoPackageMediaViewModel::class.java)

      val geoPackageName = intent.getStringExtra(GEOPACKAGE_NAME)
      require(geoPackageName != null) { "GEOPACKAGE_NAME is required to launch GeoPackageMediaActivity" }

      val mediaTable = intent.getStringExtra(GEOPACKAGE_MEDIA_TABLE)
      require(mediaTable != null) { "GEOPACKAGE_MEDIA_TABLE is required to launch GeoPackageMediaActivity" }

      val mediaId = intent.getLongExtra(GEOPACKAGE_MEDIA_ID, -1)
      require(mediaId != -1L) { "GEOPACKAGE_MEDIA_ID is required to launch GeoPackageMediaActivity" }

      viewModel.setMedia(geoPackageName, mediaTable, mediaId)

      setContent {
         GeoPackageMediaScreen(
            liveData = viewModel.geoPackageMedia,
            onOpen = { onOpen(it) },
            onClose = { finish() }
         )
      }
   }

   private fun onOpen(media: GeoPackageMedia) {
      val uri = FileProvider.getUriForFile(this, application.packageName + ".fileprovider", File(media.path))
      val intent = Intent(Intent.ACTION_VIEW).apply {
         type = contentResolver.getType(uri)
         setDataAndType(uri,contentResolver.getType(uri))
          flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
      }

      startActivity(intent)
   }

   companion object {
      private const val GEOPACKAGE_NAME = "GEOPACKAGE_NAME"
      private const val GEOPACKAGE_MEDIA_TABLE = "GEOPACKAGE_MEDIA_TABLE"
      private const val GEOPACKAGE_MEDIA_ID = "GEOPACKAGE_MEDIA_ID"

      fun intent(context: Context, geoPackage: String, mediaTable: String, mediaId: Long): Intent {
         return Intent(context, GeoPackageMediaActivity::class.java).apply {
            putExtra(GEOPACKAGE_NAME, geoPackage)
            putExtra(GEOPACKAGE_MEDIA_TABLE, mediaTable)
            putExtra(GEOPACKAGE_MEDIA_ID, mediaId)
         }
      }
   }
}