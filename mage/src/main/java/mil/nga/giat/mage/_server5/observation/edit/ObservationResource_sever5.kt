package mil.nga.giat.mage._server5.observation.edit

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import mil.nga.giat.mage.R
import mil.nga.giat.mage.sdk.datastore.DaoStore
import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import mil.nga.giat.mage.sdk.http.HttpClientManager
import mil.nga.giat.mage.sdk.http.converter.AttachmentConverterFactory
import mil.nga.giat.mage.sdk.http.resource.ObservationResource.ObservationService
import mil.nga.giat.mage.sdk.utils.MediaUtility
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.*
import java.io.File
import java.lang.Exception
import java.util.HashMap
import javax.inject.Inject

class ObservationResource_server5 @Inject constructor(
   @ApplicationContext val context: Context
) {

   companion object {
      private val LOG_NAME = ObservationResource_server5::class.java.name
   }

   interface ObservationService_server5 {
      @Multipart
      @JvmSuppressWildcards
      @POST("/api/events/{eventId}/observations/{observationId}/attachments")
      fun createAttachment(@Path("eventId") eventId: String, @Path("observationId") observationId: String, @PartMap parts: Map<String, RequestBody>): Call<Attachment?>
   }

   fun createAttachment(attachment: Attachment): Attachment {
      try {
         val baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue))
         val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(AttachmentConverterFactory.create())
            .client(HttpClientManager.getInstance().httpClient())
            .build()

         val service = retrofit.create(ObservationService::class.java)
         val eventId = attachment.observation.event.remoteId
         val observationId = attachment.observation.remoteId
         val parts: MutableMap<String, RequestBody> = HashMap()
         val attachmentFile = File(attachment.localPath)
         val mimeType = MediaUtility.getMimeType(attachment.localPath)
         val fileBody = RequestBody.create(MediaType.parse(mimeType), attachmentFile)
         parts["attachment\"; filename=\"" + attachmentFile.name + "\""] = fileBody
         val response = service.createAttachment(eventId, observationId, attachment.remoteId, parts).execute()

         if (response.isSuccessful) {
            val returnedAttachment = response.body()
            attachment.contentType = returnedAttachment!!.contentType
            attachment.name = returnedAttachment.name
            attachment.remoteId = returnedAttachment.remoteId
            attachment.remotePath = returnedAttachment.remotePath
            attachment.size = returnedAttachment.size
            attachment.url = returnedAttachment.url
            attachment.isDirty = returnedAttachment.isDirty
            DaoStore.getInstance(context).attachmentDao.update(attachment)
         } else {
            Log.e(LOG_NAME, "Bad request.")
            if (response.errorBody() != null) {
               Log.e(LOG_NAME, response.errorBody()!!.string())
            }
         }
      } catch (e: Exception) {
         Log.e(LOG_NAME, "Failure saving observation.", e)
      }
      return attachment
   }
}