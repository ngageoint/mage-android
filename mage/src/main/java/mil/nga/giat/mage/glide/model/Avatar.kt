package mil.nga.giat.mage.glide.model

import mil.nga.giat.mage.sdk.datastore.user.User

data class Avatar(val remoteUri: String?, val localUri: String?, val lastModified: Long) {

    companion object {
        fun forUser(user: User): Avatar {
            return Avatar(user.avatarUrl, user.userLocal?.localAvatarPath, user.lastModified.time)
        }
    }
}
