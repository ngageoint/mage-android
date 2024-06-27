package mil.nga.giat.mage.di

import android.app.Application
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.data.datasource.layer.LayerLocalDataSource
import mil.nga.giat.mage.map.cache.CacheProvider
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class GeoPackageModule {

    @Provides
    @Singleton
    fun provideCacheProvider(
        application: Application,
        layerLocalDataSource: LayerLocalDataSource,
        eventLocalDataSource: EventLocalDataSource,
        preferences: SharedPreferences
    ): CacheProvider {
        val cacheProvider = CacheProvider(
            application = application,
            layerLocalDataSource = layerLocalDataSource,
            eventLocalDataSource = eventLocalDataSource,
            preferences = preferences
        )
        CoroutineScope(Dispatchers.IO).launch {
            cacheProvider.refreshTileOverlays()
        }
        return cacheProvider
    }
}