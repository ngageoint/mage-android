package mil.nga.giat.mage.sdk.utils;

import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.nio.ByteOrder;

import mil.nga.sf.Geometry;
import mil.nga.sf.util.ByteReader;
import mil.nga.sf.util.ByteWriter;
import mil.nga.sf.wkb.GeometryReader;
import mil.nga.sf.wkb.GeometryWriter;

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
    @Nullable
    public static Geometry toGeometry(byte[] geometryBytes) {
        ByteReader reader = new ByteReader(geometryBytes);
        reader.setByteOrder(ByteOrder.BIG_ENDIAN);
        Geometry geometry = null;
        try {
            geometry = GeometryReader.readGeometry(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
            GeometryWriter.writeGeometry(writer, geometry);
            bytes = writer.getBytes();
        } catch (IOException e) {
            Log.e(GeometryUtility.class.getSimpleName(), "Problem reading observation.", e);
        } finally {
            writer.close();
        }
        return bytes;
    }

}
