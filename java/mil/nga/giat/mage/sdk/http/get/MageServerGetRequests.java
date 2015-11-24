package mil.nga.giat.mage.sdk.http.get;

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.gson.deserializer.LayerDeserializer;
import mil.nga.giat.mage.sdk.http.client.HttpClientManager;
import mil.nga.giat.mage.sdk.jackson.deserializer.StaticFeatureDeserializer;
import mil.nga.giat.mage.sdk.utils.ZipUtility;

/**
 * A class that contains common GET requests to the MAGE server.
 * 
 * @author wiedemanns
 * 
 */
public class MageServerGetRequests {

	private static final String LOG_NAME = MageServerGetRequests.class.getName();
	private static StaticFeatureDeserializer featureDeserializer = new StaticFeatureDeserializer();

	public static final String OBSERVATION_ICON_PATH = "/icons/observations";

	public static void getAndSaveObservationIcons(Context context) {
		HttpEntity entity = null;
		try {
			URL serverURL = new URL(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue)));

			String currentEventId = EventHelper.getInstance(context).getCurrentEvent().getRemoteId();
			if (currentEventId != null) {
				URL observationIconsURL = new URL(serverURL, "/api/events/" + currentEventId + "/form/icons.zip");
				DefaultHttpClient httpclient = HttpClientManager.getInstance(context).getHttpClient();
				Log.d(LOG_NAME, observationIconsURL.toString());
				HttpGet get = new HttpGet(observationIconsURL.toURI());
				HttpResponse response = httpclient.execute(get);

				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					entity = response.getEntity();
					File directory = new File(context.getFilesDir() + OBSERVATION_ICON_PATH);
					File zipFile = new File(directory, currentEventId + ".zip");
					if (!zipFile.getParentFile().exists()) {
						zipFile.getParentFile().mkdirs();
					}
					if (zipFile.exists()) {
						zipFile.delete();
					}
					if(!zipFile.exists()) {
						zipFile.createNewFile();
					}
					File zipDirectory = new File(directory, currentEventId);
					if(!zipDirectory.exists()) {
						zipDirectory.mkdirs();
					}
					// copy stream to file
					ByteStreams.copy(entity.getContent(), new FileOutputStream(zipFile));

					Log.d(LOG_NAME, "Unzipping " + zipFile.getAbsolutePath() + " to " + zipDirectory.getAbsolutePath() + ".");
					// unzip file
					ZipUtility.unzip(zipFile, zipDirectory);
					// delete the zip
					zipFile.delete();
				} else {
					String error = EntityUtils.toString(response.getEntity());
					Log.e(LOG_NAME, "Bad request.");
					Log.e(LOG_NAME, error);
				}
			} else {
				Log.e(LOG_NAME, "Could not pull the observation icons, because the event id was: " + String.valueOf(currentEventId));
			}
		} catch (Exception e) {
			// this block should never flow exceptions up! Log for now.
			Log.e(LOG_NAME, "There was a failure while retrieving the observation icons.", e);
		} finally {
			try {
				if (entity != null) {
					entity.consumeContent();
				}
			} catch (Exception e) {
				Log.w(LOG_NAME, "Trouble cleaning up after GET request.", e);
			}
		}
	}

	public static List<Layer> getStaticLayers(Context context) {
		Event currentEvent = EventHelper.getInstance(context).getCurrentEvent();
		final Gson layerDeserializer = LayerDeserializer.getGsonBuilder(currentEvent);
		List<Layer> layers = new ArrayList<Layer>();
		DefaultHttpClient httpclient = HttpClientManager.getInstance(context).getHttpClient();
		HttpEntity entity = null;
		try {
			String currentEventId = currentEvent.getRemoteId();
			// FIXME : not sure the server is respecting the type flag anymore!
			Uri uri = Uri.parse(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue))).buildUpon().appendPath("api").appendPath("events").appendPath(currentEventId).appendPath("layers").appendQueryParameter("type", "Feature").build();

			HttpGet get = new HttpGet(uri.toString());
			HttpResponse response = httpclient.execute(get);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				entity = response.getEntity();
				JSONArray featureArray = new JSONArray(EntityUtils.toString(entity));
				for (int i = 0; i < featureArray.length(); i++) {
					JSONObject feature = featureArray.getJSONObject(i);
					if (feature != null) {
						layers.add(layerDeserializer.fromJson(feature.toString(), Layer.class));
					}
				}
			} else {
				entity = response.getEntity();
				String error = EntityUtils.toString(entity);
				Log.e(LOG_NAME, "Bad request.");
				Log.e(LOG_NAME, error);
			}
		} catch (Exception e) {
			// this block should never flow exceptions up! Log for now.
			Log.e(LOG_NAME, "Failure parsing layer information.", e);
		} finally {
			try {
				if (entity != null) {
					entity.consumeContent();
				}
			} catch (Exception e) {
				Log.w(LOG_NAME, "Trouble cleaning up after GET request.", e);
			}
		}
		return layers;
	}

	public static Collection<StaticFeature> getStaticFeatures(Context context, Layer layer) {
		long start = 0;

		Collection<StaticFeature> staticFeatures = new ArrayList<StaticFeature>();
		HttpEntity entity = null;
		try {
			URL serverURL = new URL(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue)));

			URL staticFeatureURL = new URL(serverURL, "/api/events/" + layer.getEvent().getRemoteId() + "/layers/" + layer.getRemoteId() + "/features");
			DefaultHttpClient httpclient = HttpClientManager.getInstance(context).getHttpClient();
			Log.d(LOG_NAME, staticFeatureURL.toString());
			HttpGet get = new HttpGet(staticFeatureURL.toURI());
			HttpResponse response = httpclient.execute(get);

			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				entity = response.getEntity();
				start = System.currentTimeMillis();
				staticFeatures = featureDeserializer.parseStaticFeatures(entity.getContent(), layer);
			} else {
				String error = EntityUtils.toString(response.getEntity());
				Log.e(LOG_NAME, "Bad request.");
				Log.e(LOG_NAME, error);
			}
		} catch (Exception e) {
			// this block should never flow exceptions up! Log for now.
			Log.e(LOG_NAME, "There was a failure while retrieving static features.", e);
		} finally {
			try {
				if (entity != null) {
					entity.consumeContent();
				}
			} catch (Exception e) {
				Log.w(LOG_NAME, "Trouble cleaning up after GET request.", e);
			}
		}

		long stop = System.currentTimeMillis();

		if (staticFeatures.size() > 0) {
			Log.d(LOG_NAME, "Took " + (stop - start) + " millis to deserialize " + staticFeatures.size() + " static features.");
		}
		return staticFeatures;
	}
}