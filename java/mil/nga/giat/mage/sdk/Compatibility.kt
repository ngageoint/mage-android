package mil.nga.giat.mage.sdk

import android.content.Context
import androidx.preference.PreferenceManager

class Compatibility {

   data class Server(val major: Int, val minor: Int)

   companion object {
      val servers = listOf(
         Server(5, 4),
         Server(6, 0)
      )

      fun isCompatibleWith(major: Int, minor: Int): Boolean {
         for (server: Server in servers) {
            if (server.major == major && server.minor <= minor) {
               return true
            }
         }

         return false;
      }

      fun isServerVersion5(applicationContext: Context): Boolean {
         val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
         val majorVersion: Int = sharedPreferences.getInt(applicationContext.getString(R.string.serverVersionMajorKey), 0)
         return majorVersion == 5
      }
   }
}