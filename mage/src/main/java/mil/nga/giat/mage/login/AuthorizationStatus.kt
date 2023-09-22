package mil.nga.giat.mage.login

import mil.nga.giat.mage.database.model.user.User

sealed class AuthorizationStatus {
	data class Success(
      val user: User,
      val token: String,
      val tokenExpiration: String
   ): AuthorizationStatus()
   object FailInvalidServer : AuthorizationStatus()
   data class FailAuthorization(val user: User? = null) : AuthorizationStatus()
   data class FailAuthentication(val user: User? = null, val message: String) : AuthorizationStatus()
}