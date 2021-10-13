package mil.nga.giat.mage.observation.sync

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import mil.nga.giat.mage.R
import mil.nga.giat.mage._server5.observation.edit.ObservationResource_server5
import mil.nga.giat.mage.sdk.Compatibility
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import mil.nga.giat.mage.sdk.datastore.observation.AttachmentHelper
import mil.nga.giat.mage.sdk.http.HttpClientManager
import mil.nga.giat.mage.sdk.http.converter.AttachmentConverterFactory
import mil.nga.giat.mage.sdk.http.resource.ObservationResource
import mil.nga.giat.mage.sdk.utils.MediaUtility
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Retrofit
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.util.*

class AttachmentSyncWorker(var context: Context, params: WorkerParameters) :
   Worker(context, params) {

   private fun Result.withFlag(flag: Int): Int {
      return when (this) {
         is Result.Success -> RESULT_FAILURE_FLAG or flag
         is Result.Retry -> RESULT_RETRY_FLAG or flag
         else -> RESULT_SUCCESS_FLAG or flag
      }
   }

   private fun Int.containsFlag(flag: Int): Boolean {
      return (this or flag) == this
   }

   companion object {
      private val LOG_NAME = AttachmentSyncWorker::class.java.simpleName

      private const val RESULT_SUCCESS_FLAG = 0
      private const val RESULT_FAILURE_FLAG = 1
      private const val RESULT_RETRY_FLAG = 2

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

   override fun doWork(): Result {
      var result = RESULT_SUCCESS_FLAG

      try {
         result = syncAttachments()
      } catch (e: Exception) {
         Log.e(LOG_NAME, "Failed to sync attachments", e)
      }

      return if (result.containsFlag(RESULT_RETRY_FLAG)) {
         Result.retry()
      } else {
         Result.success()
      }
   }

   private fun syncAttachments(): Int {
      var result = RESULT_SUCCESS_FLAG

      val attachmentHelper = AttachmentHelper.getInstance(applicationContext)
      for (attachment in attachmentHelper.dirtyAttachments.filter { !it.observation.remoteId.isNullOrEmpty() }) {
         try {
            result = syncAttachment(attachment).withFlag(result)
         } catch (e: Exception) {
            Log.e(LOG_NAME, "Failed to sync attachment", e)
         }
      }

      return result
   }

   private fun syncAttachment(attachment: Attachment): Result {
      if (!attachment.url.isNullOrEmpty()) {
         Log.i(LOG_NAME, "Already pushed attachment ${attachment.id}, skipping.")
         return Result.success()
      }

      Log.d(LOG_NAME, "Staging attachment with id: ${attachment.id}")
      stageForUpload(attachment)
      Log.d(LOG_NAME, "Pushing attachment with id: ${attachment.id}")

      try {
         val baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue))
         val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl!!)
            .addConverterFactory(AttachmentConverterFactory.create())
            .client(HttpClientManager.getInstance().httpClient())
            .build()

         val eventId = attachment.observation.event.remoteId
         val observationId = attachment.observation.remoteId

         val parts = HashMap<String, RequestBody>()
         val attachmentFile = File(attachment.localPath)
         val mimeType = MediaUtility.getMimeType(attachment.localPath)
         val fileBody = RequestBody.create(MediaType.parse(mimeType), attachmentFile)
         parts["attachment\"; filename=\"" + attachmentFile.name + "\""] = fileBody

         val response = if (Compatibility.isServerVersion5(applicationContext)) {
            val service = retrofit.create(ObservationResource_server5.ObservationService_server5::class.java)
            service.createAttachment(eventId, observationId, parts).execute()
         } else {
            val service = retrofit.create(ObservationResource.ObservationService::class.java)
            service.createAttachment(eventId, observationId, attachment.remoteId, parts).execute()
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

            return Result.success()
         } else {
            Log.e(LOG_NAME, "Bad request.")
            response.errorBody()?.let {
               Log.e(LOG_NAME, it.string())
            }

            return if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) Result.failure() else Result.retry()
         }
      } catch (e: IOException) {
         Log.e(LOG_NAME, "Failure saving attachment.", e)
         return Result.retry()
      }
   }

   fun resizeImage(file: File, scaleTo: Int = 1024) {
      val bmOptions = BitmapFactory.Options()
      bmOptions.inJustDecodeBounds = true
      BitmapFactory.decodeFile(file.absolutePath, bmOptions)
      val photoW = bmOptions.outWidth
      val photoH = bmOptions.outHeight

      // Determine how much to scale down the image
      val scaleFactor = Math.min(photoW / scaleTo, photoH / scaleTo)

      bmOptions.inJustDecodeBounds = false
      bmOptions.inSampleSize = scaleFactor

      val resized = BitmapFactory.decodeFile(file.absolutePath, bmOptions) ?: return
      file.outputStream().use {
         resized.compress(Bitmap.CompressFormat.JPEG, 75, it)
         resized.recycle()
      }
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
}