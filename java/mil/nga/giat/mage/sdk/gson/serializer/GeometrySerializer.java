package mil.nga.giat.mage.sdk.gson.serializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.List;

import mil.nga.wkb.geom.Geometry;
import mil.nga.wkb.geom.GeometryCollection;
import mil.nga.wkb.geom.GeometryType;
import mil.nga.wkb.geom.LineString;
import mil.nga.wkb.geom.MultiLineString;
import mil.nga.wkb.geom.MultiPoint;
import mil.nga.wkb.geom.MultiPolygon;
import mil.nga.wkb.geom.Point;
import mil.nga.wkb.geom.Polygon;

public class GeometrySerializer implements JsonSerializer<Geometry> {

	public static Gson getGsonBuilder() {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Geometry.class, new GeometrySerializer());
		gsonBuilder.registerTypeAdapter(Point.class, new GeometrySerializer());
		gsonBuilder.registerTypeAdapter(MultiPoint.class, new GeometrySerializer());
		gsonBuilder.registerTypeAdapter(LineString.class, new GeometrySerializer());
		gsonBuilder.registerTypeAdapter(MultiLineString.class, new GeometrySerializer());
		gsonBuilder.registerTypeAdapter(Polygon.class, new GeometrySerializer());
		gsonBuilder.registerTypeAdapter(MultiPolygon.class, new GeometrySerializer());
		gsonBuilder.registerTypeAdapter(GeometryCollection.class, new GeometrySerializer());

		return gsonBuilder.create();
	}
	
	@Override
	public JsonElement serialize(Geometry geometry, Type type, JsonSerializationContext jsonSerializationContext) {
		JsonObject json = new JsonObject();

		GeometryType geometryType = geometry.getGeometryType();
		switch(geometryType){
			case POINT:
				json.addProperty("type", "Point");
				json.add("coordinates", toJson((Point) geometry));
				break;
			case MULTIPOINT:
				json.addProperty("type", "MultiPoint");
				json.add("coordinates", toJson((MultiPoint) geometry));
				break;
			case LINESTRING:
				json.addProperty("type", "LineString");
				json.add("coordinates", toJson((LineString) geometry));
				break;
			case MULTILINESTRING:
				json.addProperty("type", "MultiLineString");
				json.add("coordinates", toJson((MultiLineString) geometry));
				break;
			case POLYGON:
				json.addProperty("type", "Polygon");
				json.add("coordinates", toJson((Polygon) geometry));
				break;
			case MULTIPOLYGON:
				json.addProperty("type", "MultiPolygon");
				json.add("coordinates", toJson((MultiPolygon) geometry));
				break;
			case GEOMETRYCOLLECTION:
				json.addProperty("type", "GeometryCollection");
				json.add("geometries", toJson(jsonSerializationContext, (GeometryCollection<Geometry>) geometry));
				break;
			default:
				throw new IllegalArgumentException("Unsupported geometry type: " + geometryType);
		}

		return json;
	}

	private JsonArray toJson(LineString geometry) {
		return toJson(geometry.getPoints(), false);
	}

	private JsonArray toJson(LineString geometry, boolean close) {
		return toJson(geometry.getPoints(), close);
	}

	private JsonArray toJson(Polygon polygon) {
		JsonArray result = new JsonArray();
		result.add(toJson(polygon.getRings().get(0), true));
		for (int i = 1; i < polygon.numRings(); i++) {
			result.add(toJson(polygon.getRings().get(i), true));
		}
		return result;
	}

	private JsonArray toJson(List<Point> points, boolean close) {
		JsonArray result = new JsonArray();
		for (Point point : points) {
			result.add(toJson(point));
		}
		if(close) {
			Point firstPoint = points.get(0);
			Point lastPoint = points.get(points.size() - 1);
			if (firstPoint.getX() != lastPoint.getX() || firstPoint.getY() != lastPoint.getY()) {
				result.add(toJson(firstPoint));
			}
		}
		return result;
	}

	private JsonArray toJson(Point point) {
		JsonArray coordinate = new JsonArray();
		coordinate.add(new JsonPrimitive(point.getX()));
		coordinate.add(new JsonPrimitive(point.getY()));
		if (point.hasZ()) {
			coordinate.add(new JsonPrimitive(point.getZ()));
		}
		return coordinate;
	}

	private JsonArray toJson(MultiPoint multiPoint){
		JsonArray coordinates = new JsonArray();
		for(Point point: multiPoint.getPoints()){
			coordinates.add(toJson(point));
		}
		return coordinates;
	}

	private JsonArray toJson(MultiLineString multiLineString){
		JsonArray coordinates = new JsonArray();
		for (LineString lineString: multiLineString.getLineStrings()) {
			coordinates.add(toJson(lineString));
		}
		return coordinates;
	}

	private JsonArray toJson(MultiPolygon multiPolygon){
		JsonArray coordinates = new JsonArray();
		for (Polygon polygon: multiPolygon.getPolygons()) {
			coordinates.add(toJson(polygon));
		}
		return coordinates;
	}

	private JsonArray toJson(JsonSerializationContext jsonSerializationContext, GeometryCollection<Geometry> geometryCollection){
		JsonArray geometries = new JsonArray();
		for (Geometry geometry: geometryCollection.getGeometries()) {
			geometries.add(jsonSerializationContext.serialize(geometry));
		}
		return geometries;
	}

}
