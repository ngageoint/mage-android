package mil.nga.giat.mage.contact

import android.net.Uri

class Email private constructor(
   private val emailAddress: String,
   message: String,
   username: String?,
   authenticationStrategy: String?,
) {
   private val subject: String = if (message.contains("device", ignoreCase = true)) {
      if (message.contains("register")) {
         "Please approve my device"
      } else {
         "Device ID issue"
      }
   } else {
      if (message.contains("approved", true) || message.contains("activate", true)) {
         "Please activate my MAGE account"
      } else if (message.contains("disabled",true)) {
         "Please Enable My MAGE Account"
      } else if (message.contains("locked", true)) {
         "Please Unlock My MAGE Account"
      } else {
         "User Sign-in Issue"
      }
   }

   private val body: String =
       listOf(
         username?.let { "username:\n$it\n\n" } ?: "",
         authenticationStrategy?.let { "Authentication Strategy:\n$it\n\n" } ?: "",
         "Message:\n$message").joinToString("")

   fun uri(): Uri {
      return Uri.Builder()
         .scheme("mailto")
         .opaquePart(emailAddress)
         .appendQueryParameter("subject", subject)
         .appendQueryParameter("body", body)
         .build()
   }

   data class Builder(
      val emailAddress: String,
      val message: String
   ) {
      private var username: String? = null
      private var authenticationStrategy: String? = null

      fun username(username: String?) = apply { this.username = username }
      fun authenticationStrategy(authenticationStrategy: String?) = apply { this.authenticationStrategy = authenticationStrategy }

      fun build() = Email(emailAddress, message, username, authenticationStrategy)
   }
}