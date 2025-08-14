package mil.nga.giat.mage.contact

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.text.Html
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import mil.nga.giat.mage.R

class ContactDialog(
   private val context: Context,
   private val preferences: SharedPreferences,
   private val title: String,
   private val message: String
) {
   var username: String? = null
   private var authenticationStrategy: String? = null

   fun setAuthenticationStrategy(authenticationStrategy: String?) {
      this.authenticationStrategy = authenticationStrategy
   }

   fun show(
      workOffline: ((Boolean) -> Unit)? = null
   ) {
      val builder = AlertDialog.Builder(context)
         .setTitle(title)
         .setMessage(addLinks())
         .setPositiveButton(android.R.string.ok) { _, _ ->
            workOffline?.invoke(false)
         }

      workOffline?.let { callback ->
         builder.setNegativeButton("Work Offline") { _, _ ->
            callback(true)
         }
      }

      val dialog = builder.show()
      dialog.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethod.getInstance()
   }

   private fun addLinks(): Spanned {
      val emailUri = preferences.getString(context.getString(R.string.contactInfoEmailKey), context.getString(R.string.contactInfoEmailDefaultValue))?.let { email ->
         Email.Builder(email, message)
            .username(username)
            .authenticationStrategy(authenticationStrategy)
            .build()
            .uri()
      }

      val phoneUri = preferences.getString(context.getString(R.string.contactInfoPhoneKey), context.getString(R.string.contactInfoPhoneDefaultValue))?.let { phone ->
         Uri.fromParts("tel", phone, null)
      }

      val html = if (emailUri != null || phoneUri != null) {
         val email = emailUri?.let { "<a href=$it>Email</a>" } ?: ""
         val phone = phoneUri?.let { "<a href=$it>Phone</a>" } ?: ""

         "$message <br/><br/>" +
           "You may contact your MAGE administrator via " +
           "${arrayOf(email, phone).joinToString(" or ")} " +
           "for further assistance."
      } else message

      return Html.fromHtml(html)
   }
}