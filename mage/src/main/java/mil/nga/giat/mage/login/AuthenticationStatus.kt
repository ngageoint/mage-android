package mil.nga.giat.mage.login

sealed class AuthenticationStatus {
   data class Success(val username: String, val token: String): AuthenticationStatus()
   data class Offline(val message: String): AuthenticationStatus()
   data class Failure(val code: Int, val message: String): AuthenticationStatus()
   data class AccountCreated(val message: String): AuthenticationStatus()
}