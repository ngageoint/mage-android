package mil.nga.giat.mage.network

import android.content.Context
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import mil.nga.giat.mage.R

class Server(
   @ApplicationContext val context: Context
) {
    var baseUrl: String
        get() {
           val url = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), "")!!
           return url.ifEmpty { "http://localhost" }
        }
        set(value) {
           PreferenceManager.getDefaultSharedPreferences(context)
              .edit()
              .putString(context.getString(R.string.serverURLKey), value)
              .apply()
        }
}