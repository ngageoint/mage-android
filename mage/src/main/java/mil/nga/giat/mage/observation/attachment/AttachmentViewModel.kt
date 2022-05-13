package mil.nga.giat.mage.observation.attachment

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mil.nga.giat.mage.R
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import mil.nga.giat.mage.sdk.datastore.observation.AttachmentHelper
import mil.nga.giat.mage.sdk.utils.MediaUtility
import okhttp3.HttpUrl
import org.apache.commons.lang3.StringUtils
import java.io.File
import javax.inject.Inject

sealed class AttachmentState(val contentType: String)
class AttachmentModel(val attachment: Attachment, contentType: String) : AttachmentState(contentType)
class AttachmentFile(val uri: Uri, contentType: String) : AttachmentState(contentType)
class AttachmentUrl(val uri: Uri, contentType: String) : AttachmentState(contentType)

@HiltViewModel
class AttachmentViewModel @Inject constructor(
   val application: Application,
   val preferences: SharedPreferences
): ViewModel() {
   private val attachmentHelper: AttachmentHelper = AttachmentHelper.getInstance(application)

   private val _attachmentUri = MutableLiveData<AttachmentState>()
   val attachmentUrl: LiveData<AttachmentState> = _attachmentUri

   fun setAttachment(path: String?) {
      if (path != null && File(path).exists()) {
         val uri = Uri.fromFile(File(path))
         _attachmentUri.value = AttachmentFile(uri,  MediaUtility.getMimeType(path))
      }
   }

   fun setAttachment(id: Long) {
      if (id >= 0) {
         viewModelScope.launch(Dispatchers.IO) {
            attachmentHelper.read(id)?.let { attachment ->
               val contentType = if (StringUtils.isBlank(attachment.contentType) || "application/octet-stream".equals(attachment.contentType, ignoreCase = true)) {
                  var name: String? = attachment.name
                  if (name == null) {
                     name = attachment.localPath
                     if (name == null) {
                        name = attachment.remotePath
                     }
                  }
                  MediaUtility.getMimeType(name)
               } else attachment.contentType

               val state = if (attachment.localPath != null) {
                  val uri = Uri.fromFile(File(attachment.localPath))
                  AttachmentFile(uri, contentType)
               } else {
                 if (contentType.startsWith("video")) {
                    val token = preferences.getString(application.getString(R.string.tokenKey), null)
                    val url = HttpUrl.parse(attachment.url)
                       ?.newBuilder()
                       ?.setQueryParameter("access_token", token)
                       ?.toString()

                    AttachmentUrl(Uri.parse(url), contentType)
                 } else {
                    AttachmentModel(attachment, contentType)
                 }
               }

               _attachmentUri.postValue(state)
            }
         }
      }
   }
}