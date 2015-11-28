package mil.nga.giat.mage.sdk.retrofit.resource;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import com.squareup.okhttp.ResponseBody;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.retrofit.HttpClient;
import mil.nga.giat.mage.sdk.retrofit.converter.ObservationConverterFactory;
import mil.nga.giat.mage.sdk.retrofit.converter.ObservationsConverterFactory;
import mil.nga.giat.mage.sdk.utils.DateFormatFactory;
import retrofit.Call;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

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
                .client(HttpClient.httpClient(context))
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
                    .client(HttpClient.httpClient(context))
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
                .client(HttpClient.httpClient(context))
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
}