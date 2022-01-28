package mil.nga.giat.mage.sdk.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureProperty;

public class StaticFeatureDeserializer extends Deserializer {

	private final GeometryDeserializer geometryDeserializer = new GeometryDeserializer();

	public List<StaticFeature> parseStaticFeatures(InputStream is, Layer layer) throws IOException {
		List<StaticFeature> features = new ArrayList<StaticFeature>();

		JsonParser parser = factory.createParser(is);
		parser.nextToken();

		if (parser.getCurrentToken() != JsonToken.START_OBJECT) {
			return features;
		}

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String name = parser.getCurrentName();
			if ("features".equals(name)) {
				parser.nextToken();
				while (parser.nextToken() != JsonToken.END_ARRAY) {
					StaticFeature feature = parseFeature(parser);
					feature.setLayer(layer);
					features.add(feature);
				}
			} else {
				parser.nextToken();
				parser.skipChildren();
			}
		}

		parser.close();
		return features;
	}

	private StaticFeature parseFeature(JsonParser parser) throws IOException {
		StaticFeature o = new StaticFeature();
		if (parser.getCurrentToken() != JsonToken.START_OBJECT) {
			return o;
		}

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String name = parser.getCurrentName();
			if ("id".equals(name)) {
				parser.nextToken();
				o.setRemoteId(parser.getText());
			} else if ("geometry".equals(name)) {
				parser.nextToken();
				o.setGeometry(geometryDeserializer.parseGeometry(parser));
			} else if ("properties".equals(name)) {
				parser.nextToken();
				o.setProperties(parseProperties(parser));
			} else {
				parser.nextToken();
				parser.skipChildren();
			}
		}

		return o;
	}

	private Collection<StaticFeatureProperty> parseProperties(JsonParser parser) throws IOException {
		Collection<StaticFeatureProperty> properties = new ArrayList<>();
		return parseProperties(parser, properties, "");
	}

	private Collection<StaticFeatureProperty> parseProperties(JsonParser parser, Collection<StaticFeatureProperty> properties, String keyPrefix) throws IOException {
		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String key = keyPrefix + parser.getCurrentName().toLowerCase();
			if (parser.nextToken() == JsonToken.START_OBJECT) {
				parseProperties(parser, properties, key);
			} else {
				String value = parser.getText();
				properties.add(new StaticFeatureProperty(key, value));
			}
		}

		return properties;
	}

}