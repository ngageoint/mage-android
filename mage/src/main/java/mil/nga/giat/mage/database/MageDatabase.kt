package mil.nga.giat.mage.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import mil.nga.giat.mage.MageApplication
import mil.nga.giat.mage.database.dao.feed.FeedDao
import mil.nga.giat.mage.database.dao.feed.FeedItemDao
import mil.nga.giat.mage.database.dao.feed.FeedLocalDao
import mil.nga.giat.mage.database.dao.observationLocation.ObservationLocationDao
import mil.nga.giat.mage.database.model.feed.Feed
import mil.nga.giat.mage.database.model.feed.FeedItem
import mil.nga.giat.mage.database.model.feed.FeedLocal
import mil.nga.giat.mage.database.dao.settings.SettingsDao
import mil.nga.giat.mage.database.model.observation.ObservationLocation
import mil.nga.giat.mage.database.model.settings.Settings

@Database(
    version = MageDatabase.VERSION,
    entities = [
        Settings::class,
        Feed::class,
        FeedLocal::class,
        FeedItem::class,
        ObservationLocation::class
    ],
    autoMigrations = [
        AutoMigration (
            from = 2,
            to = 3,
            spec = MageDatabase.Migration2To3::class
        )
    ]
)
@TypeConverters(
    DateTypeConverter::class,
    GeometryTypeConverter::class,
    JsonTypeConverter::class
)
abstract class MageDatabase : RoomDatabase() {

    companion object {
        const val VERSION = 3
    }

    abstract fun settingsDao(): SettingsDao
    abstract fun feedDao(): FeedDao
    abstract fun feedLocalDao(): FeedLocalDao
    abstract fun feedItemDao(): FeedItemDao
    abstract fun observationLocationDao(): ObservationLocationDao

    fun destroy() {
        settingsDao().destroy()
        feedDao().destroy()
        feedLocalDao().destroy()
        feedItemDao().destroy()
        observationLocationDao().destroy()
    }

    // Migration which populates observation locations from existing observations
    class Migration2To3 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            // get the old database

            // read the observations and create Observation Location room entities for the locations
        }
    }
}