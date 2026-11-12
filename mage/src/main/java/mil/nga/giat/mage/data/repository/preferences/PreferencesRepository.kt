package mil.nga.giat.mage.data.repository.preferences

import android.app.Application
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.datasource.authentication.AuthenticationStrategy
import org.json.JSONObject
import javax.inject.Inject

class PreferencesRepository @Inject constructor(
   val application: Application,
   val sharedPreferences: SharedPreferences
) {
   // The listener runs on the main thread, so it only signals a change; parsing happens
   // downstream on the collector's dispatcher.
   val authenticationStrategies: Flow<List<AuthenticationStrategy>> = callbackFlow {
      val strategiesKey = application.resources.getString(R.string.authenticationStrategiesKey)
      val listener = OnSharedPreferenceChangeListener { _, key ->
         if (key == strategiesKey) {
            trySend(Unit)
         }
      }

      sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

      trySend(Unit)

      awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
   }.conflate().map { getAuthenticationStrategies(sharedPreferences) }

   private fun getAuthenticationStrategies(preferences: SharedPreferences): List<AuthenticationStrategy> {
      val authenticationStrategies = preferences.getString(
         application.resources.getString(R.string.authenticationStrategiesKey),
         null
      )
      if (authenticationStrategies != null) {
         try {
            val json = JSONObject(authenticationStrategies)
            val strategies = json.keys().asSequence()
                  .mapNotNull { json.optJSONObject(it) }
                  .mapNotNull(AuthenticationStrategy::parse)
                  .toList()

            return strategies
         } catch (e: Exception) {
            Log.e(LOG_NAME, "Error parsing authentication strategies", e)
            return emptyList()
         }
      }

      return emptyList()
   }

   companion object {
      private val LOG_NAME = PreferencesRepository::class.java.simpleName
   }
}