package mil.nga.giat.mage.data.repository.observation

import com.j256.ormlite.stmt.QueryBuilder
import mil.nga.giat.mage.data.datasource.observation.ObservationLocalDataSource
import mil.nga.giat.mage.data.repository.map.TileRepository
import mil.nga.giat.mage.ui.map.overlay.DataSourceImage
import javax.inject.Inject

class ObservationTileRepository @Inject constructor(
    private val reference: String,
    private val observationLocalDataSource: ObservationLocalDataSource
): TileRepository {
    override suspend fun getTileableItems(
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): List<DataSourceImage> {
        return emptyList()
//        return localDataSource.getAsam(reference)?.let { asam ->
//            if (asam.latitude in minLatitude..maxLatitude && asam.longitude in minLongitude..maxLongitude) {
//                listOf(AsamImage(asam))
//            } else emptyList()
//        } ?: emptyList()
    }
}

class ObservationsTileRepository @Inject constructor(
    private val observationLocalDataSource: ObservationLocalDataSource
): TileRepository {
    override suspend fun getTileableItems(
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): List<DataSourceImage> {
    return emptyList()
//        val boundsFilters = MapBoundsFilter.filtersForBounds(
//            minLongitude = minLongitude,
//            maxLongitude = maxLongitude,
//            minLatitude = minLatitude,
//            maxLatitude = maxLatitude
//        )
//
//        val entry = filterRepository.filters.first()
//        val asamFilters = entry[DataSource.ASAM] ?: emptyList()
//        val filters = boundsFilters.toMutableList().apply { addAll(asamFilters) }
//
//        val query = QueryBuilder("asams", filters).buildQuery()
//        return localDataSource.getAsams(query).map {
//            AsamImage(it)
//        }
    }
}