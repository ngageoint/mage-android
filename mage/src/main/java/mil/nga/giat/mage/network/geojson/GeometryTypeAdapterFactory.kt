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
                value?.let { geometry ->
                    GeometryConverter.convert(geometry)?.let {
                        typeAdapter.write(`out`, it)
                    }
                }
            }

            @Throws(IOException::class)
            override fun read(`in`: JsonReader): Geometry? {
                val geometry = typeAdapter.read(`in`) ?: return null
                return GeometryConverter.convert(geometry)
            }
        }
    }
}