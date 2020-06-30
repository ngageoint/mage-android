package mil.nga.giat.mage.data.gson

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.mapbox.geojson.GeometryAdapterFactory
import mil.nga.sf.*
import java.io.IOException

class GeometryTypeAdapterFactory : TypeAdapterFactory {

    val converterFactory: Map<String, (com.mapbox.geojson.Geometry) -> Geometry> = mapOf(
            "Point" to ::convertPoint,
            "MultiPoint" to ::convertMultiPoint,
            "LineString" to ::convertLineString,
            "MultiLineString" to ::convertMultiLineString,
            "Polygon" to ::convertPolygon,
            "MultiPolygon" to ::convertMultiPolygon,
            "GeometryCollection" to ::convertGeometryCollection
    )

    override fun <T> create(gson: Gson, typeToken: TypeToken<T>): TypeAdapter<T>? {
        val rawType: Class<*> = typeToken.rawType
        return if (mil.nga.sf.Geometry::class.java.isAssignableFrom(rawType)) {
            val factory = GeometryAdapterFactory.create()
            val typeAdapter = factory.create(gson, TypeToken.get(com.mapbox.geojson.Geometry::class.java))
            geometryTypeAdapter(typeAdapter) as TypeAdapter<T>
        } else {
            null
        }
    }

    private fun geometryTypeAdapter(typeAdapter: TypeAdapter<com.mapbox.geojson.Geometry?>): TypeAdapter<Geometry?> {
        return object : TypeAdapter<Geometry?>() {
            @Throws(IOException::class)
            override fun write(out: JsonWriter, value: Geometry?) {
            }

            @Throws(IOException::class)
            override fun read(`in`: JsonReader): Geometry? {
                val geometry = typeAdapter.read(`in`) ?: return null
                return converterFactory[geometry.type()]?.invoke(geometry)
            }
        }
    }

    private fun convertPoint(geometry: com.mapbox.geojson.Geometry): Point {
        val point = geometry as com.mapbox.geojson.Point
        return Point(point.longitude(), point.latitude(), point.altitude())
    }

    private fun convertMultiPoint(geometry: com.mapbox.geojson.Geometry): MultiPoint {
        val multiPoint = geometry as com.mapbox.geojson.MultiPoint
        val points = multiPoint.coordinates().map {
            convertPoint(it)
        }

        return MultiPoint(points)
    }

    private fun convertLineString(geometry: com.mapbox.geojson.Geometry): LineString {
        val lineString = geometry as com.mapbox.geojson.LineString
        val points = lineString.coordinates().map {
            convertPoint(it)
        }

        return LineString(points)
    }

    private fun convertMultiLineString(geometry: com.mapbox.geojson.Geometry): MultiLineString {
        val multiLineString = geometry as com.mapbox.geojson.MultiLineString
        val lineStrings = multiLineString.lineStrings().map {
            convertLineString(it)
        }

        return MultiLineString(lineStrings)
    }

    private fun convertPolygon(geometry: com.mapbox.geojson.Geometry): Polygon {
        val polygon = geometry as com.mapbox.geojson.Polygon

        val rings = mutableListOf<LineString>()

        val points = polygon.outer()?.coordinates()?.map {
            convertPoint(it)
        }
        rings.add(LinearRing(points))

        polygon.inner()?.forEach {
            val innerPoints = it.coordinates().map { point ->
                convertPoint(point)
            }

            rings.add(LinearRing(innerPoints))
        }

        return Polygon(rings)
    }

    private fun convertMultiPolygon(geometry: com.mapbox.geojson.Geometry): MultiPolygon {
        val multiPolygon = geometry as com.mapbox.geojson.MultiPolygon

        val polygons = multiPolygon.polygons().map {
            convertPolygon(it)
        }

        return MultiPolygon(polygons)
    }

    private fun convertGeometryCollection(geometry: com.mapbox.geojson.Geometry): GeometryCollection<Geometry> {
        val geometryCollection = geometry as com.mapbox.geojson.GeometryCollection

        val geometries = mutableListOf<Geometry>()
        geometryCollection.geometries().forEach {
            val factory = converterFactory[it.type()]
            if (factory != null) {
                geometries.add(factory.invoke(it))
            }
        }

        return GeometryCollection(geometries)
    }
}