package mil.nga.giat.mage.sdk.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;

import mil.nga.wkb.geom.Geometry;
import mil.nga.wkb.geom.GeometryCollection;
import mil.nga.wkb.geom.LineString;
import mil.nga.wkb.geom.MultiLineString;
import mil.nga.wkb.geom.MultiPoint;
import mil.nga.wkb.geom.MultiPolygon;
import mil.nga.wkb.geom.Point;
import mil.nga.wkb.geom.Polygon;

public class GeometryDeserializer extends Deserializer {

    public Geometry parseGeometry(JsonParser parser) throws IOException {
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

        if (typeName == null) {
            throw new IOException("'type' not present");
        }

        Geometry geometry = null;
        switch (typeName) {
            case "Point":
                geometry = toPoint(coordinates);
                break;
            case "MultiPoint":
                geometry = toMultiPoint(coordinates);
                break;
            case "LineString":
                geometry = toLineString(coordinates);
                break;
            case "MultiLineString":
                geometry = toMultiLineString(coordinates);
                break;
            case "Polygon":
                geometry = toPolygon(coordinates);
                break;
            case "MultiPolygon":
                geometry = toMultiPolygon(coordinates);
                break;
            case "GeometryCollection":
                geometry = toGeometryCollection(coordinates);
                break;
            default:
                throw new IOException("'type' not supported: " + typeName);
        }

        return geometry;
    }

    /**
     * Convert a node to a Point
     *
     * @param node Point node
     * @return Point
     * @throws IOException
     */
    public Point toPoint(JsonNode node) throws IOException {
        double x = node.get(0).asDouble();
        double y = node.get(1).asDouble();
        Point point = new Point(x, y);
        return point;
    }

    /**
     * Convert a node to a MultiPoint
     *
     * @param node MultiPoint node
     * @return MultiPoint
     * @throws IOException
     */
    public MultiPoint toMultiPoint(JsonNode node) throws IOException {

        MultiPoint multiPoint = new MultiPoint();

        for (int i = 0; i < node.size(); ++i) {
            Point point = toPoint(node.get(i));
            multiPoint.addPoint(point);
        }

        return multiPoint;
    }

    /**
     * Convert a node to a LineString
     *
     * @param node LineString node
     * @return LineString
     * @throws IOException
     */
    public LineString toLineString(JsonNode node) throws IOException {

        LineString lineString = new LineString();

        for (int i = 0; i < node.size(); ++i) {
            Point point = toPoint(node.get(i));
            lineString.addPoint(point);
        }

        return lineString;
    }

    /**
     * Convert a node to a MultiLineString
     *
     * @param node MultiLineString node
     * @return MultiLineString
     * @throws IOException
     */
    public MultiLineString toMultiLineString(JsonNode node) throws IOException {

        MultiLineString multiLineString = new MultiLineString();

        for (int i = 0; i < node.size(); ++i) {
            LineString lineString = toLineString(node.get(i));
            multiLineString.addLineString(lineString);
        }

        return multiLineString;
    }

    /**
     * Convert a node to a Polygon
     *
     * @param node Polygon node
     * @return Polygon
     * @throws IOException
     */
    public Polygon toPolygon(JsonNode node) throws IOException {

        Polygon polygon = new Polygon();

        LineString polygonLineString = toLineString(node.get(0));
        polygon.addRing(polygonLineString);

        for (int i = 1; i < node.size(); i++) {
            LineString holeLineString = toLineString(node.get(i));
            polygon.addRing(holeLineString);
        }

        return polygon;
    }

    /**
     * Convert a node to a MultiPolygon
     *
     * @param node MultiPolygon node
     * @return MultiPolygon
     * @throws IOException
     */
    public MultiPolygon toMultiPolygon(JsonNode node) throws IOException {

        MultiPolygon multiPolygon = new MultiPolygon();

        for (int i = 0; i < node.size(); i++) {
            Polygon polygon = toPolygon(node.get(i));
            multiPolygon.addPolygon(polygon);
        }

        return multiPolygon;
    }

    /**
     * Convert a node to a GeometryCollection
     *
     * @param node GeometryCollection node
     * @return GeometryCollection
     * @throws IOException
     */
    public GeometryCollection<Geometry> toGeometryCollection(JsonNode node) throws IOException {

        GeometryCollection<Geometry> geometryCollection = new GeometryCollection();

        for (int i = 0; i < node.size(); i++) {
            Geometry geometry = parseGeometry(node.get(i).traverse());
            geometryCollection.addGeometry(geometry);
        }

        return geometryCollection;
    }

}