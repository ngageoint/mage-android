package mil.nga.giat.mage.sdk.jackson.deserializer;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class GeometryDeserializer extends Deserializer {
    
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public Geometry parseGeometry(JsonParser parser) throws JsonParseException, IOException {
        if (parser.getCurrentToken() != JsonToken.START_OBJECT) return null;
        
        String typeName = null;
        ArrayNode coordinates = null;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String name = parser.getCurrentName();
            if ("type".equals(name)) {
                parser.nextToken();
                typeName = parser.getText();
            } else if ("coordinates".equals(name)) {
                parser.nextToken();
                coordinates = parser.readValueAsTree();
            } else {
                parser.nextToken();
                parser.skipChildren();
            }
        }

        Geometry geometry = null;
        if (typeName.equals("Point")) {
            geometry = geometryFactory.createPoint(new Coordinate(coordinates.get(0).asDouble(), coordinates.get(1).asDouble()));
        } else if (typeName.equals("MultiPoint")) {
            geometry = geometryFactory.createMultiPoint(parseLineString(coordinates));
        } else if (typeName.equals("LineString")) {
            geometry = geometryFactory.createLineString(parseLineString(coordinates));
        } else if (typeName.equals("MultiLineString")) {
            geometry = geometryFactory.createMultiLineString(parseLineStrings(coordinates));
        } else if (typeName.equals("Polygon")) {
            geometry = parsePolygonCoordinates(coordinates);
        } else if (typeName.equals("MultiPolygon")) {
            geometry = geometryFactory.createMultiPolygon(parsePolygons(coordinates));
        } else if (typeName.equals("GeometryCollection")) {
            geometry = geometryFactory.createGeometryCollection(parseGeometries(coordinates));
        }
        
        return geometry;
    }
    
    private Coordinate parseCoordinate(JsonNode coordinate) throws JsonParseException, IOException {
        return new Coordinate(coordinate.get(0).asDouble(), coordinate.get(1).asDouble());
    }
    
    private Coordinate[] parseLineString(JsonNode array) throws JsonParseException, IOException {
        Coordinate[] points = new Coordinate[array.size()];
        for (int i = 0; i < array.size(); ++i) {
            points[i] = parseCoordinate(array.get(i));
        }
        return points;
    }
    
    private LineString[] parseLineStrings(JsonNode array) throws JsonParseException, IOException {
        LineString[] strings = new LineString[array.size()];
        for (int i = 0; i != array.size(); ++i) {
            strings[i] = geometryFactory.createLineString(parseLineString(array.get(i)));
        }
        return strings;
    }
    
    private Polygon parsePolygonCoordinates(JsonNode arrayOfRings) throws JsonParseException, IOException {
        return geometryFactory.createPolygon(parseExteriorRing(arrayOfRings), parseInteriorRings(arrayOfRings));
    }

    private Geometry[] parseGeometries(JsonNode arrayOfGeoms) throws JsonParseException, IOException {
        Geometry[] items = new Geometry[arrayOfGeoms.size()];
        for (int i = 0; i != arrayOfGeoms.size(); ++i) {
            items[i] = parseGeometry(arrayOfGeoms.get(i).traverse());
        }
        return items;
    }

    private Polygon[] parsePolygons(JsonNode arrayOfPolygons) throws JsonParseException, IOException {
        Polygon[] polygons = new Polygon[arrayOfPolygons.size()];
        for (int i = 0; i != arrayOfPolygons.size(); i++) {
            polygons[i] = parsePolygonCoordinates(arrayOfPolygons.get(i));
        }
        return polygons;
    }

    private LinearRing parseExteriorRing(JsonNode arrayOfRings) throws JsonParseException, IOException {
        return geometryFactory.createLinearRing(parseLineString(arrayOfRings.get(0)));
    }

    private LinearRing[] parseInteriorRings(JsonNode arrayOfRings) throws JsonParseException, IOException {
        LinearRing rings[] = new LinearRing[arrayOfRings.size() - 1];
        for (int i = 1; i < arrayOfRings.size(); i++) {
            rings[i - 1] = geometryFactory.createLinearRing(parseLineString(arrayOfRings.get(i)));
        }
        return rings;
    }
}