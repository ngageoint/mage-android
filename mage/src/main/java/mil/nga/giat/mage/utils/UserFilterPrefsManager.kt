package mil.nga.giat.mage.utils

import android.content.SharedPreferences
import mil.nga.giat.mage.di.PreferencesModule
import androidx.core.content.edit
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import javax.inject.Inject

const val USER_FILTER_PREFS_FILE = "user_filter_prefs_file"

class UserFilterPrefsManager @Inject constructor(
    private val userLocalDataSource: UserLocalDataSource,
    private val eventLocalDataSource: EventLocalDataSource,
    @PreferencesModule.UserFilterPreferences private val userFilterPrefs: SharedPreferences
){

    private val userFilterPrefsSelectedIdsKey = getUserFilterPrefsSelectedIdsKey()
    private val userFilterPrefsSelectedNamesKey = getUserFilterPrefsSelectedNamesKey()

    fun getUserFilterPrefsSelectedIdsKey(): String {
        var userFilterIdsPrefsKey = ""

        val userId = userLocalDataSource.readCurrentUser()?.remoteId
        val eventId = eventLocalDataSource.currentEvent?.id.toString()

        if (!userId.isNullOrBlank() && !eventId.isNullOrBlank()) {
            userFilterIdsPrefsKey = "userId_${userId}_event_${eventId}_filtered_ids_key"
        }

        return userFilterIdsPrefsKey
    }

    private fun getUserFilterPrefsSelectedNamesKey(): String {
        var userFilterNamesPrefsKey = ""

        val userId = userLocalDataSource.readCurrentUser()?.remoteId
        val eventId = eventLocalDataSource.currentEvent?.id.toString()

        if (!userId.isNullOrBlank() && !eventId.isNullOrBlank()) {
            userFilterNamesPrefsKey = "userId_${userId}_event_${eventId}_filtered_names_key"
        }

        return userFilterNamesPrefsKey
    }

    fun getUserFilterList(): List<String> {
        var userIdFilterList = emptyList<String>()
        val userIdsStr = userFilterPrefs.getString(userFilterPrefsSelectedIdsKey, null)

        if (!userIdsStr.isNullOrBlank()) {
            userIdFilterList = userIdsStr.split(",")
        }

        return userIdFilterList
    }

    fun getUserFilterDisplayNames(): String {
        return userFilterPrefs.getString(userFilterPrefsSelectedNamesKey, "")?:""
    }

    fun updateUserFilterSharedPrefs(selectedUsers: Set<UserInfo>) {
        val idsString = selectedUsers.joinToString(",") { it.id }
        val namesString = selectedUsers.joinToString(", ") { it.displayName }

        userFilterPrefs.edit {
            putString(userFilterPrefsSelectedIdsKey, idsString)
            putString(userFilterPrefsSelectedNamesKey, namesString)
        }
    }

    fun getUserFilterPrefs(): @PreferencesModule.UserFilterPreferences SharedPreferences {
        return userFilterPrefs
    }

    fun clearCurrentFilter() {
        userFilterPrefs.edit() {
            remove(userFilterPrefsSelectedIdsKey)
            remove(userFilterPrefsSelectedNamesKey)
        }
    }

    fun clearAllUserFilters() {
        userFilterPrefs.edit() { clear() }
    }
}