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
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.Team;
import mil.nga.giat.mage.sdk.datastore.user.TeamHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.gson.deserializer.EventDeserializer;
import mil.nga.giat.mage.sdk.gson.deserializer.LayerDeserializer;
import mil.nga.giat.mage.sdk.gson.deserializer.TeamDeserializer;
import mil.nga.giat.mage.sdk.gson.deserializer.UserDeserializer;
import mil.nga.giat.mage.sdk.http.client.HttpClientManager;
import mil.nga.giat.mage.sdk.jackson.deserializer.ObservationDeserializer;
import mil.nga.giat.mage.sdk.jackson.deserializer.StaticFeatureDeserializer;
import mil.nga.giat.mage.sdk.utils.DateFormatFactory;
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
        Event currentEvent = EventHelper.getInstance(context).getCurrentEvent();
        String currentEventId = currentEvent.getRemoteId();
		HttpEntity entity = null;
		try {
			URL serverURL = new URL(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue)));

            if(currentEventId != null) {
                ObservationHelper observationHelper = ObservationHelper.getInstance(context);

                Date lastModifiedDate = observationHelper.getLatestCleanLastModified(context, currentEvent);

                URL observationURL = new URL(serverURL, "/api/events/" + currentEventId + "/observations");
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
                    observations = new ObservationDeserializer(currentEvent).parseObservations(entity.getContent());
                } else {
                    entity = response.getEntity();
                    String error = EntityUtils.toString(entity);
                    Log.e(LOG_NAME, "Bad request.");
                    Log.e(LOG_NAME, error);
                }
            } else {
                Log.e(LOG_NAME, "Could not pull the observations, because the event id was: " + String.valueOf(currentEventId));
            }
		} catch (Exception e) {
			// this block should never flow exceptions up! Log for now.
			Log.e(LOG_NAME, "There was a failure while performing an Observation Fetch operation.", e);
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

    public static Collection<User> getAllUsers(Context context, List<JSONArray> userJSONCacheOut, JSONArray userJSONCacheIn, List<Exception> exceptions) {
        final Gson userDeserializer = UserDeserializer.getGsonBuilder(context);
        Collection<User> users = new ArrayList<User>();
        HttpEntity entity = null;
        try {
			if(userJSONCacheIn == null) {
				URL serverURL = new URL(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue)));
				URL userURL = new URL(serverURL, "/api/users");

				DefaultHttpClient httpclient = HttpClientManager.getInstance(context).getHttpClient();
				HttpGet get = new HttpGet(userURL.toURI());
				HttpResponse response = httpclient.execute(get);

				if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
					entity = response.getEntity();
					JSONArray json = new JSONArray(EntityUtils.toString(entity));
					if (json != null) {
						userJSONCacheOut.add(json);
						for (int i = 0; i < json.length(); i++) {
							JSONObject feature = json.getJSONObject(i);
							if (feature != null) {
								User user = userDeserializer.fromJson(feature.toString(), User.class);
								if (user != null) {
									users.add(user);
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
			} else {
				for (int i = 0; i < userJSONCacheIn.length(); i++) {
					JSONObject feature = userJSONCacheIn.getJSONObject(i);
					if (feature != null) {
						User user = userDeserializer.fromJson(feature.toString(), User.class);
						if (user != null) {
							users.add(user);
						}
					}
				}
			}
        } catch (Exception e) {
            Log.e(LOG_NAME, "There was a failure while performing an User Fetch operation.", e);
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

        return users;
    }

    public static Map<Event, Collection<Team>> getAllEvents(Context context, List<Exception> exceptions) {
        final Gson eventDeserializer = EventDeserializer.getGsonBuilder();
        final Gson teamDeserializer = TeamDeserializer.getGsonBuilder();
        Map<Event, Collection<Team>> events = new HashMap<Event, Collection<Team>>();

        HttpEntity entity = null;
        try {
            URL serverURL = new URL(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue)));
            URL eventURL = new URL(serverURL, "api/events");

            DefaultHttpClient httpclient = HttpClientManager.getInstance(context).getHttpClient();
            HttpGet get = new HttpGet(eventURL.toURI());
            HttpResponse response = httpclient.execute(get);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                entity = response.getEntity();
                JSONArray json = new JSONArray(EntityUtils.toString(entity));
                if (json != null) {
                    for (int i = 0; i < json.length(); i++) {
                        JSONObject eventJson = json.getJSONObject(i);
                        if (eventJson != null) {
                            Event event = eventDeserializer.fromJson(eventJson.toString(), Event.class);
                            if (event != null) {
                                ArrayList<Team> teams = new ArrayList<Team>();
                                JSONArray jsonTeams = eventJson.getJSONArray("teams");
                                if (jsonTeams != null) {
                                    for (int j = 0; j < jsonTeams.length(); j++) {
                                        JSONObject teamJson = jsonTeams.getJSONObject(j);
                                        if (teamJson != null) {
                                            String teamRemoteId = teamJson.getString("id");
                                            Team team = TeamHelper.getInstance(context).read(teamRemoteId);
                                            if(team == null) {
                                                team = teamDeserializer.fromJson(teamJson.toString(), Team.class);
                                            }

                                            if (team != null) {
                                                teams.add(team);
                                            }
                                        }
                                    }
                                }

                                events.put(event, teams);
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
            Log.e(LOG_NAME, "There was a failure when fetching events.", e);
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

        return events;
    }
}