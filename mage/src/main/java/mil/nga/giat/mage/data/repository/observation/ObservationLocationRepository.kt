package mil.nga.giat.mage.data.repository.observation

import android.util.Log
import mil.nga.giat.mage.data.datasource.observation.ObservationLocationLocalDataSource
import mil.nga.giat.mage.data.datasource.observation.ObservationMapItem
import javax.inject.Inject

class ObservationLocationRepository @Inject constructor(
    private val observationRepository: ObservationRepository,
    private val observationLocationLocalDataSource: ObservationLocationLocalDataSource
) {

    fun observeObservationLocation(observationLocationId: Long) = observationLocationLocalDataSource.observeObservationLocation(observationLocationId)

    fun getMapItems(
        eventRemoteId: String,
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): List<ObservationMapItem> {
        // get the observation ids of the matching locations
        val observationLocations = observationLocationLocalDataSource.observationLocations(
            eventId = eventRemoteId,
            minLatitude = minLatitude,
            maxLatitude = maxLatitude,
            minLongitude = minLongitude,
            maxLongitude = maxLongitude
        )

        val observationIds = observationLocations.map {
            it.observationId
        }
        // use these observation ids to get the observations that match the current
        // observation predicates
        // We cannot do a join query yet until observations are moved to Room
        val observationMatchedIds = observationRepository.query(observationIds).map {
            it.id
        }

        val filtered = observationLocations.filter {
            it.observationId in observationMatchedIds
        }.map {
            ObservationMapItem(it)
        }
        Log.d("Whatever", "Filtered observation count ${filtered.size}")
        return filtered
    }
}