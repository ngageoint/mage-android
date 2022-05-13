package mil.nga.giat.mage.observation.attachment

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AttachmentViewActivity: AppCompatActivity() {
   private val viewModel: AttachmentViewModel by viewModels()

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      lifecycleScope.launch(Dispatchers.IO) {
         Glide.get(applicationContext).clearDiskCache();
      }

      val attachmentId = intent.getLongExtra(ATTACHMENT_ID_EXTRA, -1)
      viewModel.setAttachment(attachmentId)

      val attachmentPath = intent.getStringExtra(ATTACHMENT_PATH_EXTRA)
      viewModel.setAttachment(attachmentPath)

      setContent {
         AttachmentViewScreen(
            liveData = viewModel.attachmentUrl,
            onClose = { finish() }
         )
      }
   }

   companion object {
      const val ATTACHMENT_ID_EXTRA = "ATTACHMENT_ID_EXTRA"
      const val ATTACHMENT_PATH_EXTRA = "ATTACHMENT_PATH_EXTRA"
   }
}