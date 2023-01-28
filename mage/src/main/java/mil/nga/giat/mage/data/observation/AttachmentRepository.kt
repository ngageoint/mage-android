package mil.nga.giat.mage.data.observation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import mil.nga.giat.mage.R
import mil.nga.giat.mage.di.server5
import mil.nga.giat.mage.network.api.AttachmentService
import mil.nga.giat.mage.network.api.AttachmentService_server5
import mil.nga.giat.mage.observation.sync.AttachmentSyncWorker
import mil.nga.giat.mage.sdk.Compatibility
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import mil.nga.giat.mage.sdk.datastore.observation.AttachmentHelper
import mil.nga.giat.mage.sdk.utils.MediaUtility
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class AttachmentRepository @Inject constructor(
   @ApplicationContext private val context: Context,
   private val attachmentService: AttachmentService,
   @server5 private val attachmentService_server5: AttachmentService_server5
) {

   private val attachmentHelper = AttachmentHelper.getInstance(context)

   suspend fun syncAttachment(attachment: Attachment) = withContext(Dispatchers.IO) {
      Log.d(LOG_NAME, "Staging attachment with id: ${attachment.id}")
      stageForUpload(attachment)
      Log.d(LOG_NAME, "Pushing attachment with id: ${attachment.id}")

      val eventId = attachment.observation.event.remoteId
      val observationId = attachment.observation.remoteId
      val attachmentFile = File(attachment.localPath)
      val mediaTypeSpec = MediaUtility.getMimeType(attachment.localPath) ?: "application/octet-stream"
      val mediaType = MediaType.parse(mediaTypeSpec)
      val contentBody = RequestBody.create(mediaType, attachmentFile)
      val contentPart = MultipartBody.Part.createFormData("attachment", attachmentFile.name, contentBody)

      val response = if (Compatibility.isServerVersion5(context)) {
         attachmentService_server5.createAttachment(eventId, observationId, contentPart)
      } else {
         attachmentService.createAttachment(eventId, observationId, attachment.remoteId, contentPart)
      }

      if (response.isSuccessful) {
         val returnedAttachment = response.body()

         Log.d(LOG_NAME, "Pushed attachment with remote_id: ${returnedAttachment?.remoteId}")

         attachment.contentType = returnedAttachment?.contentType
         attachment.name = returnedAttachment?.name
         attachment.remoteId = returnedAttachment?.remoteId
         attachment.remotePath = returnedAttachment?.remotePath
         attachment.size = returnedAttachment?.size
         attachment.url = returnedAttachment?.url
         attachment.isDirty = false

         AttachmentHelper.getInstance(context).update(attachment)
      } else {
         Log.e(LOG_NAME, "Bad request.")
         response.errorBody()?.let {
            Log.e(LOG_NAME, it.string())
         }
      }

      response
   }

   @Throws(Exception::class)
   fun stageForUpload(attachment: Attachment) {
      val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
      val outImageSize = sharedPreferences.getInt(context.getString(R.string.imageUploadSizeKey), context.resources.getInteger(R.integer.imageUploadSizeDefaultValue))

      val file = File(attachment.localPath)
      val oldExif = ExifInterface(attachment.localPath)
      val attributes = mutableMapOf<String, String>()
      exifTags.forEach { exifTag ->
         oldExif.getAttribute(exifTag)?.let { value ->
            attributes[exifTag] = value
         }
      }

      if (MediaUtility.isImage(file.absolutePath)) {
         // if not original image size
         if (outImageSize > 0) {
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.RGB_565
            options.inSampleSize = 2
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)

            // Scale file
            val inWidth = options.outWidth
            val inHeight = options.outHeight

            var outWidth = outImageSize
            var outHeight = outImageSize

            if (inWidth > inHeight) {
               outWidth = outImageSize
               outHeight = (inHeight.toDouble() / inWidth.toDouble() * outImageSize.toDouble()).toInt()
            } else if (inWidth < inHeight) {
               outWidth = (inWidth.toDouble() / inHeight.toDouble() * outImageSize.toDouble()).toInt()
               outHeight = outImageSize
            }

            val resized = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, true)
            file.outputStream().use {
               resized.compress(Bitmap.CompressFormat.JPEG, 100, it)
               resized.recycle()
            }

            val newExif = ExifInterface(attachment.localPath)
            attributes.forEach {
               newExif.setAttribute(it.key, it.value)
            }
            newExif.saveAttributes()
         }
      }
   }

   suspend fun download(attachment: Attachment, progressChannel: Channel<Float>) = withContext(Dispatchers.IO) {
      val destination: File = when {
         attachment.contentType.startsWith("image/") -> {
            MediaUtility.createImageFile()
         }
         attachment.contentType.startsWith("video/") -> {
            MediaUtility.createVideoFile()
         }
         attachment.contentType.startsWith("audio/") -> {
            MediaUtility.createAudioFile()
         }
         else -> {
            MediaUtility.createFile(MediaUtility.getFileExtension(attachment.name))
         }
      }

      try {
         val observation = attachment.observation
         val event = observation.event
         val response = attachmentService.download(event.remoteId, observation.remoteId, attachment.remoteId)

         if (response.isSuccessful) {
            val body = response.body()!!
            val contentLength = body.contentLength()

            attachment.localPath = destination.absolutePath
            streamFile(body.byteStream(), destination.outputStream(), contentLength, progressChannel)

            val updateAttachment: Attachment = attachmentHelper.read(attachment.id)
            updateAttachment.localPath = attachment.localPath
            attachmentHelper.update(updateAttachment)
         }

         destination
      } catch (e: CancellationException) {
         destination.delete()
         null
      } finally {
         progressChannel.close()
      }
   }

   private suspend fun streamFile(
      input: InputStream,
      output: OutputStream,
      length: Long,
      progressChannel: Channel<Float>
   ) {
      withContext(Dispatchers.IO) {
         input.use { input ->
            output.use { output ->
               var bytesCopied: Long = 0
               val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
               var bytes = input.read(buffer)
               while (bytes >= 0 && isActive) {
                  output.write(buffer, 0, bytes)
                  bytesCopied += bytes
                  val progress = ((bytesCopied.toDouble() / length.toDouble())).toFloat()
                  progressChannel.send(progress)
                  bytes = input.read(buffer)
               }
            }
         }
      }
   }

   suspend fun copyToCacheDir(file: File) = withContext(Dispatchers.IO) {
      val directory = File(context.cacheDir, "attachments")

      if (!directory.exists()) {
         directory.mkdirs()
      }

      val target = File(directory, file.name)

      file.copyTo(target)
   }

   companion object {
      private val LOG_NAME = AttachmentSyncWorker::class.java.simpleName

      private val exifTags = arrayOf(
         ExifInterface.TAG_APERTURE_VALUE,
         ExifInterface.TAG_DATETIME,
         ExifInterface.TAG_DATETIME_DIGITIZED,
         ExifInterface.TAG_DATETIME_ORIGINAL,
         ExifInterface.TAG_EXPOSURE_TIME,
         ExifInterface.TAG_FLASH,
         ExifInterface.TAG_FOCAL_LENGTH,
         ExifInterface.TAG_GPS_ALTITUDE,
         ExifInterface.TAG_GPS_ALTITUDE_REF,
         ExifInterface.TAG_GPS_DATESTAMP,
         ExifInterface.TAG_GPS_LATITUDE,
         ExifInterface.TAG_GPS_LATITUDE_REF,
         ExifInterface.TAG_GPS_LONGITUDE,
         ExifInterface.TAG_GPS_LONGITUDE_REF,
         ExifInterface.TAG_GPS_PROCESSING_METHOD,
         ExifInterface.TAG_GPS_TIMESTAMP,
         ExifInterface.TAG_ISO_SPEED,
         ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
         ExifInterface.TAG_MAKE,
         ExifInterface.TAG_MODEL,
         ExifInterface.TAG_ORIENTATION,
         ExifInterface.TAG_SUBSEC_TIME,
         ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
         ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
         ExifInterface.TAG_WHITE_BALANCE
      )
   }
}