package mil.nga.giat.mage.network.attachment

import mil.nga.giat.mage.database.model.observation.Attachment
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface AttachmentService {
   @Streaming
   @GET("/api/events/{eventId}/observations/{observationId}/attachments/{attachmentId}")
   suspend fun download(
      @Path("eventId") eventId: String,
      @Path("observationId") observationId: String,
      @Path("attachmentId") attachmentId: String
   ): Response<ResponseBody>

   @Multipart
   @JvmSuppressWildcards
   @PUT("/api/events/{eventId}/observations/{observationId}/attachments/{attachmentId}")
   suspend fun createAttachment(
      @Path("eventId") eventId: String,
      @Path("observationId") observationId: String,
      @Path("attachmentId") attachmentId: String,
      /*
       this is the only annotation/parameter type combination that works for uploading attachments.
       a better way would be to use @Part("attachment") to hard-code the field name the server
       requires, but that does not work either for some reason, because the server does not receive
       the attachment content.
       */
      @Part content: MultipartBody.Part
   ): Response<Attachment>
}

interface AttachmentService_server5 {
   @Multipart
   @JvmSuppressWildcards
   @POST("/api/events/{eventId}/observations/{observationId}/attachments")
   suspend fun createAttachment(
      @Path("eventId") eventId: String,
      @Path("observationId") observationId: String,
      @Part content: MultipartBody.Part
   ): Response<Attachment>
}