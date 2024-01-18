package mil.nga.giat.mage.data.datasource.settings

import mil.nga.giat.mage.database.model.settings.Settings
import mil.nga.giat.mage.network.settings.SettingsService
import javax.inject.Inject

class SettingsRemoteDataSource @Inject constructor(
   private val service: SettingsService
) {
   suspend fun fetchSettings(): Settings {
      val mapSettings = try {
         val response = service.getMapSettings()
         if (response.isSuccessful) {
            response.body()
         } else null
      } catch (e: Exception) { null }


      return Settings(mapSettings = mapSettings)
   }
}