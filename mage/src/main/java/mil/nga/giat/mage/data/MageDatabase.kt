package mil.nga.giat.mage.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import mil.nga.giat.mage.data.converters.DateTypeConverter
import mil.nga.giat.mage.data.converters.GeometryTypeConverter
import mil.nga.giat.mage.data.converters.JsonTypeConverter
import mil.nga.giat.mage.data.feed.*
import mil.nga.giat.mage.sdk.datastore.DaoStore

@Database(
        version = MageDatabase.VERSION,
        entities = [
            Feed::class,
            FeedLocal::class,
            FeedItem::class
        ]
)
@TypeConverters(DateTypeConverter::class, GeometryTypeConverter::class, JsonTypeConverter::class)
abstract class MageDatabase : RoomDatabase() {

    companion object {
        const val VERSION = 1
    }

    abstract fun feedDao(): FeedDao
    abstract fun feedLocalDao(): FeedLocalDao
    abstract fun feedItemDao(): FeedItemDao

    fun destroy(context: Context) {
        DaoStore.getInstance(context).resetDatabase()

        feedDao().destroy()
        feedLocalDao().destroy()
        feedItemDao().destroy()
    }
}