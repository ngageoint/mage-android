package mil.nga.giat.mage.compat.server5.observation.edit

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mil.nga.giat.mage.database.model.observation.Attachment
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PartMap
import retrofit2.http.Path
import javax.inject.Inject

class ObservationResource_server5 @Inject constructor(
   @ApplicationContext val context: Context
) {
   interface ObservationService_server5 {
      @Multipart
      @JvmSuppressWildcards
      @POST("/api/events/{eventId}/observations/{observationId}/attachments")
      fun createAttachment(@Path("eventId") eventId: String, @Path("observationId") observationId: String, @PartMap parts: Map<String, RequestBody>): Call<Attachment?>
   }
}