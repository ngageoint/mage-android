package mil.nga.giat.mage.observation.attachment

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.observation.AttachmentRepository
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import mil.nga.giat.mage.sdk.datastore.observation.AttachmentHelper
import mil.nga.giat.mage.sdk.utils.MediaUtility
import okhttp3.HttpUrl
import org.apache.commons.lang3.StringUtils
import java.io.*
import javax.inject.Inject

sealed class AttachmentState(val contentType: String) {
   class ImageState(val model: Any, contentType: String): AttachmentState(contentType)
   class MediaState(val uri: Uri, contentType: String): AttachmentState(contentType)
   class OtherState(val uri: Uri, contentType: String): AttachmentState(contentType)
}

data class Shareable(val type: Type, val uri: Uri, val contentType: String) {
   enum class Type { OPEN, SHARE }
}

@HiltViewModel
class AttachmentViewModel @Inject constructor(
   val application: Application,
   val preferences: SharedPreferences,
   private val attachmentRepository: AttachmentRepository
): ViewModel() {
   private val attachmentHelper: AttachmentHelper = AttachmentHelper.getInstance(application)

   private val _attachmentUri = MutableLiveData<AttachmentState>()
   val attachmentUri: LiveData<AttachmentState> = _attachmentUri

   private var attachment: Attachment? = null
   private var attachmentDownloadJob: Job? = null

   fun setAttachment(path: String?) {
      if (path != null && File(path).exists()) {
         val contentType = MediaUtility.getMimeType(path)
         _attachmentUri.value = when {
            contentType.startsWith("image/") -> {
               AttachmentState.ImageState(File(path), contentType)
            }
            contentType.startsWith("video/") -> {
               AttachmentState.MediaState(Uri.fromFile(File(path)), contentType)
            }
            contentType.startsWith("audio/") -> {
               AttachmentState.MediaState(Uri.fromFile(File(path)), contentType)
            }
            else -> {
               AttachmentState.OtherState(Uri.fromFile(File(path)), contentType)
            }
         }
      }
   }

   fun setAttachment(id: Long) {
      viewModelScope.launch(Dispatchers.IO) {
         attachment = attachmentHelper.read(id)
         attachment?.let { attachment ->
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

            val state = when {
               contentType.startsWith("image/") -> {
                  AttachmentState.ImageState(attachment, contentType)
               }
               (contentType.startsWith("video/") || contentType.startsWith("audio/"))-> {
                  if (attachment.localPath != null) {
                     AttachmentState.MediaState(Uri.fromFile(File(attachment.localPath)), contentType)
                  } else {
                     val url = HttpUrl.parse(attachment.url)
                        ?.newBuilder()
                        ?.setQueryParameter("access_token", getToken())
                        ?.toString()

                     AttachmentState.MediaState(Uri.parse(url), contentType)
                  }
               }
               else -> {
                  if (attachment.localPath != null) {
                     AttachmentState.OtherState(Uri.fromFile(File(attachment.localPath)), contentType)
                  } else {
                     val url = HttpUrl.parse(attachment.url)
                        ?.newBuilder()
                        ?.setQueryParameter("access_token", getToken())
                        ?.toString()

                     AttachmentState.OtherState(Uri.parse(url), contentType)
                  }
               }
            }
            _attachmentUri.postValue(state)
         }
      }
   }

   private val _shareable= MutableLiveData<Shareable>()
   val shareable: LiveData<Shareable> = _shareable
   private val _downloadProgress= MutableLiveData<Float?>()
   val downloadProgress: LiveData<Float?> = _downloadProgress
   fun share() {
      attachment?.let { attachment ->
         downloadAttachment(attachment) { file ->
            shareFile(file, attachment.contentType, Shareable.Type.SHARE)
         }
      }
   }

   fun open() {
      attachment?.let { attachment ->
         downloadAttachment(attachment) { file ->
            shareFile(file, attachment.contentType, Shareable.Type.OPEN)
         }
      }
   }

   fun cancelDownload() {
      attachmentDownloadJob?.cancel()
      attachmentDownloadJob = null
   }

   private fun shareFile(source: File, contentType: String, type: Shareable.Type) {
      viewModelScope.launch {
         val destination = attachmentRepository.copyToCacheDir(source)
         val uri = FileProvider.getUriForFile(application, application.packageName + ".fileprovider", destination)
         _shareable.postValue(Shareable(type, uri, contentType))
      }
   }

   private fun downloadAttachment(attachment: Attachment, complete: (file: File) -> Unit) {
      attachmentDownloadJob = viewModelScope.launch {
         val progressChannel = Channel<Float>()
         launch {
            try {
               progressChannel.consumeEach { progress->
                  _downloadProgress.postValue(progress)
               }
            } finally {
               _downloadProgress.postValue(null)
            }
         }

         attachmentRepository.download(attachment, progressChannel)?.let { file ->
            complete(file)
         }
      }
   }

   private fun getToken(): String {
      return preferences.getString(application.getString(R.string.tokenKey), null) ?: ""
   }
}