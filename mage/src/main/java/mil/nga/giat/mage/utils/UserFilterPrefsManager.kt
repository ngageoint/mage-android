package mil.nga.giat.mage.utils

import android.content.SharedPreferences
import android.text.TextUtils
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

    private val userFilterPrefsKey = getUserFilterPrefsKey()

    fun getUserFilterPrefsKey(): String {
        var userFilterPrefsKey = ""

        val userId = userLocalDataSource.readCurrentUser()?.remoteId?: ""
        val eventId = eventLocalDataSource.currentEvent?.id.toString()

        if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(eventId)) {
           userFilterPrefsKey = "userId_${userId}_event_${eventId}_user_filter_key"
        }
        return userFilterPrefsKey
    }

    fun getUserFilterList(): List<String> {
        var userIdFilterList = emptyList<String>()

        val userIdsStr = userFilterPrefs.getString(userFilterPrefsKey, null)?:""
        if (!TextUtils.isEmpty(userIdsStr)) {
            userIdFilterList = userIdsStr.split(",")
        }

        return userIdFilterList
    }

    fun updateUserFilterSharedPrefs(selectedUserIds: Set<String>) {
        val idsString = selectedUserIds.joinToString(",")
        userFilterPrefs.edit() { putString(userFilterPrefsKey, idsString) }
    }

    fun getUserFilterPrefs(): @PreferencesModule.UserFilterPreferences SharedPreferences {
        return userFilterPrefs
    }

    fun clearCurrentFilter() {
        userFilterPrefs.edit() { putString(userFilterPrefsKey, "") }
    }

    fun clearAllUserFilters() {
        userFilterPrefs.edit() { clear() }
    }
}