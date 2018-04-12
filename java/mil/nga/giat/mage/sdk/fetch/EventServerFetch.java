package mil.nga.giat.mage.sdk.fetch;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.Team;
import mil.nga.giat.mage.sdk.datastore.user.TeamEvent;
import mil.nga.giat.mage.sdk.datastore.user.TeamHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.datastore.user.UserTeam;
import mil.nga.giat.mage.sdk.http.resource.EventResource;
import mil.nga.giat.mage.sdk.http.resource.ObservationResource;
import mil.nga.giat.mage.sdk.utils.ZipUtility;

/**
 * Created by wnewman on 2/21/18.
 */

public class EventServerFetch extends AsyncTask<Void, Void, Exception> {
    public static final String OBSERVATION_ICON_PATH = "/icons/observations";

    public interface EventFetchListener {
        void onEventFetched(boolean status, Exception e);
    }

    private static final String LOG_NAME = EventServerFetch.class.getName();

    Context context;
    String eventId;
    private EventFetchListener listener;

    private DownloadImageTask avatarFetch;
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

        return null;
    }

    @Override
    protected void onPostExecute(Exception e) {
        if (e == null) {
            // users are updated, finish getting image content
            if (avatarFetch != null) {
                avatarFetch.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
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

            final ArrayList<User> avatarUsers = new ArrayList<>();
            final ArrayList<User> iconUsers = new ArrayList<>();

            TeamHelper teamHelper = TeamHelper.getInstance(context);
            for (Team team : teams.keySet()) {
                try {
                    team = teamHelper.createOrUpdate(team);

                    for (User user : teams.get(team)) {
                        user.setFetchedDate(new Date());
                        user = userHelper.createOrUpdate(user);

                        if (user.getAvatarUrl() != null) {
                            avatarUsers.add(user);
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

            avatarFetch = new DownloadImageTask(context, avatarUsers, DownloadImageTask.ImageType.AVATAR, true);
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
}
