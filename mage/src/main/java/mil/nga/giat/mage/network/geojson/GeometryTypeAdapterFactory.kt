package mil.nga.giat.mage.network.geojson

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

    private val readFactory: Map<String, (com.mapbox.geojson.Geometry) -> Geometry> = mapOf(
        "Point" to ::readPoint,
        "MultiPoint" to ::readMultiPoint,
        "LineString" to ::readLineString,
        "MultiLineString" to ::readMultiLineString,
        "Polygon" to ::readPolygon,
        "MultiPolygon" to ::readMultiPolygon,
        "GeometryCollection" to ::readGeometryCollection
    )

    private val writeFactory: Map<GeometryType, (Geometry) -> com.mapbox.geojson.Geometry> = mapOf(
        GeometryType.POINT to ::writePoint,
        GeometryType.MULTIPOINT to ::writeMultiPoint,
        GeometryType.LINESTRING to ::writeLineString,
        GeometryType.MULTILINESTRING to ::writeMultiLineString,
        GeometryType.POLYGON to ::writePolygon,
        GeometryType.MULTIPOLYGON to ::writeMultiPolygon,
        GeometryType.GEOMETRYCOLLECTION to ::writeGeometryCollection
    )

    override fun <T> create(gson: Gson, typeToken: TypeToken<T>): TypeAdapter<T>? {
        val rawType: Class<*> = typeToken.rawType
        return if (mil.nga.sf.Geometry::class.java.isAssignableFrom(rawType)) {
            val factory = GeometryAdapterFactory.create()
            val typeAdapter = factory.create(gson, TypeToken.get(com.mapbox.geojson.Geometry::class.java))
            geometryTypeAdapter(typeAdapter) as TypeAdapter<T>
        } else { null }
    }

    private fun geometryTypeAdapter(typeAdapter: TypeAdapter<com.mapbox.geojson.Geometry?>): TypeAdapter<Geometry?> {
        return object : TypeAdapter<Geometry?>() {
            @Throws(IOException::class)
            override fun write(`out`: JsonWriter, value: Geometry?) {
                value?.let {
                    typeAdapter.write(`out`, writeFactory[it.geometryType]?.invoke(it))
                }
            }

            @Throws(IOException::class)
            override fun read(`in`: JsonReader): Geometry? {
                val geometry = typeAdapter.read(`in`) ?: return null
                return readFactory[geometry.type()]?.invoke(geometry)
            }
        }
    }

    private fun readPoint(geometry: com.mapbox.geojson.Geometry): Point {
        val point = geometry as com.mapbox.geojson.Point
        return Point(point.longitude(), point.latitude(), point.altitude())
    }

    private fun writePoint(geometry: Geometry): com.mapbox.geojson.Point {
        val point = geometry as Point
        return if (point.z != null) {
            com.mapbox.geojson.Point.fromLngLat(point.x, point.y, point.z)
        } else {
            com.mapbox.geojson.Point.fromLngLat(point.x, point.y)
        }
    }

    private fun readMultiPoint(geometry: com.mapbox.geojson.Geometry): MultiPoint {
        val multiPoint = geometry as com.mapbox.geojson.MultiPoint
        val points = multiPoint.coordinates().map {
            readPoint(it)
        }

        return MultiPoint(points)
    }

    private fun writeMultiPoint(geometry: Geometry): com.mapbox.geojson.MultiPoint {
        val multiPoint = geometry as MultiPoint
        val points = multiPoint.points.map { writePoint(it) }
        return com.mapbox.geojson.MultiPoint.fromLngLats(points)
    }

    private fun readLineString(geometry: com.mapbox.geojson.Geometry): LineString {
        val lineString = geometry as com.mapbox.geojson.LineString
        val points = lineString.coordinates().map { readPoint(it) }
        return LineString(points)
    }

    private fun writeLineString(geometry: Geometry): com.mapbox.geojson.LineString {
        val lineString = geometry as LineString
        val points = lineString.points.map { writePoint(it) }
        return com.mapbox.geojson.LineString.fromLngLats(points)
    }

    private fun readMultiLineString(geometry: com.mapbox.geojson.Geometry): MultiLineString {
        val multiLineString = geometry as com.mapbox.geojson.MultiLineString
        val lineStrings = multiLineString.lineStrings().map { readLineString(it) }
        return MultiLineString(lineStrings)
    }

    private fun writeMultiLineString(geometry: Geometry): com.mapbox.geojson.MultiLineString {
        val multiLineString = geometry as MultiLineString
        val lineStrings = multiLineString.lineStrings.map { writeLineString(it) }
        return com.mapbox.geojson.MultiLineString.fromLineStrings(lineStrings)
    }

    private fun readPolygon(geometry: com.mapbox.geojson.Geometry): Polygon {
        val polygon = geometry as com.mapbox.geojson.Polygon

        val rings = mutableListOf<LineString>()
        val points = polygon.outer()?.coordinates()?.map { readPoint(it) }
        rings.add(LinearRing(points))

        polygon.inner()?.forEach {
            val innerPoints = it.coordinates().map { readPoint(it) }
            rings.add(LinearRing(innerPoints))
        }

        return Polygon(rings)
    }

    private fun writePolygon(geometry: Geometry): com.mapbox.geojson.Polygon {
        val polygon = geometry as Polygon

        val outer = writeLineString(polygon.rings.first())

        val inner = mutableListOf<com.mapbox.geojson.LineString>()
        polygon.rings.drop(1).forEach {
            inner.add(writeLineString(it))
        }

        return com.mapbox.geojson.Polygon.fromOuterInner(outer, inner)
    }

    private fun readMultiPolygon(geometry: com.mapbox.geojson.Geometry): MultiPolygon {
        val multiPolygon = geometry as com.mapbox.geojson.MultiPolygon

        val polygons = multiPolygon.polygons().map {
            readPolygon(it)
        }

        return MultiPolygon(polygons)
    }

    private fun writeMultiPolygon(geometry: Geometry): com.mapbox.geojson.MultiPolygon {
        val multiPolygon = geometry as MultiPolygon
        val polygons = multiPolygon.polygons.map { writePolygon(it) }
        return com.mapbox.geojson.MultiPolygon.fromPolygons(polygons)
    }

    private fun readGeometryCollection(geometry: com.mapbox.geojson.Geometry): GeometryCollection<Geometry> {
        val geometryCollection = geometry as com.mapbox.geojson.GeometryCollection

        val geometries = mutableListOf<Geometry>()
        geometryCollection.geometries().forEach {
            val factory = readFactory[it.type()]
            if (factory != null) {
                geometries.add(factory.invoke(it))
            }
        }

        return GeometryCollection(geometries)
    }

    private fun writeGeometryCollection(geometry: Geometry): com.mapbox.geojson.GeometryCollection {
        val geometryCollection = geometry as GeometryCollection<Geometry>

        val geometries = mutableListOf<com.mapbox.geojson.Geometry>()
        geometryCollection.geometries.forEach {
            writeFactory[it.geometryType]?.let { factory ->
                geometries.add(factory(it))
            }
        }

        return com.mapbox.geojson.GeometryCollection.fromGeometries(geometries)
    }
}