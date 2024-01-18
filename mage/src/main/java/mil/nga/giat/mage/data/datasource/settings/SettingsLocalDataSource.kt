package mil.nga.giat.mage.data.datasource.settings

import mil.nga.giat.mage.database.dao.settings.SettingsDao
import mil.nga.giat.mage.database.model.settings.Settings
import javax.inject.Inject

class SettingsLocalDataSource @Inject constructor(
   private val dao: SettingsDao
) {
   fun upsert(settings: Settings) = dao.upsert(settings)
   suspend fun getSettings(): Settings? = dao.getSettings()
   fun observeSettings() = dao.observeSettings()
}