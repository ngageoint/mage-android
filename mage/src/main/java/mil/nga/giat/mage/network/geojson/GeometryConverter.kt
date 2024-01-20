package mil.nga.giat.mage.network.geojson

import mil.nga.sf.Geometry
import mil.nga.sf.GeometryCollection
import mil.nga.sf.GeometryType
import mil.nga.sf.LineString
import mil.nga.sf.LinearRing
import mil.nga.sf.MultiLineString
import mil.nga.sf.MultiPoint
import mil.nga.sf.MultiPolygon
import mil.nga.sf.Point
import mil.nga.sf.Polygon

class GeometryConverter {
   companion object {
      fun convert(geometry: com.mapbox.geojson.Geometry): Geometry? {
         return toGeometryFactory[geometry.type()]?.let { it(geometry) }
      }

      fun convert(geometry: Geometry): com.mapbox.geojson.Geometry? {
         return fromGeometryFactory[geometry.geometryType]?.let { it(geometry) }
      }

      private fun toPoint(geometry: com.mapbox.geojson.Geometry): Point {
         val point = geometry as com.mapbox.geojson.Point
         return Point(point.longitude(), point.latitude()).apply {
            if (point.hasAltitude()) {
               z = point.altitude()
            }
         }
      }

      private fun fromPoint(geometry: Geometry): com.mapbox.geojson.Point {
         val point = geometry as Point
         return if (point.z != null) {
            com.mapbox.geojson.Point.fromLngLat(point.x, point.y, point.z)
         } else {
            com.mapbox.geojson.Point.fromLngLat(point.x, point.y)
         }
      }

      private fun toMultiPoint(geometry: com.mapbox.geojson.Geometry): MultiPoint {
         val multiPoint = geometry as com.mapbox.geojson.MultiPoint
         val points = multiPoint.coordinates().map {
            toPoint(it)
         }

         return MultiPoint(points)
      }

      private fun fromMultiPoint(geometry: Geometry): com.mapbox.geojson.MultiPoint {
         val multiPoint = geometry as MultiPoint
         val points = multiPoint.points.map { fromPoint(it) }
         return com.mapbox.geojson.MultiPoint.fromLngLats(points)
      }

      private fun toLineString(geometry: com.mapbox.geojson.Geometry): LineString {
         val lineString = geometry as com.mapbox.geojson.LineString
         val points = lineString.coordinates().map { toPoint(it) }
         return LineString(points)
      }

      private fun fromLineString(geometry: Geometry): com.mapbox.geojson.LineString {
         val lineString = geometry as LineString
         val points = lineString.points.map { fromPoint(it) }
         return com.mapbox.geojson.LineString.fromLngLats(points)
      }

      private fun toMultiLineString(geometry: com.mapbox.geojson.Geometry): MultiLineString {
         val multiLineString = geometry as com.mapbox.geojson.MultiLineString
         val lineStrings = multiLineString.lineStrings().map { toLineString(it) }
         return MultiLineString(lineStrings)
      }

      private fun fromMultiLineString(geometry: Geometry): com.mapbox.geojson.MultiLineString {
         val multiLineString = geometry as MultiLineString
         val lineStrings = multiLineString.lineStrings.map { fromLineString(it) }
         return com.mapbox.geojson.MultiLineString.fromLineStrings(lineStrings)
      }

      private fun toPolygon(geometry: com.mapbox.geojson.Geometry): Polygon {
         val polygon = geometry as com.mapbox.geojson.Polygon

         val rings = mutableListOf<LineString>()
         val points = polygon.outer()?.coordinates()?.map { toPoint(it) }
         rings.add(LinearRing(points))

         polygon.inner()?.forEach {
            val innerPoints = it.coordinates().map { toPoint(it) }
            rings.add(LinearRing(innerPoints))
         }

         return Polygon(rings)
      }

      private fun fromPolygon(geometry: Geometry): com.mapbox.geojson.Polygon {
         val polygon = geometry as Polygon

         val outer = fromLineString(polygon.rings.first())

         val inner = mutableListOf<com.mapbox.geojson.LineString>()
         polygon.rings.drop(1).forEach {
            inner.add(fromLineString(it))
         }

         return com.mapbox.geojson.Polygon.fromOuterInner(outer, inner)
      }

      private fun toMultiPolygon(geometry: com.mapbox.geojson.Geometry): MultiPolygon {
         val multiPolygon = geometry as com.mapbox.geojson.MultiPolygon

         val polygons = multiPolygon.polygons().map {
            toPolygon(it)
         }

         return MultiPolygon(polygons)
      }

      private fun fromMultiPolygon(geometry: Geometry): com.mapbox.geojson.MultiPolygon {
         val multiPolygon = geometry as MultiPolygon
         val polygons = multiPolygon.polygons.map { fromPolygon(it) }
         return com.mapbox.geojson.MultiPolygon.fromPolygons(polygons)
      }

      private fun toGeometryCollection(geometry: com.mapbox.geojson.Geometry): GeometryCollection<Geometry> {
         val geometryCollection = geometry as com.mapbox.geojson.GeometryCollection

         val geometries = mutableListOf<Geometry>()
         geometryCollection.geometries().forEach {
            val factory = toGeometryFactory[it.type()]
            if (factory != null) {
               geometries.add(factory.invoke(it))
            }
         }

         return GeometryCollection(geometries)
      }

      private fun fromGeometryCollection(geometry: Geometry): com.mapbox.geojson.GeometryCollection {
         val geometryCollection = geometry as GeometryCollection<Geometry>

         val geometries = mutableListOf<com.mapbox.geojson.Geometry>()
         geometryCollection.geometries.forEach {
            fromGeometryFactory[it.geometryType]?.let { factory ->
               geometries.add(factory(it))
            }
         }

         return com.mapbox.geojson.GeometryCollection.fromGeometries(geometries)
      }

      private val toGeometryFactory: Map<String, (com.mapbox.geojson.Geometry) -> Geometry> = mapOf(
         "Point" to ::toPoint,
         "MultiPoint" to ::toMultiPoint,
         "LineString" to ::toLineString,
         "MultiLineString" to ::toMultiLineString,
         "Polygon" to ::toPolygon,
         "MultiPolygon" to ::toMultiPolygon,
         "GeometryCollection" to ::toGeometryCollection
      )

      private val fromGeometryFactory: Map<GeometryType, (Geometry) -> com.mapbox.geojson.Geometry> = mapOf(
         GeometryType.POINT to ::fromPoint,
         GeometryType.MULTIPOINT to ::fromMultiPoint,
         GeometryType.LINESTRING to ::fromLineString,
         GeometryType.MULTILINESTRING to ::fromMultiLineString,
         GeometryType.POLYGON to ::fromPolygon,
         GeometryType.MULTIPOLYGON to ::fromMultiPolygon,
         GeometryType.GEOMETRYCOLLECTION to ::fromGeometryCollection
      )
   }
}