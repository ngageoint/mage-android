package mil.nga.giat.mage.sdk.fetch;

import android.content.Context;
import android.util.Log;

import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.http.resource.ObservationResource;
import mil.nga.giat.mage.sdk.utils.ZipUtility;

public class ObservationBitmapFetch extends AbstractServerFetch {

	public ObservationBitmapFetch(Context context) {
		super(context);
	}

	private static final String LOG_NAME = ObservationBitmapFetch.class.getName();

	public static final String OBSERVATION_ICON_PATH = "/icons/observations";

	public void fetch(Event event) {
		ObservationResource observationResource = new ObservationResource(mContext);

		try {
			InputStream inputStream = observationResource.getObservationIcons(event);
			File directory = new File(mContext.getFilesDir() + OBSERVATION_ICON_PATH);
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
			ByteStreams.copy(inputStream, new FileOutputStream(zipFile));

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