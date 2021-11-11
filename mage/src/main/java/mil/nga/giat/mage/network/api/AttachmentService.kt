package mil.nga.giat.mage.network.api

import mil.nga.giat.mage.sdk.datastore.observation.Attachment
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface AttachmentService {
   @Streaming
   @GET("/api/events/{eventId}/observations/{observationId}/attachments/{attachmentId}")
   suspend fun getAttachment(
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
      @PartMap parts: Map<String, RequestBody>
   ): Response<Attachment>
}

interface AttachmentService_server5 {
   @Multipart
   @JvmSuppressWildcards
   @POST("/api/events/{eventId}/observations/{observationId}/attachments")
   suspend fun createAttachment(
      @Path("eventId") eventId: String,
      @Path("observationId") observationId: String,
      @PartMap parts: Map<String, RequestBody>): Response<Attachment>
}