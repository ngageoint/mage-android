package mil.nga.giat.mage.event

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mil.nga.giat.mage.dagger.module.ApplicationContext
import mil.nga.giat.mage.data.event.EventRepository
import mil.nga.giat.mage.network.Resource
import mil.nga.giat.mage.sdk.datastore.user.Event
import javax.inject.Inject

class EventViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val eventRepository: EventRepository
): ViewModel() {

    private val _syncStatus = MutableLiveData<Resource<out Event>>()
    val syncStatus: LiveData<Resource<out Event>> = _syncStatus
    fun syncEvent(event: Event): LiveData<Resource<out Event>> {
        viewModelScope.launch {
            var result = eventRepository.syncEvent(event)
            _syncStatus.value = result
        }

        return syncStatus
    }
}