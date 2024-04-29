package mil.nga.giat.mage.di

import android.app.Application
import androidx.room.Room
import com.j256.ormlite.dao.Dao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
//import mil.nga.giat.mage.database.DansMigration
import mil.nga.giat.mage.database.MageDatabase
import mil.nga.giat.mage.database.dao.feed.FeedDao
import mil.nga.giat.mage.database.dao.feed.FeedItemDao
import mil.nga.giat.mage.database.dao.feed.FeedLocalDao
import mil.nga.giat.mage.database.dao.observationLocation.ObservationLocationDao
import mil.nga.giat.mage.database.dao.settings.SettingsDao
import mil.nga.giat.mage.database.model.observation.Observation
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class RoomModule {

    @Provides
    @Singleton
    fun provideDatabase(
        application: Application
//        observationDao: Dao<Long, Observation>
    ): MageDatabase {
        return Room.databaseBuilder(application.applicationContext, MageDatabase::class.java, "mage")
//            .addMigrations(DansMigration(observationDao))
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideSettingsDao(database: MageDatabase): SettingsDao {
        return database.settingsDao()
    }

    @Provides
    @Singleton
    fun provideFeedDao(database: MageDatabase): FeedDao {
        return database.feedDao()
    }

    @Provides
    @Singleton
    fun provideFeedLocalDao(database: MageDatabase): FeedLocalDao {
        return database.feedLocalDao()
    }

    @Provides
    @Singleton
    fun provideFeedItemDao(database: MageDatabase): FeedItemDao {
        return database.feedItemDao()
    }

    @Provides
    @Singleton
    fun provideObservationLocationDao(database: MageDatabase): ObservationLocationDao {
        return database.observationLocationDao()
    }
}