package mil.nga.giat.mage.data.repository.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mil.nga.giat.mage.data.datasource.settings.SettingsLocalDataSource
import mil.nga.giat.mage.data.datasource.settings.SettingsRemoteDataSource
import mil.nga.giat.mage.database.model.settings.MapSearchType
import mil.nga.giat.mage.database.model.settings.MapSettings
import javax.inject.Inject

class SettingsRepository @Inject constructor(
   private val localDataSource: SettingsLocalDataSource,
   private val remoteDataSource: SettingsRemoteDataSource
) {
   fun observeMapSettings(): Flow<MapSettings> = localDataSource.observeSettings().map {
      it?.mapSettings ?: defaultMapSettings
   }

   suspend fun getMapSettings(): MapSettings {
      return localDataSource.getSettings()?.mapSettings ?: defaultMapSettings
   }

   suspend fun syncSettings(refresh: Boolean = false) = withContext(Dispatchers.IO) {
      if (refresh) {
         val settings = remoteDataSource.fetchSettings()
         localDataSource.upsert(settings)
      }

      localDataSource.getSettings()
   }

   companion object {
      private val defaultMapSettings = MapSettings(searchType = MapSearchType.NATIVE)
   }
}