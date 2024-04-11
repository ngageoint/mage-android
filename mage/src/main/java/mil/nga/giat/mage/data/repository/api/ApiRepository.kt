package mil.nga.giat.mage.data.repository.api

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import mil.nga.giat.mage.R
import mil.nga.giat.mage.network.api.ApiService
import mil.nga.giat.mage.sdk.Compatibility
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

sealed class ApiResponse {
   data object Success: ApiResponse()
   data class Incompatible(val version: String): ApiResponse()
   data class Error(val statusCode: Int? = null, val message: String? = null): ApiResponse()
}

class ApiRepository @Inject constructor(
   private val application: Application,
   private val apiService: ApiService,
   private val preferences: SharedPreferences
) {
   suspend fun getApi(url: String): ApiResponse {
      return try {
         val response = apiService.getApi("$url/api")
         if (response.isSuccessful) {
            val json = response.body()?.string() ?: "{}"
            val apiJson = JSONObject(json)
            removeValues()
            populateValues(SERVER_API_PREFERENCE_PREFIX, apiJson)
            parseAuthenticationStrategies(apiJson)

            val majorVersion = preferences.getInt(application.getString(R.string.serverVersionMajorKey), 0)
            val minorVersion = preferences.getInt(application.getString(R.string.serverVersionMinorKey), 0)
            val patchVersion = preferences.getInt(application.getString(R.string.serverVersionPatchKey), 0)
            val isApiCompatible = Compatibility.isCompatibleWith(majorVersion, minorVersion)
            if (isApiCompatible) {
               ApiResponse.Success
            } else {
               ApiResponse.Incompatible("$majorVersion.$minorVersion.$patchVersion")
            }
         } else {
            ApiResponse.Error(response.code(), response.errorBody()?.string())
         }
      } catch (e: Exception) {
         Log.e(LOG_NAME, "Error fetching API", e)
         ApiResponse.Error(message = e.message)
      }
   }

   private fun populateValues(sharedPreferenceName: String, json: JSONObject) {
      val iterator = json.keys()
      while (iterator.hasNext()) {
         val key = iterator.next()
         try {
            val value = json[key]
            if (value is JSONObject) {
               populateValues(
                  sharedPreferenceName + key[0].uppercaseChar() + if (key.length > 1) key.substring(
                     1
                  ) else "", value
               )
            } else {
               val editor = preferences.edit()
               val keyString = sharedPreferenceName + key[0].uppercaseChar() + if (key.length > 1) key.substring(1) else ""
               Log.i(LOG_NAME, keyString + " is " + preferences.all[keyString] + ".  Setting it to " + value + ".")
               when (value) {
                  is Number -> {
                     when (value) {
                        is Long -> editor.putLong(keyString, value)
                        is Float -> editor.putFloat(keyString, value)
                        is Double -> editor.putFloat(keyString, value.toFloat())
                        is Int -> editor.putInt(keyString, value)
                        is Short -> editor.putInt(keyString, value.toInt())
                        else -> {
                           Log.e(LOG_NAME, "$keyString with value $value is not of valid number type. Skipping this key-value pair.")
                        }
                     }
                  }
                  is Boolean -> editor.putBoolean(keyString, value)
                  is String -> editor.putString(keyString, value)
                  is Char -> editor.putString(keyString, Character.toString(value))
                  else -> {
                     try {
                        editor.putString(keyString, value.toString())
                     } catch (e: Exception) {
                        Log.e(LOG_NAME, "$keyString with value $value is not of valid type. Skipping this key-value pair.")
                     }
                  }
               }

               editor.apply()
            }
         } catch (e: Exception) {
            Log.e(LOG_NAME, "Error parsing api json", e)
         }
      }
   }

   private fun removeValues() {
      val editor = preferences.edit()
      for (key in preferences.all.keys) {
         if (key.matches(SERVER_API_PREFERENCE_PREFIX_REGEX)) {
            editor.remove(key)
         }
      }
      editor.apply()
   }

   private fun parseAuthenticationStrategies(json: JSONObject) {
      try {
         val value = json[SERVER_API_AUTHENTICATION_STRATEGIES_KEY]
         if (value is JSONObject) {
            val editor = preferences.edit()
            editor.putString(
               application.resources.getString(R.string.authenticationStrategiesKey),
               value.toString()
            )
            editor.apply()
         }
      } catch (e: JSONException) {
         Log.e(LOG_NAME, "Error parsing server api json", e)
      }
   }

   companion object {
      private val LOG_NAME = ApiRepository::class.java.name

      private val SERVER_API_PREFERENCE_PREFIX_REGEX = Regex("^g[A-Z]\\w*")
      private const val SERVER_API_PREFERENCE_PREFIX = "g"
      private const val SERVER_API_AUTHENTICATION_STRATEGIES_KEY = "authenticationStrategies"
   }
}
