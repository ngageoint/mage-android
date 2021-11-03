package mil.nga.giat.mage.network.gson

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import mil.nga.sf.*

class GeometryTypeAdapter(val context: Context): TypeAdapter<Geometry>() {
   override fun write(out: JsonWriter, value: Geometry) {
      throw UnsupportedOperationException()
   }

   override fun read(reader: JsonReader): Geometry? {
      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         reader.skipValue()
         return null
      }

      reader.beginObject()

      var typeName: String? = null
      var coordinates: JsonArray? = null
      var geometry: Geometry? = null
      while(reader.hasNext()) {
         when(reader.nextName()) {
            "type" -> typeName = reader.nextString()
            "coordinates" -> {
               coordinates = JsonParser.parseReader(reader) as? JsonArray
            }
            else -> reader.skipValue()
         }
      }

      if (typeName != null) {
         geometry = when (typeName) {
            "Point" -> toPoint(coordinates)
            "MultiPoint" -> toMultiPoint(coordinates)
            "LineString" -> toLineString(coordinates)
            "MultiLineString" -> toMultiLineString(coordinates)
            "Polygon" -> toPolygon(coordinates)
            "MultiPolygon" -> toMultiPolygon(coordinates)
            "GeometryCollection" -> toGeometryCollection(reader)
            else -> return null

         }
      }

      reader.endObject()

      return geometry
   }

   private fun toPoint(jsonArray: JsonArray?): Point? {
      if (jsonArray == null || jsonArray.size() < 2) return null

      val x = jsonArray[0].asDouble
      val y = jsonArray[1].asDouble
      return Point(x, y)
   }

   private fun toMultiPoint(jsonArray: JsonArray?): MultiPoint? {
      if (jsonArray == null) return null

      val multiPoint = MultiPoint()
      for (i in 0 until jsonArray.size()) {
         toPoint(jsonArray[i] as? JsonArray)?.let {
            multiPoint.addPoint(it)
         }
      }
      return multiPoint
   }

   private fun toLineString(jsonArray: JsonArray?): LineString? {
      if (jsonArray == null) return null

      val lineString = LineString()
      for (i in 0 until jsonArray.size()) {
         toPoint(jsonArray[i] as? JsonArray)?.let {
            lineString.addPoint(it)
         }
      }
      return lineString
   }

   private fun toMultiLineString(jsonArray: JsonArray?): MultiLineString? {
      if (jsonArray == null) return null

      val multiLineString = MultiLineString()
      for (i in 0 until jsonArray.size()) {
         toLineString(jsonArray[i] as? JsonArray)?.let {
            multiLineString.addLineString(it)
         }
      }
      return multiLineString
   }

   private fun toPolygon(jsonArray: JsonArray?): Polygon? {
      if (jsonArray == null) return null

      val polygon = Polygon()
      for (i in 0 until jsonArray.size()) {
         toLineString(jsonArray[i] as? JsonArray)?.let {
            polygon.addRing(it)
         }
      }
      return polygon
   }

   private fun toMultiPolygon(jsonArray: JsonArray?): MultiPolygon? {
      if (jsonArray == null) return null

      val multiPolygon = MultiPolygon()
      for (i in 0 until jsonArray.size()) {
         val polygon = toPolygon(jsonArray[i] as? JsonArray)
         multiPolygon.addPolygon(polygon)
      }
      return multiPolygon
   }

   private fun toGeometryCollection(reader: JsonReader): GeometryCollection<Geometry> {
      val geometryCollection = GeometryCollection<Geometry>()

      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         return geometryCollection
      }

      reader.beginObject()

      while(reader.hasNext()) {
         when(reader.nextName()) {
            "geometries" -> {
               read(reader)?.let {
                  geometryCollection.addGeometry(it)
               }
            }
            else -> reader.skipValue()
         }
      }

      reader.endObject()

      return geometryCollection
   }
}