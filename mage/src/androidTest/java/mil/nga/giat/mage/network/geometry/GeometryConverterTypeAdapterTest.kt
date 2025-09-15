package mil.nga.giat.mage.network.geometry

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.mapbox.geojson.GeometryAdapterFactory
import com.mapbox.geojson.gson.GeoJsonAdapterFactory
import mil.nga.giat.mage.network.geojson.GeometryTypeAdapterFactory
import mil.nga.sf.GeometryType
import mil.nga.sf.LineString
import mil.nga.sf.Point
import mil.nga.sf.Polygon
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.io.StringReader
import java.nio.charset.StandardCharsets

class GeometryConverterTypeAdapterTest {

   val gson = GsonBuilder()
      .registerTypeAdapterFactory(GeometryTypeAdapterFactory())
      .registerTypeAdapterFactory(GeoJsonAdapterFactory.create())
      .registerTypeAdapterFactory(GeometryAdapterFactory.create())
      .create()

   @Test
   fun should_serialize_point() {
      val typeAdapter = GeometryTypeAdapterFactory().create(gson, TypeToken.get(mil.nga.sf.Point::class.java))

      val out = ByteArrayOutputStream()
      JsonWriter(OutputStreamWriter(out, StandardCharsets.UTF_8)).use { writer ->
         typeAdapter?.write(writer, Point(1.0, 1.0))
      }

      val json = String(out.toByteArray())
      Assert.assertEquals("{\"type\":\"Point\",\"bbox\":null,\"coordinates\":[1.0,1.0]}", json)
   }

   @Test
   fun should_deserialize_point() {
      val json = "{\"type\":\"Point\",\"bbox\":null,\"coordinates\":[1.0,1.0]}"
      val typeAdapter = GeometryTypeAdapterFactory().create(gson, TypeToken.get(mil.nga.sf.Point::class.java))

      val point: Point? = JsonReader(StringReader(json)).use { reader ->
         typeAdapter?.read(reader)
      }

      Assert.assertEquals(point?.geometryType, GeometryType.POINT)
      Assert.assertTrue(point == Point(1.0, 1.0))
   }

   @Test
   fun should_serialize_line() {
      val typeAdapter = GeometryTypeAdapterFactory().create(gson, TypeToken.get(mil.nga.sf.LineString::class.java))

      val line = LineString(listOf(Point(1.0, 1.0), Point(2.0, 2.0)))

      val out = ByteArrayOutputStream()
      JsonWriter(OutputStreamWriter(out, StandardCharsets.UTF_8)).use { writer ->
         typeAdapter?.write(writer, line)
      }

      val json = String(out.toByteArray())
      Assert.assertEquals("{\"type\":\"LineString\",\"bbox\":null,\"coordinates\":[[1.0,1.0],[2.0,2.0]]}", json)
   }

   @Test
   fun should_deserialize_line() {
      val json = "{\"type\":\"LineString\",\"bbox\":null,\"coordinates\":[[1.0,1.0],[2.0,2.0]]}"
      val typeAdapter = GeometryTypeAdapterFactory().create(gson, TypeToken.get(mil.nga.sf.LineString::class.java))

      val line = JsonReader(StringReader(json)).use { reader ->
         typeAdapter?.read(reader)
      }

      Assert.assertEquals(line?.geometryType, GeometryType.LINESTRING)
      Assert.assertEquals(line?.points?.size, 2)
      Assert.assertTrue(Point(1.0, 1.0) == line?.points?.getOrNull(0))
      Assert.assertTrue(Point(2.0, 2.0) == line?.points?.getOrNull(1))
   }

   @Test
   fun should_serialize_polygon() {
      val typeAdapter = GeometryTypeAdapterFactory().create(gson, TypeToken.get(mil.nga.sf.Polygon::class.java))

      val rings = listOf(LineString(listOf(
         Point(1.0, 1.0), Point(2.0, 2.0),
         Point(2.0, 2.0), Point(3.0, 3.0),
         Point(1.0, 1.0), Point(1.0, 1.0))
      ))
      val polygon = Polygon(rings)

      val out = ByteArrayOutputStream()
      JsonWriter(OutputStreamWriter(out, StandardCharsets.UTF_8)).use { writer ->
         typeAdapter?.write(writer, polygon)
      }

      val json = String(out.toByteArray())
      Assert.assertEquals("{\"type\":\"Polygon\",\"bbox\":null,\"coordinates\":[[[1.0,1.0],[2.0,2.0],[2.0,2.0],[3.0,3.0],[1.0,1.0],[1.0,1.0]]]}", json)
   }

   @Test
   fun should_deserialize_polygon() {
      val json = "{\"type\":\"Polygon\",\"bbox\":null,\"coordinates\":[[[1.0,1.0],[2.0,2.0],[2.0,2.0],[3.0,3.0],[1.0,1.0],[1.0,1.0]]]}"
      val typeAdapter = GeometryTypeAdapterFactory().create(gson, TypeToken.get(mil.nga.sf.Polygon::class.java))

      val polygon = JsonReader(StringReader(json)).use { reader ->
         typeAdapter?.read(reader)
      }

      Assert.assertEquals(polygon?.geometryType, GeometryType.POLYGON)
      Assert.assertEquals(polygon?.rings?.size, 1)
      Assert.assertTrue(polygon?.rings?.getOrNull(0) == LineString(listOf(
         Point(1.0, 1.0), Point(2.0, 2.0),
         Point(2.0, 2.0), Point(3.0, 3.0),
         Point(1.0, 1.0), Point(1.0, 1.0))
      ))
   }
}