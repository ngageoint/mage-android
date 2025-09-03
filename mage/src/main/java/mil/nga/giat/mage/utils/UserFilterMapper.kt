package mil.nga.giat.mage.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import mil.nga.giat.mage.R
import mil.nga.giat.mage.database.model.user.User
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.lowercase

data class UserInfo(
    val id: String,
    val displayName: String,
    val userName:String,
    val avatarIcon: String):
    Comparable<UserInfo> {
    override fun compareTo(other: UserInfo): Int {
        val idComparison = this.id.compareTo(other.id)
        return idComparison
    }
}

@Singleton
class UserFilterMapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPrefs: SharedPreferences
) {
    fun toUserInfoList(users: List<User>): List<UserInfo> {
        return users.mapNotNull { toUserInfo(it) }
            .sortedBy { it.displayName.lowercase() }
    }

    //transform User object to UserInfo object
    private fun toUserInfo(user: User): UserInfo? {
        return user.remoteId?.let { userId ->
            val rawAvatarUrl = user.avatarUrl ?: ""

            UserInfo(
                id = userId,
                displayName = user.displayName ?: "",
                userName = user.username ?: "",
                avatarIcon = if (rawAvatarUrl.startsWith("http", ignoreCase = true)) {
                    rawAvatarUrl
                } else if (rawAvatarUrl.isNotBlank()) {
                    prependServerUrlToRemoteAvatarPath(rawAvatarUrl, context, sharedPrefs)
                } else {
                    ""
                }
            )
        }
    }

    private fun prependServerUrlToRemoteAvatarPath(remoteAvatarUrl: String, context: Context, sharedPrefs: SharedPreferences): String {
        var fullAvatarUrl: HttpUrl? = null

        sharedPrefs.getString(context.getString(R.string.serverURLKey), null)?.let {
            fullAvatarUrl = (it + remoteAvatarUrl).toHttpUrlOrNull()
        }

        return fullAvatarUrl?.toString() ?: ""
    }
}
