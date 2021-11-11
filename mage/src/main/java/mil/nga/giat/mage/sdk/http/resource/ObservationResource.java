package mil.nga.giat.mage.sdk.http.resource;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.Map;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.http.HttpClientManager;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PUT;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

/***
 * RESTful communication for observations
 */
public class ObservationResource {

    public interface ObservationService {
        @Streaming
        @GET("/api/events/{eventId}/observations/{observationId}/attachments/{attachmentId}")
        Call<ResponseBody> getAttachment(@Path("eventId") String eventId, @Path("observationId") String observationId, @Path("attachmentId") String attachmentId);

        @Multipart
        @PUT("/api/events/{eventId}/observations/{observationId}/attachments/{attachmentId}")
        Call<Attachment> createAttachment(@Path("eventId") String eventId, @Path("observationId") String observationId, @Path("attachmentId") String attachmentId, @PartMap Map<String, RequestBody> parts);
    }

    private static final String LOG_NAME = ObservationResource.class.getName();

    private final Context context;

    public ObservationResource(Context context) {
        this.context = context;
    }

    public ResponseBody getAttachment(Attachment attachment) throws IOException {
        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(HttpClientManager.getInstance().httpClient())
                .build();

        ObservationService service = retrofit.create(ObservationService.class);

        String eventId = attachment.getObservation().getEvent().getRemoteId();
        String observationId = attachment.getObservation().getRemoteId();
        String attachmentId = attachment.getRemoteId();
        Response<ResponseBody> response = service.getAttachment(eventId, observationId, attachmentId).execute();

        if (response.isSuccessful()) {
            return response.body();
        } else {
            Log.e(LOG_NAME, "Bad request.");
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody().string());
            }
        }

        return null;
    }
}