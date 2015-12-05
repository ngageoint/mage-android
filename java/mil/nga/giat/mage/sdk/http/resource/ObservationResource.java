package mil.nga.giat.mage.sdk.http.resource;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.http.HttpClientManager;
import mil.nga.giat.mage.sdk.http.converter.AttachmentConverterFactory;
import mil.nga.giat.mage.sdk.http.converter.ObservationConverterFactory;
import mil.nga.giat.mage.sdk.http.converter.ObservationsConverterFactory;
import mil.nga.giat.mage.sdk.utils.DateFormatFactory;
import mil.nga.giat.mage.sdk.utils.MediaUtility;
import retrofit.Call;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.PartMap;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.Streaming;

/***
 * RESTful communication for observations
 *
 * @author newmanw
 */
public class ObservationResource {

    public interface ObservationService {
        @GET("/api/events/{eventId}/observations")
        Call<Collection<Observation>> getObservations(@Path("eventId") String eventId, @Query("startDate") String startDate);

        @POST("/api/events/{eventId}/observations")
        Call<Observation> createObservation(@Path("eventId") String eventId , @Body Observation observation);

        @PUT("/api/events/{eventId}/observations/{observationId}")
        Call<Observation> updateObservation(@Path("eventId") String eventId, @Path("observationId") String observationId, @Body Observation observation);

        @GET("/api/events/{eventId}/form/icons.zip")
        Call<ResponseBody> getObservationIcons(@Path("eventId") String eventId);

        @Streaming
        @GET("/api/events/{eventId}/observations/{observationId}/attachments/{attachmentId}")
        Call<ResponseBody> getAttachment(@Path("eventId") String eventId, @Path("observationId") String observationId, @Path("attachmentId") String attachmentId);

        @Multipart
        @POST("/api/events/{eventId}/observations/{observationId}/attachments")
        Call<Attachment> createAttachment(@Path("eventId") String eventId, @Path("observationId") String observationId, @PartMap Map<String, RequestBody> parts);
    }

    private static final String LOG_NAME = ObservationResource.class.getName();

    private Context context;

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
                .client(HttpClientManager.getInstance(context).httpClient())
                .build();

        DateFormat iso8601Format = DateFormatFactory.ISO8601();
        ObservationHelper observationHelper = ObservationHelper.getInstance(context);
        Date lastModifiedDate = observationHelper.getLatestCleanLastModified(context, event);
        Log.d(LOG_NAME, "Fetching all observations after: " + iso8601Format.format(lastModifiedDate));

        ObservationService service = retrofit.create(ObservationService.class);
        try {
            Response<Collection<Observation>> response = service.getObservations(event.getRemoteId(), iso8601Format.format(lastModifiedDate)).execute();

            if (response.isSuccess()) {
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


    public Observation saveObservation(Observation observation) {
        ObservationHelper observationHelper = ObservationHelper.getInstance(context);
        Observation savedObservation = null;

        try {
            String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(ObservationConverterFactory.create(observation.getEvent()))
                    .client(HttpClientManager.getInstance(context).httpClient())
                    .build();

            ObservationService service = retrofit.create(ObservationService.class);

            Response<Observation> response;
            if (StringUtils.isEmpty(observation.getRemoteId())) {
                response = service.createObservation(observation.getEvent().getRemoteId(), observation).execute();
            } else {
                response = service.updateObservation(observation.getEvent().getRemoteId(), observation.getRemoteId(), observation).execute();
            }

            if (response.isSuccess()) {
                Observation returnedObservation = response.body();
                returnedObservation.setDirty(Boolean.FALSE);
                returnedObservation.setId(observation.getId());
                savedObservation = observationHelper.update(returnedObservation);
            } else {
                Log.e(LOG_NAME, "Bad request.");
                if (response.errorBody() != null) {
                    Log.e(LOG_NAME, response.errorBody().string());
                }
            }
        } catch (Exception e) {
            Log.e(LOG_NAME, "Failure saving observation.", e);
        }

        return savedObservation;
    }

    public InputStream getObservationIcons(Event event) throws IOException {
        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(HttpClientManager.getInstance(context).httpClient())
                .build();

        ObservationService service = retrofit.create(ObservationService.class);
        Response<ResponseBody> response = service.getObservationIcons(event.getRemoteId()).execute();

        InputStream inputStream = null;
        if (response.isSuccess()) {
            inputStream = response.body().byteStream();
        } else {
            Log.e(LOG_NAME, "Bad request.");
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody().string());
            }
        }

        return inputStream;
    }

    public ResponseBody getAttachment(Attachment attachment) throws IOException {
        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(HttpClientManager.getInstance(context).httpClient())
                .build();

        ObservationService service = retrofit.create(ObservationService.class);

        String eventId = attachment.getObservation().getEvent().getRemoteId();
        String observationId = attachment.getObservation().getRemoteId();
        String attachmentId = attachment.getRemoteId();
        Response<ResponseBody> response = service.getAttachment(eventId, observationId, attachmentId).execute();

        if (response.isSuccess()) {
            return response.body();
        } else {
            Log.e(LOG_NAME, "Bad request.");
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody().string());
            }
        }

        return null;
    }

    public Attachment createAttachment(Attachment attachment) {
        try {
            String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(AttachmentConverterFactory.create())
                    .client(HttpClientManager.getInstance(context).httpClient())
                    .build();

            ObservationService service = retrofit.create(ObservationService.class);

            String eventId = attachment.getObservation().getEvent().getRemoteId();
            String observationId = attachment.getObservation().getRemoteId();

            Map<String, RequestBody> parts = new HashMap<>();
            File attachmentFile = new File(attachment.getLocalPath());
            String mimeType = MediaUtility.getMimeType(attachment.getLocalPath());
            RequestBody fileBody = RequestBody.create(MediaType.parse(mimeType), attachmentFile);
            parts.put("attachment\"; filename=\"" + attachmentFile.getName() + "\"", fileBody);

            Response<Attachment> response = service.createAttachment(eventId, observationId, parts).execute();

            if (response.isSuccess()) {
                Attachment returnedAttachment = response.body();
                attachment.setContentType(returnedAttachment.getContentType());
                attachment.setName(returnedAttachment.getName());
                attachment.setRemoteId(returnedAttachment.getRemoteId());
                attachment.setRemotePath(returnedAttachment.getRemotePath());
                attachment.setSize(returnedAttachment.getSize());
                attachment.setUrl(returnedAttachment.getUrl());
                attachment.setDirty(returnedAttachment.isDirty());

                DaoStore.getInstance(context).getAttachmentDao().update(attachment);
            } else {
                Log.e(LOG_NAME, "Bad request.");
                if (response.errorBody() != null) {
                    Log.e(LOG_NAME, response.errorBody().string());
                }
            }
        } catch (Exception e) {
            Log.e(LOG_NAME, "Failure saving observation.", e);
        }

        return attachment;
    }
}