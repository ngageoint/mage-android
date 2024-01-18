package mil.nga.giat.mage.network.settings

import mil.nga.giat.mage.database.model.settings.MapSettings
import retrofit2.Response
import retrofit2.http.GET

interface SettingsService {

    @GET("/api/settings/map")
    suspend fun getMapSettings(): Response<MapSettings>

}