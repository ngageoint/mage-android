package mil.nga.giat.mage.data.converters

import androidx.room.TypeConverter
import com.google.gson.InstanceCreator
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import mil.nga.giat.mage.sdk.utils.GeometryUtility
import mil.nga.sf.Geometry
import mil.nga.sf.util.GeometryUtils
import java.lang.reflect.Type

class GeometryTypeConverter {

    @TypeConverter
    fun fromByteArray(value: ByteArray?): Geometry? {
        return value?.let {
            GeometryUtility.toGeometry(value)
        }
    }

    @TypeConverter
    fun fromGeometry(geometry: Geometry?): ByteArray? {
        return geometry?.let {
            GeometryUtility.toGeometryBytes(it)
        }
    }

}
class GeometryInstanceCreator: InstanceCreator<Geometry> {
    override fun createInstance(type: Type?): Geometry {
        TODO("Not yet implemented")
    }

}