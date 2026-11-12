package mil.nga.giat.mage.data.datasource.authentication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import org.json.JSONObject

enum class AuthenticationStrategyType(val type: String) {
   LOCAL("local"),
   LDAP("ldap"),
   OAUTH("oauth"),
   OPEN_ID_CONNECT("openidconnect"),
   SAML("saml");

   companion object {
      fun fromType(type: String) = AuthenticationStrategyType.entries.find { it.type.equals(type, ignoreCase = true) }
   }
}

class AuthenticationStrategy(
   val type: AuthenticationStrategyType,
   val title: String,
   val buttonText: String? = null,
   val icon: Bitmap? = null,
   val contentColor: Color? = null,
   val containerColor: Color? = null,
) {
   companion object {
      fun parse(json: JSONObject): AuthenticationStrategy? {
         val type = json.optString("type")
            .let(AuthenticationStrategyType::fromType)
            ?: return null

         return AuthenticationStrategy(
            type = type,
            title = json.stringOrNull("title") ?: "",
            buttonText = json.stringOrNull("buttonText"),
            icon = decodeBase64Bitmap(json.stringOrNull("icon")),
            contentColor = parseColor(json.stringOrNull("textColor")),
            containerColor = parseColor(json.stringOrNull("buttonColor"))
         )
      }

      // optString() returns the text "null" for a JSON null, so treat null and blank as missing.
      private fun JSONObject.stringOrNull(key: String): String? =
         if (isNull(key)) null else optString(key).trim().ifEmpty { null }

      private fun parseColor(colorString: String?): Color? = runCatching {
         colorString
            ?.take(7)
            ?.toColorInt()
            ?.let { Color(it or 0xFF000000.toInt()) }
      }.getOrNull()

      private fun decodeBase64Bitmap(base64String: String?): Bitmap? {
         if (base64String.isNullOrEmpty()) return null
         return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
         } catch (_: Exception) { null }
      }
   }
}
