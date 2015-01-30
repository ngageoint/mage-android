package mil.nga.giat.mage.sdk.http.get;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature;
import mil.nga.giat.mage.sdk.datastore.user.Role;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.gson.deserializer.LayerDeserializer;
import mil.nga.giat.mage.sdk.gson.deserializer.RoleDeserializer;
import mil.nga.giat.mage.sdk.gson.deserializer.UserDeserializer;
import mil.nga.giat.mage.sdk.http.client.HttpClientManager;
import mil.nga.giat.mage.sdk.jackson.deserializer.LocationDeserializer;
import mil.nga.giat.mage.sdk.jackson.deserializer.ObservationDeserializer;
import mil.nga.giat.mage.sdk.jackson.deserializer.StaticFeatureDeserializer;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.utils.DateFormatFactory;
import mil.nga.giat.mage.sdk.utils.ZipUtility;

/**
 * A class that contains common GET requests to the MAGE server.
 * 
 * @author travis
 * 
 */
public class MageServerGetRequests {

	private static final String LOG_NAME = MageServerGetRequests.class.getName();
	private static ObservationDeserializer observationDeserializer = new ObservationDeserializer();
	private static StaticFeatureDeserializer featureDeserializer = new StaticFeatureDeserializer();
	private static LocationDeserializer locationDeserializer = new LocationDeserializer();

	public static final String OBSERVATION_ICON_PATH = "/icons/observations";

