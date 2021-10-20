package mil.nga.giat.mage.sdk.http.resource;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.http.HttpClientManager;
import mil.nga.giat.mage.sdk.http.converter.ObservationsConverterFactory;
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

/***
 * RESTful communication for observations
 *
 * @author newmanw
 */
public class ObservationResource {

    public interface ObservationService {
        @GET("/api/events/{eventId}/observations")
        Call<Collection<Observation>> getObservations(@Path("eventId") String eventId, @Query("startDate") String startDate);

        @POST("/api/events/{eventId}/observations/id")
        Call<Observation> createObservationId(@Path("eventId") String eventId);

        @PUT("/api/events/{eventId}/observations/{observationId}")
        Call<Observation> updateObservation(@Path("eventId") String eventId, @Path("observationId") String observationId, @Body Observation observation);

        @POST("/api/events/{eventId}/observations/{observationId}/states")
        Call<JsonObject> archiveObservation(@Path("eventId") String eventId, @Path("observationId") String observationId, @Body JsonObject state);

        @Streaming
        @GET("/api/events/{eventId}/observations/{observationId}/attachments/{attachmentId}")
        Call<ResponseBody> getAttachment(@Path("eventId") String eventId, @Path("observationId") String observationId, @Path("attachmentId") String attachmentId);

        @Multipart
        @PUT("/api/events/{eventId}/observations/{observationId}/attachments/{attachmentId}")
        Call<Attachment> createAttachment(@Path("eventId") String eventId, @Path("observationId") String observationId, @Path("attachmentId") String attachmentId, @PartMap Map<String, RequestBody> parts);

        @PUT("/api/events/{eventId}/observations/{observationId}/favorite")
        Call<Observation> favoriteObservation(@Path("eventId") String eventId, @Path("observationId") String observationId);

        @DELETE("/api/events/{eventId}/observations/{observationId}/favorite")
        Call<Observation> unfavoriteObservation(@Path("eventId") String eventId, @Path("observationId") String observationId);

        @PUT("/api/events/{eventId}/observations/{observationId}/important")
        Call<Observation> addImportant(@Path("eventId") String eventId, @Path("observationId") String observationId, @Body JsonObject important);

        @DELETE("/api/events/{eventId}/observations/{observationId}/important")
        Call<Observation> removeImportant(@Path("eventId") String eventId, @Path("observationId") String observationId);
    }

    private static final String LOG_NAME = ObservationResource.class.getName();

    private final Context context;

    public ObservationResource(Context context) {
        this.context = context;
    }

    public Collection<Observation> getObservations(Event event) {
        Collection<Observation> observations = new ArrayList<>();

        if (event == null || event.getRemoteId() == null) {
            return observations;
        }

        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(ObservationsConverterFactory.create(event))
                .client(HttpClientManager.getInstance().httpClient())
                .build();

        DateFormat iso8601Format = ISO8601DateFormatFactory.ISO8601();
        ObservationHelper observationHelper = ObservationHelper.getInstance(context);
        Date lastModifiedDate = observationHelper.getLatestCleanLastModified(context, event);
        Log.d(LOG_NAME, "Fetching all observations after: " + iso8601Format.format(lastModifiedDate));

        ObservationService service = retrofit.create(ObservationService.class);
        try {
            Response<Collection<Observation>> response = service.getObservations(event.getRemoteId(), iso8601Format.format(lastModifiedDate)).execute();

            if (response.isSuccessful()) {
                observations = response.body();
            } else {
                Log.e(LOG_NAME, "Bad request.");
                if (response.errorBody() != null) {
                    Log.e(LOG_NAME, response.errorBody().string());
                }
            }
        } catch (IOException e) {
            Log.e(LOG_NAME, "There was a failure while performing an Observation Fetch operation.", e);
        }

        return observations;
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