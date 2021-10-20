package mil.nga.giat.mage.sdk.jackson.deserializer;

import android.util.Log;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationFavorite;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationForm;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationImportant;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.datastore.observation.State;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.utils.GeometryUtility;
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory;
import mil.nga.sf.Geometry;

public class ObservationDeserializer extends Deserializer {

	private static final String LOG_NAME = ObservationDeserializer.class.getName();

	private final GeometryDeserializer geometryDeserializer = new GeometryDeserializer();
	private final AttachmentDeserializer attachmentDeserializer = new AttachmentDeserializer();
	private final DateFormat iso8601Format = ISO8601DateFormatFactory.ISO8601();

    private Event event = null;

    public ObservationDeserializer(Event event) {
        this.event = event;
    }

	public List<Observation> parseObservations(InputStream is) throws IOException {
		List<Observation> observations = new ArrayList<>();

		JsonParser parser = factory.createParser(is);

		if (parser.nextToken() != JsonToken.START_ARRAY) {
            return observations;
        }

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            observations.add(parseObservation(parser));
        }

		parser.close();
		return observations;
	}

	public Observation parseObservation(InputStream is) throws IOException {
		JsonParser parser = factory.createParser(is);
		parser.nextToken();

		Observation observation = parseObservation(parser);

		parser.close();
		return observation;
	}

	public Observation parseObservation(String json) throws IOException {
		JsonParser parser = factory.createParser(json);
		parser.nextToken();

		Observation observation = parseObservation(parser);

		parser.close();
		return observation;
	}

	private Observation parseObservation(JsonParser parser) throws IOException {
		Observation observation = new Observation();
        observation.setEvent(event);

		if (parser.getCurrentToken() != JsonToken.START_OBJECT)
			return observation;

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String name = parser.getCurrentName();
			if ("id".equals(name)) {
				parser.nextToken();
				observation.setDirty(false);
				observation.setRemoteId(parser.getText());
			} else if ("userId".equals(name)) {
				parser.nextToken();
				observation.setUserId(parser.getText());
			} else if ("deviceId".equals(name)) {
				parser.nextToken();
				observation.setDeviceId(parser.getText());
			} else if ("lastModified".equals(name)) {
				parser.nextToken();
				try {
					Date d = iso8601Format.parse(parser.getText());
					observation.setLastModified(d);
				} catch (ParseException e) {
					Log.e(LOG_NAME, "Problem paring date.");
				}
			} else if ("url".equals(name)) {
				parser.nextToken();
				observation.setUrl(parser.getText());
			} else if ("state".equals(name)) {
				parser.nextToken();
				observation.setState(parseState(parser));
			} else if ("geometry".equals(name)) {
				parser.nextToken();
				observation.setGeometry(geometryDeserializer.parseGeometry(parser));
			} else if ("properties".equals(name)) {
				parser.nextToken();
				parseProperties(parser, observation);
			} else if ("attachments".equals(name)) {
				parser.nextToken();
				observation.setAttachments(parseAttachments(parser));
			} else if ("important".equals(name)) {
				parser.nextToken();
				observation.setImportant(parseImportant(parser));
			} else if ("favoriteUserIds".equals(name)) {
				parser.nextToken();
				observation.setFavorites(parseFavoriteUsers(parser));
			} else {
				parser.nextToken();
				parser.skipChildren();
			}
		}

		return observation;
	}

	private State parseState(JsonParser parser) throws IOException {
		State state = State.ACTIVE;

		if (parser.getCurrentToken() != JsonToken.START_OBJECT)
			return state;

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String name = parser.getCurrentName();
			if ("name".equals(name)) {
				parser.nextToken();
				String stateString = parser.getText();
				if (stateString != null) {
					try {
						state = State.valueOf(stateString.trim().toUpperCase());
					} catch (Exception e) {
						Log.e(LOG_NAME, "Could not parse state: " + stateString);
					}
				}
			} else {
				parser.nextToken();
				parser.skipChildren();
			}
		}

		return state;
	}

	private Collection<ObservationProperty> parseProperties(JsonParser parser, Observation observation) throws IOException {
		Collection<ObservationProperty> properties = new ArrayList<>();

		if (parser.getCurrentToken() != JsonToken.START_OBJECT)
			return properties;

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String name = parser.getCurrentName();
			if ("timestamp".equals(name)) {
				parser.nextToken();
				Serializable timestamp = parser.getText();
				try {
					Date date = iso8601Format.parse(timestamp.toString());
					observation.setTimestamp(date);
				} catch (ParseException e) {
					Log.w(LOG_NAME, "Unable to parse date: " + timestamp + " for observation: " + observation.getRemoteId(), e);
				}
			} else if ("provider".equals(name)) {
				parser.nextToken();
				observation.setProvider(parser.getText());
			} else if ("accuracy".equals(name)) {
				parser.nextToken();
				try {
					observation.setAccuracy(Float.parseFloat(parser.getText()));
				} catch (NumberFormatException e) {
					Log.e(LOG_NAME, "Could not parse observation accuracy into a float value", e);
				}
			} else if ("forms".equals(name)) {
				parser.nextToken();
				List<ObservationForm> forms = parseForms(parser);
				observation.setForms(forms);
			} else {
				parser.nextToken();
				parser.skipChildren();
			}
		}

		return properties;
	}

	private List<ObservationForm> parseForms(JsonParser parser) throws IOException {
		List<ObservationForm> forms = new ArrayList<>();

		if (parser.getCurrentToken() != JsonToken.START_ARRAY)
			return forms;

		while (parser.nextToken() != JsonToken.END_ARRAY) {
			ObservationForm form = parseForm(parser);
			forms.add(form);
		}

		return forms;
	}

	private ObservationForm parseForm(JsonParser parser) throws IOException {
		ObservationForm form = new ObservationForm();
		Collection<ObservationProperty> properties = new ArrayList<>();

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String key = parser.getCurrentName();

			if ("id".equals(key)) {
				parser.nextToken();
				form.setRemoteId(parser.getText());
			} else if ("formId".equals(key)) {
				parser.nextToken();
				form.setFormId(parser.getLongValue());
			} else {
				// parse property
				ObservationProperty property = parseProperty(parser);
				if (property != null) {
					properties.add(property);
				}
			}
		}

		form.setProperties(properties);
		return form;
	}

	private ObservationProperty parseProperty(JsonParser parser) {
		ObservationProperty property = null;

		try {
			String key = parser.getCurrentName();
			JsonToken token = parser.nextToken();

			if (token == JsonToken.START_OBJECT) {
				Geometry geometry = geometryDeserializer.parseGeometry(parser);
				byte[] geometryBytes = GeometryUtility.toGeometryBytes(geometry);
				property = new ObservationProperty(key, geometryBytes);
			} else if (token == JsonToken.START_ARRAY) {
				ArrayList<String> stringArrayList = new ArrayList<>();
				ArrayList<Attachment> attachments = new ArrayList<>();

				while (parser.nextToken() != JsonToken.END_ARRAY) {
					if (parser.currentToken() == JsonToken.START_OBJECT) {
						Attachment attachment = attachmentDeserializer.parseAttachment(parser);
						attachments.add(attachment);
					} else {
						String arrayValue = parser.getValueAsString();
						stringArrayList.add(arrayValue);
					}
				}

				if (!stringArrayList.isEmpty()) {
					Serializable value = stringArrayList;
					property = new ObservationProperty(key, value);
				} else if (!attachments.isEmpty()) {
					Serializable value = attachments;
					property = new ObservationProperty(key, value);
				}
			} else if (token != JsonToken.VALUE_NULL) {
				Serializable value = parser.getText();
				if (token.isNumeric()) {
					switch (parser.getNumberType()) {
						case BIG_DECIMAL:
							break;
						case BIG_INTEGER:
							break;
						case DOUBLE:
							value = parser.getDoubleValue();
							break;
						case FLOAT:
							value = parser.getFloatValue();
							break;
						case INT:
							value = parser.getIntValue();
							break;
						case LONG:
							value = parser.getLongValue();
							break;
						default:
							break;
					}
				} else if (token.isBoolean()) {
					value = parser.getBooleanValue();
				}

				property = new ObservationProperty(key, value);
			}
		} catch (Exception e) {
			Log.w(LOG_NAME, "Error parsing observation property, skipping...", e);
		}

		return property;
	}

	private Collection<Attachment> parseAttachments(JsonParser parser) throws IOException {
		Collection<Attachment> attachments = new ArrayList<>();

		if (parser.getCurrentToken() != JsonToken.START_ARRAY) {
			return attachments;
		}

		while (parser.nextToken() != JsonToken.END_ARRAY) {
			attachments.add(attachmentDeserializer.parseAttachment(parser));
		}

		return attachments;
	}

	private ObservationImportant parseImportant(JsonParser parser) throws IOException {
		ObservationImportant important = new ObservationImportant();
		important.setImportant(true);
		important.setDirty(false);

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String name = parser.getCurrentName();
			parser.nextToken();
			if ("userId".equals(name)) {
				important.setUserId(parser.getText());
			} else if ("timestamp".equals(name)) {
				String dateTimeString = parser.getText();
				try {
					Date d = iso8601Format.parse(dateTimeString);
					important.setTimestamp(d);
				} catch (ParseException pe) {
					Log.w(LOG_NAME, "Unable to parse date: " + dateTimeString + " for observation");
				}
			} else if ("description".equals(name)) {
				important.setDescription(parser.getText());
			} else {
				parser.skipChildren();
			}
		}

		return important;
	}

	private Collection<ObservationFavorite> parseFavoriteUsers(JsonParser parser) throws IOException {
		Collection<ObservationFavorite> favorites = new ArrayList<>();

		if (parser.getCurrentToken() != JsonToken.START_ARRAY) {
			return favorites;
		}

		while (parser.nextToken() != JsonToken.END_ARRAY) {
			String userId = parser.getValueAsString();
			ObservationFavorite favorite = new ObservationFavorite(userId, true);
			favorite.setDirty(Boolean.FALSE);
			favorites.add(favorite);
		}

		return favorites;
	}
}