	public static void getAndSaveObservationIcons(Context context) {
		HttpEntity entity = null;
		try {
			URL serverURL = new URL(PreferenceHelper.getInstance(context).getValue(R.string.serverURLKey));

			String dynamicFormString = PreferenceHelper.getInstance(context).getValue(R.string.dynamicFormKey);
			JsonObject dynamicFormJson = new JsonParser().parse(dynamicFormString).getAsJsonObject();
			String formId = dynamicFormJson.get("id").getAsString();
			if (formId != null) {
				URL observationIconsURL = new URL(serverURL, "/api/icons/" + formId + ".zip");
				DefaultHttpClient httpclient = HttpClientManager.getInstance(context).getHttpClient();
				Log.d(LOG_NAME, observationIconsURL.toString());
				HttpGet get = new HttpGet(observationIconsURL.toURI());
				HttpResponse response = httpclient.execute(get);

				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					entity = response.getEntity();
					File directory = new File(context.getFilesDir() + OBSERVATION_ICON_PATH);
					File zipFile = new File(directory, formId + ".zip");
					if (!zipFile.getParentFile().exists()) {
						zipFile.getParentFile().mkdirs();
					}
					if (zipFile.exists()) {
						zipFile.delete();
					}
					if(!zipFile.exists()) {
						zipFile.createNewFile();
					}
					File zipDirectory = new File(directory, formId);
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
				Log.e(LOG_NAME, "Could not pull the observation icons, because the form id was: " + String.valueOf(formId));
			}
		} catch (Exception e) {
			// this block should never flow exceptions up! Log for now.
			Log.e(LOG_NAME, "There was a failure while retriving the observation icons.", e);
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
	
	/**
	 * Gets layers from the server.
	 */
	public static List<Layer> getFeatureLayers(Context context) {
		List<Layer> layers = new ArrayList<Layer>();
		final Gson layerDeserializer = LayerDeserializer.getGsonBuilder();
		DefaultHttpClient httpclient = HttpClientManager.getInstance(context).getHttpClient();
		HttpEntity entity = null;
		try {
			Uri uri = Uri.parse(PreferenceHelper.getInstance(context).getValue(R.string.serverURLKey)).buildUpon().appendPath("api").appendPath("layers").appendQueryParameter("type", "Feature").build();

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

	public static List<Layer> getStaticLayers(Context context) {
		final Gson layerDeserializer = LayerDeserializer.getGsonBuilder();
		List<Layer> layers = new ArrayList<Layer>();
		DefaultHttpClient httpclient = HttpClientManager.getInstance(context).getHttpClient();
		HttpEntity entity = null;
		try {

			Uri uri = Uri.parse(PreferenceHelper.getInstance(context).getValue(R.string.serverURLKey)).buildUpon().appendPath("api").appendPath("layers").appendQueryParameter("type", "External").build();

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

	private static Layer getFieldObservationLayer(Context context) {
		Layer fieldObservationLayer = null;
		List<Layer> layers = MageServerGetRequests.getFeatureLayers(context);
		for (Layer layer : layers) {
			if (layer.getName().equals("Field Observations")) {
				fieldObservationLayer = layer;
				break;
			}
		}

		return fieldObservationLayer;
	}

	/**
	 * Makes a GET request to the MAGE server for the Field Observation Form Id.
	 * 
	 * @param context
	 * @return
	 */
	public static String getFieldObservationFormId(Context context) {
		String fieldObservationFormId = null;
		Layer fieldObservationLayer = getFieldObservationLayer(context);
		if (fieldObservationLayer != null) {
			fieldObservationFormId = fieldObservationLayer.getFormId();
		}

		return fieldObservationFormId;
	}
	
	/**
	 * Makes a GET request to the MAGE server for the Field Observation Layer Id.
	 * 
	 * @param context
	 * @return
	 */
	public static String getFieldObservationLayerId(Context context) {
		String fieldObservationLayerId = null;
		Layer fieldObservationLayer = getFieldObservationLayer(context);
		if (fieldObservationLayer != null) {
			fieldObservationLayerId = fieldObservationLayer.getRemoteId();
		}

		return fieldObservationLayerId;
	}

	public static Collection<StaticFeature> getStaticFeatures(Context context, Layer layer) {
		long start = 0;

		Collection<StaticFeature> staticFeatures = new ArrayList<StaticFeature>();
		HttpEntity entity = null;
		try {
			URL serverURL = new URL(PreferenceHelper.getInstance(context).getValue(R.string.serverURLKey));

			URL staticFeatureURL = new URL(serverURL, "/FeatureServer/" + layer.getRemoteId() + "/features");
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
			Log.e(LOG_NAME, "There was a failure while retriving static features.", e);
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

	/**
	 * Returns the observations from the server. Uses a date as in filter in the request.
	 * 
	 * @param context
	 * @return
	 */
	public static List<Observation> getObservations(Context context) {
		long start = 0;
        DateFormat iso8601Format = DateFormatFactory.ISO8601();

        List<Observation> observations = new ArrayList<Observation>();
		String fieldObservationLayerId = MageServerGetRequests.getFieldObservationLayerId(context);
		HttpEntity entity = null;
		try {
			URL serverURL = new URL(PreferenceHelper.getInstance(context).getValue(R.string.serverURLKey));

			ObservationHelper observationHelper = ObservationHelper.getInstance(context);

			Date lastModifiedDate = observationHelper.getLatestCleanLastModified(context);

			URL observationURL = new URL(serverURL, "/FeatureServer/" + fieldObservationLayerId + "/features");
			Uri.Builder uriBuilder = Uri.parse(observationURL.toURI().toString()).buildUpon();
			uriBuilder.appendQueryParameter("startDate", iso8601Format.format(lastModifiedDate));

			DefaultHttpClient httpclient = HttpClientManager.getInstance(context).getHttpClient();
			Log.d(LOG_NAME, "Fetching all observations after: " + iso8601Format.format(lastModifiedDate));
			Log.d(LOG_NAME, uriBuilder.build().toString());
			HttpGet get = new HttpGet(new URI(uriBuilder.build().toString()));
			HttpResponse response = httpclient.execute(get);

			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				entity = response.getEntity();
				start = System.currentTimeMillis();
				observations = observationDeserializer.parseObservations(entity.getContent());
			} else {
				entity = response.getEntity();
				String error = EntityUtils.toString(entity);
				Log.e(LOG_NAME, "Bad request.");
				Log.e(LOG_NAME, error);
			}
		} catch (Exception e) {
			// this block should never flow exceptions up! Log for now.
			Log.e(LOG_NAME, "There was a failure while performing an Observation Fetch opperation.", e);
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

		if (observations.size() > 0) {
			Log.d(LOG_NAME, "Took " + (stop - start) + " millis to deserialize " + observations.size() + " observations.");
		}

		return observations;
	}

	public static Collection<Location> getLocations(Context context) {
		Collection<Location> locations = new ArrayList<Location>();
		HttpEntity entity = null;
		try {
			URL serverURL = new URL(PreferenceHelper.getInstance(context).getValue(R.string.serverURLKey));
			URL locationURL = new URL(serverURL, "/api/locations/users");

			DefaultHttpClient httpclient = HttpClientManager.getInstance(context).getHttpClient();
			HttpGet get = new HttpGet(locationURL.toURI());
			HttpResponse response = httpclient.execute(get);

			if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
				entity = response.getEntity();
				locations = locationDeserializer.parseUserLocations(entity.getContent());
			} else {
				entity = response.getEntity();
				String error = EntityUtils.toString(entity);
				Log.e(LOG_NAME, "Bad request.");
				Log.e(LOG_NAME, error);
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "There was a failure while performing an Location Fetch opperation.", e);
		} finally {
			try {
				if (entity != null) {
					entity.consumeContent();
				}
			} catch (Exception e) {
				Log.w(LOG_NAME, "Trouble cleaning up after GET request.", e);
			}
		}

		return locations;
	}
	
	public static Collection<User> getAllUsers(Context context) {
		final Gson userDeserializer = UserDeserializer.getGsonBuilder(context);
		Collection<User> users = new ArrayList<User>();
		HttpEntity entity = null;
		try {
			URL serverURL = new URL(PreferenceHelper.getInstance(context).getValue(R.string.serverURLKey));
			URL userURL = new URL(serverURL, "/api/users");

			DefaultHttpClient httpclient = HttpClientManager.getInstance(context).getHttpClient();
			HttpGet get = new HttpGet(userURL.toURI());
			HttpResponse response = httpclient.execute(get);

			if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
				entity = response.getEntity();
				JSONArray featureArray = new JSONArray(EntityUtils.toString(entity));
				for (int i = 0; i < featureArray.length(); i++) {
					JSONObject feature = featureArray.getJSONObject(i);
					if (feature != null) {
						users.add(userDeserializer.fromJson(feature.toString(), User.class));
					}
				}
			} else {
				entity = response.getEntity();
				String error = EntityUtils.toString(entity);
				Log.e(LOG_NAME, "Bad request.");
				Log.e(LOG_NAME, error);
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "There was a failure while performing an User Fetch opperation.", e);
		} finally {
			try {
				if (entity != null) {
					entity.consumeContent();
				}
			} catch (Exception e) {
				Log.w(LOG_NAME, "Trouble cleaning up after GET request.", e);
			}
		}

		return users;
	}

    public static Collection<Role> getAllRoles(Context context, List<Exception> exceptions) {
        final Gson roleDeserializer = RoleDeserializer.getGsonBuilder();
        Collection<Role> roles = new ArrayList<Role>();

        HttpEntity entity = null;
        try {
            URL serverURL = new URL(PreferenceHelper.getInstance(context).getValue(R.string.serverURLKey));
            URL roleURL = new URL(serverURL, "api/roles");

            DefaultHttpClient httpclient = HttpClientManager.getInstance(context).getHttpClient();
            HttpGet get = new HttpGet(roleURL.toURI());
            HttpResponse response = httpclient.execute(get);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                entity = response.getEntity();
                JSONArray json = new JSONArray(EntityUtils.toString(entity));
                if (json != null) {
                    for (int i = 0; i < json.length(); i++) {
                        JSONObject roleJson = (JSONObject) json.get(i);
                        if (roleJson != null) {
                            Role role = roleDeserializer.fromJson(roleJson.toString(), Role.class);

                            if (role != null) {
                                roles.add(role);
                            } else {
                                // ignore updates
                            }
                        }
                    }
                }
            } else {
                entity = response.getEntity();
                String error = EntityUtils.toString(entity);
                Log.e(LOG_NAME, "Bad request.");
                Log.e(LOG_NAME, error);
                exceptions.add(new Exception("Bad request: " + error));
            }
        } catch (Exception e) {
            Log.e(LOG_NAME, "There was a failure when fetching roles.", e);
            exceptions.add(e);
        } finally {
            try {
                if (entity != null) {
                    entity.consumeContent();
                }
            } catch (Exception e) {
                Log.w(LOG_NAME, "Trouble cleaning up after GET request.", e);
            }
        }

        return roles;
    }
}