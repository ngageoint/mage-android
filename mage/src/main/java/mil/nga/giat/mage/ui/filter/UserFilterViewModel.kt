package mil.nga.giat.mage.ui.filter

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.data.repository.user.UserRepository
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.drop
import mil.nga.giat.mage.utils.UserFilterPrefsManager
import mil.nga.giat.mage.utils.UserFilterMapper
import mil.nga.giat.mage.utils.UserInfo
import mil.nga.giat.mage.utils.sort
import javax.inject.Inject


@HiltViewModel
class UserFilterViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userFilterMapper: UserFilterMapper,
    private val userLocalDataSource: UserLocalDataSource,
    private val eventLocalDataSource: EventLocalDataSource,
    private var userFilterPrefsManager: UserFilterPrefsManager
): ViewModel() {

    private val _usersForEvent = MutableStateFlow<List<UserInfo>>(emptyList())
    val usersForEvent: StateFlow<List<UserInfo>> = _usersForEvent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedUserIdsForFilter = MutableStateFlow<Set<String>>(emptySet())
    val selectedUserIdsForFilter: StateFlow<Set<String>> = _selectedUserIdsForFilter.asStateFlow()

    val currentEvent = eventLocalDataSource.currentEvent

    init {
        retrieveUsers()

        //observe changes to _selectedUserIdsForFilter and save them to shared prefs
        viewModelScope.launch {
            //this will collect every emission from _selectedUserIdsForFilter, excluding the initial value
           _selectedUserIdsForFilter.drop(1).collect { selectedIds ->
               saveSelectedFilters(selectedIds)
           }
        }
    }

    private fun retrieveUsers() {
        viewModelScope.launch {
            //show loading indicator
            _isLoading.value = true

            val eventId = currentEvent?.id

            //attempt to retrieve the event's assigned users from the /api/events/{eventId}/users API
            //if the API returns no users or an error, then fall back to retrieving users from the observations table
            if (eventId != null) {
                val userSet = userRepository.getUserSetForEvent(eventId)

                if (userSet.isNotEmpty()) {
                    val userInfoList = userSet.toList().sort()
                    _usersForEvent.value = userInfoList
                } else {
                    val event = eventLocalDataSource.read(eventId)
                    val userList = userLocalDataSource.getUsersInEvent(event).toList()
                    if (userList.isNotEmpty()) {
                        //transform the User list to a UserInfo list
                        val userInfoList: List<UserInfo> = userFilterMapper.toUserInfoList(userList)
                        _usersForEvent.value = userInfoList
                    }
                }

                //retrieve stored filter if previously set
                val userIdsToFilter = userFilterPrefsManager.getUserFilterList()
                if (userIdsToFilter.isNotEmpty()) {
                    loadPreviouslyStoredUserIdFilter(userIdsToFilter)
                }
            }

            _isLoading.value = false
        }
    }

    private fun loadPreviouslyStoredUserIdFilter(userIdsForFilter: List<String>) {
        if (userIdsForFilter.isNotEmpty()) {
            _selectedUserIdsForFilter.value = userIdsForFilter.toSet()
        } else {
            _selectedUserIdsForFilter.value = emptySet()
        }
    }

    private fun saveSelectedFilters(selectedIds: Set<String>) {
        if (selectedIds.isEmpty()) {
            userFilterPrefsManager.clearCurrentFilter()
        } else {
            userFilterPrefsManager.updateUserFilterSharedPrefs(selectedIds)
        }
    }

    fun clearSelectedUserFilters() {
        _selectedUserIdsForFilter.value = emptySet()
        userFilterPrefsManager.clearCurrentFilter()
    }

    fun toggleUserIdSelection(userInfo: UserInfo) {
        val userIdToToggle = userInfo.id

        _selectedUserIdsForFilter.value = _selectedUserIdsForFilter.value.let { currentSelectedIds ->
            if (currentSelectedIds.contains(userIdToToggle)) {
                currentSelectedIds - userIdToToggle
            } else {
                currentSelectedIds + userIdToToggle
            }
        }
    }

    //whenever the list of users or search query changes, combine block is evaluated and filtered users are updated
    val usersMatchingSearch: StateFlow<List<UserInfo>> =
        combine(_usersForEvent, _searchQuery) { users, query ->
            if (query.isBlank()) {
                users
            } else {
                users.filter {
                    it.displayName.contains(query, ignoreCase = true) || it.userName.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

}