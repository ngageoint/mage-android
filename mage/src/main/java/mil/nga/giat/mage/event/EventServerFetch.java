package mil.nga.giat.mage.event;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.giat.mage.glide.GlideApp;
import mil.nga.giat.mage.glide.model.Avatar;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.Team;
import mil.nga.giat.mage.sdk.datastore.user.TeamEvent;
import mil.nga.giat.mage.sdk.datastore.user.TeamHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.datastore.user.UserTeam;
import mil.nga.giat.mage.sdk.exceptions.EventException;
import mil.nga.giat.mage.sdk.exceptions.LayerException;
import mil.nga.giat.mage.sdk.fetch.DownloadImageTask;
import mil.nga.giat.mage.sdk.gson.deserializer.LayerDeserializer;
import mil.nga.giat.mage.sdk.http.HttpClientManager;
import mil.nga.giat.mage.sdk.http.resource.EventResource;
import mil.nga.giat.mage.sdk.http.resource.LayerResource;
import mil.nga.giat.mage.sdk.http.resource.ObservationResource;
import mil.nga.giat.mage.sdk.utils.ZipUtility;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by wnewman on 2/21/18.
 */

public class EventServerFetch extends AsyncTask<Void, Void, Exception> {
    private int MAX_AVATAR_DIMENSION = 1024;

    public static final String OBSERVATION_ICON_PATH = "/icons/observations";

    public interface EventFetchListener {
        void onEventFetched(boolean status, Exception e);
    }

    private static final String LOG_NAME = mil.nga.giat.mage.sdk.fetch.EventServerFetch.class.getName();

    Context context;
    private String eventId;
    private EventFetchListener listener;

    private DownloadImageTask iconFetch;

    public EventServerFetch(Context context, String eventId) {
        this.context = context;
        this.eventId = eventId;
    }

    public void setEventFetchListener(EventFetchListener listener) {
        this.listener = listener;
    }

    @Override
    protected Exception doInBackground(Void[] params) {
        Exception e = fetchAndSaveTeams();
        if (e != null) {
            return e;
        }

        e = fetchEventIcons();
        if (e != null) {
            return e;
        }

        e = fetchEventLayers();
        if (e != null) {
            return e;
        }

        return null;
    }

