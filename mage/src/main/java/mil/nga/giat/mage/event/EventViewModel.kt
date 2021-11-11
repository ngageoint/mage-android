package mil.nga.giat.mage.event

import android.content.Context
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mil.nga.giat.mage.data.event.EventRepository
import mil.nga.giat.mage.network.Resource
import mil.nga.giat.mage.network.api.UserService
import mil.nga.giat.mage.sdk.datastore.user.Event
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import javax.inject.Inject

@HiltViewModel
class EventViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val eventRepository: EventRepository,
    private val userService: UserService
): ViewModel() {

    val userHelper: UserHelper = UserHelper.getInstance(context)

    val events: LiveData<Resource<List<Event>>> = liveData {
        val resource = eventRepository.syncEvents()
        emit(resource)
    }

    private val _syncStatus = MutableLiveData<Resource<out Event>>()
    val syncStatus: LiveData<Resource<out Event>> = _syncStatus
    fun syncEvent(event: Event): LiveData<Resource<out Event>> {
        viewModelScope.launch(Dispatchers.IO) {
            val result = eventRepository.syncEvent(event)
            _syncStatus.postValue(result)
        }

        return syncStatus
    }

    fun setEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userHelper.readCurrentUser()
            userHelper.setCurrentEvent(user, event)
            try {
                userService.addRecentEvent(user.remoteId, event.remoteId)
            } catch(ignore: Exception) {}
        }
    }
}