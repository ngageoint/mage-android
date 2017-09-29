package mil.nga.giat.mage.sdk.utils;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteOrder;

import mil.nga.wkb.geom.Geometry;
import mil.nga.wkb.io.ByteReader;
import mil.nga.wkb.io.ByteWriter;
import mil.nga.wkb.io.WkbGeometryReader;
import mil.nga.wkb.io.WkbGeometryWriter;

/**
 * Geometry Utilities
 *
 * @author osbornb
 */
public class GeometryUtility {

    /**
     * Convert well-known binary bytes to a Geometry
     *
     * @param geometryBytes geometry bytes
     * @return geometry
     */
    public static Geometry toGeometry(byte[] geometryBytes) {
        ByteReader reader = new ByteReader(geometryBytes);
        reader.setByteOrder(ByteOrder.BIG_ENDIAN);
        Geometry geometry = WkbGeometryReader.readGeometry(reader);
        return geometry;
    }

    /**
     * Convert a Geometry to well-known binary bytes
     *
     * @param geometry geometry
     * @return well-known binary bytes
     */
    public static byte[] toGeometryBytes(Geometry geometry) {
        byte[] bytes = null;
        ByteWriter writer = new ByteWriter();
        try {
            writer.setByteOrder(ByteOrder.BIG_ENDIAN);
            WkbGeometryWriter.writeGeometry(writer, geometry);
            bytes = writer.getBytes();
        } catch (IOException e) {
            Log.e(GeometryUtility.class.getSimpleName(), "Problem reading observation.", e);
        } finally {
            writer.close();
        }
        return bytes;
    }

}
