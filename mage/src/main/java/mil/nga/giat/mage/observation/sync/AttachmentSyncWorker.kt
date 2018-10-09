package mil.nga.giat.mage.observation.sync

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.preference.PreferenceManager
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.RequestBody
import mil.nga.giat.mage.sdk.R
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import mil.nga.giat.mage.sdk.datastore.observation.AttachmentHelper
import mil.nga.giat.mage.sdk.http.HttpClientManager
import mil.nga.giat.mage.sdk.http.converter.AttachmentConverterFactory
import mil.nga.giat.mage.sdk.http.resource.ObservationResource
import mil.nga.giat.mage.sdk.utils.MediaUtility
import retrofit.Retrofit
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.*

class AttachmentSyncWorker(var context: Context, params: WorkerParameters) : Worker(context, params) {

    private fun Worker.Result.withFlag(flag: Int): Int {
        when(this) {
            Worker.Result.FAILURE -> return AttachmentSyncWorker.RESULT_FAILURE_FLAG or flag;
            Worker.Result.RETRY -> return AttachmentSyncWorker.RESULT_RETRY_FLAG or flag;
            else -> return AttachmentSyncWorker.RESULT_SUCCESS_FLAG or flag;
        }
    }

    private fun Int.containsFlag(flag: Int): Boolean {
        return (this or flag) == this
    }

    companion object {
        private val LOG_NAME = AttachmentSyncWorker::class.java.simpleName

        private val RESULT_SUCCESS_FLAG = 0
        private val RESULT_FAILURE_FLAG = 1
        private val RESULT_RETRY_FLAG = 2
    }

    override fun doWork(): Result {
        var result = syncAttachments();

        if (result.containsFlag(AttachmentSyncWorker.RESULT_RETRY_FLAG)) {
            return Result.RETRY
        } else if (result.containsFlag(AttachmentSyncWorker.RESULT_FAILURE_FLAG)) {
            return Result.FAILURE
        } else {
            return Result.SUCCESS
        }
    }

    private fun syncAttachments(): Int {
        var result = AttachmentSyncWorker.RESULT_SUCCESS_FLAG

        val attachmentHelper = AttachmentHelper.getInstance(applicationContext);
        for (attachment in attachmentHelper.dirtyAttachments.filter { !it.observation.remoteId.isNullOrEmpty() }) {
            result = save(attachment).withFlag(result)
        }

        return result
    }

    private fun save(attachment: Attachment): Result {
        if (!attachment.remoteId.isNullOrEmpty()) {
            Log.i(LOG_NAME, "Already pushed attachment ${attachment.id}, skipping.")
            return Result.SUCCESS
        }

        Log.d(LOG_NAME, "Staging attachment with id: ${attachment.id}")
        stageForUpload(attachment)
        Log.d(LOG_NAME, "Pushing attachment with id: ${attachment.id}")

        try {
            val baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue))
            val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(AttachmentConverterFactory.create())
                    .client(HttpClientManager.getInstance(context).httpClient())
                    .build()

            val service = retrofit.create<ObservationResource.ObservationService>(ObservationResource.ObservationService::class.java)

            val eventId = attachment.observation.event.remoteId
            val observationId = attachment.observation.remoteId

            val parts = HashMap<String, RequestBody>()
            val attachmentFile = File(attachment.localPath)
            val mimeType = MediaUtility.getMimeType(attachment.localPath)
            val fileBody = RequestBody.create(MediaType.parse(mimeType), attachmentFile)
            parts["attachment\"; filename=\"" + attachmentFile.getName() + "\""] = fileBody

            val response = service.createAttachment(eventId, observationId, parts).execute()

            if (response.isSuccess) {
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

                return Result.SUCCESS
            } else {
                Log.e(LOG_NAME, "Bad request.")
                response.errorBody().let {
                    Log.e(LOG_NAME, it?.string())
                }

                return if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) Result.FAILURE else Result.RETRY
            }
        } catch (e: IOException) {
            Log.e(LOG_NAME, "Failure saving observation.", e)
            return Result.RETRY
        }
    }

    @Throws(Exception::class)
    fun stageForUpload(attachment: Attachment) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val outImageSize = sharedPreferences.getInt(context.getString(R.string.imageUploadSizeKey), context.getResources().getInteger(R.integer.imageUploadSizeDefaultValue))

        val file = File(attachment.localPath)
        if (MediaUtility.isImage(file.absolutePath)) {
            // if not original image size
            if (outImageSize > 0) {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                val imageFileName = "MAGE_$timeStamp"
                val directory = context.getExternalFilesDir("media")
                val thumbnail = File.createTempFile(
                        imageFileName, /* prefix */
                        ".jpg", /* suffix */
                        directory      /* directory */
                )

                val options = BitmapFactory.Options()
                options.inPreferredConfig = Bitmap.Config.RGB_565
                options.inSampleSize = 2
                var bitmap = BitmapFactory.decodeFile(file.absolutePath, options)

                // Scale file
                val inWidth = bitmap.width
                val inHeight = bitmap.height

                var outWidth = outImageSize
                var outHeight = outImageSize

                if (inWidth > inHeight) {
                    outWidth = outImageSize
                    outHeight = (inHeight.toDouble() / inWidth.toDouble() * outImageSize.toDouble()).toInt()
                } else if (inWidth < inHeight) {
                    outWidth = (inWidth.toDouble() / inHeight.toDouble() * outImageSize.toDouble()).toInt()
                    outHeight = outImageSize
                }
                bitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, true)

                val out = FileOutputStream(thumbnail)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)

                out.flush()
                out.close()
                bitmap.recycle()
                MediaUtility.copyExifData(file, thumbnail)

                attachment.localPath = thumbnail.absolutePath
            }
        }
    }


}