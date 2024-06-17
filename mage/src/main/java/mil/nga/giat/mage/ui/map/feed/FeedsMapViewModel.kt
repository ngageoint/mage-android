package mil.nga.giat.mage.ui.map.feed

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import mil.nga.giat.mage.data.repository.event.EventRepository
import mil.nga.giat.mage.map.preference.MapLayerPreferences
import javax.inject.Inject

@HiltViewModel
class FeedsMapViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val mapLayerPreferences: MapLayerPreferences,
    val preferences: SharedPreferences
): ViewModel() {

    private val event = flow {
        val event = eventRepository.getCurrentEvent()
        if (event != null) {
            mapLayerPreferences.getEnabledFeeds(event.id).toList()
        }
        emit(event)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val feedIds = event.flatMapLatest { event ->
        mapLayerPreferences.getEnabledFeeds(event?.id)
        mapLayerPreferences.feedIds
    }
}