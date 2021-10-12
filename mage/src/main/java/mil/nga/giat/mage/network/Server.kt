package mil.nga.giat.mage.network

import android.content.Context
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import mil.nga.giat.mage.sdk.R

class Server(@ApplicationContext val context: Context) {
    var baseUrl: String
        get() = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), "")!!
        set(value) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putString(context.getString(R.string.serverURLKey), value)
                    .apply()
        }
}