    @Override
    protected void onPostExecute(Exception e) {
        if (e == null) {
            if (iconFetch != null) {
                iconFetch.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }

        if (listener != null) {
            listener.onEventFetched(e == null, e);
        }
    }

    /**
     * Create teams
     */
    private Exception fetchAndSaveTeams() {
        Long start = System.currentTimeMillis();

        UserHelper userHelper = UserHelper.getInstance(context);

        EventResource eventResource = new EventResource(context);
        userHelper.deleteUserTeams();
        Log.d(LOG_NAME, "Attempting to fetch teams...");

        try {
            Map<Team, Collection<User>> teams = eventResource.getTeams(eventId.toString());
            Log.d(LOG_NAME, "Fetched " + teams.size() + " teams");

            final ArrayList<User> iconUsers = new ArrayList<>();

            TeamHelper teamHelper = TeamHelper.getInstance(context);
            for (Team team : teams.keySet()) {
                try {
                    team = teamHelper.createOrUpdate(team);

                    for (User user : teams.get(team)) {
                        user.setFetchedDate(new Date());
                        user = userHelper.createOrUpdate(user);

                        if (user.getAvatarUrl() != null) {
                            GlideApp.with(context)
                                    .download(Avatar.Companion.forUser(user))
                                    .submit(MAX_AVATAR_DIMENSION, MAX_AVATAR_DIMENSION);
                        }

                        if (user.getIconUrl() != null) {
                            iconUsers.add(user);
                        }

                        if (userHelper.read(user.getRemoteId()) == null) {
                            user = userHelper.createOrUpdate(user);
                        }

                        // populate the user/team join table
                        userHelper.create(new UserTeam(user, team));
                    }

                    // populate the team/event join table
                    Event event = EventHelper.getInstance(context).read(eventId);
                    teamHelper.create(new TeamEvent(team, event));
                } catch (Exception e) {
                    Log.e(LOG_NAME, "There was a failure while performing a team fetch operation.", e);
                    return e;
                }
            }

            TeamHelper.getInstance(context).syncTeams(teams.keySet());

            iconFetch = new DownloadImageTask(context, iconUsers, DownloadImageTask.ImageType.ICON, true);
        } catch (Exception e) {
            Log.e(LOG_NAME, "Problem fetching teams.", e);
            return e;
        }

        Long end = System.currentTimeMillis();
        Log.d(LOG_NAME, "Pulled and saved users in " + (end - start) / 1000 + " seconds");
        return null;
    }

    private Exception fetchEventIcons() {

        ObservationResource observationResource = new ObservationResource(context);

        try {
            InputStream inputStream = observationResource.getObservationIcons(eventId);
            File directory = new File(context.getFilesDir() + OBSERVATION_ICON_PATH);
            File zipFile = new File(directory, eventId + ".zip");
            if (!zipFile.getParentFile().exists()) {
                zipFile.getParentFile().mkdirs();
            }

            if (zipFile.exists()) {
                zipFile.delete();
            }

            if (!zipFile.exists()) {
                zipFile.createNewFile();
            }

            File zipDirectory = new File(directory, eventId);
            if (!zipDirectory.exists()) {
                zipDirectory.mkdirs();
            }

            // copy stream to file
            FileOutputStream outputStream = new FileOutputStream(zipFile);
            ByteStreams.copy(inputStream, outputStream);
            inputStream.close();
            outputStream.close();

            Log.d(LOG_NAME, "Unzipping " + zipFile.getAbsolutePath() + " to " + zipDirectory.getAbsolutePath() + ".");

            // unzip file
            ZipUtility.unzip(zipFile, zipDirectory);

            // delete the zip
            zipFile.delete();
        } catch (Exception e) {
            Log.e(LOG_NAME, "There was a failure while retrieving the observation icons.", e);
            return e;
        }

        return null;
    }

    private Exception fetchEventLayers() {
        Collection<Layer> layers = new ArrayList<>();

        Event event;
        try {
            event = EventHelper.getInstance(context).read(eventId);
        } catch (EventException e) {
            Log.e(LOG_NAME,"Error reading event", e);
            return e;
        }

        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(mil.nga.giat.mage.sdk.R.string.serverURLKey), context.getString(mil.nga.giat.mage.sdk.R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(LayerDeserializer.getGsonBuilder(event)))
                .client(HttpClientManager.getInstance().httpClient())
                .build();

        LayerResource.LayerService service = retrofit.create(LayerResource.LayerService.class);
        try {
            Response<Collection<Layer>> response = service.getLayers(event.getRemoteId(), "GeoPackage").execute();
            if (response.isSuccessful()) {
                layers = response.body();
            } else {
                Log.e(LOG_NAME, "Bad request.");
                if (response.errorBody() != null) {
                    Log.e(LOG_NAME, response.errorBody().string());
                }
            }
        } catch (IOException e) {
            Log.e(LOG_NAME, "Error fetching event geopackage layers", e);
            return e;
        }

        LayerHelper layerHelper = LayerHelper.getInstance(context);
        try {
            layerHelper.deleteAll("GeoPackage");

            GeoPackageManager manager = GeoPackageFactory.getManager(context);
            for (Layer layer : layers) {
                // Check if geopackage has been downloaded as part of another event
                String relativePath = String.format("MAGE/geopackages/%s/%s", layer.getRemoteId(), layer.getFileName());
                File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), relativePath);
                if (file.exists() && manager.existsAtExternalFile(file)) {
                    layer.setLoaded(true);
                    layer.setRelativePath(relativePath);
                }
                layerHelper.create(layer);
            }
        } catch (LayerException e) {
            Log.e(LOG_NAME, "Error saving geopackage layers", e);
            return e;
        }

        return null;
    }
}
