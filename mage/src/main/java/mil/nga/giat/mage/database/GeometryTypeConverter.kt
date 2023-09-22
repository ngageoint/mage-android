package mil.nga.giat.mage.database

import androidx.room.TypeConverter
import mil.nga.giat.mage.sdk.utils.toBytes
import mil.nga.giat.mage.sdk.utils.toGeometry
import mil.nga.sf.Geometry

class GeometryTypeConverter {

    @TypeConverter
    fun fromByteArray(value: ByteArray?): Geometry? {
        return value?.toGeometry()
    }

    @TypeConverter
    fun fromGeometry(geometry: Geometry?): ByteArray? {
        return geometry?.toBytes()
    }

}
