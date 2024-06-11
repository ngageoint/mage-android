package mil.nga.giat.mage.ui.location

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import mil.nga.giat.mage.data.repository.location.LocationRepository
import mil.nga.giat.mage.database.model.location.Location
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.glide.model.Avatar
import mil.nga.giat.mage.map.UserMapState
import mil.nga.giat.mage.utils.DateFormatFactory
import java.text.DateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val application: Application,
    private val repository: LocationRepository
): ViewModel() {

    // TODO use the right formatter here based on preferences
    var dateFormat: DateFormat =
        DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault(), application)

    private val referenceFlow = MutableSharedFlow<Long>(replay = 1)
    fun setLocationId(id: Long) {
        Log.d("LocationViewModel", "setLocationId: $id")
        viewModelScope.launch {
            referenceFlow.emit(id)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val location = referenceFlow.flatMapLatest { id ->
        Log.d("LocationViewModel", "reference flow location: $id")
        repository.observeLocation(id).transform {
            Log.d("LocationViewModel", "map location: $it")
            it?.let { emit(toUserState(it.user, it)) }
        }
    }.asLiveData()

    fun toUserState(user: User, location: Location): UserMapState {
        return UserMapState(
            id = user.id,
            title = dateFormat.format(location.timestamp),
            primary = user.displayName,
            geometry = location.geometry,
            image = Avatar.forUser(user),
            email = user.email,
            phone = user.primaryPhone
        )
    }
}