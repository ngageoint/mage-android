package mil.nga.giat.mage.sdk.fetch;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.common.io.ByteStreams;

import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.exceptions.EventException;
import mil.nga.giat.mage.sdk.http.resource.ObservationResource;
import mil.nga.giat.mage.sdk.utils.ZipUtility;

public class EventIconFetchIntentService extends IntentService {

	private static final String LOG_NAME = EventIconFetchIntentService.class.getName();

	public static final String OBSERVATION_ICON_PATH = "/icons/observations";
	public static String EXTRA_EVENT_ID = "EXTRA_EVENT_ID";
	public static String EXTRA_EVENT_IDS = "EXTRA_EVENT_IDS";
	public static final String BROADCAST_EVENT_ICONS_ACTION = EventIconFetchIntentService.class.getCanonicalName();

	public EventIconFetchIntentService() {
		super(LOG_NAME);
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		for (Long eventId : getEventIds(intent)) {
			try {
				Event event = EventHelper.getInstance(getApplicationContext()).read(eventId);
				fetchEventIcons(event);
			} catch (EventException e) {
				Log.e(LOG_NAME, "Could not read event from database", e);
			}
		}

		Intent localIntent = new Intent(EventIconFetchIntentService.BROADCAST_EVENT_ICONS_ACTION);
		localIntent.addCategory(Intent.CATEGORY_DEFAULT);
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	}

	private Collection<Long> getEventIds(Intent intent) {
		Collection<Long> eventIds = new ArrayList<>();

		if (intent.hasExtra(EXTRA_EVENT_ID)) {
			eventIds.add(intent.getLongExtra(EXTRA_EVENT_ID, 0));
		}

		if (intent.hasExtra(EXTRA_EVENT_IDS)) {
			Long[] ids = ArrayUtils.toObject(intent.getLongArrayExtra(EXTRA_EVENT_IDS));
			eventIds.addAll(Arrays.asList(ids));
		}

		return eventIds;
	}

	private void fetchEventIcons(Event event) {

		ObservationResource observationResource = new ObservationResource(getApplicationContext());

		try {
			InputStream inputStream = observationResource.getObservationIcons(event);
			File directory = new File(getApplicationContext().getFilesDir() + OBSERVATION_ICON_PATH);
			File zipFile = new File(directory, event.getRemoteId() + ".zip");
			if (!zipFile.getParentFile().exists()) {
				zipFile.getParentFile().mkdirs();
			}

			if (zipFile.exists()) {
				zipFile.delete();
			}

			if (!zipFile.exists()) {
				zipFile.createNewFile();
			}

			File zipDirectory = new File(directory, event.getRemoteId());
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
		}
	}
}
