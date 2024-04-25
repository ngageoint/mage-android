package mil.nga.giat.mage.data.repository.map

import mil.nga.giat.mage.ui.map.overlay.DataSourceImage

interface TileRepository {
    suspend fun getTileableItems(
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): List<DataSourceImage>
}