package mil.nga.giat.mage.sdk.http.post;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.List;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.gson.deserializer.UserDeserializer;
import mil.nga.giat.mage.sdk.gson.serializer.LocationSerializer;
import mil.nga.giat.mage.sdk.gson.serializer.ObservationSerializer;
import mil.nga.giat.mage.sdk.http.client.HttpClientManager;
import mil.nga.giat.mage.sdk.jackson.deserializer.AttachmentDeserializer;
import mil.nga.giat.mage.sdk.jackson.deserializer.LocationDeserializer;
import mil.nga.giat.mage.sdk.jackson.deserializer.ObservationDeserializer;
import mil.nga.giat.mage.sdk.preferences.PreferenceHelper;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * A class that contains common POST requests to the MAGE server.
 * 
 * @author wiedemanns
 * 
 */
public class MageServerPostRequests {

	private static final String LOG_NAME = MageServerPostRequests.class.getName();

    private static AttachmentDeserializer attachmentDeserializer = new AttachmentDeserializer();
    private static LocationDeserializer locationDeserializer = new LocationDeserializer();

	/**
	 * POST an {@link Observation} to the server.
	 * 
	 * @param observation
	 *            The Observation to post.
	 * @param context
	 */
	public static Observation postObservation(Observation observation, Context context) {

		ObservationHelper observationHelper = ObservationHelper.getInstance(context);
		Observation savedObservation = null;

		HttpEntity entity = null;
		HttpEntityEnclosingRequestBase request = null;
		try {
			String observationEventIdString = String.valueOf(observation.getEvent().getId());
			DefaultHttpClient httpClient = HttpClientManager.getInstance(context).getHttpClient();

			URL serverURL = new URL(PreferenceHelper.getInstance(context).getValue(R.string.serverURLKey));
			URI endpointUri = null;

			if (observation.getRemoteId() == null || observation.getRemoteId().trim().isEmpty()) {
				endpointUri = new URL(serverURL + "/api/events/" + observationEventIdString + "/observations").toURI();
				request = new HttpPost(endpointUri);
			} else {
				endpointUri = new URL(serverURL + "/api/events/" + observationEventIdString + "/observations/" + observation.getRemoteId()).toURI();
				request = new HttpPut(endpointUri);
			}
			request.addHeader("Content-Type", "application/json; charset=utf-8");
			Gson gson = ObservationSerializer.getGsonBuilder();
			request.setEntity(new StringEntity(gson.toJson(observation)));

			HttpResponse response = httpClient.execute(request);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				entity = response.getEntity();
				Observation returnedObservation = new ObservationDeserializer(observation.getEvent()).parseObservation(entity.getContent());
				returnedObservation.setDirty(Boolean.FALSE);
				returnedObservation.setId(observation.getId());
				savedObservation = observationHelper.update(returnedObservation);
			} else {
				entity = response.getEntity();
				String error = EntityUtils.toString(entity);
				Log.e(LOG_NAME, "Bad request.");
				Log.e(LOG_NAME, error);
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "Failure pushing observation.", e);
		} finally {
			try {
				if (entity != null) {
					entity.consumeContent();
				}
			} catch (Exception e) {
                Log.w(LOG_NAME, "Trouble cleaning up after POST request.", e);
			}
		}
		return savedObservation;
	}

	/**
	 * POST an {@link Attachment} to the server.
	 * 
	 * @param attachment
	 *            The attachment to post.
	 * @param context
	 */
	// The following code will sometimes fail to post attachments
	public static Attachment postAttachment(Attachment attachment, Context context) {
		DefaultHttpClient httpClient = HttpClientManager.getInstance(context).getHttpClient();
		HttpEntity entity = null;
		try {
			Log.d(LOG_NAME, "Pushing attachment " + attachment.getId() + " to " + attachment.getObservation().getUrl() + "/attachments");
			URL endpoint = new URL(attachment.getObservation().getUrl() + "/attachments");
			
			HttpPost request = new HttpPost(endpoint.toURI());
			String mimeType = MediaUtility.getMimeType(attachment.getLocalPath());
			
			Log.d(LOG_NAME, "Mime type is: " + mimeType);

			FileBody fileBody = new FileBody(new File(attachment.getLocalPath()), mimeType);
			FormBodyPart fbp = new FormBodyPart("attachment", fileBody);

			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart(fbp);

			request.setEntity(reqEntity);
			HttpResponse response = httpClient.execute(request);
			entity = response.getEntity();
			if (entity != null) {
				Attachment a = attachmentDeserializer.parseAttachment(entity.getContent());
				attachment.setContentType(a.getContentType());
				attachment.setName(a.getName());
				attachment.setRemoteId(a.getRemoteId());
				attachment.setRemotePath(a.getRemotePath());
				attachment.setSize(a.getSize());
				attachment.setUrl(a.getUrl());
				attachment.setDirty(a.isDirty());

				// TODO go save this attachment again
				DaoStore.getInstance(context).getAttachmentDao().update(attachment);
			}

		} catch (Exception e) {
			Log.e(LOG_NAME, "Failure pushing attachment: " + attachment.getLocalPath(), e);
		} finally {
			try {
				if (entity != null) {
					entity.consumeContent();
				}
			} catch (Exception e) {
                Log.w(LOG_NAME, "Trouble cleaning up after POST request.", e);
			}
		}
		return attachment;
	}

	public static User postProfilePicture(User user, String abslutePath, Context context) {
		DefaultHttpClient httpClient = HttpClientManager.getInstance(context).getHttpClient();
		HttpEntity entity = null;
		try {
			Log.d(LOG_NAME, "Pushing profile picture for  " + user.getRemoteId());
			URL serverURL = new URL(PreferenceHelper.getInstance(context).getValue(R.string.serverURLKey));
			URL endpoint = new URL(serverURL + "/api/users/" + user.getRemoteId());
			
			HttpPut request = new HttpPut(endpoint.toURI());
			String mimeType = MediaUtility.getMimeType(abslutePath);
			
			Log.d(LOG_NAME, "Mime type is: " + mimeType);

			FileBody fileBody = new FileBody(new File(abslutePath), mimeType);
			FormBodyPart fbp = new FormBodyPart("avatar", fileBody);

			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart(fbp);

			request.setEntity(reqEntity);
			HttpResponse response = httpClient.execute(request);
			entity = response.getEntity();
			if (entity != null) {
				final Gson userDeserializer = UserDeserializer.getGsonBuilder(context);
				String entityString = EntityUtils.toString(entity);
				
				JSONObject userJson = new JSONObject(entityString);
				if (userJson != null) {
					User newUser = userDeserializer.fromJson(userJson.toString(), User.class);
					UserHelper userHelper = UserHelper.getInstance(context);
					if (newUser != null) {
						User oldUser = userHelper.read(newUser.getRemoteId());
						if (oldUser == null) {
							newUser.setCurrentUser(user.isCurrentUser());
							newUser.setFetchedDate(new Date());
							newUser = userHelper.create(newUser);
							Log.d(LOG_NAME, "Created user with remote_id " + user.getRemoteId());
						} else {
							// perform update?
							newUser.setId(oldUser.getId());
							newUser.setCurrentUser(user.isCurrentUser());
							newUser.setFetchedDate(new Date());
							userHelper.update(newUser);
							Log.d(LOG_NAME, "Updated user with remote_id " + newUser.getRemoteId());
						}
						return newUser;
					}
				}
				return user;
			} else {
				Log.e(LOG_NAME, "Unable to save profile picture.");
				return user;
			}

		} catch (Exception e) {
			Log.e(LOG_NAME, "Failure pushing profile picture: " + abslutePath, e);
		} finally {
			try {
				if (entity != null) {
					entity.consumeContent();
				}
			} catch (Exception e) {
                Log.w(LOG_NAME, "Trouble cleaning up after POST request.", e);
			}
		}
		return user;
	}
	
	public static Boolean postLocations(List<Location> locations, Context context) {
		LocationHelper locationHelper = LocationHelper.getInstance(context);
		
		Boolean status = false;
		HttpEntity entity = null;
		try {
			URL serverURL = new URL(PreferenceHelper.getInstance(context).getValue(R.string.serverURLKey));
			URI endpointUri = new URL(serverURL + "/api/locations").toURI();

			DefaultHttpClient httpClient = HttpClientManager.getInstance(context).getHttpClient();
			HttpPost request = new HttpPost(endpointUri);
			request.addHeader("Content-Type", "application/json; charset=utf-8");
			Gson gson = LocationSerializer.getGsonBuilder(context);
			request.setEntity(new StringEntity(gson.toJson(locations, new TypeToken<List<Location>>(){}.getType())));

			HttpResponse response = httpClient.execute(request);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				entity = response.getEntity();
				// it is imperative that the order of the returnedLocations match the order that was posted!!!
				// if the order changes from the server, all of this will break!
				List<Location> returnedLocations = locationDeserializer.parseLocations(entity.getContent());
				for(int i=0; i < returnedLocations.size(); i++) {
					//Log.i(LOG_NAME, "old, new : " + locations.get(i).getTimestamp() + ", " + returnedLocations.get(i).getTimestamp());
					Location returnedLocation = returnedLocations.get(i);
					returnedLocation.setId(locations.get(i).getId());
					// locations that are posted are only from the current user
					returnedLocation.setUser(UserHelper.getInstance(context).readCurrentUser());
					locationHelper.update(returnedLocation);
				}
				status = true;
			} else {
				entity = response.getEntity();
				String error = EntityUtils.toString(entity);
				Log.e(LOG_NAME, "Bad request.");
				Log.e(LOG_NAME, error);
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, "Failure posting location.", e);
		} finally {
			try {
	            if (entity != null) {
	                entity.consumeContent();
	            }
	        } catch (Exception e) {
	            Log.w(LOG_NAME, "Trouble cleaning up after POST request.", e);
	        }
		}
		return status;
	}

    public static Boolean postCurrentUsersRecentEvent(Event event, Context context) {
        Boolean status = false;
        HttpEntity entity = null;
        try {
            UserHelper userHelper = UserHelper.getInstance(context);
            User currentUser = userHelper.readCurrentUser();

            URL serverURL = new URL(PreferenceHelper.getInstance(context).getValue(R.string.serverURLKey));
            URI endpointUri = new URL(serverURL + "/api/users/" + currentUser.getRemoteId() + "/events/" + event.getRemoteId() + "/recent").toURI();

            DefaultHttpClient httpClient = HttpClientManager.getInstance(context).getHttpClient();
            HttpPost request = new HttpPost(endpointUri);
            request.addHeader("Content-Type", "application/json; charset=utf-8");

            HttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                status = true;
            } else {
                entity = response.getEntity();
                String error = EntityUtils.toString(entity);
                Log.e(LOG_NAME, "Bad request.");
                Log.e(LOG_NAME, error);
            }
        } catch (Exception e) {
            Log.e(LOG_NAME, "Failure posting user's recent event.", e);
        } finally {
            try {
                if (entity != null) {
                    entity.consumeContent();
                }
            } catch (Exception e) {
                Log.w(LOG_NAME, "Trouble cleaning up after POST request.", e);
            }
        }
        return status;
    }


    public static Boolean logout(Context context) {
        Boolean status = false;
        HttpEntity entity = null;
        try {

            URL serverURL = new URL(PreferenceHelper.getInstance(context).getValue(R.string.serverURLKey));
            URI endpointUri = new URL(serverURL + "/api/logout").toURI();

            DefaultHttpClient httpClient = HttpClientManager.getInstance(context).getHttpClient();
            HttpPost request = new HttpPost(endpointUri);
            request.addHeader("Content-Type", "application/json; charset=utf-8");

            HttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                status = true;
            } else {
                entity = response.getEntity();
                String error = EntityUtils.toString(entity);
                Log.e(LOG_NAME, "Bad request.");
                Log.e(LOG_NAME, error);
            }
        } catch (Exception e) {
            Log.e(LOG_NAME, "Failure logging out of server.", e);
        } finally {
            try {
                if (entity != null) {
                    entity.consumeContent();
                }
            } catch (Exception e) {
                Log.w(LOG_NAME, "Trouble cleaning up after POST request.", e);
            }
        }
        return status;
    }
}
