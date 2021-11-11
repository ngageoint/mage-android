package mil.nga.giat.mage.dagger.module

import android.app.Application
import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mil.nga.giat.mage.data.MageDatabase
import mil.nga.giat.mage.data.feed.FeedDao
import mil.nga.giat.mage.data.feed.FeedItemDao
import mil.nga.giat.mage.data.feed.FeedLocalDao
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class RoomModule {

    @Provides
    @Singleton
    fun provideDatabase(application: Application): MageDatabase {
        return Room.databaseBuilder(application.applicationContext, MageDatabase::class.java, "mage")
                .fallbackToDestructiveMigration()
                .build()
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
}