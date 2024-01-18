package mil.nga.giat.mage.di

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import mil.nga.giat.mage.R
import mil.nga.giat.mage.sdk.utils.ISO8601DateFormatFactory
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

data class Token(
   val username: String,
   val token: String,
   val expirationDate: Date
) {
   fun isExpired(): Boolean {
      return Date().after(expirationDate)
   }
}

sealed class TokenStatus {
   data class Active(val token: Token): TokenStatus()
   data object Expired: TokenStatus()
   data object Logout: TokenStatus()
}

@Singleton
class TokenProvider @Inject constructor (
   private val application: Application,
   private val preferences: SharedPreferences
): LiveData<TokenStatus>() {

   init {
      val username = preferences.getString(application.getString(R.string.sessionUserKey), "")!!
      val tokenValue = preferences.getString(application.getString(R.string.tokenKey), "")!!
      val expiration = preferences.getString(application.getString(R.string.tokenExpirationDateKey), "")!!
      val expirationDate =
         try {
            ISO8601DateFormatFactory.ISO8601().parse(expiration)
         } catch (_: Exception) {
            Date()
         }

      val token = Token(
         token = tokenValue,
         expirationDate = expirationDate,
         username = username
      )

      value = TokenStatus.Active(token)
   }

   fun updateToken(
      username: String,
      authenticationStrategy: String,
      token: String,
      expiration: String
   ) {
      val expirationDate = ISO8601DateFormatFactory.ISO8601().parse(expiration)!!

      preferences.edit()
         .putString(application.getString(R.string.sessionUserKey), username)
         .putString(application.getString(R.string.sessionStrategyKey), authenticationStrategy)
         .putString(application.getString(R.string.tokenKey), token)
         .putString(application.getString(R.string.tokenExpirationDateKey), expiration)
         .putLong(application.getString(R.string.tokenExpirationLengthKey), expirationDate.time - Date().time)
         .apply()

      val status = TokenStatus.Active(
         Token(
            username = username,
            token = token,
            expirationDate = expirationDate
         )
      )

      postValue(status)
   }

   fun signout() {
      removeToken()
      postValue(TokenStatus.Logout)
   }

   fun expireToken() {
      removeToken()
      postValue(TokenStatus.Expired)
   }

   private fun removeToken() {
      preferences.edit()
         .remove(application.getString(R.string.tokenKey))
         .remove(application.getString(R.string.tokenExpirationDateKey))
         .apply()
   }

   fun isExpired(): Boolean {
      return value?.let { tokenStatus ->
         when (tokenStatus) {
            is TokenStatus.Active -> tokenStatus.token.isExpired()
            else -> true
         }
      } ?: true
   }
}