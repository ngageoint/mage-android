package mil.nga.giat.mage.sdk.utils

import mil.nga.sf.Geometry
import mil.nga.sf.util.ByteReader
import mil.nga.sf.util.ByteWriter
import mil.nga.sf.wkb.GeometryReader
import mil.nga.sf.wkb.GeometryWriter
import java.lang.Exception
import java.nio.ByteOrder

fun ByteArray.toGeometry(): Geometry? {
   val reader = ByteReader(this)
   reader.byteOrder = ByteOrder.BIG_ENDIAN

   return try {
      GeometryReader.readGeometry(reader)
   } catch (e: Exception) { null }
}

fun Geometry.toBytes(): ByteArray {
   val writer = ByteWriter()
   return try {
      writer.byteOrder = ByteOrder.BIG_ENDIAN
      GeometryWriter.writeGeometry(writer, this)
      writer.bytes
   } finally {
      writer.close()
   }
}