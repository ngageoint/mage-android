package mil.nga.giat.mage.network.geojson

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.mapbox.geojson.GeometryAdapterFactory
import mil.nga.sf.*
import java.io.IOException

class GeometryTypeAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, typeToken: TypeToken<T>): TypeAdapter<T>? {
        val rawType: Class<*> = typeToken.rawType
        return if (mil.nga.sf.Geometry::class.java.isAssignableFrom(rawType)) {
            val factory = GeometryAdapterFactory.create()
            val writeAdapter = factory.create(gson, TypeToken.get(com.mapbox.geojson.Geometry::class.java))
            geometryTypeAdapter(writeAdapter) as TypeAdapter<T>
        } else { null }
    }

    private fun geometryTypeAdapter(writeAdapter: TypeAdapter<com.mapbox.geojson.Geometry?>): TypeAdapter<Geometry?> {
        return object : TypeAdapter<Geometry?>() {
            @Throws(IOException::class)
            override fun write(`out`: JsonWriter, value: Geometry?) {
                value?.let { geometry ->
                    GeometryConverter.convert(geometry)?.let {
                        writeAdapter.write(`out`, it)
                    }
                }
            }

            @Throws(IOException::class)
            override fun read(`in`: JsonReader): Geometry? {
                val jsonElement = JsonParser.parseReader(`in`)
                if (jsonElement == null || jsonElement.isJsonNull) return null
                val obj = runCatching { jsonElement.asJsonObject }.getOrNull() ?: return null
                val type = obj.get("type")?.asString ?: return null
                val json = jsonElement.toString()
                // Use Mapbox's own fromJson() which correctly handles coordinate arrays
                // (e.g. [lng,lat] within Polygon rings) rather than the Gson adapter chain,
                // which falls back to reflection and expects JSON objects for Point coordinates.
                val mapboxGeometry: com.mapbox.geojson.Geometry? = when (type) {
                    "Point" -> com.mapbox.geojson.Point.fromJson(json)
                    "MultiPoint" -> com.mapbox.geojson.MultiPoint.fromJson(json)
                    "LineString" -> com.mapbox.geojson.LineString.fromJson(json)
                    "MultiLineString" -> com.mapbox.geojson.MultiLineString.fromJson(json)
                    "Polygon" -> com.mapbox.geojson.Polygon.fromJson(json)
                    "MultiPolygon" -> com.mapbox.geojson.MultiPolygon.fromJson(json)
                    "GeometryCollection" -> com.mapbox.geojson.GeometryCollection.fromJson(json)
                    else -> null
                }
                return mapboxGeometry?.let { GeometryConverter.convert(it) }
            }
        }
    }
}