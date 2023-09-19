package mil.nga.giat.mage.event

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mil.nga.giat.mage.data.repository.event.EventRepository
import mil.nga.giat.mage.network.Resource
import mil.nga.giat.mage.network.user.UserService
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.data.repository.layer.LayerRepository
import javax.inject.Inject

@HiltViewModel
class EventViewModel @Inject constructor(
   private val eventRepository: EventRepository,
   private val userService: UserService,
   private val userLocalDataSource: UserLocalDataSource,
   private val layerRepository: LayerRepository
): ViewModel() {

   val events: LiveData<List<Event>> = liveData {
      val events = eventRepository.getEvents(forceUpdate = true)
      emit(events)
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
         userLocalDataSource.readCurrentUser()?.let { user ->
            userLocalDataSource.setCurrentEvent(user, event)
            layerRepository.fetchFeatureLayers(event,false)
            layerRepository.fetchImageryLayers()
            try {
               userService.addRecentEvent(user.remoteId, event.remoteId)
            } catch(ignore: Exception) {}
         }
      }
   }
}