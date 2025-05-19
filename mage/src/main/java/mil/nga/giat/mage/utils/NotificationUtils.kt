package mil.nga.giat.mage.utils

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import mil.nga.giat.mage.R

object NotificationUtils {

    //determine if notifications should be presented to the user
    fun canSendNotifications(preferences: SharedPreferences, context: Context): Boolean {
        val areNotificationsEnabledInMage = preferences.getBoolean(context.getString(R.string.notificationsEnabledKey), context.resources.getBoolean(R.bool.notificationsEnabledDefaultValue))
        val isNotificationsPermissionApproved = isNotificationsPermissionGranted(context)

        return areNotificationsEnabledInMage && isNotificationsPermissionApproved
    }

    //for Android 13 above, check if POST_NOTIFICATIONS permission has been granted. For below Android 13, check that user has not disabled notifications
    fun isNotificationsPermissionGranted(context: Context): Boolean {
        val isNotificationsPermissionApproved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.areNotificationsEnabled()
        }

        return isNotificationsPermissionApproved
    }
}