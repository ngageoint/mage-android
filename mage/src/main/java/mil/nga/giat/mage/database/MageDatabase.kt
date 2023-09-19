package mil.nga.giat.mage.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import mil.nga.giat.mage.database.dao.feed.FeedDao
import mil.nga.giat.mage.database.dao.feed.FeedItemDao
import mil.nga.giat.mage.database.dao.feed.FeedLocalDao
import mil.nga.giat.mage.database.model.feed.Feed
import mil.nga.giat.mage.database.model.feed.FeedItem
import mil.nga.giat.mage.database.model.feed.FeedLocal
import mil.nga.giat.mage.database.dao.MageSqliteOpenHelper

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

    fun destroy() {
        feedDao().destroy()
        feedLocalDao().destroy()
        feedItemDao().destroy()
    }
}