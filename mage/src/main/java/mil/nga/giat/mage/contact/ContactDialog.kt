package mil.nga.giat.mage.contact

import android.R
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.text.Html
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

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

   fun show() {
      val dialog = AlertDialog.Builder(context)
         .setTitle(title)
         .setMessage(addLinks())
         .setPositiveButton(R.string.ok, null)
         .show()

      dialog.findViewById<TextView>(R.id.message)?.movementMethod = LinkMovementMethod.getInstance()
   }

   private fun addLinks(): Spanned {
      val emailUri = preferences.getString(ADMIN_EMAIL_PREFERENCE_KEY, null)?.let { email ->
         Email.Builder(email, message)
            .username(username)
            .authenticationStrategy(authenticationStrategy)
            .build()
            .uri()
      }

      val phoneUri = preferences.getString(ADMIN_PHONE_PREFERENCE_KEY, null)?.let { phone ->
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

   companion object {
      const val ADMIN_EMAIL_PREFERENCE_KEY = "gContactinfoEmail"
      const val ADMIN_PHONE_PREFERENCE_KEY = "gContactinfoPhone"
   }
}